/*
 * Copyright 2019 The Error Prone Authors.
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

import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.AssignmentTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.ReturnTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;

/**
 * Finds and fixes unnecessarily boxed return expressions.
 *
 * @author awturner@google.com (Andy Turner)
 */
@BugPattern(
    summary = "This expression can be implicitly boxed.",
    explanation =
        "It is unnecessary for this assignment or return expression to be boxed explicitly.",
    severity = SeverityLevel.SUGGESTION)
public class UnnecessaryBoxedAssignment extends BugChecker
    implements AssignmentTreeMatcher, ReturnTreeMatcher, VariableTreeMatcher {
  private static final Matcher<ExpressionTree> VALUE_OF_MATCHER =
      staticMethod().onClass(UnnecessaryBoxedAssignment::isBoxableType).named("valueOf");

  @Override
  public Description matchReturn(ReturnTree tree, VisitorState state) {
    return matchCommon(tree.getExpression(), state);
  }

  @Override
  public Description matchAssignment(AssignmentTree tree, VisitorState state) {
    return matchCommon(tree.getExpression(), state);
  }

  @Override
  public Description matchVariable(VariableTree tree, VisitorState state) {
    return matchCommon(tree.getInitializer(), state);
  }

  private Description matchCommon(ExpressionTree expression, VisitorState state) {
    if (expression == null || !VALUE_OF_MATCHER.matches(expression, state)) {
      return Description.NO_MATCH;
    }
    MethodInvocationTree methodInvocationTree = (MethodInvocationTree) expression;
    if (methodInvocationTree.getArguments().size() != 1) {
      return Description.NO_MATCH;
    }
    ExpressionTree arg = methodInvocationTree.getArguments().get(0);
    Type argType = ASTHelpers.getType(arg);
    if (ASTHelpers.isSameType(argType, state.getSymtab().stringType, state)) {
      return Description.NO_MATCH;
    }
    // Don't fix if there is an implicit primitive widening. This would need a cast, and that's not
    // clearly better than using valueOf.
    if (!ASTHelpers.isSameType(
        state.getTypes().unboxedTypeOrType(argType),
        state.getTypes().unboxedType(ASTHelpers.getType(expression)),
        state)) {
      return Description.NO_MATCH;
    }
    return describeMatch(expression, SuggestedFix.replace(expression, state.getSourceForNode(arg)));
  }

  private static boolean isBoxableType(Type type, VisitorState state) {
    Type unboxedType = state.getTypes().unboxedType(type);
    return unboxedType != null && unboxedType.getTag() != TypeTag.NONE;
  }
}
