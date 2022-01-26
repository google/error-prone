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

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;

import com.google.auto.value.AutoValue;
import com.google.common.base.VerifyException;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.NewClassTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ThrowTree;

/** Bugpattern to discourage throwing base exception classes. */
@BugPattern(
    summary =
        "Base exception classes should be treated as abstract. If the exception is intended to be"
            + " caught, throw a domain-specific exception. Otherwise, prefer a more specific"
            + " exception for clarity. Common alternatives include: AssertionError,"
            + " IllegalArgumentException, IllegalStateException, and (Guava's) VerifyException.",
    explanation =
        "1. Defensive coding: Using a generic exception would force a caller that wishes to catch"
            + " it to potentially catch unrelated exceptions as well."
            + "\n\n"
            + "2. Clarity: Base exception classes offer no information on the nature of the"
            + " failure.",
    severity = WARNING)
public final class ThrowSpecificExceptions extends BugChecker implements NewClassTreeMatcher {
  private static final ImmutableList<AbstractLikeException> ABSTRACT_LIKE_EXCEPTIONS =
      ImmutableList.of(
          AbstractLikeException.of(RuntimeException.class, VerifyException.class),
          AbstractLikeException.of(Throwable.class, AssertionError.class),
          AbstractLikeException.of(Error.class, AssertionError.class));

  @Override
  public Description matchNewClass(NewClassTree tree, VisitorState state) {
    if (tree.getClassBody() != null
        || !(state.getPath().getParentPath().getLeaf() instanceof ThrowTree)
        || state.errorProneOptions().isTestOnlyTarget()) {
      return Description.NO_MATCH;
    }
    for (AbstractLikeException abstractLikeException : ABSTRACT_LIKE_EXCEPTIONS) {
      if (abstractLikeException.matcher().matches(tree, state)) {
        SuggestedFix.Builder fix = SuggestedFix.builder();
        String className =
            SuggestedFixes.qualifyType(state, fix, abstractLikeException.replacement());
        return describeMatch(tree, SuggestedFix.replace(tree.getIdentifier(), className));
      }
    }
    return Description.NO_MATCH;
  }

  @AutoValue
  abstract static class AbstractLikeException {
    abstract Matcher<ExpressionTree> matcher();

    abstract String replacement();

    static AbstractLikeException of(Class<?> abstractLikeException, Class<?> replacement) {
      return new AutoValue_ThrowSpecificExceptions_AbstractLikeException(
          Matchers.constructor().forClass(abstractLikeException.getName()), replacement.getName());
    }
  }
}
