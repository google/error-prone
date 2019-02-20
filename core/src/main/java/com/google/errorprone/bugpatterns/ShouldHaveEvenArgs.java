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

package com.google.errorprone.bugpatterns;

import static com.google.common.collect.Iterables.getLast;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.util.ASTHelpers.getSymbol;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;
import java.util.List;

/**
 * Checks that variable argument methods have even number of arguments.
 *
 * @author bhagwani@google.com (Sumit Bhagwani)
 */
@BugPattern(
    name = "ShouldHaveEvenArgs",
    summary = "This method must be called with an even number of arguments.",
    severity = ERROR)
public class ShouldHaveEvenArgs extends BugChecker implements MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> MATCHER =
      instanceMethod()
          .onDescendantOfAny(
              "com.google.common.truth.MapSubject",
              "com.google.common.truth.MapSubject.UsingCorrespondence",
              "com.google.common.truth.MultimapSubject",
              "com.google.common.truth.MultimapSubject.UsingCorrespondence")
          .named("containsExactly");

  @Override
  public Description matchMethodInvocation(
      MethodInvocationTree methodInvocationTree, VisitorState state) {
    if (!MATCHER.matches(methodInvocationTree, state)) {
      return Description.NO_MATCH;
    }
    if (methodInvocationTree.getArguments().size() % 2 == 0) {
      return Description.NO_MATCH;
    }
    List<? extends ExpressionTree> arguments = methodInvocationTree.getArguments();

    MethodSymbol methodSymbol = getSymbol(methodInvocationTree);
    if (methodSymbol == null || !methodSymbol.isVarArgs()) {
      return Description.NO_MATCH;
    }
    Type varArgsArrayType = getLast(methodSymbol.params()).type;
    Type lastArgType = ASTHelpers.getType(getLast(arguments));
    if (arguments.size() == methodSymbol.params().size()
        && ASTHelpers.isSameType(varArgsArrayType, lastArgType, state)) {
      return Description.NO_MATCH;
    }
    return describeMatch(methodInvocationTree);
  }
}
