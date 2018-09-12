/*
 * Copyright 2017 The Error Prone Authors.
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
import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.tree.JCTree;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
    name = "BooleanParameter",
    summary = "Use parameter comments to document ambiguous literals",
    severity = SUGGESTION,
    providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION)
public class BooleanParameter extends BugChecker
    implements MethodInvocationTreeMatcher, NewClassTreeMatcher {

  private static final ImmutableSet<String> EXCLUDED_NAMES =
      ImmutableSet.of("default", "defValue", "defaultValue", "value");

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    handleArguments(tree, tree.getArguments(), state);
    return NO_MATCH;
  }

  @Override
  public Description matchNewClass(NewClassTree tree, VisitorState state) {
    handleArguments(tree, tree.getArguments(), state);
    return NO_MATCH;
  }

  private void handleArguments(
      Tree tree, List<? extends ExpressionTree> arguments, VisitorState state) {
    if (arguments.size() < 2 && areSingleArgumentsSelfDocumenting(tree)) {
      // single-argument methods are often self-documenting
      return;
    }
    if (arguments.stream().noneMatch(BooleanParameter::isBooleanLiteral)) {
      return;
    }
    MethodSymbol sym = (MethodSymbol) ASTHelpers.getSymbol(tree);
    if (NamedParameterComment.containsSyntheticParameterName(sym)) {
      return;
    }
    int start = ((JCTree) tree).getStartPosition();
    int end = state.getEndPosition(getLast(arguments));
    String source = state.getSourceCode().subSequence(start, end).toString();
    Deque<ErrorProneToken> tokens =
        new ArrayDeque<>(ErrorProneTokens.getTokens(source, state.context));
    forEachPair(
        sym.getParameters().stream(),
        arguments.stream(),
        (p, c) -> checkParameter(p, c, start, tokens, state));
  }

  private void checkParameter(
      VarSymbol paramSym,
      ExpressionTree a,
      int start,
      Deque<ErrorProneToken> tokens,
      VisitorState state) {
    if (!isBooleanLiteral(a)) {
      return;
    }
    if (state.getTypes().unboxedTypeOrType(paramSym.type).getTag() != TypeTag.BOOLEAN) {
      // don't suggest on non-boolean (e.g., generic) parameters)
      return;
    }
    String name = paramSym.getSimpleName().toString();
    if (name.length() < 2) {
      // single-character parameter names aren't helpful
      return;
    }
    if (EXCLUDED_NAMES.contains(name)) {
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
    return token.comments().stream()
        .filter(c -> c.getStyle() == BLOCK)
        .anyMatch(
            c ->
                NamedParameterComment.PARAMETER_COMMENT_PATTERN
                    .matcher(Comments.getTextFromComment(c))
                    .matches());
  }

  private static boolean isBooleanLiteral(ExpressionTree tree) {
    return tree.getKind() == Kind.BOOLEAN_LITERAL;
  }

  private static boolean areSingleArgumentsSelfDocumenting(Tree tree) {
    // Consider single-argument booleans for classes whose names contain "Boolean" to be self-
    // documenting. This is aimed at classes like AtomicBoolean which simply wrap a value.
    if (tree instanceof NewClassTree) {
      return ((NewClassTree) tree).getIdentifier().toString().toLowerCase().contains("boolean");
    }
    return true;
  }
}
