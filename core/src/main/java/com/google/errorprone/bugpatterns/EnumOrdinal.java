/*
 * Copyright 2024 The Error Prone Authors.
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
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;
import static com.google.errorprone.util.ASTHelpers.getSymbol;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ArrayAccessTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;

/** Discourages the use of {@link Enum#ordinal()} and other ways to access enum values by index. */
@BugPattern(
    summary =
        "You should almost never invoke the Enum.ordinal() method or depend on the enum values by"
            + " index.",
    severity = WARNING)
public final class EnumOrdinal extends BugChecker
    implements MethodInvocationTreeMatcher, ArrayAccessTreeMatcher {

  private static final Matcher<ExpressionTree> ORDINAL =
      instanceMethod().onDescendantOf("java.lang.Enum").named("ordinal");

  private static final Matcher<ExpressionTree> VALUES =
      staticMethod().onDescendantOf("java.lang.Enum").named("values");

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (ORDINAL.matches(tree, state)) {
      return describeMatch(tree);
    }
    return Description.NO_MATCH;
  }

  @Override
  public Description matchArrayAccess(ArrayAccessTree tree, VisitorState state) {
    if (!(tree.getExpression() instanceof MethodInvocationTree mit)) {
      return Description.NO_MATCH;
    }
    if (!VALUES.matches(tree.getExpression(), state)) {
      return Description.NO_MATCH;
    }
    MethodSymbol methodInvocationSymbol = getSymbol(mit);
    ClassSymbol enclosingClassSymbol = getSymbol(state.findEnclosing(ClassTree.class));
    if (methodInvocationSymbol.isEnclosedBy(enclosingClassSymbol)) {
      return Description.NO_MATCH;
    }
    return describeMatch(tree);
  }
}
