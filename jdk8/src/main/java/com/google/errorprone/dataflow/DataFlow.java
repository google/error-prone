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
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

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

import java.util.Objects;

import javax.annotation.processing.ProcessingEnvironment;

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
  private static LoadingCache<AnalysisParams, Analysis<?, ?, ?>> analysisCache =
      CacheBuilder.newBuilder().build(
          new CacheLoader<AnalysisParams, Analysis<?, ?, ?>>() {
            @Override
            public Analysis<?, ?, ?> load(AnalysisParams key) {
              final ProcessingEnvironment env = key.getEnvironment();
              final ControlFlowGraph cfg = key.getCFG();
              final TransferFunction<?, ?> transfer = key.getTransferFunction();

              @SuppressWarnings({"unchecked", "rawtypes"})
              final Analysis<?, ?, ?> analysis = new Analysis(env, transfer);
              analysis.performAnalysis(cfg);
              return analysis;
            }
          });

  private static LoadingCache<CFGParams, ControlFlowGraph> cfgCache =
      CacheBuilder.newBuilder().maximumSize(1).build(
          new CacheLoader<CFGParams, ControlFlowGraph>() {
            @Override
            public ControlFlowGraph load(CFGParams key) {
              final TreePath methodPath = key.getMethodPath();
              final MethodTree method = (MethodTree) methodPath.getLeaf();
              final BlockTree body = method.getBody();
              final TreePath bodyPath = new TreePath(methodPath, body);
              final ClassTree classTree = null;
              final UnderlyingAST ast = new UnderlyingAST.CFGMethod(method, classTree);
              final ProcessingEnvironment env = key.getEnvironment();

              analysisCache.invalidateAll();
              CompilationUnitTree root = bodyPath.getCompilationUnit();
              // TODO(user), replace with faster build(bodyPath, env, ast, false, false);
              return CFGBuilder.build(root, env, ast, false, false);
            }
          });

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
   *
   * <p>For caching, we make the following assumptions:
   * - if two paths to methods are {@code equal}, their control flow graph is the same.
   * - if two transfer functions are {@code equal}, and are run over the same control flow graph,
   *   the analysis result is the same.
   * - for all contexts, the analysis result is the same.
   */
  public static <A extends AbstractValue<A>, S extends Store<S>,
                 T extends TransferFunction<A, S>> Result<A, S, T>
      methodDataflow(TreePath methodPath, Context context, T transfer) {
    final Tree leaf = methodPath.getLeaf();
    Preconditions.checkArgument(leaf instanceof MethodTree,
        "Leaf of methodPath must be of type MethodTree, but was %s", leaf.getClass().getName());

    final MethodTree method = (MethodTree) leaf;
    Preconditions.checkNotNull(method.getBody(),
        "Method to analyze must have a body. Method passed in: %s() in file %s",
        method.getName(),
        methodPath.getCompilationUnit().getSourceFile().getName());

    final ProcessingEnvironment env = JavacProcessingEnvironment.instance(context);
    final ControlFlowGraph cfg = cfgCache.getUnchecked(new CFGParams(methodPath, env));
    final AnalysisParams aparams = new AnalysisParams(transfer, cfg, env);
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

    final MethodTree method = (MethodTree) enclosingMethodPath.getLeaf();
    if (method.getBody() == null) {
      // expressions can occur in abstract methods, for example {@code Map.Entry} in:
      //
      //   abstract Set<Map.Entry<K, V>> entries();
      return null;
    }

    return methodDataflow(enclosingMethodPath, context, transfer).getAnalysis().getValue(expr);
  }

  private static final class CFGParams {
    final ProcessingEnvironment env;
    final TreePath methodPath;

    public ProcessingEnvironment getEnvironment() {
      return env;
    }

    public TreePath getMethodPath() {
      return methodPath;
    }

    public CFGParams(TreePath methodPath, ProcessingEnvironment env) {
      this.env = env;
      this.methodPath = methodPath;
    }

    @Override
    public int hashCode() {
      return Objects.hash(methodPath);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      CFGParams other = (CFGParams) obj;
      return Objects.equals(methodPath, other.methodPath);
    }
  }

  private static final class AnalysisParams {
    private final ProcessingEnvironment env;
    private final ControlFlowGraph cfg;
    private final TransferFunction<?, ?> transfer;

    public AnalysisParams(TransferFunction<?, ?> trans, ControlFlowGraph cfg,
        ProcessingEnvironment env) {
      this.env = env;
      this.cfg = cfg;
      this.transfer = trans;
    }

    public ProcessingEnvironment getEnvironment() {
      return env;
    }

    public ControlFlowGraph getCFG() {
      return cfg;
    }

    @SuppressWarnings("rawtypes")
    public TransferFunction getTransferFunction() {
      return transfer;
    }

    @Override
    public int hashCode() {
      return Objects.hash(cfg, transfer);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      AnalysisParams other = (AnalysisParams) obj;
      return Objects.equals(cfg, other.cfg)
          && Objects.equals(transfer, other.transfer);
    }
  }
}
