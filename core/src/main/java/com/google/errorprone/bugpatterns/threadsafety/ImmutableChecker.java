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
import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Description.NO_MATCH;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.annotations.Immutable;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MemberReferenceTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.NewClassTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.TypeParameterTreeMatcher;
import com.google.errorprone.bugpatterns.threadsafety.ImmutableAnalysis.ViolationReporter;
import com.google.errorprone.bugpatterns.threadsafety.ThreadSafety.Violation;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Description.Builder;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.TypeVariableSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree.JCMemberReference;
import com.sun.tools.javac.tree.JCTree.JCNewClass;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
    name = "Immutable",
    summary = "Type declaration annotated with @Immutable is not immutable",
    category = JDK,
    severity = ERROR,
    documentSuppression = false,
    providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION)
public class ImmutableChecker extends BugChecker
    implements ClassTreeMatcher,
        NewClassTreeMatcher,
        TypeParameterTreeMatcher,
        MethodInvocationTreeMatcher,
        MemberReferenceTreeMatcher {

  private final WellKnownMutability wellKnownMutability;
  private final ImmutableSet<String> immutableAnnotations;

  @Deprecated // Used reflectively, but you should pass in ErrorProneFlags to get custom mutability
  public ImmutableChecker() {
    this(ErrorProneFlags.empty());
  }

  ImmutableChecker(ImmutableSet<String> immutableAnnotations) {
    this(ErrorProneFlags.empty(), immutableAnnotations);
  }

  public ImmutableChecker(ErrorProneFlags flags) {
    this(flags, ImmutableSet.of(Immutable.class.getName()));
  }

  private ImmutableChecker(ErrorProneFlags flags, ImmutableSet<String> immutableAnnotations) {
    this.wellKnownMutability = WellKnownMutability.fromFlags(flags);
    this.immutableAnnotations = immutableAnnotations;
  }

  // check instantiations of `@ImmutableTypeParameter`s in method references
  @Override
  public Description matchMemberReference(MemberReferenceTree tree, VisitorState state) {
    return checkInvocation(
        tree, ((JCMemberReference) tree).referentType, state, ASTHelpers.getSymbol(tree));
  }

  // check instantiations of `@ImmutableTypeParameter`s in method invocations
  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    return checkInvocation(
        tree, ASTHelpers.getType(tree.getMethodSelect()), state, ASTHelpers.getSymbol(tree));
  }

  @Override
  public Description matchNewClass(NewClassTree tree, VisitorState state) {
    // check instantiations of `@ImmutableTypeParameter`s in generic constructor invocations
    checkInvocation(
        tree, ((JCNewClass) tree).constructorType, state, ((JCNewClass) tree).constructor);
    // check instantiations of `@ImmutableTypeParameter`s in class constructor invocations
    ImmutableAnalysis analysis = createImmutableAnalysis(state);
    Violation info =
        analysis.checkInstantiation(
            ASTHelpers.getSymbol(tree.getIdentifier()).getTypeParameters(),
            ASTHelpers.getType(tree).getTypeArguments());
    if (info.isPresent()) {
      state.reportMatch(buildDescription(tree).setMessage(info.message()).build());
    }
    return NO_MATCH;
  }

  private ImmutableAnalysis createImmutableAnalysis(VisitorState state) {
    return new ImmutableAnalysis(this, state, wellKnownMutability, immutableAnnotations);
  }

  private Description checkInvocation(
      Tree tree, Type methodType, VisitorState state, Symbol symbol) {
    ImmutableAnalysis analysis = createImmutableAnalysis(state);
    Violation info = analysis.checkInvocation(methodType, symbol);
    if (info.isPresent()) {
      state.reportMatch(buildDescription(tree).setMessage(info.message()).build());
    }
    return NO_MATCH;
  }

  @Override
  public Description matchTypeParameter(TypeParameterTree tree, VisitorState state) {
    Symbol sym = ASTHelpers.getSymbol(tree);
    if (sym == null) {
      return NO_MATCH;
    }
    ImmutableAnalysis analysis = createImmutableAnalysis(state);
    if (!analysis.hasThreadSafeTypeParameterAnnotation((TypeVariableSymbol) sym)) {
      return NO_MATCH;
    }
    switch (sym.owner.getKind()) {
      case METHOD:
      case CONSTRUCTOR:
        return NO_MATCH;
      default: // fall out
    }
    AnnotationInfo info = analysis.getImmutableAnnotation(sym.owner, state);
    if (info == null) {
      return buildDescription(tree)
          .setMessage("@Immutable is only supported on immutable classes")
          .build();
    }
    return NO_MATCH;
  }

  @Override
  public Description matchClass(ClassTree tree, VisitorState state) {
    ImmutableAnalysis analysis = createImmutableAnalysis(state);
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
      typarams.put(
          typaram.getName().toString(), (TypeVariableSymbol) ASTHelpers.getSymbol(typaram));
    }
    SetView<String> difference = Sets.difference(annotation.containerOf(), typarams.keySet());
    if (!difference.isEmpty()) {
      return buildDescription(tree)
          .setMessage(
              String.format(
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
            .map(Entry::getKey)
            .collect(toImmutableSet());
    if (!immutableAndContainer.isEmpty()) {
      return buildDescription(tree)
          .setMessage(
              String.format(
                  "using both @ImmutableTypeParameter and containerOf is redundant: %s",
                  Joiner.on("', '").join(immutableAndContainer)))
          .build();
    }

    // Main path for @Immutable-annotated types:
    //
    // Check that the fields (including inherited fields) are immutable, and
    // validate the type hierarchy superclass.

    ClassSymbol sym = ASTHelpers.getSymbol(tree);

    Violation info =
        analysis.checkForImmutability(
            Optional.of(tree),
            immutableTypeParametersInScope(ASTHelpers.getSymbol(tree), state, analysis),
            ASTHelpers.getType(tree),
            (Tree matched, Violation violation) ->
                describeClass(matched, sym, annotation, violation));

    if (!info.isPresent()) {
      return NO_MATCH;
    }

    return describeClass(tree, sym, annotation, info).build();
  }

  private Description.Builder describeClass(
      Tree tree, ClassSymbol sym, AnnotationInfo annotation, Violation info) {
    String message;
    if (sym.getQualifiedName().contentEquals(annotation.typeName())) {
      message = "type annotated with @Immutable could not be proven immutable: " + info.message();
    } else {
      message =
          String.format(
              "Class extends @Immutable type %s, but is not immutable: %s",
              annotation.typeName(), info.message());
    }
    return buildDescription(tree).setMessage(message);
  }

  // Anonymous classes

  /** Check anonymous implementations of {@code @Immutable} types. */
  private Description handleAnonymousClass(
      ClassTree tree, VisitorState state, ImmutableAnalysis analysis) {
    ClassSymbol sym = ASTHelpers.getSymbol(tree);
    if (sym == null) {
      return NO_MATCH;
    }
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
              public Builder describe(Tree tree, Violation info) {
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
        String.format(
            "Class extends @Immutable type %s, but is not immutable: %s",
            superType, info.message());
    return buildDescription(tree).setMessage(message);
  }

  // Strong behavioural subtyping

  /** Check for classes without {@code @Immutable} that have immutable supertypes. */
  private Description checkSubtype(ClassTree tree, VisitorState state) {
    ClassSymbol sym = ASTHelpers.getSymbol(tree);
    if (sym == null) {
      return NO_MATCH;
    }
    Type superType = immutableSupertype(sym, state);
    if (superType == null) {
      return NO_MATCH;
    }
    String message =
        String.format(
            "Class extends @Immutable type %s, but is not annotated as immutable", superType);
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
      if (superType.equals(sym.type)) {
        continue;
      }
      // Don't use getImmutableAnnotation here: subtypes of trusted types are
      // also trusted, only check for explicitly annotated supertypes.
      if (immutableAnnotations.stream()
          .anyMatch(annotation -> ASTHelpers.hasAnnotation(superType.tsym, annotation, state))) {
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
   * @Immutable(containerOf="T") class C<T> {
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
