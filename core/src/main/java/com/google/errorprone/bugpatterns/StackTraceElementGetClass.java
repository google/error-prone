/*
 * Copyright 2021 The Error Prone Authors.
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

import static com.google.errorprone.matchers.Matchers.instanceMethod;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;

/**
 * Checks for usages of {@code StackTraceElement#getClass} method.
 *
 * <p>{@code StackTraceElement#getClass} returns the Class object for {@code StackTraceElement}. In
 * almost all the scenarios this is not intended and is a potential source of bugs. The most common
 * usage of this method is to retrieve the name of the class where exception occurred, in such cases
 * {@code StackTraceElement#getClassName} can be used instead. In case Class object for {@code
 * StackTraceElement} is required it can be obtained using {code StackTraceElement#class} method.
 */
@BugPattern(
    name = "StackTraceElementGetClass",
    summary =
        "Calling getClass on StackTraceElement returns the Class object for StackTraceElement, you"
            + " probably meant to retrieve the class containing the execution point represented by"
            + " this stack trace element.",
    severity = SeverityLevel.ERROR)
public class StackTraceElementGetClass extends BugChecker implements MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> GET_CLASS_MATCHER =
      instanceMethod().onExactClass("java.lang.StackTraceElement").named("getClass");
  private static final Matcher<ExpressionTree> GET_NAME_MATCHER =
      instanceMethod().onExactClass("java.lang.Class").named("getName");
  private static final Matcher<ExpressionTree> GET_SIMPLE_NAME_MATCHER =
      instanceMethod().onExactClass("java.lang.Class").named("getSimpleName");

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!GET_CLASS_MATCHER.matches(tree, state)) {
      return Description.NO_MATCH;
    }
    Description.Builder descriptionBuilder = buildDescription(tree);
    Tree parentTree = state.getPath().getParentPath().getLeaf();
    if (parentTree instanceof ExpressionTree
        && (GET_NAME_MATCHER.matches((ExpressionTree) parentTree, state)
            || GET_SIMPLE_NAME_MATCHER.matches((ExpressionTree) parentTree, state))) {
      SuggestedFix.Builder fixBuilder =
          SuggestedFix.builder()
              .replace(
                  parentTree,
                  state.getSourceForNode(ASTHelpers.getReceiver(tree)) + ".getClassName");
      if (GET_SIMPLE_NAME_MATCHER.matches((ExpressionTree) parentTree, state)) {
        fixBuilder.setShortDescription(
            "Replace with getClassName. WARNING: This returns the fully-qualified name of class.");
      }
      descriptionBuilder.addFix(fixBuilder.build());
    }
    if (!(parentTree instanceof ExpressionStatementTree)) {
      descriptionBuilder.addFix(SuggestedFix.replace(tree, "StackTraceElement.class"));
    }
    return descriptionBuilder.build();
  }
}
