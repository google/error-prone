/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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

import static com.google.errorprone.BugPattern.Category.MOCKITO;
import static com.google.errorprone.BugPattern.MaturityLevel.MATURE;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;

import com.google.common.collect.Iterables;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;

/**
 * @author cushon@google.com (Liam Miller-Cushon)
 */
@BugPattern(
  name = "MockitoUsage",
  summary = "Missing method call for verify(mock) here",
  category = MOCKITO,
  severity = ERROR,
  maturity = MATURE
)
public class MockitoUsage extends BugChecker implements MethodInvocationTreeMatcher {

  private static final String MESSAGE_FORMAT = "Missing method call for %s here";

  private static final Matcher<ExpressionTree> MOCK_METHOD =
      anyOf(
          staticMethod().onClass("org.mockito.Mockito").withSignature("<T>when(T)"),
          staticMethod().onClass("org.mockito.Mockito").withSignature("<T>verify(T)"));

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!MOCK_METHOD.matches(tree, state)) {
      return Description.NO_MATCH;
    }
    if (state.getPath().getParentPath().getLeaf().getKind() != Tree.Kind.EXPRESSION_STATEMENT) {
      return Description.NO_MATCH;
    }
    String message = String.format(MESSAGE_FORMAT, state.getSourceForNode(tree));
    return buildDescription(tree).addFix(buildFix(tree, state)).setMessage(message).build();
  }

  /** Rewrite `verify(foo.bar())` to `verify(foo).bar()`, or delete the call. */
  private Fix buildFix(MethodInvocationTree tree, VisitorState state) {
    MethodInvocationTree mockitoCall = tree;
    Tree argument = Iterables.getOnlyElement(mockitoCall.getArguments());
    if (argument.getKind() == Kind.METHOD_INVOCATION
        && ASTHelpers.getSymbol(tree).getSimpleName().contentEquals("verify")) {
      MethodInvocationTree invocation = (MethodInvocationTree) argument;
      String mockitoPart = state.getSourceForNode(mockitoCall.getMethodSelect());
      String receiver = state.getSourceForNode(ASTHelpers.getReceiver(invocation));
      String call = state.getSourceForNode(invocation).substring(receiver.length());
      return SuggestedFix.replace(tree, String.format("%s(%s)%s", mockitoPart, receiver, call));
    }

    // delete entire expression statement
    Tree parent = state.getPath().getParentPath().getLeaf();
    if (parent.getKind() == Kind.EXPRESSION_STATEMENT) {
      return SuggestedFix.delete(parent);
    }

    return SuggestedFix.delete(tree);
  }
}
