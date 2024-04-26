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

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.matchers.Matchers.staticMethod;
import static com.google.errorprone.util.ASTHelpers.getReceiver;

import com.google.common.collect.ImmutableList;
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

  private static final Matcher<ExpressionTree> FORMATTED =
      instanceMethod().onExactClass("java.lang.String").named("formatted");

  private static final Pattern SPECIFIER_ALLOW_LIST_REGEX = Pattern.compile("%(d|s|S|c|b|B)");

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (STRING_FORMAT_METHOD_MATCHER.matches(tree, state)) {
      ImmutableList<ExpressionTree> arguments =
          tree.getArguments().stream().skip(1).collect(toImmutableList());
      if (shouldRefactorStringFormat(tree.getArguments().get(0), arguments)) {
        return describeMatch(
            tree,
            SuggestedFix.replace(
                tree, getFormattedUnifiedString(tree.getArguments().get(0), arguments)));
      }
    }
    if (FORMATTED.matches(tree, state)) {
      if (shouldRefactorStringFormat(getReceiver(tree), tree.getArguments())) {
        return describeMatch(
            tree,
            SuggestedFix.replace(
                tree, getFormattedUnifiedString(getReceiver(tree), tree.getArguments())));
      }
    }
    return Description.NO_MATCH;
  }

  /**
   * Should only format String.format() invocations where all the arguments are literals (format
   * string included). Format strings (first argument) as variables or constants are excluded from
   * refactoring. The refactoring also has an allowlist of "non trivial" formatting specifiers. This
   * is done since there are some instances where the String.format() invocation is justified even
   * with a CONSTANT but non-literal format string.
   */
  private static boolean shouldRefactorStringFormat(
      ExpressionTree formatString, List<? extends ExpressionTree> arguments) {
    if (!(formatString instanceof LiteralTree)
        || !arguments.stream().allMatch(argumentTree -> argumentTree instanceof LiteralTree)) {
      return false;
    }
    return onlyContainsSpecifiersInAllowList((String) ((LiteralTree) formatString).getValue());
  }

  private static boolean onlyContainsSpecifiersInAllowList(String formatString) {
    String noSpecifierFormatBase = SPECIFIER_ALLOW_LIST_REGEX.matcher(formatString).replaceAll("");
    // If it still has a specifier after the replacement, it means that it was not on the allowlist.
    return !noSpecifierFormatBase.contains("%");
  }

  /**
   * Formats the string originally on the String.format to be a unified string with all the literal
   * parameters, when available.
   */
  private static String getFormattedUnifiedString(
      ExpressionTree formatString, List<? extends ExpressionTree> arguments) {
    String unescapedFormatString =
        String.format(
            (String) ((LiteralTree) formatString).getValue(),
            arguments.stream()
                .map(literalTree -> ((LiteralTree) literalTree).getValue())
                .toArray(Object[]::new));
    return '"' + SourceCodeEscapers.javaCharEscaper().escape(unescapedFormatString) + '"';
  }
}
