/*
 * Copyright 2022 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.fixes.SuggestedFixes.qualifyType;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.methodIsConstructor;
import static com.google.errorprone.matchers.Matchers.methodIsNamed;
import static com.google.errorprone.matchers.Matchers.not;
import static com.google.errorprone.util.ASTHelpers.findPathFromEnclosingNodeToTopLevel;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.streamSuperMethods;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import java.util.Collections;

/**
 * Warns on classes or methods being named similarly to contextual keywords, or invoking such
 * methods.
 */
@BugPattern(
    severity = WARNING,
    summary =
        "Avoid naming of classes and methods that is similar to contextual keywords.  When invoking"
            + " such a method, qualify it.")
public final class NamedLikeContextualKeyword extends BugChecker
    implements ClassTreeMatcher, MethodTreeMatcher, MethodInvocationTreeMatcher {

  // Refer to JLS 19 §3.9.
  // Note that "non-sealed" is not a valid class name
  private static final ImmutableSet<String> DISALLOWED_CLASS_NAMES =
      ImmutableSet.of(
          "exports",
          "opens",
          "requires",
          "uses",
          "module",
          "permits",
          "sealed",
          "var",
          "provides",
          "to",
          "with",
          "open",
          "record",
          "transitive",
          "yield");
  private static final Matcher<MethodTree> DISALLOWED_METHOD_NAME_MATCHER =
      allOf(not(methodIsConstructor()), methodIsNamed("yield"));
  private static final ImmutableSet<String> AUTO_PROCESSORS =
      ImmutableSet.of(
          "com.google.auto.value.processor.AutoValueProcessor",
          "com.google.auto.value.processor.AutoOneOfProcessor");

  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    MethodSymbol methodSymbol = ASTHelpers.getSymbol(tree);

    // Don't alert if an @Auto... class (safe since reference always qualified).
    if (isInGeneratedAutoCode(state)) {
      return NO_MATCH;
    }

    // Don't alert if method is an override (this includes interfaces)
    if (!streamSuperMethods(methodSymbol, state.getTypes()).findAny().isPresent()
        && DISALLOWED_METHOD_NAME_MATCHER.matches(tree, state)) {
      return describeMatch(tree);
    }

    return NO_MATCH;
  }

  @Override
  public Description matchClass(ClassTree tree, VisitorState state) {
    if (DISALLOWED_CLASS_NAMES.contains(tree.getSimpleName().toString())) {
      return describeMatch(tree);
    }

    return NO_MATCH;
  }

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    ExpressionTree select = tree.getMethodSelect();
    if (!(select instanceof IdentifierTree)) {
      return NO_MATCH;
    }
    if (!((IdentifierTree) select).getName().contentEquals("yield")) {
      return NO_MATCH;
    }
    SuggestedFix.Builder fix = SuggestedFix.builder();
    String qualifier = getQualifier(state, getSymbol(tree), fix);
    return describeMatch(tree, fix.prefixWith(select, qualifier + ".").build());
  }

  private static String getQualifier(
      VisitorState state, MethodSymbol sym, SuggestedFix.Builder fix) {
    if (sym.isStatic()) {
      return qualifyType(state, fix, sym.owner.enclClass());
    }
    TreePath path = findPathFromEnclosingNodeToTopLevel(state.getPath(), ClassTree.class);
    if (sym.isMemberOf(getSymbol((ClassTree) path.getLeaf()), state.getTypes())) {
      return "this";
    }
    while (true) {
      path = findPathFromEnclosingNodeToTopLevel(path, ClassTree.class);
      ClassSymbol enclosingClass = getSymbol((ClassTree) path.getLeaf());
      if (sym.isMemberOf(enclosingClass, state.getTypes())) {
        return qualifyType(state, fix, enclosingClass) + ".this";
      }
    }
  }

  private static boolean isInGeneratedAutoCode(VisitorState state) {
    return !Collections.disjoint(ASTHelpers.getGeneratedBy(state), AUTO_PROCESSORS);
  }
}
