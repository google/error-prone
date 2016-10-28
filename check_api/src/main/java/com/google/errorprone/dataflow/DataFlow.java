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

import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;
import javax.annotation.Nullable;
import javax.annotation.processing.ProcessingEnvironment;
import org.checkerframework.dataflow.analysis.AbstractValue;
import org.checkerframework.dataflow.analysis.Analysis;
import org.checkerframework.dataflow.analysis.Store;
import org.checkerframework.dataflow.analysis.TransferFunction;
import org.checkerframework.dataflow.cfg.CFGBuilder;
import org.checkerframework.dataflow.cfg.ControlFlowGraph;
import org.checkerframework.dataflow.cfg.UnderlyingAST;

/**
 * Provides a wrapper around {@link org.checkerframework.dataflow.analysis.Analysis}.
 *
 * @author konne@google.com (Konstantin Weitz)
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

  /*
   * We cache both the control flow graph and the analyses that are run on it.
   * We tuned performance to the following assumptions (which are currently true for error-prone):
   *
   * <ul>
   * <li> all dataflow analyses for a method are finished before another method is analyzed
   * <li> multiple dataflow analyses for the same method are executed in arbitrary order
   * </ul>
   *
   * TODO(user): Write a test that checks these assumptions
   */
  private static final LoadingCache<AnalysisParams, Analysis<?, ?, ?>> analysisCache =
      CacheBuilder.newBuilder()
          .build(
              new CacheLoader<AnalysisParams, Analysis<?, ?, ?>>() {
                @Override
                public Analysis<?, ?, ?> load(AnalysisParams key) {
                  final ProcessingEnvironment env = key.environment();
                  final ControlFlowGraph cfg = key.cfg();
                  final TransferFunction<?, ?> transfer = key.transferFunction();

                  @SuppressWarnings({"unchecked", "rawtypes"})
                  final Analysis<?, ?, ?> analysis = new Analysis(env, transfer);
                  analysis.performAnalysis(cfg);
                  return analysis;
                }
              });

  private static final LoadingCache<CfgParams, ControlFlowGraph> cfgCache =
      CacheBuilder.newBuilder()
          .maximumSize(1)
          .build(
              new CacheLoader<CfgParams, ControlFlowGraph>() {
                @Override
                public ControlFlowGraph load(CfgParams key) {
                  final TreePath methodPath = key.methodPath();
                  final UnderlyingAST ast;
                  if (methodPath.getLeaf() instanceof LambdaExpressionTree) {
                    ast = new UnderlyingAST.CFGLambda((LambdaExpressionTree) methodPath.getLeaf());
                  } else { // must be a method per findEnclosingMethodOrLambda
                    MethodTree method = (MethodTree) methodPath.getLeaf();
                    ast = new UnderlyingAST.CFGMethod(method, /*classTree*/ null);
                  }
                  final ProcessingEnvironment env = key.environment();

                  analysisCache.invalidateAll();
                  CompilationUnitTree root = methodPath.getCompilationUnit();
                  // TODO(user), replace with faster build(bodyPath, env, ast, false, false);
                  return CFGBuilder.build(root, env, ast, false, false);
                }
              });

  // TODO(user), remove once we merge jdk8 specific's with core
  private static <T> TreePath findEnclosingMethodOrLambda(TreePath path) {
    while (path != null) {
      if (path.getLeaf() instanceof MethodTree || path.getLeaf() instanceof LambdaExpressionTree) {
        return path;
      }
      path = path.getParentPath();
    }
    return null;
  }

  /**
   * Run the {@code transfer} dataflow analysis over the method or lambda which is the leaf of the
   * {@code methodPath}.
   *
   * <p>For caching, we make the following assumptions:
   * - if two paths to methods are {@code equal}, their control flow graph is the same.
   * - if two transfer functions are {@code equal}, and are run over the same control flow graph,
   *   the analysis result is the same.
   * - for all contexts, the analysis result is the same.
   */
  private static <A extends AbstractValue<A>, S extends Store<S>, T extends TransferFunction<A, S>>
      Result<A, S, T> methodDataflow(TreePath methodPath, Context context, T transfer) {
    final ProcessingEnvironment env = JavacProcessingEnvironment.instance(context);
    final ControlFlowGraph cfg = cfgCache.getUnchecked(CfgParams.create(methodPath, env));
    final AnalysisParams aparams = AnalysisParams.create(transfer, cfg, env);
    @SuppressWarnings("unchecked")
    final Analysis<A, S, T> analysis = (Analysis<A, S, T>) analysisCache.getUnchecked(aparams);

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
   *
   * @return dataflow result for the given expression or {@code null} if the expression is not
   *     part of a method or lambda
   */
  @Nullable
  public static <A extends AbstractValue<A>, S extends Store<S>,
                 T extends TransferFunction<A, S>> A
      expressionDataflow(TreePath exprPath, Context context, T transfer) {
    final Tree leaf = exprPath.getLeaf();
    Preconditions.checkArgument(
        leaf instanceof ExpressionTree,
        "Leaf of exprPath must be of type ExpressionTree, but was %s",
        leaf.getClass().getName());

    final ExpressionTree expr = (ExpressionTree) leaf;
    final TreePath enclosingMethodPath = findEnclosingMethodOrLambda(exprPath);
    if (enclosingMethodPath == null) {
      // TODO(user) this can happen in field initialization.
      // Currently not supported because it only happens in ~2% of cases.
      return null;
    }

    final Tree method = enclosingMethodPath.getLeaf();
    if (method instanceof MethodTree && ((MethodTree) method).getBody() == null) {
      // expressions can occur in abstract methods, for example {@code Map.Entry} in:
      //
      //   abstract Set<Map.Entry<K, V>> entries();
      return null;
    }

    return methodDataflow(enclosingMethodPath, context, transfer).getAnalysis().getValue(expr);
  }

  @AutoValue
  abstract static class CfgParams {
    abstract TreePath methodPath();

    // Should not be used for hashCode or equals
    private ProcessingEnvironment environment;

    private static CfgParams create(TreePath methodPath, ProcessingEnvironment environment) {
      CfgParams cp = new AutoValue_DataFlow_CfgParams(methodPath);
      cp.environment = environment;
      return cp;
    }

    ProcessingEnvironment environment() {
      return environment;
    }
  }

  @AutoValue
  abstract static class AnalysisParams {

    abstract TransferFunction<?, ?> transferFunction();

    abstract ControlFlowGraph cfg();

    // Should not be used for hashCode or equals
    private ProcessingEnvironment environment;

    private static AnalysisParams create(
        TransferFunction<?, ?> transferFunction,
        ControlFlowGraph cfg,
        ProcessingEnvironment environment) {
      AnalysisParams ap = new AutoValue_DataFlow_AnalysisParams(transferFunction, cfg);
      ap.environment = environment;
      return ap;
    }

    ProcessingEnvironment environment() {
      return environment;
    }
  }
}
