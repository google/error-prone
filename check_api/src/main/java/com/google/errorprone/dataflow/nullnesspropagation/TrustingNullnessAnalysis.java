/*
 * Copyright 2016 The Error Prone Authors.
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

package com.google.errorprone.dataflow.nullnesspropagation;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.errorprone.dataflow.AccessPathStore;
import com.google.errorprone.dataflow.DataFlow;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.util.Context;
import java.io.Serializable;
import javax.lang.model.element.ElementKind;
import org.checkerframework.dataflow.analysis.Analysis;
import org.checkerframework.dataflow.cfg.CFGBuilder;
import org.checkerframework.dataflow.cfg.ControlFlowGraph;
import org.checkerframework.dataflow.cfg.UnderlyingAST;

/**
 * An interface to the "trusting" nullness analysis. This variant "trusts" {@code Nullable}
 * annotations, similar to how a modular nullness checker like the checkerframework's would, meaning
 * method parameters, fields, and method returns are assumed {@link Nullness#NULLABLE} only if
 * annotated so.
 */
public final class TrustingNullnessAnalysis implements Serializable {

  private static final Context.Key<TrustingNullnessAnalysis> TRUSTING_NULLNESS_KEY =
      new Context.Key<>();

  /**
   * Retrieve an instance of {@link TrustingNullnessAnalysis} from the {@code context}. If there is
   * no {@link TrustingNullnessAnalysis} currently in the {@code context}, create one, insert it,
   * and return it.
   */
  public static TrustingNullnessAnalysis instance(Context context) {
    TrustingNullnessAnalysis instance = context.get(TRUSTING_NULLNESS_KEY);
    if (instance == null) {
      instance = new TrustingNullnessAnalysis();
      context.put(TRUSTING_NULLNESS_KEY, instance);
    }
    return instance;
  }

  private final TrustingNullnessPropagation nullnessPropagation = new TrustingNullnessPropagation();

  // Use #instance to instantiate
  private TrustingNullnessAnalysis() {}

  /**
   * Returns the {@link Nullness} of the leaf of {@code exprPath}.
   *
   * <p>If the leaf required the compiler to generate autoboxing or autounboxing calls, {@code
   * getNullness} returns the {@code Nullness} <i>after</i> the boxing/unboxing. This implies that,
   * in those cases, it will always return {@code NONNULL}.
   */
  public Nullness getNullness(TreePath exprPath, Context context) {
    try {
      nullnessPropagation.setContext(context).setCompilationUnit(exprPath.getCompilationUnit());
      return DataFlow.expressionDataflow(exprPath, context, nullnessPropagation);
    } finally {
      nullnessPropagation.setContext(null).setCompilationUnit(null);
    }
  }

  /**
   * Returns {@link Nullness} of the initializer of the {@link VariableTree} at the leaf of the
   * given {@code fieldDeclPath}. Returns {@link Nullness#NULL} should there be no initializer.
   */
  // TODO(kmb): Fold this functionality into Dataflow.expressionDataflow
  public Nullness getFieldInitializerNullness(TreePath fieldDeclPath, Context context) {
    Tree decl = fieldDeclPath.getLeaf();
    checkArgument(
        decl instanceof VariableTree && ((JCVariableDecl) decl).sym.getKind() == ElementKind.FIELD,
        "Leaf of fieldDeclPath must be a field declaration: %s",
        decl);

    ExpressionTree initializer = ((VariableTree) decl).getInitializer();
    if (initializer == null) {
      // An uninitialized field is null or 0 to start :)
      return ((JCVariableDecl) decl).type.isPrimitive() ? Nullness.NONNULL : Nullness.NULL;
    }
    TreePath initializerPath = TreePath.getPath(fieldDeclPath, initializer);
    ClassTree classTree = (ClassTree) fieldDeclPath.getParentPath().getLeaf();
    JavacProcessingEnvironment javacEnv = JavacProcessingEnvironment.instance(context);
    UnderlyingAST ast = new UnderlyingAST.CFGStatement(decl, classTree);
    ControlFlowGraph cfg =
        CFGBuilder.build(
            initializerPath,
            ast,
            /* assumeAssertionsEnabled */ false,
            /* assumeAssertionsDisabled */ false,
            javacEnv);
    try {
      nullnessPropagation
          .setContext(context)
          .setCompilationUnit(fieldDeclPath.getCompilationUnit());

      Analysis<Nullness, AccessPathStore<Nullness>, TrustingNullnessPropagation> analysis =
          new Analysis<>(nullnessPropagation, javacEnv);
      analysis.performAnalysis(cfg);
      return analysis.getValue(initializer);
    } finally {
      nullnessPropagation.setContext(null).setCompilationUnit(null);
    }
  }

  public static boolean hasNullableAnnotation(Symbol symbol) {
    return Nullness.fromAnnotationsOn(symbol).orElse(null) == Nullness.NULLABLE;
  }
}
