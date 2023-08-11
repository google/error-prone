/*
 * Copyright 2023 The Error Prone Authors.
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
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.FieldMatchers.staticField;
import static com.google.errorprone.matchers.Matchers.anyOf;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.TryTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.TryTree;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    summary =
        "Don't use try-with-resources to manage standard output streams, closing the stream will"
            + " cause subsequent output to standard output or standard error to be lost",
    severity = WARNING)
public class ClosingStandardOutputStreams extends BugChecker implements TryTreeMatcher {
  private static final Matcher<ExpressionTree> MATCHER =
      anyOf(staticField("java.lang.System", "err"), staticField("java.lang.System", "out"));

  @Override
  public Description matchTry(TryTree tree, VisitorState state) {
    new SuppressibleTreePathScanner<Void, Void>(state) {

      @Override
      public Void visitTry(TryTree tree, Void unused) {
        tree.getResources().forEach(r -> r.accept(this, null));
        return null;
      }

      @Override
      public Void visitMemberSelect(MemberSelectTree tree, Void unused) {
        if (MATCHER.matches(tree, state)) {
          state.reportMatch(describeMatch(tree));
        }
        return null;
      }
    }.scan(state.getPath(), null);
    return NO_MATCH;
  }
}
