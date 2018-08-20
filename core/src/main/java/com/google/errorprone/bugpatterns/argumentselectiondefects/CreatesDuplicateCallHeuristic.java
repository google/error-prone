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

package com.google.errorprone.bugpatterns.argumentselectiondefects;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.VisitorState;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import java.util.List;
import java.util.Objects;

/**
 * Detect whether our suggestion would create a method call which duplicates another one in this
 * block.
 *
 * @author andrewrice@google.com (Andrew Rice)
 */
class CreatesDuplicateCallHeuristic implements Heuristic {

  /**
   * Returns true if there are no other calls to this method which already have an actual parameter
   * in the position we are moving this one too.
   */
  @Override
  public boolean isAcceptableChange(
      Changes changes, Tree node, MethodSymbol symbol, VisitorState state) {
    return findArgumentsForOtherInstances(symbol, node, state).stream()
        .allMatch(arguments -> !anyArgumentsMatch(changes.changedPairs(), arguments));
  }

  /**
   * Return true if the replacement name is equal to the argument name for any replacement position.
   */
  private static boolean anyArgumentsMatch(
      List<ParameterPair> changedPairs, List<Parameter> arguments) {
    return changedPairs.stream()
        .anyMatch(
            change ->
                Objects.equals(
                    change.actual().text(), arguments.get(change.formal().index()).text()));
  }

  /**
   * Find all the other calls to {@code calledMethod} within the method (or class) which enclosed
   * the original call.
   *
   * <p>We are interested in two different cases: 1) where there are other calls to the method we
   * are calling; 2) declarations of the method we are calling (this catches the case when there is
   * a recursive call with the arguments correctly swapped).
   *
   * @param calledMethod is the method call we are analysing for swaps
   * @param currentNode is the tree node the method call occurred at
   * @param state is the current visitor state
   * @return a list containing argument lists for each call found
   */
  private static List<List<Parameter>> findArgumentsForOtherInstances(
      MethodSymbol calledMethod, Tree currentNode, VisitorState state) {

    Tree enclosingNode = ASTHelpers.findEnclosingNode(state.getPath(), MethodTree.class);
    if (enclosingNode == null) {
      enclosingNode = ASTHelpers.findEnclosingNode(state.getPath(), ClassTree.class);
    }
    if (enclosingNode == null) {
      return ImmutableList.of();
    }

    ImmutableList.Builder<List<Parameter>> resultBuilder = ImmutableList.builder();
    new TreeScanner<Void, Void>() {
      @Override
      public Void visitMethodInvocation(MethodInvocationTree methodInvocationTree, Void aVoid) {
        addToResult(ASTHelpers.getSymbol(methodInvocationTree), methodInvocationTree);
        return super.visitMethodInvocation(methodInvocationTree, aVoid);
      }

      @Override
      public Void visitNewClass(NewClassTree newClassTree, Void aVoid) {
        addToResult(ASTHelpers.getSymbol(newClassTree), newClassTree);
        return super.visitNewClass(newClassTree, aVoid);
      }

      @Override
      public Void visitMethod(MethodTree methodTree, Void aVoid) {
        MethodSymbol methodSymbol = ASTHelpers.getSymbol(methodTree);
        if (methodSymbol != null) {
          // if the method declared here is the one we are calling then add it
          addToResult(methodSymbol, methodTree);

          // if any supermethod of the one declared here is the one we are calling then add it
          for (MethodSymbol superSymbol :
              ASTHelpers.findSuperMethods(methodSymbol, state.getTypes())) {
            addToResult(superSymbol, methodTree);
          }
        }
        return super.visitMethod(methodTree, aVoid);
      }

      private void addToResult(MethodSymbol foundSymbol, Tree tree) {
        if (foundSymbol != null
            && Objects.equals(calledMethod, foundSymbol)
            && !currentNode.equals(tree)) {
          resultBuilder.add(createParameterList(tree));
        }
      }

      private ImmutableList<Parameter> createParameterList(Tree tree) {
        if (tree instanceof MethodInvocationTree) {
          return Parameter.createListFromExpressionTrees(
              ((MethodInvocationTree) tree).getArguments());
        }
        if (tree instanceof NewClassTree) {
          return Parameter.createListFromExpressionTrees(((NewClassTree) tree).getArguments());
        }
        if (tree instanceof MethodTree) {
          return Parameter.createListFromVariableTrees(((MethodTree) tree).getParameters());
        }
        return ImmutableList.of();
      }
    }.scan(enclosingNode, null);

    return resultBuilder.build();
  }
}
