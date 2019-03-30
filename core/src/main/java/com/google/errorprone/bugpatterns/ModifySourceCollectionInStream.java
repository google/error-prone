/*
 * Copyright 2019 The Error Prone Authors.
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
import static com.google.errorprone.matchers.Matchers.isSubtypeOf;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MemberReferenceTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import javax.annotation.Nullable;

/**
 * Identify the backing collection source of a stream and reports if the source is mutated during
 * the stream operations.
 *
 * @author deltazulu@google.com (Donald Duo Zhao)
 */
@BugPattern(
    name = "ModifySourceCollectionInStream",
    summary = "Modifying the backing source during stream operations may cause unintended results.",
    severity = WARNING)
public class ModifySourceCollectionInStream extends BugChecker
    implements MemberReferenceTreeMatcher, MethodInvocationTreeMatcher {

  private static final ImmutableList<String> STATE_MUTATION_METHOD_NAMES =
      ImmutableList.of("add", "addAll", "clear", "remove", "removeAll", "retainAll");

  private static final ImmutableList<String> STREAM_CREATION_METHOD_NAMES =
      ImmutableList.of("stream", "parallelStream");

  private static final Matcher<ExpressionTree> COLLECTION_TO_STREAM_MATCHER =
      instanceMethod()
          .onDescendantOf("java.util.Collection")
          .namedAnyOf(STREAM_CREATION_METHOD_NAMES);

  /** Covers common stream structures, including Stream, IntStream, LongStream, DoubleStream. */
  private static final Matcher<ExpressionTree> STREAM_API_INVOCATION_MATCHER =
      instanceMethod().onDescendantOfAny("java.util.stream.BaseStream");

  private static final Matcher<ExpressionTree> MUTATION_METHOD_MATCHER =
      instanceMethod()
          .onDescendantOf("java.util.Collection")
          .namedAnyOf(STATE_MUTATION_METHOD_NAMES);

  @Override
  public Description matchMemberReference(MemberReferenceTree tree, VisitorState state) {
    if (!isSubtypeOf("java.util.Collection").matches(tree.getQualifierExpression(), state)
        || STATE_MUTATION_METHOD_NAMES.stream()
            .noneMatch(methodName -> methodName.contentEquals(tree.getName()))) {
      return Description.NO_MATCH;
    }

    MethodInvocationTree methodInvocationTree = state.findEnclosing(MethodInvocationTree.class);
    return isStreamApiInvocationOnStreamSource(
            methodInvocationTree, ASTHelpers.getReceiver(tree), state)
        ? describeMatch(tree)
        : Description.NO_MATCH;
  }

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!MUTATION_METHOD_MATCHER.matches(tree, state)) {
      return Description.NO_MATCH;
    }

    // The enclosing method invocation of the method reference doesn't dereferenced an expression.
    // e.g. calling other methods defined in the same class.
    ExpressionTree mutatedReceiver = ASTHelpers.getReceiver(tree);
    if (mutatedReceiver == null) {
      return Description.NO_MATCH;
    }

    TreePath pathToLambdaExpression = state.findPathToEnclosing(LambdaExpressionTree.class);
    // Case for a method reference not enclosed in a lambda expression,
    // e.g. BiConsumer<ArrayList, Integer> biConsumer = ArrayList::add;
    if (pathToLambdaExpression == null) {
      return Description.NO_MATCH;
    }

    // Starting from the immediate enclosing method invocation of the lambda expression.
    Tree parentNode = pathToLambdaExpression.getParentPath().getLeaf();
    if (!(parentNode instanceof ExpressionTree)) {
      return Description.NO_MATCH;
    }

    return isStreamApiInvocationOnStreamSource((ExpressionTree) parentNode, mutatedReceiver, state)
        ? describeMatch(tree)
        : Description.NO_MATCH;
  }

  /**
   * Returns true if and only if the given MethodInvocationTree
   *
   * <p>1) is a Stream API invocation, .e.g. map, filter, collect 2) the source of the stream has
   * the same expression representation as streamSourceExpression.
   */
  private static boolean isStreamApiInvocationOnStreamSource(
      @Nullable ExpressionTree rootTree,
      ExpressionTree streamSourceExpression,
      VisitorState visitorState) {
    ExpressionTree expressionTree = rootTree;
    while (STREAM_API_INVOCATION_MATCHER.matches(expressionTree, visitorState)) {
      expressionTree = ASTHelpers.getReceiver(expressionTree);
    }

    if (!COLLECTION_TO_STREAM_MATCHER.matches(expressionTree, visitorState)) {
      return false;
    }

    return isSameExpression(ASTHelpers.getReceiver(expressionTree), streamSourceExpression);
  }

  // TODO(b/125767228): Consider a rigorous implementation to check tree structure equivalence.
  @SuppressWarnings("TreeToString") // Indented to ignore whitespace, comments, and source position.
  private static boolean isSameExpression(ExpressionTree leftTree, ExpressionTree rightTree) {
    // The left tree and right tree must have the same symbol resolution.
    // This ensures the symbol kind on field, parameter or local var.
    if (ASTHelpers.getSymbol(leftTree) != ASTHelpers.getSymbol(rightTree)) {
      return false;
    }

    String leftTreeTextRepr = stripPrefixIfPresent(leftTree.toString(), "this.");
    String rightTreeTextRepr = stripPrefixIfPresent(rightTree.toString(), "this.");
    return leftTreeTextRepr.contentEquals(rightTreeTextRepr);
  }

  private static String stripPrefixIfPresent(String originalText, String prefix) {
    return originalText.startsWith(prefix) ? originalText.substring(prefix.length()) : originalText;
  }
}
