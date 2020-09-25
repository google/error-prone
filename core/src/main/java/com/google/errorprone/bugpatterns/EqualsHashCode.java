/*
 * Copyright 2014 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.equalsMethodDeclaration;
import static com.google.errorprone.matchers.Matchers.instanceEqualsInvocation;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
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
    severity = ERROR,
    tags = StandardTags.FRAGILE_CODE)
public class EqualsHashCode extends BugChecker implements ClassTreeMatcher {

  private static final Matcher<MethodTree> DIRECTLY_RETURNS_EQUALS_INVOCATION =
      Matchers.singleStatementReturnMatcher(instanceEqualsInvocation());

  @Override
  public Description matchClass(ClassTree classTree, VisitorState state) {

    TypeSymbol symbol = ASTHelpers.getSymbol(classTree);
    if (symbol.getKind() != ElementKind.CLASS) {
      return NO_MATCH;
    }
    // don't flag java.lang.Object
    if (symbol == state.getSymtab().objectType.tsym) {
      return NO_MATCH;
    }
    MethodTree equals = null;
    for (Tree member : classTree.getMembers()) {
      if (!(member instanceof MethodTree)) {
        continue;
      }
      MethodTree methodTree = (MethodTree) member;
      if (equalsMethodDeclaration().matches(methodTree, state)) {
        equals = methodTree;
      }
    }
    if (equals == null || isSuppressed(equals)) {
      return NO_MATCH;
    }
    if (DIRECTLY_RETURNS_EQUALS_INVOCATION.matches(equals, state)) {
      return NO_MATCH;
    }
    MethodSymbol hashCodeSym =
        ASTHelpers.resolveExistingMethod(
            state,
            symbol,
            state.getName("hashCode"),
            ImmutableList.<Type>of(),
            ImmutableList.<Type>of());

    if (!hashCodeSym.owner.equals(state.getSymtab().objectType.tsym)) {
      return NO_MATCH;
    }
    return describeMatch(equals);
  }
}
