/*
 * Copyright 2018 The Error Prone Authors.
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

package com.google.errorprone.dataflow.nullnesspropagation.inference;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Streams;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol.CompletionFailure;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.TypeVariableSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ArrayType;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCArrayAccess;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.TreeInfo;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.List;

/**
 * Eagerly traverse one {@code MethodTree} at a time and accumulate constraints between nullness
 * qualifiers. Those constraints are then solved as needed in {@code InferredNullability}. Based on
 * Java type inference as defined in JLS section 18.
 *
 * @author bennostein@google.com (Benno Stein)
 */
public class NullnessQualifierInference extends TreeScanner<Void, Void> {

  private static final LoadingCache<Tree, InferredNullability> inferenceCache =
      CacheBuilder.newBuilder()
          .maximumSize(1)
          .build(
              new CacheLoader<Tree, InferredNullability>() {
                @Override
                public InferredNullability load(Tree methodOrInitializer) {
                  NullnessQualifierInference inferenceEngine =
                      new NullnessQualifierInference(methodOrInitializer);
                  inferenceEngine.scan(methodOrInitializer, null);
                  return new InferredNullability(inferenceEngine.qualifierConstraints);
                }
              });

  public static InferredNullability getInferredNullability(Tree methodOrInitializerOrLambda) {
    checkArgument(
        methodOrInitializerOrLambda instanceof MethodTree
            || methodOrInitializerOrLambda instanceof LambdaExpressionTree
            || methodOrInitializerOrLambda instanceof BlockTree
            || methodOrInitializerOrLambda instanceof VariableTree,
        "Tree `%s` is not a lambda, initializer, or method.",
        methodOrInitializerOrLambda);
    try {
      return inferenceCache.getUnchecked(methodOrInitializerOrLambda);
    } catch (UncheckedExecutionException e) {
      throw e.getCause() instanceof CompletionFailure ? (CompletionFailure) e.getCause() : e;
    }
  }

  private final MutableGraph<InferenceVariable> qualifierConstraints;
  private final Tree currentMethodOrInitializerOrLambda;

  private NullnessQualifierInference(Tree currentMethodOrInitializerOrLambda) {
    this.currentMethodOrInitializerOrLambda = currentMethodOrInitializerOrLambda;
    this.qualifierConstraints = GraphBuilder.directed().build();

    // Initialize graph with standard nullness lattice; see ASCII art diagram in
    // com.google.errorprone.dataflow.nullnesspropagation.Nullness for more details.
    qualifierConstraints.putEdge(ProperInferenceVar.BOTTOM, ProperInferenceVar.NONNULL);
    qualifierConstraints.putEdge(ProperInferenceVar.BOTTOM, ProperInferenceVar.NULL);
    qualifierConstraints.putEdge(ProperInferenceVar.NONNULL, ProperInferenceVar.NULLABLE);
    qualifierConstraints.putEdge(ProperInferenceVar.NULL, ProperInferenceVar.NULLABLE);
  }

  @Override
  public Void visitIdentifier(IdentifierTree node, Void unused) {
    Type declaredType = ((JCIdent) node).sym.type;
    generateConstraintsFromIdentifierUse(declaredType, node, new ArrayDeque<>());
    return super.visitIdentifier(node, unused);
  }

  private void generateConstraintsFromIdentifierUse(
      Type type, Tree sourceTree, ArrayDeque<Integer> argSelector) {
    List<Type> typeArguments = type.getTypeArguments();
    int numberOfTypeArgs = typeArguments.size();
    for (int i = 0; i < numberOfTypeArgs; i++) {
      argSelector.push(i);
      generateConstraintsFromIdentifierUse(typeArguments.get(i), sourceTree, argSelector);
      argSelector.pop();
    }
    ProperInferenceVar.fromTypeIfAnnotated(type)
        .ifPresent(
            annot -> {
              qualifierConstraints.putEdge(
                  TypeArgInferenceVar.create(ImmutableList.copyOf(argSelector), sourceTree), annot);
              qualifierConstraints.putEdge(
                  annot, TypeArgInferenceVar.create(ImmutableList.copyOf(argSelector), sourceTree));
            });
  }

