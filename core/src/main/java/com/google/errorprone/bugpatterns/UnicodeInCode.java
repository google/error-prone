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
import static com.google.errorprone.util.SourceCodeEscapers.javaCharEscaper;
import static java.lang.String.format;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableRangeSet;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.CompilationUnitTreeMatcher;
import com.google.errorprone.fixes.FixedPosition;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ErrorProneToken;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.tools.javac.parser.Tokens.TokenKind;

/** Bans using non-ASCII Unicode characters outside string literals and comments. */
@BugPattern(
    severity = ERROR,
    summary =
        "Avoid using non-ASCII Unicode characters outside of comments and literals, as they can be"
            + " confusing.")
public final class UnicodeInCode extends BugChecker implements CompilationUnitTreeMatcher {
  @Override
  public Description matchCompilationUnit(CompilationUnitTree tree, VisitorState state) {
    RangeSet<Integer> violations = TreeRangeSet.create();
    String sourceCode = state.getSourceCode().toString();

    for (int i = 0; i < sourceCode.length(); ++i) {
      if (!isAcceptableAscii(sourceCode, i)) {
        violations.add(Range.closedOpen(i, i + 1));
      }
    }

    if (violations.isEmpty()) {
      return NO_MATCH;
    }

    ImmutableRangeSet<Integer> permissibleUnicodeRegions =
        suppressedRegions(state).union(commentsAndLiterals(state, sourceCode));

    for (var range : violations.asDescendingSetOfRanges()) {
      if (!permissibleUnicodeRegions.encloses(range)) {
        state.reportMatch(
            buildDescription(new FixedPosition(tree, range.lowerEndpoint()))
                .setMessage(
                    format(
                        "Avoid using non-ASCII Unicode character (%s) outside of comments and"
                            + " literals, as they can be confusing.",
                        javaCharEscaper()
                            .escape(
                                sourceCode.substring(
                                    range.lowerEndpoint(), range.upperEndpoint()))))
                .build());
      }
    }
    return NO_MATCH;
  }

  private static boolean isAcceptableAscii(String sourceCode, int i) {
    char c = sourceCode.charAt(i);
    if (isAcceptableAscii(c)) {
      return true;
    }
    if (c == 0x1a && i == sourceCode.length() - 1) {
      // javac inserts ASCII_SUB characters at the end of the input, see:
      // https://github.com/google/error-prone/issues/3092
      return true;
    }
    return false;
  }

  private static boolean isAcceptableAscii(char c) {
    return (c >= 0x20 && c <= 0x7E) || c == '\n' || c == '\r' || c == '\t';
  }

  private static ImmutableRangeSet<Integer> commentsAndLiterals(VisitorState state, String source) {
    ImmutableList<ErrorProneToken> tokens = getTokens(source, state.context);
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
