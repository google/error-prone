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
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import java.util.List;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
  name = "MockitoUsage",
  summary = "Missing method call for verify(mock) here",
  category = MOCKITO,
  severity = ERROR,
  providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION
)
public class MockitoUsage extends BugChecker implements MethodInvocationTreeMatcher {

  private static final String MESSAGE_FORMAT = "Missing method call for %s here";

  private static final Matcher<ExpressionTree> MOCK_METHOD =
      anyOf(
          staticMethod().onClass("org.mockito.Mockito").withSignature("<T>when(T)"),
          staticMethod().onClass("org.mockito.Mockito").withSignature("<T>verify(T)"),
          staticMethod()
              .onClass("org.mockito.Mockito")
              .withSignature("<T>verify(T,org.mockito.verification.VerificationMode)"));

  private static final Matcher<ExpressionTree> NEVER_METHOD =
      staticMethod().onClass("org.mockito.Mockito").withSignature("never()");

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!MOCK_METHOD.matches(tree, state)) {
      return Description.NO_MATCH;
    }
    if (state.getPath().getParentPath().getLeaf().getKind() != Tree.Kind.EXPRESSION_STATEMENT) {
      return Description.NO_MATCH;
    }
    String message = String.format(MESSAGE_FORMAT, state.getSourceForNode(tree));
    Description.Builder builder = buildDescription(tree).setMessage(message);
    buildFix(builder, tree, state);
    return builder.build();
  }

  /**
   * Create fixes for invalid assertions.
   *
   * <ul>
   *   <li>Rewrite `verify(mock.bar())` to `verify(mock).bar()`
   *   <li>Rewrite `verify(mock.bar(), times(N))` to `verify(mock, times(N)).bar()`
   *   <li>Rewrite `verify(mock, never())` to `verifyZeroInteractions(mock)`
   *   <li>Finally, offer to delete the mock statement.
   * </ul>
   */
  private void buildFix(
      Description.Builder builder, MethodInvocationTree tree, VisitorState state) {
    MethodInvocationTree mockitoCall = tree;
    List<? extends ExpressionTree> args = mockitoCall.getArguments();
    Tree mock = mockitoCall.getArguments().get(0);
    boolean isVerify = ASTHelpers.getSymbol(tree).getSimpleName().contentEquals("verify");
    if (isVerify && mock.getKind() == Kind.METHOD_INVOCATION) {
      MethodInvocationTree invocation = (MethodInvocationTree) mock;
      String verify = state.getSourceForNode(mockitoCall.getMethodSelect());
      String receiver = state.getSourceForNode(ASTHelpers.getReceiver(invocation));
      String mode = args.size() > 1 ? ", " + state.getSourceForNode(args.get(1)) : "";
      String call = state.getSourceForNode(invocation).substring(receiver.length());
      builder.addFix(
          SuggestedFix.replace(tree, String.format("%s(%s%s)%s", verify, receiver, mode, call)));
    }
    if (isVerify && args.size() > 1 && NEVER_METHOD.matches(args.get(1), state)) {
      // TODO(cushon): handle times(0) the same as never()
      builder.addFix(
          SuggestedFix.builder()
              .addStaticImport("org.mockito.Mockito.verifyZeroInteractions")
              .replace(tree, String.format("verifyZeroInteractions(%s)", mock))
              .build());
    }
    // Always suggest the naive semantics-preserving option, which is just to
    // delete the assertion:
    Tree parent = state.getPath().getParentPath().getLeaf();
    if (parent.getKind() == Kind.EXPRESSION_STATEMENT) {
      // delete entire expression statement
      builder.addFix(SuggestedFix.delete(parent));
    } else {
      builder.addFix(SuggestedFix.delete(tree));
    }
  }
}
