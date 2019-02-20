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

import com.google.auto.value.AutoValue;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Streams;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.google.errorprone.dataflow.nullnesspropagation.Nullness;
import com.google.errorprone.dataflow.nullnesspropagation.NullnessAnnotations;
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
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.CompletionFailure;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.TypeVariableSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ArrayType;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCArrayAccess;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.TreeInfo;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import javax.annotation.Nullable;

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

  /**
   * &lt;= constraints between inference variables: an edge from A to B means A &lt;= B. In other
   * words, edges point "upwards" in the lattice towards Top == Nullable.
   */
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
    Symbol sym = ((JCIdent) node).sym;
    if (sym instanceof VarSymbol) {
      Type declaredType = sym.type;
      generateConstraintsFromAnnotations(
          ((JCIdent) node).type, sym, declaredType, node, new ArrayDeque<>());
    }
    return super.visitIdentifier(node, unused);
  }

  private void generateConstraintsFromAnnotations(
      Type inferredType,
      @Nullable Symbol decl,
      @Nullable Type declaredType,
      Tree sourceTree,
      ArrayDeque<Integer> argSelector) {
    checkArgument(decl == null || argSelector.isEmpty());
    List<Type> inferredTypeArguments = inferredType.getTypeArguments();
    List<Type> declaredTypeArguments =
        declaredType != null ? declaredType.getTypeArguments() : ImmutableList.of();
    int numberOfTypeArgs = inferredTypeArguments.size();
    for (int i = 0; i < numberOfTypeArgs; i++) {
      argSelector.push(i);
      generateConstraintsFromAnnotations(
          inferredTypeArguments.get(i),
          /*decl=*/ null,
          i < declaredTypeArguments.size() ? declaredTypeArguments.get(i) : null,
          sourceTree,
          argSelector);
      argSelector.pop();
    }

    Optional<Nullness> fromAnnotations = extractExplicitNullness(declaredType, decl);
    if (!fromAnnotations.isPresent()) {
      // Check declared type before inferred type so that type annotations on the declaration take
      // precedence (just like declaration annotations) over annotations on the inferred type.
      // For instance, we want a @Nullable T m() to take precedence over annotations on T's inferred
      // type (e.g., @NotNull String), whether @Nullable is a declaration or type annotation.
      fromAnnotations = NullnessAnnotations.fromAnnotationsOn(inferredType);
    }
    if (!fromAnnotations.isPresent()) {
      // Check bounds last so explicit annotations take precedence. Even for bounds we still use
      // equality constraint below since we have to assume the bound as the "worst" case.
      fromAnnotations = NullnessAnnotations.getUpperBound(declaredType);
    }
    // Use equality constraints even for top-level type, since we want to "trust" the annotation
    fromAnnotations
        .map(ProperInferenceVar::create)
        .ifPresent(
            annot -> {
              InferenceVariable var =
                  TypeArgInferenceVar.create(ImmutableList.copyOf(argSelector), sourceTree);
              qualifierConstraints.putEdge(var, annot);
              qualifierConstraints.putEdge(annot, var);
            });
  }

  @Override
  public Void visitAssignment(AssignmentTree node, Void unused) {
    Type lhsType =
        node.getVariable() instanceof ArrayAccessTree
            ? ((JCArrayAccess) node.getVariable()).getExpression().type
            : TreeInfo.symbol((JCTree) node.getVariable()).type;
    generateConstraintsForWrite(lhsType, null, node.getExpression(), node);
    return super.visitAssignment(node, unused);
  }

  @Override
  public Void visitVariable(VariableTree node, Void unused) {
    if (node.getInitializer() != null) {
      Symbol symbol = TreeInfo.symbolFor((JCTree) node);
      generateConstraintsForWrite(symbol.type, symbol, node.getInitializer(), node);
    }
    return super.visitVariable(node, unused);
  }

  @Override
  public Void visitReturn(ReturnTree node, Void unused) {
    if (node.getExpression() != null && currentMethodOrInitializerOrLambda instanceof MethodTree) {
      MethodSymbol sym =
          ((MethodSymbol) TreeInfo.symbolFor((JCTree) currentMethodOrInitializerOrLambda));
      generateConstraintsForWrite(sym.getReturnType(), sym, node.getExpression(), node);
    }
    return super.visitReturn(node, unused);
  }

  private static ImmutableList<TypeAndSymbol> expandVarargsToArity(
      List<VarSymbol> formalArgs, int arity) {
    ImmutableList.Builder<TypeAndSymbol> result = ImmutableList.builderWithExpectedSize(arity);
    int numberOfVarArgs = arity - formalArgs.size() + 1;

    for (Iterator<VarSymbol> argsIterator = formalArgs.iterator(); argsIterator.hasNext(); ) {
      VarSymbol arg = argsIterator.next();
      if (argsIterator.hasNext()) {
        // Not the variadic argument: just add to result
        result.add(TypeAndSymbol.create(arg.type, arg));
      } else {
        // Variadic argument: extract the type and add it to result the proper number of times
        Type varArgType = ((ArrayType) arg.type).elemtype;
        for (int idx = 0; idx < numberOfVarArgs; idx++) {
          result.add(TypeAndSymbol.create(varArgType));
        }
      }
    }

    return result.build();
  }

  @Override
  public Void visitMethodInvocation(MethodInvocationTree node, Void unused) {
    JCMethodInvocation sourceNode = (JCMethodInvocation) node;
    MethodSymbol callee = (MethodSymbol) TreeInfo.symbol(sourceNode.getMethodSelect());

    ImmutableList<TypeAndSymbol> formalParameters =
        callee.isVarArgs()
            ? expandVarargsToArity(callee.getParameters(), sourceNode.args.size())
            : callee.getParameters().stream()
                .map(var -> TypeAndSymbol.create(var.type, var))
                .collect(ImmutableList.toImmutableList());

    // Generate constraints for each argument write.
    Streams.forEachPair(
        formalParameters.stream(),
        sourceNode.getArguments().stream(),
        (formal, actual) -> {
          // formal parameter type (no l-val b/c that would wrongly constrain the method return)
          generateConstraintsForWrite(formal.type(), formal.symbol(), actual, /*lVal=*/ null);
        });

    // Generate constraints for method return
    generateConstraintsFromAnnotations(
        sourceNode.type, callee, callee.getReturnType(), sourceNode, new ArrayDeque<>());

    // If return type is parameterized by a generic type on receiver, collate references to that
    // generic between the receiver and the result/argument types.
    if (!callee.isStatic() && node.getMethodSelect() instanceof JCFieldAccess) {
      JCFieldAccess fieldAccess = ((JCFieldAccess) node.getMethodSelect());
      for (TypeVariableSymbol tvs : fieldAccess.selected.type.tsym.getTypeParameters()) {
        Type rcvrtype = fieldAccess.selected.type.tsym.type;
        // Note this should be a singleton set, one for each type parameter
        ImmutableSet<InferenceVariable> rcvrReferences =
            findUnannotatedTypeVarRefs(tvs, rcvrtype, /*decl=*/ null, fieldAccess.selected);
        Type restype = fieldAccess.sym.type.asMethodType().restype;
        findUnannotatedTypeVarRefs(tvs, restype, fieldAccess.sym, node)
            .forEach(
                resRef ->
                    rcvrReferences.forEach(
                        rcvrRef -> qualifierConstraints.putEdge(resRef, rcvrRef)));
        Streams.forEachPair(
            formalParameters.stream(),
            node.getArguments().stream(),
            (formal, actual) ->
                findUnannotatedTypeVarRefs(tvs, formal.type(), formal.symbol(), actual)
                    .forEach(
                        argRef ->
                            rcvrReferences.forEach(
                                rcvrRef -> qualifierConstraints.putEdge(argRef, rcvrRef))));
      }
    }

    // Get all references to each typeVar in the return type and formal parameters and relate them
    // in the constraint graph; covariant in the return type, contravariant in the argument types.
    // Annotated type var references override the type var's inferred qualifier, so ignore them.
    //
    // Additionally generate equality constraints between inferred types that are instantiations of
    // type parameters.  For instance, if a method type parameter <T> was instantiated List<String>
    // for a given call site m(x), and T appears in the return type as Optional<T>, then the
    // expression's inferred type will be Optional<List<String>> and we generate constraints to
    // equate T[0] = m(x)[0, 0].  If m's parameter's type is T then the argument type's inferred
    // type is List<String> and we also generate constraints to equate T[0] = x[0], which will
    // allow the inference to conclude later that x[0] = m(x)[0, 0], meaning the nullness qualifier
    // for x's <String> is the same as the one for m(x)'s <String>.
    for (TypeVariableSymbol typeVar : callee.getTypeParameters()) {
      TypeVariableInferenceVar typeVarIV = TypeVariableInferenceVar.create(typeVar, node);
      visitUnannotatedTypeVarRefsAndEquateInferredComponents(
          typeVarIV,
          callee.getReturnType(),
          callee,
          node,
          iv -> qualifierConstraints.putEdge(typeVarIV, iv));
      Streams.forEachPair(
          formalParameters.stream(),
          node.getArguments().stream(),
          (formal, actual) ->
              visitUnannotatedTypeVarRefsAndEquateInferredComponents(
                  typeVarIV,
                  formal.type(),
                  formal.symbol(),
                  actual,
                  iv -> qualifierConstraints.putEdge(iv, typeVarIV)));
    }
    return super.visitMethodInvocation(node, unused);
  }

  private static void visitTypeVarRefs(
      TypeVariableSymbol typeVar,
      Type declaredType,
      ArrayDeque<Integer> partialSelector,
      @Nullable Type inferredType,
      TypeComponentConsumer consumer) {
    List<Type> declaredTypeArguments = declaredType.getTypeArguments();
    List<Type> inferredTypeArguments =
        inferredType != null ? inferredType.getTypeArguments() : ImmutableList.of();
    for (int i = 0; i < declaredTypeArguments.size(); i++) {
      partialSelector.push(i);
      visitTypeVarRefs(
          typeVar,
          declaredTypeArguments.get(i),
          partialSelector,
          i < inferredTypeArguments.size() ? inferredTypeArguments.get(i) : null,
          consumer);
      partialSelector.pop();
    }
    if (declaredType.tsym.equals(typeVar)) {
      consumer.accept(declaredType, partialSelector, inferredType);
    }
  }

  @FunctionalInterface
  private interface TypeComponentConsumer {
    void accept(
        Type declaredType, ArrayDeque<Integer> declaredTypeSelector, @Nullable Type inferredType);
  }

  private static ImmutableSet<InferenceVariable> findUnannotatedTypeVarRefs(
      TypeVariableSymbol typeVar, Type declaredType, @Nullable Symbol decl, Tree sourceNode) {
    ImmutableSet.Builder<InferenceVariable> result = ImmutableSet.builder();
    visitTypeVarRefs(
        typeVar,
        declaredType,
        new ArrayDeque<>(),
        null,
        (typeVarRef, selector, unused) -> {
          if (!extractExplicitNullness(typeVarRef, selector.isEmpty() ? decl : null).isPresent()) {
            result.add(TypeArgInferenceVar.create(ImmutableList.copyOf(selector), sourceNode));
          }
        });
    return result.build();
  }

  private void visitUnannotatedTypeVarRefsAndEquateInferredComponents(
      TypeVariableInferenceVar typeVar,
      Type type,
      @Nullable Symbol decl,
      Tree sourceNode,
      Consumer<TypeArgInferenceVar> consumer) {
    visitTypeVarRefs(
        typeVar.typeVar(),
        type,
        new ArrayDeque<>(),
        ((JCExpression) sourceNode).type,
        (declaredType, selector, inferredType) -> {
          if (!extractExplicitNullness(type, selector.isEmpty() ? decl : null).isPresent()) {
            consumer.accept(TypeArgInferenceVar.create(ImmutableList.copyOf(selector), sourceNode));
          }

          if (inferredType == null) {
            return;
          }

          List<Type> typeArguments = inferredType.getTypeArguments();
          int depth = selector.size();
          for (int i = 0; i < typeArguments.size(); ++i) {
            selector.push(i);
            visitTypeComponents(
                typeArguments.get(i),
                selector,
                sourceNode,
                typeArg -> {
                  TypeVariableInferenceVar typeVarComponent =
                      typeVar.withSelector(
                          typeArg
                              .typeArgSelector()
                              .subList(depth, typeArg.typeArgSelector().size()));
                  qualifierConstraints.putEdge(typeVarComponent, typeArg);
                  qualifierConstraints.putEdge(typeArg, typeVarComponent);
                });
            selector.pop();
          }
        });
  }

  private void visitTypeComponents(
      Type type,
      ArrayDeque<Integer> partialSelector,
      Tree sourceNode,
      Consumer<TypeArgInferenceVar> consumer) {
    List<Type> typeArguments = type.getTypeArguments();
    for (int i = 0; i < typeArguments.size(); ++i) {
      partialSelector.push(i);
      visitTypeComponents(typeArguments.get(i), partialSelector, sourceNode, consumer);
      partialSelector.pop();
    }

    consumer.accept(TypeArgInferenceVar.create(ImmutableList.copyOf(partialSelector), sourceNode));
  }

  private static void findUnannotatedTypeVarRefs(
      TypeVariableSymbol typeVar,
      Tree sourceNode,
      Type type,
      @Nullable Symbol decl,
      ArrayDeque<Integer> partialSelector,
      ImmutableSet.Builder<InferenceVariable> resultBuilder) {
    checkArgument(decl == null || partialSelector.isEmpty());
    List<Type> typeArguments = type.getTypeArguments();
    for (int i = 0; i < typeArguments.size(); i++) {
      partialSelector.push(i);
      findUnannotatedTypeVarRefs(
          typeVar,
          sourceNode,
          typeArguments.get(i),
          /*decl=*/ null,
          partialSelector,
          resultBuilder);
      partialSelector.pop();
    }
    if (type.tsym.equals(typeVar) && !extractExplicitNullness(type, decl).isPresent()) {
      resultBuilder.add(
          TypeArgInferenceVar.create(ImmutableList.copyOf(partialSelector), sourceNode));
    }
  }

  private static Optional<Nullness> extractExplicitNullness(
      @Nullable Type type, @Nullable Symbol symbol) {
    if (symbol != null) {
      Optional<Nullness> result = NullnessAnnotations.fromAnnotationsOn(symbol);
      if (result.isPresent()) {
        return result;
      }
    }
    return NullnessAnnotations.fromAnnotationsOn(type);
  }

  /**
   * Generate inference variable constraints derived from this write, including proper bounds from
   * type annotations on the declared type {@code lType} of the r-val as well as relationships
   * between type parameters of the l-val and r-val (if given). l-val is optional so this method is
   * usable for method arguments, and note that the l-val is a statement in other cases (return and
   * variable declarations); the l-val only appears useful when it's an assignment
   */
  private void generateConstraintsForWrite(
      Type lType, @Nullable Symbol decl, ExpressionTree rVal, @Nullable Tree lVal) {
    // TODO(kmb): Consider just visiting these expression types
    if (rVal.getKind() == Kind.NULL_LITERAL) {
      qualifierConstraints.putEdge(
          ProperInferenceVar.NULL, TypeArgInferenceVar.create(ImmutableList.of(), rVal));
      qualifierConstraints.putEdge(
          TypeArgInferenceVar.create(ImmutableList.of(), rVal), ProperInferenceVar.NULL);
    } else if ((rVal instanceof LiteralTree)
        || (rVal instanceof NewClassTree)
        || (rVal instanceof NewArrayTree)
        || ((rVal instanceof IdentifierTree)
            && ((IdentifierTree) rVal).getName().contentEquals("this"))) {
      qualifierConstraints.putEdge(
          ProperInferenceVar.NONNULL, TypeArgInferenceVar.create(ImmutableList.of(), rVal));
      qualifierConstraints.putEdge(
          TypeArgInferenceVar.create(ImmutableList.of(), rVal), ProperInferenceVar.NONNULL);
    }
    generateConstraintsForWrite(lType, decl, rVal, lVal, new ArrayDeque<>());
  }

  private void generateConstraintsForWrite(
      Type lType,
      @Nullable Symbol decl,
      ExpressionTree rVal,
      @Nullable Tree lVal,
      ArrayDeque<Integer> argSelector) {
    checkArgument(decl == null || argSelector.isEmpty());
    List<Type> typeArguments = lType.getTypeArguments();
    for (int i = 0; i < typeArguments.size(); i++) {
      argSelector.push(i);
      generateConstraintsForWrite(typeArguments.get(i), /*decl=*/ null, rVal, lVal, argSelector);
      argSelector.pop();
    }

    ImmutableList<Integer> argSelectorList = ImmutableList.copyOf(argSelector);

    // If there is an explicit annotation, trust it and constrain the corresponding type arg
    // inference variable to be equal to that proper inference variable.
    boolean isBound = false;
    Optional<Nullness> fromAnnotations = extractExplicitNullness(lType, decl);
    if (!fromAnnotations.isPresent()) {
      fromAnnotations = NullnessAnnotations.getUpperBound(lType);
      isBound = true;
    }
    // Top-level target types implicitly only constrain from above: for instance, a method
    // parameter annotated @Nullable can be called with a non-null argument just fine. Same
    // goes for bounded type parameters and ? extends @Nullable type parameters, but not for
    // invariant generic type parameters such as List<@Nullable String> which rVal needs to
    // satisfy exactly, so we generate equality constraints for those.
    boolean oneSided = isBound || argSelector.isEmpty();
    fromAnnotations
        .map(ProperInferenceVar::create)
        .ifPresent(
            annot -> {
              InferenceVariable var = TypeArgInferenceVar.create(argSelectorList, rVal);
              qualifierConstraints.putEdge(var, annot);
              if (!oneSided) {
                qualifierConstraints.putEdge(annot, var);
              }
            });

    if (lVal != null) {
      // Constrain this type or type argument on the rVal to be <= its lVal counterpart
      qualifierConstraints.putEdge(
          TypeArgInferenceVar.create(argSelectorList, rVal),
          TypeArgInferenceVar.create(argSelectorList, lVal));
    }
  }

  /** Pair of a {@link Type} and an optional {@link Symbol}. */
  @AutoValue
  abstract static class TypeAndSymbol {
    static TypeAndSymbol create(Type type) {
      return create(type, /*symbol=*/ null);
    }

    static TypeAndSymbol create(Type type, @Nullable VarSymbol symbol) {
      return new AutoValue_NullnessQualifierInference_TypeAndSymbol(type, symbol);
    }

    abstract Type type();

    @Nullable
    abstract VarSymbol symbol();
  }
}
