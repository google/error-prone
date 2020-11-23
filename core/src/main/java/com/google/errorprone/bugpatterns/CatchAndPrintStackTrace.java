/*
 * Copyright 2018 The Error Prone Authors.
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
import static com.google.errorprone.matchers.Matchers.expressionStatement;
import static com.google.errorprone.matchers.Matchers.matchSingleStatementBlock;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;

import com.google.common.collect.Iterables;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.CatchTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.CatchTree;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    name = "CatchAndPrintStackTrace",
    summary =
        "Logging or rethrowing exceptions should usually be preferred to catching and calling"
            + " printStackTrace",
    severity = WARNING)
public class CatchAndPrintStackTrace extends BugChecker implements CatchTreeMatcher {

  private static final Matcher<BlockTree> MATCHER =
      matchSingleStatementBlock(
          expressionStatement(
              instanceMethod().onDescendantOf("java.lang.Throwable").named("printStackTrace")));

  @Override
  public Description matchCatch(CatchTree tree, VisitorState state) {
    if (MATCHER.matches(tree.getBlock(), state)) {
      return describeMatch(Iterables.getOnlyElement(tree.getBlock().getStatements()));
    }
    return NO_MATCH;
  }
}
