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
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.TreeInfo;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
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
    Type declaredType = ((JCIdent) node).sym.type;
    generateConstraintsFromAnnotations(declaredType, node, new ArrayDeque<>());
    return super.visitIdentifier(node, unused);
  }

  private void generateConstraintsFromAnnotations(
      Type type, Tree sourceTree, ArrayDeque<Integer> argSelector) {
    List<Type> typeArguments = type.getTypeArguments();
    int numberOfTypeArgs = typeArguments.size();
    for (int i = 0; i < numberOfTypeArgs; i++) {
      argSelector.push(i);
      generateConstraintsFromAnnotations(typeArguments.get(i), sourceTree, argSelector);
      argSelector.pop();
    }
    // Use equality constraints even for top-level type, since we want to "trust" the annotation
    // TODO(b/121398981): skip for T extends @<Annot> since they constrain one side only
    ProperInferenceVar.fromTypeIfAnnotated(type)
        .ifPresent(
            annot -> {
              qualifierConstraints.putEdge(
                  TypeArgInferenceVar.create(ImmutableList.copyOf(argSelector), sourceTree), annot);
              qualifierConstraints.putEdge(
                  annot, TypeArgInferenceVar.create(ImmutableList.copyOf(argSelector), sourceTree));
            });
  }

  private void generateConstraintsFromAnnotations(
      MethodSymbol symbol, JCMethodInvocation sourceTree, ArrayDeque<Integer> argSelector) {
    List<Type> typeArguments = sourceTree.type.getTypeArguments();
    int numberOfTypeArgs = typeArguments.size();
    for (int i = 0; i < numberOfTypeArgs; i++) {
      argSelector.push(i);
      generateConstraintsFromAnnotations(typeArguments.get(i), sourceTree, argSelector);
      argSelector.pop();
    }

    // First check if the given symbol is directly annotated; if not, look for implicit annotations
    // on the inferred type of the expression.  The latter for instance propagates a type parameter
    // T instantiated as <@Nullable String> to the result of a method returning T.
    Optional<InferenceVariable> fromAnnotations =
        Nullness.fromAnnotationsOn(symbol).map(ProperInferenceVar::create);
    if (!fromAnnotations.isPresent()) {
      fromAnnotations = ProperInferenceVar.fromTypeIfAnnotated(sourceTree.type);
    }
    // Use equality constraints here, since we want to "trust" the annotation.  For instance,
    // a method return annotated @Nullable requires us assume the method might really return null,
    // and a method return annotated @Nonnull should allow us to assume it really returns non-null.
    fromAnnotations.ifPresent(
        annot -> {
          qualifierConstraints.putEdge(
              TypeArgInferenceVar.create(ImmutableList.copyOf(argSelector), sourceTree), annot);
          // TODO(b/121398981): skip for T extends @<Annot> since they constrain one side only
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
          // TODO(b/116977632): constraints for actual parameter type (i.e. after type variable
          // substitution) without ignoring annotations directly on the parameter or vararg
          generateConstraintsForWrite(formal.type(), formal.symbol(), actual, /*lVal=*/ null);
        });

    // Generate constraints for method return
    generateConstraintsFromAnnotations(callee, sourceNode, new ArrayDeque<>());

    // If return type is parameterized by a generic type on receiver, collate references to that
    // generic between the receiver and the result/argument types.
    if (node.getMethodSelect() instanceof JCFieldAccess) {
      JCFieldAccess fieldAccess = ((JCFieldAccess) node.getMethodSelect());
      for (TypeVariableSymbol tvs : fieldAccess.selected.type.tsym.getTypeParameters()) {
        Type rcvrtype = fieldAccess.selected.type.tsym.type;
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
    // TODO(b/116977632): generate constraints for the instantiation of type parameters, e.g.,
    //    if type parameter T was instantiated List<String>, we not only need to capture constraints
    //    for T == List<String> itself, but also for <String>.  This may be as simple as introducing
    //    selectors into TypeVariableInferenceVar and then relating
    //    T[0] < node[0] and arg[0] < T[0], for all generic parameters of T's instantiation
    for (TypeVariableSymbol typeVar : callee.getTypeParameters()) {
      InferenceVariable typeVarIV = TypeVariableInferenceVar.create(typeVar, node);
      for (InferenceVariable iv :
          findUnannotatedTypeVarRefs(typeVar, callee.getReturnType(), callee, node)) {
        qualifierConstraints.putEdge(typeVarIV, iv);
      }
      Streams.forEachPair(
          formalParameters.stream(),
          node.getArguments().stream(),
          (formal, actual) -> {
            for (InferenceVariable iv :
                findUnannotatedTypeVarRefs(typeVar, formal.type(), formal.symbol(), actual)) {
              qualifierConstraints.putEdge(iv, typeVarIV);
            }
          });
    }
    return super.visitMethodInvocation(node, unused);
  }

  private static ImmutableSet<InferenceVariable> findUnannotatedTypeVarRefs(
      TypeVariableSymbol typeVar, Type type, @Nullable Symbol decl, Tree sourceNode) {
    ImmutableSet.Builder<InferenceVariable> result = ImmutableSet.builder();
    findUnannotatedTypeVarRefs(typeVar, sourceNode, type, decl, new ArrayDeque<>(), result);
    return result.build();
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

  private static Optional<Nullness> extractExplicitNullness(Type type, @Nullable Symbol symbol) {
    if (symbol != null) {
      Optional<Nullness> result = Nullness.fromAnnotationsOn(symbol);
      if (result.isPresent()) {
        return result;
      }
    }
    return toNullness(type.getAnnotationMirrors());
  }

  private static Optional<Nullness> toNullness(List<?> annotations) {
    return Nullness.fromAnnotations(
        annotations.stream().map(Object::toString).collect(ImmutableList.toImmutableList()));
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
    Optional<InferenceVariable> fromAnnotations =
        extractExplicitNullness(lType, decl).map(ProperInferenceVar::create);
    if (!fromAnnotations.isPresent()) {
      fromAnnotations = ProperInferenceVar.fromTypeIfAnnotated(lType);
    }
    fromAnnotations.ifPresent(
        annot -> {
          qualifierConstraints.putEdge(TypeArgInferenceVar.create(argSelectorList, rVal), annot);
          if (!argSelector.isEmpty()) {
            // Top-level target types implicitly only constrain from above: for instance, a
            // local variable annotated @Nullable can be initialized with a non-null value just
            // fine.  This isn't true for invariant generic type parameters such as
            // List<@Nullable String> which rVal needs to satisfy exactly, so we generate
            // equality constraints for those.
            // TODO(b/121398981): skip for ? extends @<Annot> since they constrain one side only
            qualifierConstraints.putEdge(annot, TypeArgInferenceVar.create(argSelectorList, rVal));
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
