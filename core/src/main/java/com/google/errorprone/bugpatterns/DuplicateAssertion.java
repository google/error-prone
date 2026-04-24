/*
 * Copyright 2025 The Error Prone Authors.
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
import static com.google.errorprone.VisitorState.memoize;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.assertEqualsInvocation;
import static com.google.errorprone.matchers.Matchers.assertNotEqualsInvocation;
import static com.google.errorprone.matchers.Matchers.staticMethod;
import static com.google.errorprone.util.ASTHelpers.getReceiver;
import static com.google.errorprone.util.ASTHelpers.getStartPosition;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.isSameType;
import static com.google.errorprone.util.ASTHelpers.isSubtype;

import com.google.common.collect.ImmutableSetMultimap;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.BlockTreeMatcher;
import com.google.errorprone.bugpatterns.threadsafety.ConstantExpressions;
import com.google.errorprone.bugpatterns.threadsafety.ConstantExpressions.ConstantExpression;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.suppliers.Supplier;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Type;
import javax.inject.Inject;

/** A BugPattern; see the {@code summary}. */
@BugPattern(summary = "This assertion is duplicate.", severity = SeverityLevel.WARNING)
public final class DuplicateAssertion extends BugChecker implements BlockTreeMatcher {
  private static final Matcher<ExpressionTree> JUNIT_ASSERT =
      anyOf(assertEqualsInvocation(), assertNotEqualsInvocation());

  private static final Matcher<ExpressionTree> PRECONDITIONS =
      anyOf(
          staticMethod()
              .onClass("com.google.common.base.Preconditions")
              .namedAnyOf("checkArgument", "checkState", "checkNotNull"),
          staticMethod().onClass("com.google.common.base.Verify"),
          staticMethod().onClass("java.util.Objects").named("requireNonNull"));

  private final ConstantExpressions constantExpressions;

  @Inject
  DuplicateAssertion(ConstantExpressions constantExpressions) {
    this.constantExpressions = constantExpressions;
  }

  @Override
  public Description matchBlock(BlockTree tree, VisitorState state) {
    var assertionLines = extractAssertionLines(tree, state);

    for (var entry : assertionLines.entries()) {
      var source = entry.getKey();
      var line = entry.getValue();
      if (assertionLines.containsEntry(source, line - 1)) {
        state.reportMatch(
            buildDescription(tree.getStatements().get(line))
                .setMessage("This assertion is duplicated on the line above. Is that a mistake?")
                .build());
      }
    }

    return NO_MATCH;
  }

  private ImmutableSetMultimap<Assertion, Integer> extractAssertionLines(
      BlockTree tree, VisitorState state) {
    ImmutableSetMultimap.Builder<Assertion, Integer> lines = ImmutableSetMultimap.builder();
    for (int i = 0; i < tree.getStatements().size(); i++) {
      int finalI = i;
      var statement = tree.getStatements().get(i);
      if (!(statement instanceof ExpressionStatementTree est)) {
        continue;
      }
      if (!(est.getExpression() instanceof MethodInvocationTree mit)) {
        continue;
      }
      for (ExpressionTree receiver = mit;
          receiver instanceof MethodInvocationTree method;
          receiver = getReceiver(receiver)) {
        var symbol = getSymbol(method);
        if (JUNIT_ASSERT.matches(method, state)) {
          constantExpressions
              .constantExpression(
                  method.getArguments().get(argumentsToSkip(method, state) + 1), state)
              .ifPresent(
                  ce -> lines.put(new Assertion(state.getSourceForNode(statement), ce), finalI));
        } else if (PRECONDITIONS.matches(method, state)
            && method.equals(est.getExpression())
            && !method.getArguments().isEmpty()) {
          // Only consider the line up to the end of the first argument, given a duplicated
          // assertion with a different message is definitely bad.
          int start = getStartPosition(method);
          int end = state.getEndPosition(method.getArguments().get(0));
          String source = state.getSourceCode().subSequence(start, end).toString();
          constantExpressions
              .constantExpression(method.getArguments().get(0), state)
              .ifPresent(ce -> lines.put(new Assertion(source, ce), finalI));
        } else if ((symbol.getSimpleName().contentEquals("assertThat")
                || isSubjectBuilderThatMethod(method, state))
            && method.getArguments().size() == 1) {
          constantExpressions
              .constantExpression(getOnlyElement(method.getArguments()), state)
              .ifPresent(
                  ce -> lines.put(new Assertion(state.getSourceForNode(statement), ce), finalI));
        }
      }
    }
    return lines.build();
  }

  private static boolean isSubjectBuilderThatMethod(ExpressionTree method, VisitorState state) {
    var symbol = getSymbol(method);
    if (!symbol.getSimpleName().contentEquals("that")) {
      return false;
    }

    // TODO: b/447504139 - Maybe match any "that" method that returns a subclass of Subject?
    var receiver = getReceiver(method);
    return isSubtype(getType(receiver), STANDARD_SUBJECT_BUILDER.get(state), state)
        || isSubtype(getType(receiver), SIMPLE_SUBJECT_BUILDER.get(state), state)
        || isSubtype(getType(receiver), CUSTOM_SUBJECT_BUILDER.get(state), state);
  }

  /** Returns the number of arguments to skip so we ignore {@code message} arguments. */
  private static int argumentsToSkip(MethodInvocationTree tree, VisitorState state) {
    var parameters = getSymbol(tree).getParameters();
    if (!isSameType(parameters.getFirst().type, state.getSymtab().stringType, state)) {
      return 0;
    }
    if (parameters.size() == 2) {
      return 0;
    }
    return 1;
  }

  private record Assertion(String line, ConstantExpression assertee) {}

  private static final Supplier<Type> STANDARD_SUBJECT_BUILDER =
      memoize(state -> state.getTypeFromString("com.google.common.truth.StandardSubjectBuilder"));

  private static final Supplier<Type> SIMPLE_SUBJECT_BUILDER =
      memoize(state -> state.getTypeFromString("com.google.common.truth.SimpleSubjectBuilder"));

  private static final Supplier<Type> CUSTOM_SUBJECT_BUILDER =
      memoize(state -> state.getTypeFromString("com.google.common.truth.CustomSubjectBuilder"));
}
