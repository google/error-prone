/*
 * Copyright 2014 Google Inc. All Rights Reserved.
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

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.isSameType;
import static com.google.errorprone.matchers.Matchers.methodHasParameters;
import static com.google.errorprone.matchers.Matchers.methodHasVisibility;
import static com.google.errorprone.matchers.Matchers.methodIsNamed;
import static com.google.errorprone.matchers.Matchers.methodReturns;
import static com.google.errorprone.matchers.Matchers.variableType;
import static com.google.errorprone.matchers.MethodVisibility.Visibility.PUBLIC;
import static com.google.errorprone.suppliers.Suppliers.BOOLEAN_TYPE;
import static com.google.errorprone.suppliers.Suppliers.OBJECT_TYPE;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.code.Type;
import javax.lang.model.element.ElementKind;

/**
 * Classes that override equals should also override hashCode.
 *
 * @author cushon@google.com (Liam Miller-Cushon)
 */
@BugPattern(
  name = "EqualsHashCode",
  summary = "Classes that override equals should also override hashCode.",
  category = JDK,
  severity = WARNING,
  tags = StandardTags.FRAGILE_CODE
)
public class EqualsHashCode extends BugChecker implements ClassTreeMatcher {

  private static final Matcher<MethodTree> EQUALS_MATCHER =
      allOf(
          methodIsNamed("equals"),
          methodHasVisibility(PUBLIC),
          methodReturns(BOOLEAN_TYPE),
          methodHasParameters(variableType(isSameType(OBJECT_TYPE))));

  @Override
  public Description matchClass(ClassTree classTree, VisitorState state) {

    TypeSymbol symbol = ASTHelpers.getSymbol(classTree);
    if (symbol.getKind() != ElementKind.CLASS) {
      return Description.NO_MATCH;
    }

    MethodTree equals = null;
    for (Tree member : classTree.getMembers()) {
      if (!(member instanceof MethodTree)) {
        continue;
      }
      MethodTree methodTree = (MethodTree) member;
      if (EQUALS_MATCHER.matches(methodTree, state)) {
        equals = methodTree;
      }
    }
    if (equals == null || isSuppressed(equals)) {
      return Description.NO_MATCH;
    }

    MethodSymbol hashCodeSym =
        ASTHelpers.resolveExistingMethod(
            state,
            symbol,
            state.getName("hashCode"),
            ImmutableList.<Type>of(),
            ImmutableList.<Type>of());

    if (hashCodeSym.owner.equals(state.getSymtab().objectType.tsym)) {
      return describeMatch(equals);
    }
    return Description.NO_MATCH;
  }
}
