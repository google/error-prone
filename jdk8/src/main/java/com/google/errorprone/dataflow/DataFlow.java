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

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
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

  public static <A extends AbstractValue<A>, S extends Store<S>,
      T extends TransferFunction<A, S>> Result<A, S, T> dataflow(MethodTree method, TreePath path,
      Context context, T transfer) {
    CompilationUnitTree root = path.getCompilationUnit();
    ProcessingEnvironment env = JavacProcessingEnvironment.instance(context);
    ClassTree classTree = null;
    UnderlyingAST ast = new UnderlyingAST.CFGMethod(method, classTree);

    final ControlFlowGraph cfg = CFGBuilder.build(root, env, ast);
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
}
