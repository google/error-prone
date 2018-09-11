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
package com.google.errorprone.util;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import com.google.common.collect.TreeRangeSet;
import com.google.errorprone.VisitorState;
import com.google.errorprone.util.Commented.Position;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.parser.Tokens.Comment;
import com.sun.tools.javac.parser.Tokens.TokenKind;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Position.LineMap;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

/**
 * Utilities for attaching comments to relevant AST nodes
 *
 * @author andrewrice@google.com (Andrew Rice)
 */
public class Comments {

  /**
   * Attach comments to nodes on arguments of constructor calls. Calls such as {@code new Test(
   * param1 /* c1 *&#47;, /* c2 *&#47; param2)} will attach the comment c1 to {@code param1} and the
   * comment c2 to {@code param2}.
   *
   * <p>Warning: this is expensive to compute as it involves re-tokenizing the source for this node.
   *
   * <p>Currently this method will only tokenize the source code of the method call itself. However,
   * the source positions in the returned {@code Comment} objects are adjusted so that they are
   * relative to the whole file.
   */
  public static ImmutableList<Commented<ExpressionTree>> findCommentsForArguments(
      NewClassTree newClassTree, VisitorState state) {
    int startPosition = ((JCTree) newClassTree).getStartPosition();
    return findCommentsForArguments(
        newClassTree, newClassTree.getArguments(), startPosition, state);
  }

  /**
   * Attach comments to nodes on arguments of method calls. Calls such as {@code test(param1 /* c1
   * *&#47;, /* c2 *&#47; param2)} will attach the comment c1 to {@code param1} and the comment c2
   * to {@code param2}.
   *
   * <p>Warning: this is expensive to compute as it involves re-tokenizing the source for this node
   *
   * <p>Currently this method will only tokenize the source code of the method call itself. However,
   * the source positions in the returned {@code Comment} objects are adjusted so that they are
   * relative to the whole file.
   */
  public static ImmutableList<Commented<ExpressionTree>> findCommentsForArguments(
      MethodInvocationTree methodInvocationTree, VisitorState state) {
    int startPosition = state.getEndPosition(methodInvocationTree.getMethodSelect());
    return findCommentsForArguments(
        methodInvocationTree, methodInvocationTree.getArguments(), startPosition, state);
  }

  /**
   * Extract the text body from a comment.
   *
   * <p>This currently includes asterisks that start lines in the body of block comments. Do not
   * rely on this behaviour.
   *
   * <p>TODO(andrewrice) Update this method to handle block comments properly if we find the need
   */
  public static String getTextFromComment(Comment comment) {
    switch (comment.getStyle()) {
      case BLOCK:
        return comment.getText().replaceAll("^\\s*/\\*\\s*(.*?)\\s*\\*/\\s*", "$1");
      case LINE:
        return comment.getText().replaceAll("^\\s*//\\s*", "");
      default:
        return comment.getText();
    }
  }

  private static ImmutableList<Commented<ExpressionTree>> findCommentsForArguments(
      Tree tree, List<? extends ExpressionTree> arguments, int startPosition, VisitorState state) {

    if (arguments.isEmpty()) {
      return ImmutableList.of();
    }

    CharSequence sourceCode = state.getSourceCode();
    Optional<Integer> endPosition = computeEndPosition(tree, sourceCode, state);
    if (!endPosition.isPresent()) {
      return noComments(arguments);
    }

    CharSequence source = sourceCode.subSequence(startPosition, endPosition.get());

    if (CharMatcher.is('/').matchesNoneOf(source)) {
      return noComments(arguments);
    }

    // The token position of the end of the method invocation
    int invocationEnd = state.getEndPosition(tree) - startPosition;

    // Ignore comments nested inside arguments.
    TreeRangeSet<Integer> exclude = TreeRangeSet.create();
    arguments.forEach(
        a -> exclude.add(Range.closed(((JCTree) a).getStartPosition(), state.getEndPosition(a))));

    ErrorProneTokens errorProneTokens = new ErrorProneTokens(source.toString(), state.context);
    ImmutableList<ErrorProneToken> tokens = errorProneTokens.getTokens();
    LineMap lineMap = errorProneTokens.getLineMap();

    ArgumentTracker argumentTracker = new ArgumentTracker(arguments, startPosition, state, lineMap);
    TokenTracker tokenTracker = new TokenTracker(lineMap);

    argumentTracker.advance();
    for (ErrorProneToken token : tokens) {
      tokenTracker.advance(token);
      if (tokenTracker.atStartOfLine() && !tokenTracker.wasPreviousLineEmpty()) {
        // if the token is at the start of a line it could still have a comment attached which was
        // on the previous line
        for (Comment c : token.comments()) {
          if (exclude.intersects(Range.closedOpen(token.pos(), token.endPos()))) {
            continue;
          }
          if (tokenTracker.isCommentOnPreviousLine(c)
              && token.pos() <= argumentTracker.currentArgumentStartPosition
              && argumentTracker.isPreviousArgumentOnPreviousLine()) {
            // token was on the previous line so therefore we should add it to the previous comment
            // unless the previous argument was not on the previous line with it
            argumentTracker.addCommentToPreviousArgument(c, Position.ANY);
          } else {
            // if the comment comes after the end of the invocation and its not on the same line
            // as the final argument then we need to ignore it
            if (c.getSourcePos(0) <= invocationEnd
                || lineMap.getLineNumber(c.getSourcePos(0))
                    <= lineMap.getLineNumber(argumentTracker.currentArgumentEndPosition)) {
              argumentTracker.addCommentToCurrentArgument(c, Position.ANY);
            }
          }
        }
      } else {
        // we add all the before-comments from the first token of the argument
        // we add all the after-comments from the last token of the argument
        if (token.pos() == argumentTracker.currentArgumentStartPosition) {
          argumentTracker.addAllCommentsToCurrentArgument(token.comments(), Position.BEFORE);
        }
        if (token.endPos() > argumentTracker.currentArgumentEndPosition) {
          argumentTracker.addAllCommentsToCurrentArgument(token.comments(), Position.AFTER);
        }
      }
      if (token.pos() >= argumentTracker.currentArgumentEndPosition) {
        // We are between arguments so wait for a (lexed) comma to delimit them
        if (token.kind() == TokenKind.COMMA) {
          if (!argumentTracker.hasMoreArguments()) {
            break;
          }
          argumentTracker.advance();
        }
      }
    }

    return argumentTracker.build();
  }

