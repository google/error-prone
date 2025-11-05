/*
 * Copyright 2016 The Error Prone Authors.
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

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.threadsafety.ConstantExpressions;
import com.google.errorprone.bugpatterns.threadsafety.ConstantExpressions.ConstantExpressionVisitor;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.inject.Inject;

/**
 * Points out if Truth Library assert is called on a constant.
 *
 * @author bhagwani@google.com (Sumit Bhagwani)
 */
@BugPattern(
    summary = "Truth Library assert is called on a constant.",
    severity = WARNING,
    tags = StandardTags.FRAGILE_CODE)
public class TruthConstantAsserts extends BugChecker implements MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> ASSERT_THAT =
      staticMethod().onClass("com.google.common.truth.Truth").named("assertThat");

  private static final Matcher<ExpressionTree> TRUTH_SUBJECT_CALL =
      instanceMethod()
          .onDescendantOf("com.google.common.truth.Subject")
          .namedAnyOf("isEqualTo", "isNotEqualTo")
          .withParameters("java.lang.Object");

  private final ConstantExpressions constantExpressions;

  @Inject
  TruthConstantAsserts(ConstantExpressions constantExpressions) {
    this.constantExpressions = constantExpressions;
  }

  @Override
  public Description matchMethodInvocation(
      MethodInvocationTree methodInvocationTree, VisitorState state) {
    if (methodInvocationTree.getArguments().isEmpty()) {
      return Description.NO_MATCH;
    }
    if (!TRUTH_SUBJECT_CALL.matches(methodInvocationTree, state)) {
      return Description.NO_MATCH;
    }
    ExpressionTree rec = ASTHelpers.getReceiver(methodInvocationTree);
    if (rec == null) {
      return Description.NO_MATCH;
    }
    if (!ASSERT_THAT.matches(rec, state)) {
      return Description.NO_MATCH;
    }
    ExpressionTree expr = getOnlyElement(((MethodInvocationTree) rec).getArguments());
    if (expr == null) {
      return Description.NO_MATCH;
    }
    // check that argument of assertThat is a constant
    if (!constantIsh(expr, state)) {
      return Description.NO_MATCH;
    }
    // check that expectation isn't a constant
    ExpressionTree expectation = getOnlyElement(methodInvocationTree.getArguments());
    if (constantIsh(expectation, state)) {
      return Description.NO_MATCH;
    }
    SuggestedFix fix = SuggestedFix.swap(expr, expectation, state);
    return describeMatch(methodInvocationTree, fix);
  }

  private boolean constantIsh(ExpressionTree tree, VisitorState state) {
    var constant = constantExpressions.constantExpression(tree, state).orElse(null);
    if (constant == null) {
      return false;
    }
    // Identifiers can be considered constants, but they're exactly what we usually assert on! So
    // don't consider them to be constants in this context.
    AtomicBoolean involvesIdentifiers = new AtomicBoolean();
    constant.accept(
        new ConstantExpressionVisitor() {
          @Override
          public void visitIdentifier(Symbol identifier) {
            if (!(identifier instanceof MethodSymbol)) {
              involvesIdentifiers.set(true);
            }
          }
        });
    return !involvesIdentifiers.get();
  }
}
