/*
 * Copyright 2015 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.threadsafety;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.common.collect.Streams.stream;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getReceiver;
import static com.google.errorprone.util.ASTHelpers.getReceiverType;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.hasAnnotation;
import static com.google.errorprone.util.ASTHelpers.isSubtype;
import static com.google.errorprone.util.ASTHelpers.targetType;
import static java.lang.String.format;
import static java.util.stream.Collectors.joining;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.google.errorprone.BugPattern;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.annotations.Immutable;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.LambdaExpressionTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MemberReferenceTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.NewClassTreeMatcher;
import com.google.errorprone.bugpatterns.threadsafety.ImmutableAnalysis.ViolationReporter;
import com.google.errorprone.bugpatterns.threadsafety.ThreadSafety.Violation;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.code.Symbol.TypeVariableSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ClassType;
import com.sun.tools.javac.tree.JCTree.JCMemberReference;
import com.sun.tools.javac.tree.JCTree.JCNewClass;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.lang.model.element.ElementKind;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    name = "Immutable",
    summary = "Type declaration annotated with @Immutable is not immutable",
    severity = ERROR,
    documentSuppression = false)
public class ImmutableChecker extends BugChecker
    implements ClassTreeMatcher,
        LambdaExpressionTreeMatcher,
        NewClassTreeMatcher,
        MethodInvocationTreeMatcher,
        MethodTreeMatcher,
        MemberReferenceTreeMatcher {

  private final WellKnownMutability wellKnownMutability;
  private final ImmutableSet<String> immutableAnnotations;
  private final boolean matchLambdas;

  ImmutableChecker(ImmutableSet<String> immutableAnnotations) {
    this(ErrorProneFlags.empty(), immutableAnnotations);
  }

  public ImmutableChecker(ErrorProneFlags flags) {
    this(flags, ImmutableSet.of(Immutable.class.getName()));
  }

  private ImmutableChecker(ErrorProneFlags flags, ImmutableSet<String> immutableAnnotations) {
    this.wellKnownMutability = WellKnownMutability.fromFlags(flags);
    this.immutableAnnotations = immutableAnnotations;
    this.matchLambdas = flags.getBoolean("ImmutableChecker:MatchLambdas").orElse(true);
  }

  @Override
  public Description matchLambdaExpression(LambdaExpressionTree tree, VisitorState state) {
    if (!matchLambdas) {
      return NO_MATCH;
    }
    TypeSymbol lambdaType = getType(tree).tsym;
    ImmutableAnalysis analysis = createImmutableAnalysis(state);
    Violation info =
        analysis.checkInstantiation(
            lambdaType.getTypeParameters(), getType(tree).getTypeArguments());

    if (info.isPresent()) {
      state.reportMatch(buildDescription(tree).setMessage(info.message()).build());
    }
    if (!hasImmutableAnnotation(lambdaType, state)) {
      return NO_MATCH;
    }
    Set<VarSymbol> variablesClosed = new HashSet<>();
    SetMultimap<ClassSymbol, MethodSymbol> typesClosed = LinkedHashMultimap.create();
    Set<VarSymbol> variablesOwnedByLambda = new HashSet<>();

    new TreePathScanner<Void, Void>() {
      @Override
      public Void visitVariable(VariableTree tree, Void unused) {
        var symbol = getSymbol(tree);
        variablesOwnedByLambda.add(symbol);
        return super.visitVariable(tree, null);
      }

      @Override
      public Void visitMethodInvocation(MethodInvocationTree tree, Void unused) {
        if (getReceiver(tree) == null) {
          var symbol = getSymbol(tree);
          if (!symbol.isStatic()) {
            effectiveTypeOfThis(symbol, getCurrentPath(), state)
                .ifPresent(t -> typesClosed.put(t, symbol));
          }
        }
        return super.visitMethodInvocation(tree, null);
      }

      @Override
      public Void visitMemberSelect(MemberSelectTree tree, Void unused) {
        // Special case the access of fields to allow accessing fields which would pass an immutable
        // check.
        if (tree.getExpression() instanceof IdentifierTree
            && getSymbol(tree) instanceof VarSymbol) {
          handleIdentifier(getSymbol(tree));
          // If we're only seeing a field access, don't complain about the fact we closed around
          // `this`.
          if (tree.getExpression() instanceof IdentifierTree
              && ((IdentifierTree) tree.getExpression()).getName().contentEquals("this")) {
            return null;
          }
        }
        return super.visitMemberSelect(tree, null);
      }

      @Override
      public Void visitIdentifier(IdentifierTree tree, Void unused) {
        handleIdentifier(getSymbol(tree));
        return super.visitIdentifier(tree, null);
      }

      private void handleIdentifier(Symbol symbol) {
        if (symbol instanceof VarSymbol && !variablesOwnedByLambda.contains(symbol)) {
          variablesClosed.add((VarSymbol) symbol);
        }
      }
    }.scan(state.getPath(), null);

    ImmutableSet<String> typarams =
        immutableTypeParametersInScope(getSymbol(tree), state, analysis);
    variablesClosed.stream()
        .map(closedVariable -> checkClosedLambdaVariable(closedVariable, tree, typarams, analysis))
        .filter(Violation::isPresent)
        .forEachOrdered(
            v -> {
              String message = formLambdaReason(lambdaType) + ", but " + v.message();
              state.reportMatch(buildDescription(tree).setMessage(message).build());
            });
    for (var entry : typesClosed.asMap().entrySet()) {
      var classSymbol = entry.getKey();
      var methods = entry.getValue();
      if (!hasImmutableAnnotation(classSymbol.type.tsym, state)) {
        String message =
            format(
                "%s, but accesses instance method(s) '%s' on '%s' which is not @Immutable.",
                formLambdaReason(lambdaType),
                methods.stream().map(Symbol::getSimpleName).collect(joining(", ")),
                classSymbol.getSimpleName());
        state.reportMatch(buildDescription(tree).setMessage(message).build());
      }
    }

    return NO_MATCH;
  }

  /**
   * Gets the effective type of `this`, had the bare invocation of {@code symbol} been qualified
   * with it.
   */
  private static Optional<ClassSymbol> effectiveTypeOfThis(
      MethodSymbol symbol, TreePath currentPath, VisitorState state) {
    return stream(currentPath.iterator())
        .filter(ClassTree.class::isInstance)
        .map(t -> ASTHelpers.getSymbol((ClassTree) t))
        .filter(c -> isSubtype(c.type, symbol.owner.type, state))
        .findFirst();
  }

  private Violation checkClosedLambdaVariable(
      VarSymbol closedVariable,
      LambdaExpressionTree tree,
      ImmutableSet<String> typarams,
      ImmutableAnalysis analysis) {
    if (!closedVariable.getKind().equals(ElementKind.FIELD)) {
      return analysis.isThreadSafeType(false, typarams, closedVariable.type);
    }
    return analysis.isFieldImmutable(
        Optional.empty(),
        typarams,
        (ClassSymbol) closedVariable.owner,
        (ClassType) closedVariable.owner.type,
        closedVariable,
        (t, v) -> buildDescription(tree));
  }

  private static String formLambdaReason(TypeSymbol typeSymbol) {
    return "This lambda implements @Immutable interface '" + typeSymbol.getSimpleName() + "'";
  }

  private boolean hasImmutableAnnotation(TypeSymbol tsym, VisitorState state) {
    return immutableAnnotations.stream()
        .anyMatch(annotation -> hasAnnotation(tsym, annotation, state));
  }

  @Override
  public Description matchMemberReference(MemberReferenceTree tree, VisitorState state) {
    // check instantiations of `@ImmutableTypeParameter`s in method references
    checkInvocation(tree, getSymbol(tree), ((JCMemberReference) tree).referentType, state);
    if (!matchLambdas) {
      return NO_MATCH;
    }
    ImmutableAnalysis analysis = createImmutableAnalysis(state);
    TypeSymbol memberReferenceType = targetType(state).type().tsym;
    Violation info =
        analysis.checkInstantiation(
            memberReferenceType.getTypeParameters(), getType(tree).getTypeArguments());

    if (info.isPresent()) {
      state.reportMatch(buildDescription(tree).setMessage(info.message()).build());
    }
    if (!hasImmutableAnnotation(memberReferenceType, state)) {
      return NO_MATCH;
    }
    if (getSymbol(getReceiver(tree)) instanceof ClassSymbol) {
      return NO_MATCH;
    }
    var receiverType = getReceiverType(tree);
    ImmutableSet<String> typarams =
        immutableTypeParametersInScope(getSymbol(tree), state, analysis);
    var violation =
        analysis.isThreadSafeType(/* allowContainerTypeParameters= */ true, typarams, receiverType);
    if (violation.isPresent()) {
      return buildDescription(tree)
          .setMessage(
              "This method reference implements @Immutable interface "
                  + memberReferenceType.getSimpleName()
                  + ", but "
                  + violation.message())
          .build();
    }
    return NO_MATCH;
  }

  // check instantiations of `@ImmutableTypeParameter`s in method invocations
  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    checkInvocation(tree, getSymbol(tree), ASTHelpers.getType(tree.getMethodSelect()), state);
    return NO_MATCH;
  }

  @Override
  public Description matchNewClass(NewClassTree tree, VisitorState state) {
    // check instantiations of `@ImmutableTypeParameter`s in generic constructor invocations
    checkInvocation(tree, getSymbol(tree), ((JCNewClass) tree).constructorType, state);
    // check instantiations of `@ImmutableTypeParameter`s in class constructor invocations
    checkInstantiation(
        tree,
        state,
        getSymbol(tree.getIdentifier()).getTypeParameters(),
        ASTHelpers.getType(tree).getTypeArguments());

    ClassTree classBody = tree.getClassBody();

    // Only anonymous classes have a body next to the new operator.
    if (classBody != null) {
      // check instantiations of `@ImmutableTypeParameter`s in anonymous class constructor
      // invocations
      checkClassTreeInstantiation(classBody, state, createImmutableAnalysis(state));
    }

    return NO_MATCH;
  }

  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    checkInstantiation(
        tree,
        state,
        getSymbol(tree).getTypeParameters(),
        ASTHelpers.getType(tree).getTypeArguments());
    return NO_MATCH;
  }

  private ImmutableAnalysis createImmutableAnalysis(VisitorState state) {
    return new ImmutableAnalysis(this, state, wellKnownMutability, immutableAnnotations);
  }

  private void checkInvocation(
      Tree tree, MethodSymbol symbol, Type methodType, VisitorState state) {
    ImmutableAnalysis analysis = createImmutableAnalysis(state);
    Violation info = analysis.checkInvocation(methodType, symbol);
    if (info.isPresent()) {
      state.reportMatch(buildDescription(tree).setMessage(info.message()).build());
    }
  }

  private void checkInstantiation(
      Tree tree,
      VisitorState state,
      ImmutableAnalysis analysis,
      Collection<TypeVariableSymbol> typeParameters,
      Collection<Type> typeArguments) {
    Violation info = analysis.checkInstantiation(typeParameters, typeArguments);

    if (info.isPresent()) {
      state.reportMatch(buildDescription(tree).setMessage(info.message()).build());
    }
  }

  private void checkInstantiation(
      Tree tree,
      VisitorState state,
      Collection<TypeVariableSymbol> typeParameters,
      Collection<Type> typeArguments) {
    checkInstantiation(tree, state, createImmutableAnalysis(state), typeParameters, typeArguments);
  }

  @Override
  public Description matchClass(ClassTree tree, VisitorState state) {
    ImmutableAnalysis analysis = createImmutableAnalysis(state);
    checkClassTreeInstantiation(tree, state, analysis);

    if (tree.getSimpleName().length() == 0) {
      // anonymous classes have empty names
      // TODO(cushon): once Java 8 happens, require @Immutable on anonymous classes
      return handleAnonymousClass(tree, state, analysis);
    }

    AnnotationInfo annotation = analysis.getImmutableAnnotation(tree, state);
    if (annotation == null) {
      // If the type isn't annotated we don't check for immutability, but we do
      // report an error if it extends/implements any @Immutable-annotated types.
      return checkSubtype(tree, state);
    }

    // Special-case visiting declarations of known-immutable types; these uses
    // of the annotation are "trusted".
    if (wellKnownMutability.getKnownImmutableClasses().containsValue(annotation)) {
      return NO_MATCH;
    }

    // Check that the types in containerOf actually exist
    Map<String, TypeVariableSymbol> typarams = new HashMap<>();
    for (TypeParameterTree typaram : tree.getTypeParameters()) {
      typarams.put(typaram.getName().toString(), (TypeVariableSymbol) getSymbol(typaram));
    }
    SetView<String> difference = Sets.difference(annotation.containerOf(), typarams.keySet());
    if (!difference.isEmpty()) {
      return buildDescription(tree)
          .setMessage(
              format(
                  "could not find type(s) referenced by containerOf: %s",
                  Joiner.on("', '").join(difference)))
          .build();
    }
    ImmutableSet<String> immutableAndContainer =
        typarams.entrySet().stream()
            .filter(
                e ->
                    annotation.containerOf().contains(e.getKey())
                        && analysis.hasThreadSafeTypeParameterAnnotation(e.getValue()))
            .map(Map.Entry::getKey)
            .collect(toImmutableSet());
    if (!immutableAndContainer.isEmpty()) {
      return buildDescription(tree)
          .setMessage(
              format(
                  "using both @ImmutableTypeParameter and containerOf is redundant: %s",
                  Joiner.on("', '").join(immutableAndContainer)))
          .build();
    }

    // Main path for @Immutable-annotated types:
    //
    // Check that the fields (including inherited fields) are immutable, and
    // validate the type hierarchy superclass.

    ClassSymbol sym = getSymbol(tree);

    Violation info =
        analysis.checkForImmutability(
            Optional.of(tree),
            immutableTypeParametersInScope(getSymbol(tree), state, analysis),
            ASTHelpers.getType(tree),
            (Tree matched, Violation violation) ->
                describeClass(matched, sym, annotation, violation));

    if (!info.isPresent()) {
      return NO_MATCH;
    }

    return describeClass(tree, sym, annotation, info).build();
  }

  private void checkClassTreeInstantiation(
      ClassTree tree, VisitorState state, ImmutableAnalysis analysis) {
    for (Tree implementTree : tree.getImplementsClause()) {
      checkInstantiation(
          tree,
          state,
          analysis,
          getSymbol(implementTree).getTypeParameters(),
          ASTHelpers.getType(implementTree).getTypeArguments());
    }

    Tree extendsClause = tree.getExtendsClause();
    if (extendsClause != null) {
      checkInstantiation(
          tree,
          state,
          analysis,
          getSymbol(extendsClause).getTypeParameters(),
          ASTHelpers.getType(extendsClause).getTypeArguments());
    }
  }

  private Description.Builder describeClass(
      Tree tree, ClassSymbol sym, AnnotationInfo annotation, Violation info) {
    String message;
    if (sym.getQualifiedName().contentEquals(annotation.typeName())) {
      message = "type annotated with @Immutable could not be proven immutable: " + info.message();
    } else {
      message =
          format(
              "Class extends @Immutable type %s, but is not immutable: %s",
              annotation.typeName(), info.message());
    }
    return buildDescription(tree).setMessage(message);
  }

  // Anonymous classes

  /** Check anonymous implementations of {@code @Immutable} types. */
  private Description handleAnonymousClass(
      ClassTree tree, VisitorState state, ImmutableAnalysis analysis) {
    ClassSymbol sym = getSymbol(tree);
    Type superType = immutableSupertype(sym, state);
    if (superType == null) {
      return NO_MATCH;
    }
    // We don't need to check that the superclass has an immutable instantiation.
    // The anonymous instance can only be referred to using a superclass type, so
    // the type arguments will be validated at any type use site where we care about
    // the instance's immutability.
    //
    // Also, we have no way to express something like:
    //
    // public static <@Immutable T> ImmutableBox<T> create(T t) {
    //   return new ImmutableBox<>(t);
    // }
    ImmutableSet<String> typarams = immutableTypeParametersInScope(sym, state, analysis);
    Violation info =
        analysis.areFieldsImmutable(
            Optional.of(tree),
            typarams,
            ASTHelpers.getType(tree),
            new ViolationReporter() {
              @Override
              public Description.Builder describe(Tree tree, Violation info) {
                return describeAnonymous(tree, superType, info);
              }
            });
    if (!info.isPresent()) {
      return NO_MATCH;
    }
    return describeAnonymous(tree, superType, info).build();
  }

  private Description.Builder describeAnonymous(Tree tree, Type superType, Violation info) {
    String message =
        format(
            "Class extends @Immutable type %s, but is not immutable: %s",
            superType, info.message());
    return buildDescription(tree).setMessage(message);
  }

  // Strong behavioural subtyping

  /** Check for classes without {@code @Immutable} that have immutable supertypes. */
  private Description checkSubtype(ClassTree tree, VisitorState state) {
    ClassSymbol sym = getSymbol(tree);
    Type superType = immutableSupertype(sym, state);
    if (superType == null) {
      return NO_MATCH;
    }
    String message =
        format("Class extends @Immutable type %s, but is not annotated as immutable", superType);
    Fix fix =
        SuggestedFix.builder()
            .prefixWith(tree, "@Immutable ")
            .addImport(Immutable.class.getName())
            .build();
    return buildDescription(tree).setMessage(message).addFix(fix).build();
  }

  /**
   * Returns the type of the first superclass or superinterface in the hierarchy annotated with
   * {@code @Immutable}, or {@code null} if no such super type exists.
   */
  private Type immutableSupertype(Symbol sym, VisitorState state) {
    for (Type superType : state.getTypes().closure(sym.type)) {
      if (superType.tsym.equals(sym.type.tsym)) {
        continue;
      }
      // Don't use getImmutableAnnotation here: subtypes of trusted types are
      // also trusted, only check for explicitly annotated supertypes.
      if (hasImmutableAnnotation(superType.tsym, state)) {
        return superType;
      }
      // We currently trust that @interface annotations are immutable, but don't enforce that
      // custom interface implementations are also immutable. That means the check can be
      // defeated by writing a custom mutable annotation implementation, and passing it around
      // using the superclass type.
      //
      // TODO(b/25630189): fix this
      //
      // if (superType.tsym.getKind() == ElementKind.ANNOTATION_TYPE) {
      //   return superType;
      // }
    }
    return null;
  }

  /**
   * Gets the set of in-scope immutable type parameters from the containerOf specs on
   * {@code @Immutable} annotations.
   *
   * <p>Usually only the immediately enclosing declaration is searched, but it's possible to have
   * cases like:
   *
   * <pre>
   * {@code @}Immutable(containerOf="T") class C<T> {
   *   class Inner extends ImmutableCollection<T> {}
   * }
   * </pre>
   */
  private static ImmutableSet<String> immutableTypeParametersInScope(
      Symbol sym, VisitorState state, ImmutableAnalysis analysis) {
    if (sym == null) {
      return ImmutableSet.of();
    }
    ImmutableSet.Builder<String> result = ImmutableSet.builder();
    OUTER:
    for (Symbol s = sym; s.owner != null; s = s.owner) {
      switch (s.getKind()) {
        case INSTANCE_INIT:
          continue;
        case PACKAGE:
          break OUTER;
        default:
          break;
      }
      AnnotationInfo annotation = analysis.getImmutableAnnotation(s, state);
      if (annotation == null) {
        continue;
      }
      for (TypeVariableSymbol typaram : s.getTypeParameters()) {
        String name = typaram.getSimpleName().toString();
        if (annotation.containerOf().contains(name)) {
          result.add(name);
        }
      }
      if (s.isStatic()) {
        break;
      }
    }
    return result.build();
  }
}
