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
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.JUnitMatchers;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.StatementTree;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import java.util.List;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
    name = "TestExceptionChecker",
    category = JUNIT,
    summary =
        "Using @Test(expected=...) is discouraged, since the test will pass if *any* statement in"
            + " the test method throws the expected exception",
    severity = WARNING,
    tags = StandardTags.FRAGILE_CODE,
    providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION)
public class TestExceptionChecker extends AbstractTestExceptionChecker {

  @Override
  protected Description handleStatements(
      MethodTree tree, VisitorState state, JCExpression expectedException, SuggestedFix baseFix) {
    List<? extends StatementTree> statements = tree.getBody().getStatements();
    if (statements.size() == 1) {
      return NO_MATCH;
    }
    Description.Builder description = buildDescription(tree);
    List<SuggestedFix> fixes =
        Lists.reverse(statements).stream()
            .filter(t -> !JUnitMatchers.containsTestMethod(t))
            .map(
                s ->
                    buildFix(
                        state,
                        SuggestedFix.builder().merge(baseFix),
                        expectedException,
                        ImmutableList.of(s)))
            .collect(toImmutableList());
    if (!fixes.isEmpty()) {
      description.addAllFixes(fixes);
    } else {
      description.addFix(
          buildFix(
              state,
              SuggestedFix.builder().merge(baseFix),
              expectedException,
              ImmutableList.of(getLast(statements))));
    }
    return description.build();
  }
}
