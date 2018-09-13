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
import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
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
import com.sun.source.util.TreePath;
import java.util.List;

/** @author anishvisaria98@gmail.com (Anish Visaria) */
@BugPattern(
    name = "ModifyCollectionInEnhancedForLoop",
    summary =
        "Modifying a collection while iterating over it in a loop may cause a"
            + " ConcurrentModificationException to be thrown.",
    category = JDK,
    severity = WARNING)
public class ModifyCollectionInEnhancedForLoop extends BugChecker
    implements MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> MATCHER =
      instanceMethod()
          .onDescendantOf("java.util.Collection")
          .namedAnyOf("add", "addAll", "clear", "remove", "removeAll", "retainAll");

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!MATCHER.matches(tree, state)) {
      return NO_MATCH;
    }
    if (state.getTypes().closure(ASTHelpers.getSymbol(tree).enclClass().asType()).stream()
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
    if (!enclosingLoop(state.getPath(), collection)) {
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
  static boolean enclosingLoop(TreePath path, ExpressionTree collection) {
    for (Tree node : path) {
      switch (node.getKind()) {
        case METHOD:
        case CLASS:
        case LAMBDA_EXPRESSION:
          return false;
        case ENHANCED_FOR_LOOP:
          if (sameVariable(collection, ((EnhancedForLoopTree) node).getExpression())) {
            return true;
          }
          break;
        default: // fall out
      }
    }
    return false;
  }
}
