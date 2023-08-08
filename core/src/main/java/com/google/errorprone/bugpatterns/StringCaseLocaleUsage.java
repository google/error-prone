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
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.google.errorprone.util.ASTHelpers.getReceiver;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.sun.tools.javac.parser.Tokens.TokenKind.RPAREN;

import com.google.common.collect.Streams;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.google.errorprone.util.ErrorProneTokens;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.util.Position;

/**
 * A {@link BugChecker} that flags calls to {@link String#toLowerCase()} and {@link
 * String#toUpperCase()}, as these methods implicitly rely on the environment's default locale.
 */
@BugPattern(
    summary =
        "Specify a `Locale` when calling `String#to{Lower,Upper}Case`. (Note: there are multiple"
            + " suggested fixes; the third may be most appropriate if you're dealing with ASCII"
            + " Strings.)",
    severity = WARNING)
public final class StringCaseLocaleUsage extends BugChecker implements MethodInvocationTreeMatcher {
  private static final Matcher<ExpressionTree> DEFAULT_LOCALE_CASE_CONVERSION =
      instanceMethod()
          .onExactClass(String.class.getName())
          .namedAnyOf("toLowerCase", "toUpperCase")
          .withNoParameters();

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!DEFAULT_LOCALE_CASE_CONVERSION.matches(tree, state)) {
      return Description.NO_MATCH;
    }

    int closingParenPosition = getClosingParenPosition(tree, state);
    if (closingParenPosition == Position.NOPOS) {
      return describeMatch(tree);
    }

    return buildDescription(tree)
        .addFix(suggestLocale(closingParenPosition, "Locale.ROOT"))
        .addFix(suggestLocale(closingParenPosition, "Locale.getDefault()"))
        .addFix(suggestAscii(tree, state))
        .build();
  }

  private static SuggestedFix suggestLocale(int insertPosition, String locale) {
    return SuggestedFix.builder()
        .addImport("java.util.Locale")
        .replace(insertPosition, insertPosition, locale)
        .build();
  }

  private static SuggestedFix suggestAscii(MethodInvocationTree tree, VisitorState state) {
    ExpressionTree receiver = getReceiver(tree);
    if (receiver == null) {
      return SuggestedFix.emptyFix();
    }
    var fix =
        SuggestedFix.builder()
            .setShortDescription(
                "Replace with Ascii.toLower/UpperCase; this changes behaviour for non-ASCII"
                    + " Strings");
    String ascii = SuggestedFixes.qualifyType(state, fix, "com.google.common.base.Ascii");
    fix.replace(
        tree,
        String.format(
            "%s.%s(%s)", ascii, getSymbol(tree).getSimpleName(), state.getSourceForNode(receiver)));
    return fix.build();
  }

  // TODO: Consider making this a helper method in `SuggestedFixes`.
  private static int getClosingParenPosition(MethodInvocationTree tree, VisitorState state) {
    int startPosition = ASTHelpers.getStartPosition(tree);
    if (startPosition == Position.NOPOS) {
      return Position.NOPOS;
    }

    return Streams.findLast(
            ErrorProneTokens.getTokens(state.getSourceForNode(tree), state.context).stream()
                .filter(t -> t.kind() == RPAREN))
        .map(token -> startPosition + token.pos())
        .orElse(Position.NOPOS);
  }
}
