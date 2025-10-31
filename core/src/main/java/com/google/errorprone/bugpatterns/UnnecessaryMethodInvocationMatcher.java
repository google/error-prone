/*
 * Copyright 2019 The Error Prone Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.staticMethod;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.matchers.method.MethodMatchers.MethodMatcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Type;
import java.util.List;

/**
 * {@link Matchers#methodInvocation(Matcher)} is not exactly deprecated, but it is legacy, and in
 * particular is not needed when the argument is a MethodMatcher, since MethodMatcher already does
 * the unwrapping that methodInvocation does.
 *
 * @author amalloy@google.com (Alan Malloy)
 */
@BugPattern(
    summary = "It is not necessary to wrap a MethodMatcher with methodInvocation().",
    severity = WARNING)
public class UnnecessaryMethodInvocationMatcher extends BugChecker
    implements MethodInvocationTreeMatcher {

  private static final String MATCHERS = Matchers.class.getCanonicalName();
  private static final Matcher<ExpressionTree> METHOD_INVOCATION =
      staticMethod().onClass(MATCHERS).named("methodInvocation");
  private static final Matcher<ExpressionTree> COMBINATOR =
      staticMethod().onClass(MATCHERS).namedAnyOf("allOf", "anyOf", "not");

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!METHOD_INVOCATION.matches(tree, state)) {
      return NO_MATCH;
    }
    List<? extends ExpressionTree> arguments = tree.getArguments();
    if (arguments.size() != 1) {
      // We can only unwrap if they haven't specified additional behavior.
      return NO_MATCH;
    }
    Type methodMatcherType = state.getTypeFromString(MethodMatcher.class.getCanonicalName());
    ExpressionTree argument = arguments.get(0);
    if (!containsOnlyMethodMatchers(argument, methodMatcherType, state)) {
      return NO_MATCH;
    }
    return describeMatch(tree, SuggestedFix.replace(tree, state.getSourceForNode(argument)));
  }

  private static boolean containsOnlyMethodMatchers(
      ExpressionTree expressionTree, Type methodMatcherType, VisitorState state) {
    if (ASTHelpers.isSubtype(ASTHelpers.getType(expressionTree), methodMatcherType, state)) {
      return true;
    }
    if (!COMBINATOR.matches(expressionTree, state)) {
      return false;
    }
    for (ExpressionTree argument : ((MethodInvocationTree) expressionTree).getArguments()) {
      if (!containsOnlyMethodMatchers(argument, methodMatcherType, state)) {
        return false;
      }
    }
    return true;
  }
}
