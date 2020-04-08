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

/** Bugpattern to discourage throwing base exception classes.. */
@BugPattern(
    name = "ThrowSpecificExceptions",
    summary =
        "Consider throwing more specific exceptions rather than (e.g.) RuntimeException. Throwing"
            + " generic exceptions forces any users of the API that wish to handle the failure"
            + " mode to catch very non-specific exceptions that convey little information.",
    severity = WARNING)
public final class ThrowSpecificExceptions extends BugChecker implements NewClassTreeMatcher {
  private static final ImmutableList<AbstractLikeException> ABSTRACT_LIKE_EXCEPTIONS =
      ImmutableList.of(
          AbstractLikeException.of(RuntimeException.class, IllegalStateException.class),
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
