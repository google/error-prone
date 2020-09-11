/*
 * Copyright 2017 The Error Prone Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.errorprone.bugpatterns.android;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.hasModifier;
import static com.google.errorprone.matchers.Matchers.isSameType;
import static com.google.errorprone.matchers.Matchers.methodHasParameters;
import static com.google.errorprone.matchers.Matchers.methodIsNamed;
import static com.google.errorprone.matchers.Matchers.not;
import static com.google.errorprone.util.ASTHelpers.constValue;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.isSubtype;
import static com.google.errorprone.util.ASTHelpers.resolveExistingMethod;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.util.FatalError;
import javax.lang.model.element.Modifier;

/** @author epmjohnston@google.com (Emily P.M. Johnston) */
@BugPattern(
    name = "FragmentInjection",
    summary =
        "Classes extending PreferenceActivity must implement isValidFragment such that it does not"
            + " unconditionally return true to prevent vulnerability to fragment injection"
            + " attacks.",
    severity = WARNING,
    tags = StandardTags.LIKELY_ERROR)
public class FragmentInjection extends BugChecker implements ClassTreeMatcher {

  private static final Matcher<MethodTree> OVERRIDES_IS_VALID_FRAGMENT =
      allOf(methodIsNamed("isValidFragment"), methodHasParameters(isSameType("java.lang.String")));

  @Override
  public Description matchClass(ClassTree tree, VisitorState state) {
    // Only examine classes that extend PreferenceActivity.
    Type preferenceActivityType = state.getTypeFromString("android.preference.PreferenceActivity");
    if (!isSubtype(getType(tree), preferenceActivityType, state)) {
      return NO_MATCH;
    }

    // Examine each method in the class. Complain if isValidFragment not implemented.
    TypeSymbol preferenceActivityTypeSymbol = preferenceActivityType.tsym;
    boolean methodNotImplemented = true;
    try {
      MethodSymbol isValidFragmentMethodSymbol =
          resolveExistingMethod(
              state,
              getSymbol(tree),
              state.getName("isValidFragment"),
              ImmutableList.of(state.getSymtab().stringType),
              ImmutableList.<Type>of());
      methodNotImplemented = isValidFragmentMethodSymbol.owner.equals(preferenceActivityTypeSymbol);
    } catch (FatalError e) {
      // If isValidFragment method symbol is not found, then we must be compiling against an old SDK
      // version (< 19) in which isValidFragment is not yet implemented, and neither this class nor
      // any of its super classes have implemented it.
    }
    // If neither this class nor any super class besides PreferenceActivity implements
    // isValidFragment, and this is not an abstract class, emit warning.
    if (methodNotImplemented && not(hasModifier(Modifier.ABSTRACT)).matches(tree, state)) {
      return buildDescription(tree)
          .setMessage("Class extending PreferenceActivity does not implement isValidFragment.")
          .build();
    }

    // Check the implementation of isValidFragment. Complain if it always returns true.
    MethodTree isValidFragmentMethodTree = getMethod(OVERRIDES_IS_VALID_FRAGMENT, tree, state);
    if (isValidFragmentMethodTree != null) {
      if (isValidFragmentMethodTree.accept(ALWAYS_RETURNS_TRUE, null)) {
        return buildDescription(isValidFragmentMethodTree)
            .setMessage("isValidFragment unconditionally returns true.")
            .build();
      }
    }
    return NO_MATCH;
  }

  /*
   * Return the first method tree on the given class tree that matches the given method matcher,
   * or null if one does not exist.
   */
  private static MethodTree getMethod(
      Matcher<MethodTree> methodMatcher, ClassTree classTree, VisitorState state) {
    for (Tree member : classTree.getMembers()) {
      if (member instanceof MethodTree) {
        MethodTree memberTree = (MethodTree) member;
        if (methodMatcher.matches(memberTree, state)) {
          return memberTree;
        }
      }
    }
    return null;
  }

  /*
   * A tree scanner for isValidFragment, accepts methods that return true on all code paths.
   */
  private static final TreeScanner<Boolean, Void> ALWAYS_RETURNS_TRUE =
      new TreeScanner<Boolean, Void>() {

        @Override
        public Boolean visitReturn(ReturnTree node, Void unused) {
          ExpressionTree returnExpression = node.getExpression();
          Boolean returnValue = constValue(returnExpression, Boolean.class);
          return firstNonNull(returnValue, false);
        }

        @Override
        public Boolean reduce(Boolean r1, Boolean r2) {
          // And together the results of all visits. If any visit was false (return value was false
          // or not constant), the method implementation is okay.
          // null value means not a return statement, doesn't change anything, so treat as true.
          return (r1 == null || r1) && (r2 == null || r2);
        }
      };
}
