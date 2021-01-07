/*
 * Copyright 2017 The Error Prone Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.errorprone.bugpatterns;

import static com.google.common.collect.Iterables.getLast;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.google.errorprone.util.ASTHelpers.getReceiver;
import static com.google.errorprone.util.ASTHelpers.sameVariable;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.TreePath;
import java.util.List;

/** @author anishvisaria98@gmail.com (Anish Visaria) */
@BugPattern(
    name = "ModifyCollectionInEnhancedForLoop",
    summary =
        "Modifying a collection while iterating over it in a loop may cause a"
            + " ConcurrentModificationException to be thrown or lead to undefined behavior.",
    severity = WARNING)
public class ModifyCollectionInEnhancedForLoop extends BugChecker
    implements MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> MATCHER =
      anyOf(
          instanceMethod()
              .onDescendantOf("java.util.Collection")
              .namedAnyOf("add", "addAll", "clear", "remove", "removeAll", "retainAll"),
          instanceMethod()
              .onDescendantOf("java.util.Map")
              .namedAnyOf(
                  "put",
                  "putAll",
                  "putIfAbsent",
                  "clear",
                  "remove",
                  "replace",
                  "replaceAll",
                  "merge"));

  private static final Matcher<ExpressionTree> MAP_SET_MATCHER =
      instanceMethod().onDescendantOf("java.util.Map").namedAnyOf("entrySet", "keySet", "values");

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!MATCHER.matches(tree, state)) {
      return NO_MATCH;
    }
    if (state.getTypes().closure(ASTHelpers.getReceiverType(tree)).stream()
        .anyMatch(
            s ->
                s.asElement()
                    .packge()
                    .getQualifiedName()
                    .toString()
                    .startsWith("java.util.concurrent"))) {
      return NO_MATCH;
    }
    if (blockEndsInBreakOrReturn(state)) {
      return NO_MATCH;
    }
    ExpressionTree collection = getReceiver(tree);
    if (collection == null) {
      return NO_MATCH;
    }
    if (!enclosingLoop(state, collection)) {
      return NO_MATCH;
    }
    return describeMatch(tree);
  }

  private static boolean blockEndsInBreakOrReturn(VisitorState state) {
    TreePath statementPath = state.findPathToEnclosing(StatementTree.class);
    if (statementPath == null) {
      return false;
    }
    Tree parent = statementPath.getParentPath().getLeaf();
    if (!(parent instanceof BlockTree)) {
      return false;
    }
    StatementTree statement = (StatementTree) statementPath.getLeaf();
    List<? extends StatementTree> statements = ((BlockTree) parent).getStatements();
    int idx = statements.indexOf(statement);
    if (idx == -1 || idx == statements.size()) {
      return false;
    }
    switch (getLast(statements).getKind()) {
      case BREAK:
      case RETURN:
        return true;
      default:
        return false;
    }
  }

  /** Returns true if {@code collection} is modified by an enclosing loop. */
  private static boolean enclosingLoop(VisitorState state, ExpressionTree collection) {
    for (Tree node : state.getPath()) {
      switch (node.getKind()) {
        case METHOD:
        case CLASS:
        case LAMBDA_EXPRESSION:
          return false;
        case ENHANCED_FOR_LOOP:
          if (sameCollection(collection, ((EnhancedForLoopTree) node).getExpression(), state)) {
            return true;
          }
          break;
        default: // fall out
      }
    }
    return false;
  }

  /**
   * Returns true if {@code loopExpression} is defined over the same collection as {@code
   * collection}.
   */
  private static boolean sameCollection(
      ExpressionTree collection, ExpressionTree loopExpression, VisitorState state) {
    if (sameVariable(collection, loopExpression)) {
      return true;
    }

    // Check if the loopExpression is a .keySet(), .entrySet, or .values() of the map
    if (loopExpression.getKind() == Kind.METHOD_INVOCATION) {
      ExpressionTree receiver = getReceiver(loopExpression);
      return receiver != null
          && sameVariable(collection, receiver)
          && MAP_SET_MATCHER.matches(loopExpression, state);
    }
    return false;
  }
}
