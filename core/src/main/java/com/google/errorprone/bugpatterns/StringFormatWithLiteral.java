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
import static com.google.errorprone.matchers.Matchers.staticMethod;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.SourceCodeEscapers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodInvocationTree;
import java.util.List;
import java.util.regex.Pattern;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    summary = "There is no need to use String.format() when all the arguments are literals.",
    severity = WARNING)
public final class StringFormatWithLiteral extends BugChecker
    implements MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> STRING_FORMAT_METHOD_MATCHER =
      staticMethod().onClass("java.lang.String").named("format");

  private static final Pattern SPECIFIER_ALLOW_LIST_REGEX = Pattern.compile("%(d|s|S|c|b|B)");

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (STRING_FORMAT_METHOD_MATCHER.matches(tree, state) && shouldRefactorStringFormat(tree)) {
      return describeMatch(tree, refactor(tree));
    }
    return Description.NO_MATCH;
  }

  /**
   * Should only format String.format() invocations where all the arguments are literals (format
   * string included). Format strings (first argument) as variables or constants are excluded from
   * refactoring. The refactoring also has an allowlist of "non trivial" formatting specifiers. This
   * is done since there are some instances where the String.format() invocation is justified even
   * with
   */
  private static boolean shouldRefactorStringFormat(MethodInvocationTree tree) {
    if (!tree.getArguments().stream()
        .allMatch(argumentTree -> argumentTree instanceof LiteralTree)) {
      return false;
    }
    LiteralTree formatString = (LiteralTree) tree.getArguments().get(0);
    return onlyContainsSpecifiersInAllowList((String) formatString.getValue());
  }

  private static boolean onlyContainsSpecifiersInAllowList(String formatString) {
    String noSpecifierFormatBase = SPECIFIER_ALLOW_LIST_REGEX.matcher(formatString).replaceAll("");
    // If it still has a specifier after the replacement, it means that it was not on the allowlist.
    return !noSpecifierFormatBase.contains("%");
  }

  private static SuggestedFix refactor(MethodInvocationTree tree) {
    return SuggestedFix.replace(
        tree, getFormattedUnifiedString(getFormatString(tree), tree.getArguments()));
  }

  /**
   * Formats the string originally on the String.format to be a unified string with all the literal
   * parameters, when available.
   */
  private static String getFormattedUnifiedString(
      String formatString, List<? extends ExpressionTree> arguments) {
    String unescapedFormatString =
        String.format(
            formatString,
            arguments.stream()
                .skip(1) // skip the format string argument.
                .map(literallTree -> ((LiteralTree) literallTree).getValue())
                .toArray(Object[]::new));
    return '"' + SourceCodeEscapers.javaCharEscaper().escape(unescapedFormatString) + '"';
  }

  private static String getFormatString(MethodInvocationTree tree) {
    LiteralTree formatStringTree = (LiteralTree) tree.getArguments().get(0);
    return formatStringTree.getValue().toString();
  }
}
