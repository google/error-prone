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

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.util.ASTHelpers.constValue;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;

/**
 * A check that bans using placeholders in {@link Thread.Builder#name(String)} and {@link
 * Thread.Builder#name(String, int)}.
 */
@BugPattern(
    summary =
        "Thread.Builder.name() does not accept placeholders (e.g., %d or %s)."
            + " threadBuilder.name(String) accepts a constant name and threadBuilder.name(String,"
            + " int) accepts a constant name _prefix_ and an initial counter value.",
    severity = ERROR,
    tags = StandardTags.LIKELY_ERROR)
public final class ThreadBuilderNameWithPlaceholder extends BugChecker
    implements MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> THREAD_BUILDER_NAME =
      instanceMethod().onDescendantOf("java.lang.Thread.Builder").named("name");

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (THREAD_BUILDER_NAME.matches(tree, state)) {
      String nameValue = constValue(tree.getArguments().get(0), String.class);
      if (nameValue != null && (nameValue.contains("%d") || nameValue.contains("%s"))) {
        return describeMatch(tree);
      }
    }
    return Description.NO_MATCH;
  }
}
