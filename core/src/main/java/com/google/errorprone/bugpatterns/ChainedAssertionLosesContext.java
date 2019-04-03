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

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.BugPattern.ProvidesFix.REQUIRES_HUMAN_ATTENTION;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.bugpatterns.ImplementAssertionWithChaining.makeCheckDescription;
import static com.google.errorprone.bugpatterns.ProvideDescriptionToCheck.findThatCall;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.staticMethod;
import static com.google.errorprone.util.ASTHelpers.getDeclaredSymbol;
import static com.google.errorprone.util.ASTHelpers.isSubtype;
import static com.sun.source.tree.Tree.Kind.CLASS;
import static java.util.stream.Collectors.joining;
import static javax.lang.model.element.Modifier.STATIC;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;

/**
 * Identifies calls to {@code assertThat} and similar methods inside the implementation of a {@code
 * Subject} assertion method. These calls should instead use {@code check(...)}.
 *
 * <pre>{@code
 * // Before:
 * public void hasFoo() {
 *   assertThat(actual().foo()).isEqualTo(expected);
 * }
 *
 * // After:
 * public void hasFoo() {
 *   check("foo()").that(actual().foo()).isEqualTo(expected);
 * }
 * }</pre>
 */
@BugPattern(
    name = "ChainedAssertionLosesContext",
    summary =
        "Inside a Subject, use check(...) instead of assert*() to preserve user-supplied messages"
            + " and other settings.",
    severity = WARNING,
    providesFix = REQUIRES_HUMAN_ATTENTION)
public final class ChainedAssertionLosesContext extends BugChecker
    implements MethodInvocationTreeMatcher {
  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!inInstanceMethodOfSubjectImplementation(state)) {
      return NO_MATCH;
    }

    if (STANDARD_ASSERT_THAT.matches(tree, state)) {
      String checkDescription = makeCheckDescription(getOnlyElement(tree.getArguments()), state);
      if (checkDescription == null) {
        // TODO(cpovirk): Generate a suggested fix without a check description.
        return NO_MATCH;
      }
      return replace(tree, "check(%s).that(%s)", checkDescription, sourceForArgs(tree, state));
    } else if (ASSERT_ABOUT.matches(tree, state)) {
      String checkDescription = findThatCallAndMakeCheckDescription(state);
      if (checkDescription == null) {
        // TODO(cpovirk): Generate a suggested fix without a check description.
        return NO_MATCH;
      }
      return replace(tree, "check(%s).about(%s)", checkDescription, sourceForArgs(tree, state));
    } else if (ASSERT_WITH_MESSAGE.matches(tree, state)) {
      String checkDescription = findThatCallAndMakeCheckDescription(state);
      if (checkDescription == null) {
        // TODO(cpovirk): Generate a suggested fix without a check description.
        return NO_MATCH;
      }
      return replace(
          tree, "check(%s).withMessage(%s)", checkDescription, sourceForArgs(tree, state));
    } else if (ASSERT.matches(tree, state)) {
      String checkDescription = findThatCallAndMakeCheckDescription(state);
      if (checkDescription == null) {
        // TODO(cpovirk): Generate a suggested fix without a check description.
        return NO_MATCH;
      }
      return replace(tree, "check(%s)", checkDescription);
    } else {
      /*
       * TODO(cpovirk): If it's an assertThat method other than Truth.assertThat, then find the
       * appropriate Subject.Factory, and generate check().about(...).that(...).
       */
      return NO_MATCH;
    }
  }

  private Description replace(Tree tree, String format, Object... args) {
    return describeMatch(tree, SuggestedFix.replace(tree, String.format(format, args)));
  }

  private static String sourceForArgs(MethodInvocationTree tree, VisitorState state) {
    // TODO(cpovirk): This might lose comments.
    return tree.getArguments().stream()
        .map(arg -> state.getSourceForNode(arg))
        .collect(joining(", "));
  }

  private static boolean inInstanceMethodOfSubjectImplementation(VisitorState state) {
    /*
     * All the checks here are mostly a no-op because, in static methods or methods outside Subject,
     * makeCheckDescription will fail to find a call to actual(), so the check won't fire. But they
     * set up for a future in which we issue a warning even if we can't produce a check description
     * for the suggested fix automatically.
     */
    TreePath pathToEnclosingMethod = state.findPathToEnclosing(MethodTree.class);
    if (pathToEnclosingMethod == null) {
      return false;
    }
    MethodTree enclosingMethod = (MethodTree) pathToEnclosingMethod.getLeaf();
    if (enclosingMethod.getModifiers().getFlags().contains(STATIC)) {
      return false;
    }
    Tree enclosingClass = pathToEnclosingMethod.getParentPath().getLeaf();
    if (enclosingClass == null || enclosingClass.getKind() != CLASS) {
      return false;
    }
    /*
     * TODO(cpovirk): Ideally we'd also recognize types nested inside Subject implementations, like
     * IterableSubject.UsingCorrespondence.
     */
    return isSubtype(
        getDeclaredSymbol(enclosingClass).type,
        state.getTypeFromString("com.google.common.truth.Subject"),
        state);
  }

  private static String findThatCallAndMakeCheckDescription(VisitorState state) {
    MethodInvocationTree thatCall = findThatCall(state);
    if (thatCall == null) {
      return null;
    }
    return makeCheckDescription(getOnlyElement(thatCall.getArguments()), state);
  }

  private static final Matcher<ExpressionTree> STANDARD_ASSERT_THAT =
      staticMethod().onClass("com.google.common.truth.Truth").named("assertThat");
  private static final Matcher<ExpressionTree> ASSERT_ABOUT =
      staticMethod().onClass("com.google.common.truth.Truth").named("assertAbout");
  private static final Matcher<ExpressionTree> ASSERT_WITH_MESSAGE =
      staticMethod().onClass("com.google.common.truth.Truth").named("assertWithMessage");
  private static final Matcher<ExpressionTree> ASSERT =
      staticMethod().onClass("com.google.common.truth.Truth").named("assert_");
}
