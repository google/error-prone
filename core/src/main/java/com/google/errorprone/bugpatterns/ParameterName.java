/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

import static com.google.common.collect.Iterables.getLast;
import static com.google.common.collect.Streams.forEachPair;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.sun.tools.javac.parser.Tokens.Comment.CommentStyle.BLOCK;

import com.google.common.collect.Range;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.NewClassTreeMatcher;
import com.google.errorprone.bugpatterns.argumentselectiondefects.NamedParameterComment;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.google.errorprone.util.Comments;
import com.google.errorprone.util.ErrorProneToken;
import com.google.errorprone.util.ErrorProneTokens;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.parser.Tokens.Comment;
import com.sun.tools.javac.tree.JCTree;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.regex.Matcher;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
  name = "ParameterName",
  summary =
      "Detects `/* name= */`-style comments on actual parameters where the name doesn't match the"
          + " formal parameter",
  severity = WARNING
)
public class ParameterName extends BugChecker
    implements MethodInvocationTreeMatcher, NewClassTreeMatcher {

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    checkArguments(tree, tree.getArguments(), state);
    return NO_MATCH;
  }

  @Override
  public Description matchNewClass(NewClassTree tree, VisitorState state) {
    checkArguments(tree, tree.getArguments(), state);
    return NO_MATCH;
  }

  private void checkArguments(
      Tree tree, List<? extends ExpressionTree> arguments, VisitorState state) {
    if (arguments.isEmpty()) {
      return;
    }
    MethodSymbol sym = (MethodSymbol) ASTHelpers.getSymbol(tree);
    if (NamedParameterComment.containsSyntheticParameterName(sym)) {
      return;
    }
    int start = ((JCTree) tree).getStartPosition();
    int end = state.getEndPosition(getLast(arguments));
    String source = state.getSourceCode().subSequence(start, end).toString();
    if (!source.contains("/*")) {
      // fast path if the arguments don't contain anything that looks like a comment
      return;
    }
    Deque<ErrorProneToken> tokens =
        new ArrayDeque<>(ErrorProneTokens.getTokens(source, state.context));
    forEachPair(
        sym.getParameters().stream(),
        arguments.stream(),
        (p, a) -> {
          while (!tokens.isEmpty()
              && ((start + tokens.peekFirst().pos()) < ((JCTree) a).getStartPosition())) {
            tokens.removeFirst();
          }
          if (tokens.isEmpty()) {
            return;
          }
          Range<Integer> argRange =
              Range.closedOpen(((JCTree) a).getStartPosition(), state.getEndPosition(a));
          if (!argRange.contains(start + tokens.peekFirst().pos())) {
            return;
          }
          checkArgument(p, a, start, tokens.removeFirst(), state);
        });
  }

  private void checkArgument(
      VarSymbol formal,
      ExpressionTree actual,
      int start,
      ErrorProneToken token,
      VisitorState state) {
    List<Comment> matches = new ArrayList<>();
    for (Comment comment : token.comments()) {
      if (comment.getStyle() != BLOCK) {
        continue;
      }
      Matcher m =
          NamedParameterComment.PARAMETER_COMMENT_PATTERN.matcher(
              Comments.getTextFromComment(comment));
      if (!m.matches()) {
        continue;
      }
      String name = m.group(1);
      if (formal.getSimpleName().contentEquals(name)) {
        // If there are multiple comments, bail if any one of them is an exact match.
        return;
      }
      matches.add(comment);
    }
    for (Comment match : matches) {
      state.reportMatch(
          buildDescription(actual)
              .setMessage(
                  String.format(
                      "%s does not match parameter name '%s'",
                      match.getText(), formal.getSimpleName()))
              .addFix(
                  SuggestedFix.replace(
                      start + match.getSourcePos(0),
                      start + match.getSourcePos(match.getText().length() - 1) + 1,
                      String.format("/* %s= */", formal.getSimpleName())))
              .build());
    }
  }
}
