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

package com.google.errorprone.bugpatterns.collectionincompatibletype;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.matchers.Matchers.staticMethod;
import static com.google.errorprone.util.ASTHelpers.getReceiver;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.Signatures.prettyType;
import static java.lang.String.format;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Type;

/** A BugPattern; see the summary. */
@BugPattern(summary = "The types passed to this assertion are incompatible.", severity = WARNING)
public final class AssertSameIncompatible extends BugChecker
    implements MethodInvocationTreeMatcher {
  private static final Matcher<ExpressionTree> JUNIT_MATCHER =
      staticMethod()
          .onClassAny("org.junit.Assert", "junit.framework.Assert", "junit.framework.TestCase")
          .namedAnyOf("assertSame", "assertNotSame");

  private static final Matcher<ExpressionTree> TRUTH_MATCHER =
      instanceMethod()
          .onDescendantOf("com.google.common.truth.Subject")
          .namedAnyOf("isSameInstanceAs", "isNotSameInstanceAs");

  private static final ImmutableSet<String> NEGATIVE_ASSERTIONS =
      ImmutableSet.of("assertNotSame", "isNotSameInstanceAs");

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (JUNIT_MATCHER.matches(tree, state)) {
      return handleJUnit(tree, state);
    }
    if (TRUTH_MATCHER.matches(tree, state)) {
      return handleTruth(tree, state);
    }
    return NO_MATCH;
  }

  private Description handleJUnit(MethodInvocationTree tree, VisitorState state) {
    var arguments = tree.getArguments();
    // Grab the last and penultimate arguments to deal with the message-taking overloads.
    var expected = getType(arguments.reversed().get(1));
    var actual = getType(arguments.reversed().get(0));
    return handle(
        tree,
        actual,
        expected,
        NEGATIVE_ASSERTIONS.contains(getSymbol(tree).getSimpleName().toString()),
        state);
  }

  private Description handleTruth(MethodInvocationTree tree, VisitorState state) {
    var expected = getType(tree.getArguments().get(0));
    var assertThat = getReceiver(tree);
    if ((!(assertThat instanceof MethodInvocationTree mit)
        || !getSymbol(mit).getSimpleName().contentEquals("assertThat")
        || mit.getArguments().size() != 1)) {
      return NO_MATCH;
    }
    var actual = getType(getOnlyElement(mit.getArguments()));
    return handle(
        tree, actual, expected, getSymbol(tree).getSimpleName().toString().contains("Not"), state);
  }

  private Description handle(
      Tree tree, Type actual, Type expected, boolean inverted, VisitorState state) {
    if (compatible(actual, expected, state)) {
      return NO_MATCH;
    }
    return buildDescription(tree)
        .setMessage(
            format(
                "The types passed to this assertion are incompatible (this check will always %s):"
                    + " type `%s` is not compatible with `%s`",
                inverted ? "pass" : "fail", prettyType(actual), prettyType(expected)))
        .build();
  }

  private static boolean compatible(Type typeA, Type typeB, VisitorState state) {
    var types = state.getTypes();
    Type erasedA = types.erasure(typeA);
    Type erasedB = types.erasure(typeB);
    return types.isCastable(erasedA, erasedB) || types.isCastable(erasedB, erasedA);
  }
}