  @Override
  public Void visitAssignment(AssignmentTree node, Void unused) {
    Type lhsType =
        node.getVariable() instanceof ArrayAccessTree
            ? ((JCArrayAccess) node.getVariable()).getExpression().type
            : TreeInfo.symbol((JCTree) node.getVariable()).type;
    generateConstraintsForWrite(lhsType, node.getExpression(), node);
    return super.visitAssignment(node, unused);
  }

  @Override
  public Void visitVariable(VariableTree node, Void unused) {
    if (node.getInitializer() != null) {
      generateConstraintsForWrite(
          TreeInfo.symbolFor((JCTree) node).type, node.getInitializer(), node);
    }
    return super.visitVariable(node, unused);
  }

  @Override
  public Void visitReturn(ReturnTree node, Void unused) {
    if (node.getExpression() != null && currentMethodOrInitializerOrLambda instanceof MethodTree) {
      Type returnType =
          ((MethodSymbol) TreeInfo.symbolFor((JCTree) currentMethodOrInitializerOrLambda))
              .getReturnType();
      generateConstraintsForWrite(returnType, node.getExpression(), node);
    }
    return super.visitReturn(node, unused);
  }

  private static ImmutableList<Type> expandVarargsToArity(List<VarSymbol> formalArgs, int arity) {
    ImmutableList.Builder<Type> result = ImmutableList.builderWithExpectedSize(arity);
    int numberOfVarArgs = arity - formalArgs.size() + 1;

    for (Iterator<VarSymbol> argsIterator = formalArgs.iterator(); argsIterator.hasNext(); ) {
      VarSymbol arg = argsIterator.next();
      if (argsIterator.hasNext()) {
        // Not the variadic argument: just add to result
        result.add(arg.type);
      } else {
        // Variadic argument: extract the type and add it to result the proper number of times
        Type varArgType = ((ArrayType) arg.type).elemtype;
        for (int idx = 0; idx < numberOfVarArgs; idx++) {
          result.add(varArgType);
        }
      }
    }

    return result.build();
  }

  @Override
  public Void visitMethodInvocation(MethodInvocationTree node, Void unused) {
    JCMethodInvocation sourceNode = (JCMethodInvocation) node;
    MethodSymbol callee = (MethodSymbol) TreeInfo.symbol(sourceNode.getMethodSelect());

    ImmutableList<Type> formalParameters =
        callee.isVarArgs()
            ? expandVarargsToArity(callee.getParameters(), sourceNode.args.size())
            : callee.getParameters().stream()
                .map(var -> var.type)
                .collect(ImmutableList.toImmutableList());

    // generate constraints for each argument write.
    Streams.forEachPair(
        formalParameters.stream(),
        sourceNode.getArguments().stream(),
        (formal, actual) -> {
          // formal parameter type
          generateConstraintsForWrite(formal, actual, node);
          // actual parameter type (i.e. after type variable substitution)
          generateConstraintsForWrite(actual.type, actual, node);
        });

    // if return type is parameterized by a generic type on receiver, collate references to that
    // generic between the receiver and the result/argument types.
    if (node.getMethodSelect() instanceof JCFieldAccess) {
      JCFieldAccess fieldAccess = ((JCFieldAccess) node.getMethodSelect());
      for (TypeVariableSymbol tvs : fieldAccess.selected.type.tsym.getTypeParameters()) {
        Type rcvrtype = fieldAccess.selected.type.tsym.type;
        ImmutableSet<InferenceVariable> rcvrReferences =
            getReferencesToTypeVar(tvs, rcvrtype, fieldAccess.selected);
        Type restype = fieldAccess.sym.type.asMethodType().restype;
        getReferencesToTypeVar(tvs, restype, node)
            .forEach(
                resRef ->
                    rcvrReferences.forEach(
                        rcvrRef -> qualifierConstraints.putEdge(resRef, rcvrRef)));
        Streams.forEachPair(
            formalParameters.stream(),
            node.getArguments().stream(),
            (formal, actual) ->
                getReferencesToTypeVar(tvs, formal, actual)
                    .forEach(
                        argRef ->
                            rcvrReferences.forEach(
                                rcvrRef -> qualifierConstraints.putEdge(argRef, rcvrRef))));
      }
    }

    // Get all references to each typeVar in the return type and formal parameters and relate them
    // in the constraint graph; covariant in the return type, contravariant in the argument types.
    for (TypeVariableSymbol typeVar : callee.getTypeParameters()) {
      InferenceVariable typeVarIV = TypeVariableInferenceVar.create(typeVar, node);
      for (InferenceVariable iv : getReferencesToTypeVar(typeVar, callee.getReturnType(), node)) {
        qualifierConstraints.putEdge(typeVarIV, iv);
      }
      Streams.forEachPair(
          formalParameters.stream(),
          node.getArguments().stream(),
          (formal, actual) -> {
            for (InferenceVariable iv : getReferencesToTypeVar(typeVar, formal, actual)) {
              qualifierConstraints.putEdge(iv, typeVarIV);
            }
          });
    }
    return super.visitMethodInvocation(node, unused);
  }

