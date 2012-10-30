/*
 * Copyright 2012 Google Inc. All Rights Reserved.
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

import static com.google.errorprone.BugPattern.Category.GUAVA;
import static com.google.errorprone.BugPattern.MaturityLevel.PROPOSED;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.DescribingMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.util.List;

/**
 * @author Louis Wasserman
 */
@BugPattern(name = "PreconditionsTooManyArgs",
    summary = "Precondition check format string expects fewer arguments",
    explanation = "The Guava Preconditions checks expect error messages to use %s as a "
        + "placeholder, and to take the corresponding number of arguments.  This bug can indicate "
        + "an improper format string, or simply forgetting to add all the arguments.",
    category = GUAVA, maturity = PROPOSED, severity = ERROR)
public class PreconditionsTooManyArgs extends DescribingMatcher<MethodInvocationTree> {

  @SuppressWarnings("unchecked")
  private static final
      Matcher<MethodInvocationTree> PRECONDITIONS_CHECK = Matchers.methodSelect(
          Matchers.<ExpressionTree>anyOf(
            Matchers.staticMethod("com.google.common.base.Preconditions", "checkArgument"),
            Matchers.staticMethod("com.google.common.base.Preconditions", "checkNotNull"),
            Matchers.staticMethod("com.google.common.base.Preconditions", "checkState")));

  private static int expectedArguments(String formatString) {
    int count = 0;
    for (int i = formatString.indexOf("%s"); i != -1; i = formatString.indexOf("%s", i + 1)) {
      count++;
    }
    return count;
  }

  @Override
  public boolean matches(MethodInvocationTree t, VisitorState state) {
    if (PRECONDITIONS_CHECK.matches(t, state) && t.getArguments().size() >= 2
        && t.getArguments().get(1) instanceof LiteralTree) {
      LiteralTree formatStringTree = (LiteralTree) t.getArguments().get(1);
      if (formatStringTree.getValue() instanceof String) {
        String formatString = (String) formatStringTree.getValue();
        int expectedArgs = expectedArguments(formatString);
        return expectedArgs < t.getArguments().size() - 2;
      }
    }
    return false;
  }

  /**
   * Matches most {@code java.util.Formatter} and {@code java.text.MessageFormat}
   * format placeholders, other than %s itself.
   *
   * This does not need to be completely exhaustive, since it is only used to suggest fixes.
   */
  private static final String BAD_PLACEHOLDER_REGEX = "%(?:\\d+\\$)??[dbBhHScCdoxXeEfgGaAtTn]|" +
      "\\{\\d+\\}";

  @Override
  public Description describe(MethodInvocationTree t, VisitorState state) {
    LiteralTree formatTree = (LiteralTree) t.getArguments().get(1);
    String formatString = (String) formatTree.getValue();

    String fixedFormatString = formatString.replaceAll(BAD_PLACEHOLDER_REGEX, "%s");
    SuggestedFix fix = new SuggestedFix();
    if (expectedArguments(fixedFormatString) == t.getArguments().size() - 2) {
      fix.replace(formatTree, "\"" + fixedFormatString + "\"");
      return new Description(formatTree, diagnosticMessage, fix);
    } else {
      fix.replace(t, state.getTreeMaker()
          .App((JCExpression) t.getMethodSelect(), List.of((JCExpression) t.getArguments().get(0)))
          .toString());
      return new Description(t, diagnosticMessage, fix);
    }
  }

  public static class Scanner extends com.google.errorprone.Scanner {
    public DescribingMatcher<MethodInvocationTree> matcher = new PreconditionsTooManyArgs();

    @Override
    public Void visitMethodInvocation(MethodInvocationTree node, VisitorState visitorState) {
      evaluateMatch(node, visitorState, matcher);
      return super.visitMethodInvocation(node, visitorState);
    }
  }
}
