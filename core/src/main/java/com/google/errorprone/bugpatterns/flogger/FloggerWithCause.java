/*
 * Copyright 2020 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.flogger;

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.matchers.Matchers.isSubtypeOf;
import static com.google.errorprone.matchers.method.MethodMatchers.constructor;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.predicates.TypePredicates;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCNewClass;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/** Flogger's withCause(Throwable) method checks */
@BugPattern(
    name = "FloggerWithCause",
    summary =
        "Calling withCause(Throwable) with an inline allocated Throwable is discouraged."
            + " Consider using withStackTrace(StackSize) instead, and specifying a reduced"
            + " stack size (e.g. SMALL, MEDIUM or LARGE) instead of FULL, to improve"
            + " performance.",
    severity = WARNING)
public class FloggerWithCause extends BugChecker implements MethodInvocationTreeMatcher {

  private static final String STACK_SIZE_MEDIUM_IMPORT =
      "com.google.common.flogger.StackSize.MEDIUM";

  private static final Matcher<ExpressionTree> WITH_CAUSE_MATCHER =
      instanceMethod().onDescendantOf("com.google.common.flogger.LoggingApi").named("withCause");

  private static final Matcher<ExpressionTree> FIXABLE_THROWABLE_MATCHER =
      constructor()
          .forClass(
              TypePredicates.isExactTypeAny(
                  ImmutableList.of(
                      "java.lang.AssertionError",
                      "java.lang.Error",
                      "java.lang.Exception",
                      "java.lang.IllegalArgumentException",
                      "java.lang.IllegalStateException",
                      "java.lang.RuntimeException",
                      "java.lang.Throwable",
                      "com.google.photos.be.util.StackTraceLoggerException")))
          .withParameters(ImmutableList.of());

  private static final Matcher<Tree> THROWABLE_MATCHER = isSubtypeOf("java.lang.Throwable");

  private static final Matcher<ExpressionTree> THROWABLE_STRING_MATCHER =
      Matchers.anyOf(
          instanceMethod().onDescendantOf("java.lang.Throwable").named("getMessage"),
          instanceMethod().onDescendantOf("java.lang.Throwable").named("toString"));

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!WITH_CAUSE_MATCHER.matches(tree, state)) {
      return Description.NO_MATCH;
    }

    ExpressionTree cause = Iterables.getOnlyElement(tree.getArguments());

    if (cause.getKind() == Kind.NEW_CLASS) {
      Optional<ExpressionTree> throwableArgument = getThrowableArgument(cause, state);
      if (throwableArgument.isPresent() || FIXABLE_THROWABLE_MATCHER.matches(cause, state)) {
        List<Fix> fixes = getFixes(tree, state, throwableArgument.orElse(null));
        return getDescription(tree, fixes);
      }
    }

    return Description.NO_MATCH;
  }

  private Description getDescription(MethodInvocationTree tree, List<Fix> fixes) {
    Description.Builder description = buildDescription(tree);
    for (Fix fix : fixes) {
      description.addFix(fix);
    }
    return description.build();
  }

  private static List<Fix> getFixes(
      MethodInvocationTree tree, VisitorState state, ExpressionTree throwableArgument) {
    if (throwableArgument != null) {
      String withCauseReplacement = ".withCause(" + state.getSourceForNode(throwableArgument) + ")";
      String withStackTraceAndWithCauseReplacement =
          ".withStackTrace(MEDIUM)" + withCauseReplacement;
      return Arrays.asList(
          getFix(tree, state, withCauseReplacement),
          getFix(tree, state, withStackTraceAndWithCauseReplacement, STACK_SIZE_MEDIUM_IMPORT));
    }

    return Collections.singletonList(
        getFix(tree, state, ".withStackTrace(MEDIUM)", STACK_SIZE_MEDIUM_IMPORT));
  }

  private static Optional<ExpressionTree> getThrowableArgument(
      ExpressionTree cause, VisitorState state) {
    for (ExpressionTree argument : ((JCNewClass) cause).getArguments()) {
      if (THROWABLE_MATCHER.matches(argument, state)) {
        return Optional.of(argument);
      }
      if (THROWABLE_STRING_MATCHER.matches(argument, state)) {
        return Optional.ofNullable(ASTHelpers.getReceiver(argument));
      }
    }
    return Optional.empty();
  }

  /**
   * Returns fix that has current method replaced with provided method replacement string and
   * provided static import added to it
   */
  private static Fix getFix(
      MethodInvocationTree tree, VisitorState state, String replacement, String importString) {
    return getFixBuilder(tree, state, replacement).addStaticImport(importString).build();
  }

  /** Returns fix that has current method replaced with provided method replacement string */
  private static Fix getFix(MethodInvocationTree tree, VisitorState state, String replacement) {
    return getFixBuilder(tree, state, replacement).build();
  }

  private static SuggestedFix.Builder getFixBuilder(
      MethodInvocationTree tree, VisitorState state, String methodReplacement) {
    int methodStart = getMethodStart(tree, state);
    int methodEnd = getMethodEnd(tree, state);
    return SuggestedFix.builder().replace(methodStart, methodEnd, methodReplacement);
  }

  private static int getMethodStart(MethodInvocationTree tree, VisitorState state) {
    return state.getEndPosition((JCTree) ASTHelpers.getReceiver(tree));
  }

  private static int getMethodEnd(MethodInvocationTree tree, VisitorState state) {
    return state.getEndPosition((JCTree) tree);
  }
}
