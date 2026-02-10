/*
 * Copyright 2026 The Error Prone Authors.
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
import static com.google.errorprone.util.ASTHelpers.getStartPosition;
import static com.google.errorprone.util.ASTHelpers.hasExplicitSource;
import static com.google.errorprone.util.ASTHelpers.isEnumConstant;

import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.EmptyStatementTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ErrorProneToken;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.EmptyStatementTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.parser.Tokens;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    summary = "Unnecessary semicolons should be omitted. For empty block statements, prefer {}.",
    severity = WARNING)
public final class UnnecessarySemicolon extends BugChecker
    implements EmptyStatementTreeMatcher, ClassTreeMatcher {

  @Override
  public Description matchEmptyStatement(EmptyStatementTree tree, VisitorState state) {
    if (state.getPath().getParentPath().getLeaf() instanceof StatementTree parent) {
      switch (parent.getKind()) {
        case BLOCK, CASE -> {}
        default -> {
          // Don't remove empty statements that aren't inside blocks, to avoid rewriting e.g.:
          // while (true) ;
          return describeMatch(tree, SuggestedFix.replace(tree, "{}"));
        }
      }
    }
    return describeMatch(
        tree,
        deleteWithLeadingWhitespace(getStartPosition(tree), state.getEndPosition(tree), state));
  }

  @Override
  public Description matchClass(ClassTree tree, VisitorState state) {
    for (int i = 0; i < tree.getMembers().size(); i++) {
      Tree current = tree.getMembers().get(i);
      if (!hasExplicitSource(current, state)) {
        continue;
      }
      int from = state.getEndPosition(current);
      int to;
      if (i == (tree.getMembers().size() - 1)) {
        if (isEnumConstant(current)) {
          // Tolerate extra ; after a list of enum constants
          // enum E { ONE; }
          continue;
        }
        to = state.getEndPosition(tree);
      } else {
        Tree next = tree.getMembers().get(i + 1);
        if (!hasExplicitSource(next, state)) {
          continue;
        }
        if (isEnumConstant(current) && !isEnumConstant(next)) {
          // semicolons are required between enum constants and other declarations:
          // enum E {
          //   ONE;
          //   int x;
          // }
          continue;
        }
        to = getStartPosition(next);
      }
      ImmutableList<ErrorProneToken> tokens = state.getOffsetTokens(from, to);
      for (ErrorProneToken token : tokens) {
        if (token.kind() == Tokens.TokenKind.SEMI) {
          state.reportMatch(
              describeMatch(
                  current, deleteWithLeadingWhitespace(token.pos(), token.endPos(), state)));
        }
      }
    }
    return NO_MATCH;
  }

  private static final CharMatcher WHITESPACE = CharMatcher.whitespace();

  // google-java-format partial formatting doesn't handle deletion-only changes,
  // so remove leading whitespace as well.
  private static SuggestedFix deleteWithLeadingWhitespace(int start, int end, VisitorState state) {
    CharSequence source = state.getSourceCode();
    while (WHITESPACE.matches(source.charAt(start - 1))) {
      start--;
    }
    return SuggestedFix.replace(start, end, "");
  }
}