  private static ImmutableList<Commented<ExpressionTree>> noComments(
      List<? extends ExpressionTree> arguments) {
    return arguments.stream()
        .map(a -> Commented.<ExpressionTree>builder().setTree(a).build())
        .collect(toImmutableList());
  }

  /**
   * Finds the end position of this MethodInvocationTree. This is complicated by the fact that
   * sometimes a comment will fall outside of the bounds of the tree.
   *
   * <p>For example:
   *
   * <pre>
   *   test(arg1,  // comment1
   *        arg2); // comment2
   *   int i;
   * </pre>
   *
   * In this case {@code comment2} lies beyond the end of the invocation tree. In order to get the
   * comment we need the tokenizer to lex the token which follows the invocation ({@code int} in the
   * example).
   *
   * <p>We over-approximate this end position by looking for the next node in the AST and using the
   * end position of this node.
   *
   * <p>As a heuristic we first scan for any {@code /} characters on the same line as the end of the
   * method invocation. If we don't find any then we use the end of the method invocation as the end
   * position.
   *
   * @return the end position of the tree or Optional.empty if we are unable to calculate it
   */
  @VisibleForTesting
  static Optional<Integer> computeEndPosition(
      Tree methodInvocationTree, CharSequence sourceCode, VisitorState state) {
    int invocationEnd = state.getEndPosition(methodInvocationTree);
    if (invocationEnd == -1) {
      return Optional.empty();
    }

    // Finding a good end position is expensive so first check whether we have any comment at
    // the end of our line. If we don't then we can just use the end of the methodInvocationTree
    int nextNewLine = CharMatcher.is('\n').indexIn(sourceCode, invocationEnd);
    if (nextNewLine == -1) {
      return Optional.of(invocationEnd);
    }

    if (CharMatcher.is('/').matchesNoneOf(sourceCode.subSequence(invocationEnd, nextNewLine))) {
      return Optional.of(invocationEnd);
    }

    int nextNodeEnd = state.getEndPosition(getNextNodeOrParent(methodInvocationTree, state));
    if (nextNodeEnd == -1) {
      return Optional.of(invocationEnd);
    }

    return Optional.of(nextNodeEnd);
  }

  /**
   * Find the node which (approximately) follows this one in the tree. This works by walking upwards
   * to find enclosing block (or class) and then looking for the node after the subtree we walked.
   * If our subtree is the last of the block then we return the node for the block instead, if we
   * can't find a suitable block we return the parent node.
   */
  private static Tree getNextNodeOrParent(Tree current, VisitorState state) {
    Tree predecessorNode = current;
    TreePath enclosingPath = state.getPath();
    while (enclosingPath != null
        && !(enclosingPath.getLeaf() instanceof BlockTree)
        && !(enclosingPath.getLeaf() instanceof ClassTree)) {
      predecessorNode = enclosingPath.getLeaf();
      enclosingPath = enclosingPath.getParentPath();
    }

    if (enclosingPath == null) {
      return state.getPath().getParentPath().getLeaf();
    }

    Tree parent = enclosingPath.getLeaf();

    if (parent instanceof BlockTree) {
      return after(predecessorNode, ((BlockTree) parent).getStatements(), parent);
    } else if (parent instanceof ClassTree) {
      return after(predecessorNode, ((ClassTree) parent).getMembers(), parent);
    }
    return parent;
  }

