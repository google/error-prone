/*
 * Copyright 2017 The Error Prone Authors.
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

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.Iterables.getLast;
import static com.google.errorprone.BugPattern.Category.JUNIT;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.JUnitMatchers;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import java.util.List;
import javax.annotation.Nullable;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
    name = "ExpectedExceptionChecker",
    category = JUNIT,
    summary =
        "Calls to ExpectedException#expect should always be followed by exactly one statement.",
    severity = WARNING,
    tags = StandardTags.FRAGILE_CODE,
    providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION)
public class ExpectedExceptionChecker extends AbstractExpectedExceptionChecker {
  @Override
  protected Description handleMatch(
      MethodTree tree,
      VisitorState state,
      List<Tree> expectations,
      List<StatementTree> suffix,
      @Nullable StatementTree failure) {
    if (suffix.size() <= 1) {
      // for now, allow ExpectedException as long as it's testing that exactly one statement throws
      return NO_MATCH;
    }
    BaseFix baseFix = buildBaseFix(state, expectations, failure);
    // provide fixes to wrap each of the trailing statements in a lambda
    // skip statements that look like assertions
    ImmutableList<Fix> fixes =
        Lists.reverse(suffix).stream()
            .filter(t -> !JUnitMatchers.containsTestMethod(t))
            .map(t -> baseFix.build(ImmutableList.of(t)))
            .collect(toImmutableList());
    if (fixes.isEmpty()) {
      fixes = ImmutableList.of(baseFix.build(ImmutableList.of(getLast(suffix))));
    }
    return buildDescription(tree).addAllFixes(fixes).build();
  }
}
