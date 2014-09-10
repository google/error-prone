/*
 * Copyright 2014 Google Inc. All Rights Reserved.
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

package com.google.errorprone.dataflow;

import com.google.common.base.Preconditions;

import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;

import org.checkerframework.dataflow.analysis.AbstractValue;
import org.checkerframework.dataflow.analysis.Analysis;
import org.checkerframework.dataflow.analysis.Store;
import org.checkerframework.dataflow.analysis.TransferFunction;
import org.checkerframework.dataflow.cfg.CFGBuilder;
import org.checkerframework.dataflow.cfg.ControlFlowGraph;
import org.checkerframework.dataflow.cfg.UnderlyingAST;

import javax.annotation.processing.ProcessingEnvironment;

/**
 * Provides a wrapper around {@link org.checkerframework.dataflow.analysis.Analysis}.
 */
public final class DataFlow {
  /**
   * A pair of Analysis and ControlFlowGraph.
   */
  public static interface Result<A extends AbstractValue<A>, S extends Store<S>,
      T extends TransferFunction<A, S>> {
    Analysis<A, S, T> getAnalysis();

    ControlFlowGraph getControlFlowGraph();
  }

  // TODO(user), remove once we merge jdk8 specific's with core
  public static <T> TreePath findPathFromEnclosingNodeToTopLevel(TreePath path,
      Class<T> klass) {
    while (path != null && !(klass.isInstance(path.getLeaf()))) {
      path = path.getParentPath();
    }
    return path;
  }

  /**
   * Run the {@code transfer} dataflow analysis over the method which is the leaf of the
   * {@code methodPath}.
   */
  public static <A extends AbstractValue<A>, S extends Store<S>,
                 T extends TransferFunction<A, S>> Result<A, S, T>
      methodDataflow(TreePath methodPath, Context context, T transfer) {
    final Tree leaf = methodPath.getLeaf();
    Preconditions.checkArgument(leaf instanceof MethodTree,
        "Leaf of methodPath must be of type MethodTree, but was %s", leaf.getClass().getName());

    final MethodTree method = (MethodTree) leaf;
    final BlockTree body = method.getBody();
    Preconditions.checkNotNull(body,
        "Method to analyze must have a body. Method passed in: %s in file %s",
        method.getName(),
        methodPath.getCompilationUnit().getSourceFile().getName());

    final ProcessingEnvironment env = JavacProcessingEnvironment.instance(context);
    final ClassTree classTree = null;
    final UnderlyingAST ast = new UnderlyingAST.CFGMethod(method, classTree);
    final TreePath bodyPath = new TreePath(methodPath, body);

    CompilationUnitTree root = bodyPath.getCompilationUnit();
    // TODO(user), replace with build(bodyPath, env, ast, false, false);
    final ControlFlowGraph cfg = CFGBuilder.build(root, env, ast, false, false);

    final Analysis<A, S, T> analysis = new Analysis<A, S, T>(env, transfer);
    analysis.performAnalysis(cfg);

    return new Result<A, S, T>() {
      @Override
      public Analysis<A, S, T> getAnalysis() {
        return analysis;
      }

      @Override
      public ControlFlowGraph getControlFlowGraph() {
        return cfg;
      }
    };
  }

  /**
   * Run the {@code transfer} dataflow analysis to compute the abstract value of the expression
   * which is the leaf of {@code exprPath}.
   */
  public static <A extends AbstractValue<A>, S extends Store<S>,
                 T extends TransferFunction<A, S>> A
      expressionDataflow(TreePath exprPath, Context context, T transfer) {
    final Tree leaf = exprPath.getLeaf();
    Preconditions.checkArgument(leaf instanceof ExpressionTree,
        "Leaf of exprPath must be of type ExpressionTree, but was %s", leaf.getClass().getName());

    final ExpressionTree expr = (ExpressionTree) leaf;
    final TreePath enclosingMethodPath =
        findPathFromEnclosingNodeToTopLevel(exprPath, MethodTree.class);

    if (enclosingMethodPath == null) {
      // TODO(user) this can happen in field initialization.
      // Currently not supported because it only happens in ~2% of cases.
      return null;
    }
    return methodDataflow(enclosingMethodPath, context, transfer).getAnalysis().getValue(expr);
  }
}