  /**
   * Find the element in the iterable following the target
   *
   * @param target is the element to search for
   * @param iterable is the iterable to search
   * @param defaultValue will be returned if there is no item following the searched for item
   * @return the item following {@code target} or {@code defaultValue} if not found
   */
  private static <T> T after(T target, Iterable<? extends T> iterable, T defaultValue) {
    Iterator<? extends T> iterator = iterable.iterator();
    while (iterator.hasNext()) {
      if (iterator.next().equals(target)) {
        break;
      }
    }

    if (iterator.hasNext()) {
      return iterator.next();
    }

    return defaultValue;
  }

  /** This class is used to keep track of state between lines of code when consuming tokens */
  private static class TokenTracker {

    private final LineMap lineMap;

    private int tokensOnCurrentLine = 0;
    private int currentLineNumber = -1;
    private boolean previousLineEmpty = true;

    TokenTracker(LineMap lineMap) {
      this.lineMap = lineMap;
    }

    void advance(ErrorProneToken token) {
      int line = lineMap.getLineNumber(token.pos());
      if (line != currentLineNumber) {
        currentLineNumber = line;
        previousLineEmpty = tokensOnCurrentLine == 0;
        tokensOnCurrentLine = 0;
      } else {
        tokensOnCurrentLine++;
      }
    }

    boolean isCommentOnPreviousLine(Comment c) {
      int tokenLine = lineMap.getLineNumber(c.getSourcePos(0));
      return tokenLine == currentLineNumber - 1;
    }

    boolean atStartOfLine() {
      return tokensOnCurrentLine == 0;
    }

    boolean wasPreviousLineEmpty() {
      return previousLineEmpty;
    }
  }

  /**
   * This class is used to keep track of the arguments we are processing. It keeps a window of the
   * current and previous argument as builders so that more comments can be added to them as we find
   * them. When we advance everything is shuffled down: the builder for the previous argument is
   * built and put in the final results list, the builder for the current argument is moved to
   * previous and a new builder is made for the next argument. We also track the positions of the
   * current and previous argument so that we know whether a comment occurred before or after it
   */
  private static class ArgumentTracker {

    private final VisitorState state;
    private final Iterator<? extends ExpressionTree> argumentsIterator;
    private final int offset;
    private final LineMap lineMap;

    private Commented.Builder<ExpressionTree> currentCommentedResultBuilder = null;
    private Commented.Builder<ExpressionTree> previousCommentedResultBuilder = null;
    private final ImmutableList.Builder<Commented<ExpressionTree>> resultBuilder =
        ImmutableList.builder();

    private int currentArgumentStartPosition = -1;
    private int currentArgumentEndPosition = -1;
    private int previousArgumentEndPosition = -1;

    ArgumentTracker(
        Iterable<? extends ExpressionTree> arguments,
        int offset,
        VisitorState state,
        LineMap lineMap) {
      this.state = state;
      this.offset = offset;
      this.argumentsIterator = arguments.iterator();
      this.lineMap = lineMap;
    }

    void advance() {
      ExpressionTree nextArgument = argumentsIterator.next();

      currentArgumentEndPosition = state.getEndPosition(nextArgument) - offset;
      previousArgumentEndPosition = currentArgumentStartPosition;
      currentArgumentStartPosition = ((JCTree) nextArgument).getStartPosition() - offset;

      if (previousCommentedResultBuilder != null) {
        resultBuilder.add(previousCommentedResultBuilder.build());
      }
      previousCommentedResultBuilder = currentCommentedResultBuilder;
      currentCommentedResultBuilder = Commented.<ExpressionTree>builder().setTree(nextArgument);
    }

    /** Returns the final result. The object should not be used after calling this method */
    ImmutableList<Commented<ExpressionTree>> build() {
      if (previousCommentedResultBuilder != null) {
        resultBuilder.add(previousCommentedResultBuilder.build());
      }

      if (currentCommentedResultBuilder != null) {
        resultBuilder.add(currentCommentedResultBuilder.build());
      }

      return resultBuilder.build();
    }

    boolean isPreviousArgumentOnPreviousLine() {
      return lineMap.getLineNumber(previousArgumentEndPosition)
          == lineMap.getLineNumber(currentArgumentStartPosition) - 1;
    }

    void addCommentToPreviousArgument(Comment c, Position position) {
      previousCommentedResultBuilder.addComment(c, previousArgumentEndPosition, offset, position);
    }

    void addCommentToCurrentArgument(Comment c, Position position) {
      currentCommentedResultBuilder.addComment(c, currentArgumentStartPosition, offset, position);
    }

    void addAllCommentsToCurrentArgument(Iterable<Comment> comments, Position position) {
      currentCommentedResultBuilder.addAllComment(
          comments, currentArgumentStartPosition, offset, position);
    }

    boolean hasMoreArguments() {
      return argumentsIterator.hasNext();
    }
  }
}