  private static ImmutableSet<InferenceVariable> getReferencesToTypeVar(
      TypeVariableSymbol typeVar, Type type, Tree sourceNode) {
    ImmutableSet.Builder<InferenceVariable> result = ImmutableSet.builder();
    getTypeVarReferences(typeVar, sourceNode, type, new ArrayDeque<>(), result);
    return result.build();
  }

  private static void getTypeVarReferences(
      TypeVariableSymbol typeVar,
      Tree sourceNode,
      Type type,
      ArrayDeque<Integer> partialSelector,
      ImmutableSet.Builder<InferenceVariable> resultBuilder) {

    List<Type> typeArguments = type.getTypeArguments();
    for (int i = 0; i < typeArguments.size(); i++) {
      partialSelector.push(i);
      getTypeVarReferences(
          typeVar, sourceNode, typeArguments.get(i), partialSelector, resultBuilder);
      partialSelector.pop();
    }
    if (type.tsym.equals(typeVar)) {
      resultBuilder.add(
          TypeArgInferenceVar.create(ImmutableList.copyOf(partialSelector), sourceNode));
    }
  }

  /**
   * Generate inference variable constraints derived from this write, including proper bounds from
   * type annotations on the declared type {@code lType} of the l-val as well as relationships
   * between type parameters of the l-val and r-val
   */
  private void generateConstraintsForWrite(Type lType, ExpressionTree rVal, Tree sourceTree) {
    if (rVal.getKind() == Kind.NULL_LITERAL) {
      qualifierConstraints.putEdge(
          ProperInferenceVar.NULL, TypeArgInferenceVar.create(ImmutableList.of(), sourceTree));
    } else if ((rVal instanceof LiteralTree)
        || (rVal instanceof NewClassTree)
        || (rVal instanceof NewArrayTree)
        || ((rVal instanceof IdentifierTree)
            && ((IdentifierTree) rVal).getName().contentEquals("this"))) {
      qualifierConstraints.putEdge(
          ProperInferenceVar.NONNULL, TypeArgInferenceVar.create(ImmutableList.of(), sourceTree));
    }
    generateConstraintsForWrite(lType, rVal, sourceTree, new ArrayDeque<>());
  }

  private void generateConstraintsForWrite(
      Type lType, ExpressionTree rVal, Tree sourceTree, ArrayDeque<Integer> argSelector) {
    List<Type> typeArguments = lType.getTypeArguments();
    for (int i = 0; i < typeArguments.size(); i++) {
      argSelector.push(i);
      generateConstraintsForWrite(typeArguments.get(i), rVal, sourceTree, argSelector);
      argSelector.pop();
    }

    ImmutableList<Integer> argSelectorList = ImmutableList.copyOf(argSelector);

    // If there is an explicit annotation, trust it and constrain the corresponding type arg
    // inference variable to be equal to that proper inference variable.
    ProperInferenceVar.fromTypeIfAnnotated(lType)
        .ifPresent(
            annot -> {
              qualifierConstraints.putEdge(
                  TypeArgInferenceVar.create(argSelectorList, sourceTree), annot);
              qualifierConstraints.putEdge(
                  annot, TypeArgInferenceVar.create(argSelectorList, sourceTree));
            });

    // Constrain this type or type argument on the rVal to be <= its lVal counterpart
    qualifierConstraints.putEdge(
        TypeArgInferenceVar.create(argSelectorList, rVal),
        TypeArgInferenceVar.create(argSelectorList, sourceTree));
  }
}
