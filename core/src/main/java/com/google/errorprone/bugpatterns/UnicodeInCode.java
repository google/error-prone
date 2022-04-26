/*
 * Copyright 2021 The Error Prone Authors.
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
import static com.google.common.collect.Streams.concat;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ErrorProneTokens.getTokens;
import static java.lang.String.format;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableRangeSet;
import com.google.common.collect.Range;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.CompilationUnitTreeMatcher;
import com.google.errorprone.fixes.FixedPosition;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ErrorProneToken;
import com.google.errorprone.util.SourceCodeEscapers;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.tools.javac.parser.Tokens.TokenKind;
import java.util.LinkedHashMap;
import java.util.Map;

/** Bans using non-ASCII Unicode characters outside string literals and comments. */
@BugPattern(
    severity = ERROR,
    summary =
        "Avoid using non-ASCII Unicode characters outside of comments and literals, as they can be"
            + " confusing.")
public final class UnicodeInCode extends BugChecker implements CompilationUnitTreeMatcher {
  @Override
  public Description matchCompilationUnit(CompilationUnitTree tree, VisitorState state) {
    ImmutableRangeSet<Integer> commentsAndLiterals = commentsAndLiterals(state);

    Map<Integer, Character> violations = new LinkedHashMap<>();

    CharSequence sourceCode = state.getSourceCode();

    for (int i = 0; i < sourceCode.length(); ++i) {
      char c = sourceCode.charAt(i);

      if (!isAcceptableAscii(c) && !commentsAndLiterals.contains(i)) {
        violations.put(i, c);
      }
    }

    if (violations.isEmpty()) {
      return NO_MATCH;
    }

    ImmutableRangeSet<Integer> suppressedRegions = suppressedRegions(state);

    for (var e : violations.entrySet()) {
      int violatingLocation = e.getKey();
      char c = e.getValue();
      if (!suppressedRegions.contains(violatingLocation)) {
        state.reportMatch(
            buildDescription(new FixedPosition(tree, violatingLocation))
                .setMessage(
                    format(
                        "Avoid using non-ASCII Unicode character (%s) outside of comments and"
                            + " literals, as they can be confusing.",
                        SourceCodeEscapers.javaCharEscaper().escape(Character.toString(c))))
                .build());
      }
    }
    return NO_MATCH;
  }

  private static boolean isAcceptableAscii(char c) {
    return (c >= 0x20 && c <= 0x7E) || c == '\n' || c == '\r' || c == '\t';
  }

  private static ImmutableRangeSet<Integer> commentsAndLiterals(VisitorState state) {
    ImmutableList<ErrorProneToken> tokens =
        getTokens(state.getSourceCode().toString(), state.context);
    return ImmutableRangeSet.unionOf(
        concat(
                tokens.stream()
                    .filter(
                        t ->
                            t.kind().equals(TokenKind.STRINGLITERAL)
                                || t.kind().equals(TokenKind.CHARLITERAL))
                    .map(t -> Range.closed(t.pos(), t.endPos())),
                tokens.stream()
                    .flatMap(t -> t.comments().stream())
                    .map(
                        c ->
                            Range.closed(
                                c.getSourcePos(0), c.getSourcePos(0) + c.getText().length())))
            .collect(toImmutableList()));
  }
}
