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
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Matchers.staticMethod;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.tree.JCTree;
import java.util.regex.Pattern;

/** @author Louis Wasserman */
@BugPattern(
  name = "PreconditionsInvalidPlaceholder",
  summary = "Preconditions only accepts the %s placeholder in error message strings",
  explanation =
      "The Guava Preconditions checks take error message template strings that "
          + "look similar to format strings but only accept %s as a placeholder. This check "
          + "points out places where there is a non-%s placeholder in a Preconditions error "
          + "message template string and the number of arguments does not match the number of "
          + "%s placeholders.",
  category = GUAVA,
  severity = WARNING,
  tags = StandardTags.LIKELY_ERROR,
  providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION
)
public class PreconditionsInvalidPlaceholder extends BugChecker
    implements MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> PRECONDITIONS_CHECK =
      Matchers.<ExpressionTree>anyOf(
          staticMethod().onClass("com.google.common.base.Preconditions").named("checkArgument"),
          staticMethod().onClass("com.google.common.base.Preconditions").named("checkNotNull"),
          staticMethod().onClass("com.google.common.base.Preconditions").named("checkState"));

  private static int expectedArguments(String formatString) {
    int count = 0;
    for (int i = formatString.indexOf("%s"); i != -1; i = formatString.indexOf("%s", i + 1)) {
      count++;
    }
    return count;
  }

  @Override
  public Description matchMethodInvocation(MethodInvocationTree t, VisitorState state) {
    if (PRECONDITIONS_CHECK.matches(t, state)
        && t.getArguments().size() >= 2
        && t.getArguments().get(1) instanceof LiteralTree) {
      LiteralTree formatStringTree = (LiteralTree) t.getArguments().get(1);
      if (formatStringTree.getValue() instanceof String) {
        String formatString = (String) formatStringTree.getValue();
        int expectedArgs = expectedArguments(formatString);
        if (expectedArgs < t.getArguments().size() - 2
            && BAD_PLACEHOLDER_REGEX.matcher(formatString).find()) {
          return describe(t, state);
        }
      }
    }
    return Description.NO_MATCH;
  }

  /**
   * Matches most {@code java.util.Formatter} and {@code java.text.MessageFormat} format
   * placeholders, other than %s itself.
   *
   * <p>This does not need to be completely exhaustive, since it is only used to suggest fixes.
   */
  private static final Pattern BAD_PLACEHOLDER_REGEX =
      Pattern.compile("\\$s|%(?:\\d+\\$)??[dbBhHScCdoxXeEfgGaAtTn]|\\{\\d+\\}");

  public Description describe(MethodInvocationTree t, VisitorState state) {
    LiteralTree formatTree = (LiteralTree) t.getArguments().get(1);

    String fixedFormatString =
        BAD_PLACEHOLDER_REGEX.matcher(state.getSourceForNode((JCTree) formatTree)).replaceAll("%s");
    if (expectedArguments(fixedFormatString) == t.getArguments().size() - 2) {
      return describeMatch(formatTree, SuggestedFix.replace(formatTree, fixedFormatString));
    } else {
      int missing = t.getArguments().size() - 2 - expectedArguments(fixedFormatString);
      StringBuilder builder = new StringBuilder(fixedFormatString);
      builder.deleteCharAt(builder.length() - 1);
      builder.append(" [%s");
      for (int i = 1; i < missing; i++) {
        builder.append(", %s");
      }
      builder.append("]\"");
      return describeMatch(t, SuggestedFix.replace(formatTree, builder.toString()));
    }
  }
}
