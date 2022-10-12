/*
 * Copyright 2022 The Error Prone Authors.
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
import static com.google.errorprone.util.ASTHelpers.getReceiver;
import static com.google.errorprone.util.ASTHelpers.getSymbol;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol.MethodSymbol;

/** A BugPattern; see the summary */
@BugPattern(
    summary = "Mockito cannot mock final methods, and can't detect this at runtime",
    altNames = {"MockitoBadFinalMethod"},
    severity = WARNING)
public final class CannotMockFinalMethod extends BugChecker implements MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> WHEN =
      staticMethod().onClass("org.mockito.Mockito").named("when");

  private static final Matcher<ExpressionTree> VERIFY =
      staticMethod().onClass("org.mockito.Mockito").named("verify");

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (WHEN.matches(tree, state)) {
      ExpressionTree firstArgument = tree.getArguments().get(0);
      if (!(firstArgument instanceof MethodInvocationTree)) {
        return NO_MATCH;
      }
      return describe(tree, getSymbol((MethodInvocationTree) firstArgument));
    }
    var receiver = getReceiver(tree);
    if (receiver != null && VERIFY.matches(receiver, state)) {
      return describe(tree, getSymbol(tree));
    }
    return NO_MATCH;
  }

  private Description describe(MethodInvocationTree tree, MethodSymbol methodSymbol) {
    return (methodSymbol.flags() & Flags.FINAL) == 0 ? NO_MATCH : describeMatch(tree);
  }
}
