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
import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.sun.tools.javac.parser.Tokens.Comment.CommentStyle.BLOCK;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Range;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.argumentselectiondefects.NamedParameterComment;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.google.errorprone.util.Comments;
import com.google.errorprone.util.ErrorProneToken;
import com.google.errorprone.util.ErrorProneTokens;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.tree.JCTree;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
  name = "BooleanParameter",
  summary = "Use parameter comments to document ambiguous literals",
  severity = SUGGESTION,
  providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION
)
public class BooleanParameter extends BugChecker implements MethodInvocationTreeMatcher {

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    List<? extends ExpressionTree> arguments = tree.getArguments();
    if (arguments.size() < 2) {
      // single-argument methods are often self-documentating
      return NO_MATCH;
    }
    if (arguments.stream().noneMatch(a -> a.getKind() == Kind.BOOLEAN_LITERAL)) {
      return NO_MATCH;
    }
    MethodSymbol sym = ASTHelpers.getSymbol(tree);
    if (NamedParameterComment.containsSyntheticParameterName(sym)) {
      return NO_MATCH;
    }
    int start = ((JCTree) tree).getStartPosition();
    int end = state.getEndPosition(getLast(tree.getArguments()));
    String source = state.getSourceCode().subSequence(start, end).toString();
    Deque<ErrorProneToken> tokens =
        new ArrayDeque<>(ErrorProneTokens.getTokens(source, state.context));
    forEachPair(
        sym.getParameters().stream(),
        arguments.stream(),
        (p, c) -> checkParameter(p, c, start, tokens, state));
    return NO_MATCH;
  }

  private static final ImmutableSet<String> BLACKLIST = ImmutableSet.of("value");

  private void checkParameter(
      VarSymbol paramSym,
      ExpressionTree a,
      int start,
      Deque<ErrorProneToken> tokens,
      VisitorState state) {
    if (a.getKind() != Kind.BOOLEAN_LITERAL) {
      return;
    }
    String name = paramSym.getSimpleName().toString();
    if (name.length() < 2) {
      // single-character parameter names aren't helpful
      return;
    }
    if (BLACKLIST.contains(name)) {
      return;
    }
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
    if (hasParameterComment(tokens.removeFirst())) {
      return;
    }
    state.reportMatch(
        describeMatch(
            a, SuggestedFix.prefixWith(a, String.format("/* %s= */", paramSym.getSimpleName()))));
  }

  private static boolean hasParameterComment(ErrorProneToken token) {
    return token
        .comments()
        .stream()
        .filter(c -> c.getStyle() == BLOCK)
        .anyMatch(
            c ->
                NamedParameterComment.PARAMETER_COMMENT_PATTERN
                    .matcher(Comments.getTextFromComment(c))
                    .matches());
  }
}
