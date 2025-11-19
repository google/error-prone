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

package com.google.errorprone.bugpatterns.threadsafety;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.errorprone.util.ASTHelpers.isStatic;

import com.google.auto.value.AutoBuilder;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.common.collect.Streams;
import com.google.errorprone.VisitorState;
import com.google.errorprone.annotations.Immutable;
import com.google.errorprone.annotations.ImmutableTypeParameter;
import com.google.errorprone.annotations.ThreadSafe;
import com.google.errorprone.annotations.ThreadSafeTypeParameter;
import com.google.errorprone.bugpatterns.CanBeStaticAnalyzer;
import com.google.errorprone.suppliers.Supplier;
import com.google.errorprone.util.ASTHelpers;
import com.google.errorprone.util.MoreAnnotations;
import com.sun.source.tree.ClassTree;
import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Attribute.Compound;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.code.Symbol.TypeVariableSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ArrayType;
import com.sun.tools.javac.code.Type.ClassType;
import com.sun.tools.javac.code.Type.TypeVar;
import com.sun.tools.javac.code.Type.WildcardType;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.util.Name;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.lang.model.element.ElementKind;
import javax.lang.model.type.TypeKind;
import org.jspecify.annotations.Nullable;
import org.pcollections.ConsPStack;

/**
 * A class which gives information about the annotation of types; if a type isn't annotated, {@link
 * Violation} gives information as to why it is not.
 */
public final class ThreadSafety {
  private final VisitorState state;
  private final Purpose purpose;
  private final boolean markerAnnotationInherited;
  private final ThreadSafetyKnownTypes knownTypes;
  private final ImmutableSet<String> markerAnnotations;
  private final ImmutableSet<String> acceptedAnnotations;
  private final ImmutableSet<String> containerOfAnnotation;
  private final ImmutableSet<String> suppressAnnotation;
  private final ImmutableSet<String> typeParameterAnnotation;
  private final ImmutableSet<String> acceptedTypeParameterAnnotation;

  public static Builder builder() {
    return new AutoBuilder_ThreadSafety_Builder()
        .acceptedAnnotations(ImmutableSet.of())
        .containerOfAnnotation(ImmutableSet.of())
        .suppressAnnotation(ImmutableSet.of())
        .typeParameterAnnotation(ImmutableSet.of())
        .acceptedTypeParameterAnnotation(ImmutableSet.of());
  }

  public static Builder threadSafeBuilder(WellKnownThreadSafety wellKnownThreadSafety) {
    Builder builder =
        ThreadSafety.builder()
            .purpose(Purpose.FOR_THREAD_SAFE_CHECKER)
            .markerAnnotationInherited(false)
            .knownTypes(wellKnownThreadSafety)
            .markerAnnotations(ImmutableSet.of(ThreadSafe.class.getName()))
            .acceptedAnnotations(ImmutableSet.of(Immutable.class.getName()))
            .typeParameterAnnotation(ImmutableSet.of(ThreadSafeTypeParameter.class.getName()))
            .acceptedTypeParameterAnnotation(
                ImmutableSet.of(ImmutableTypeParameter.class.getName()));
    return builder;
  }

  /** Builder for {@link ThreadSafety}. */
  @AutoBuilder(ofClass = ThreadSafety.class)
  public abstract static class Builder {

    /** See {@link Purpose}. */
    public abstract Builder purpose(Purpose purpose);

    /** Whether to assume the marker annotation is implicitly inherited by subclasses. */
    public abstract Builder markerAnnotationInherited(boolean markerAnnotationInherited);

    /** Information about known types and whether they're known to be safe or unsafe. */
    public abstract Builder knownTypes(ThreadSafetyKnownTypes knownTypes);

    /**
     * Annotations that will cause a class to be tested with this {@link ThreadSafety} instance; for
     * example, when testing a class for immutability, this should be @Immutable.
     */
    public abstract Builder markerAnnotations(Iterable<String> markerAnnotations);

    /**
     * Annotations that do *not* cause a class to be tested, but which are treated as valid
     * annotations to pass the test; for example, if @ThreadSafe is the marker
     * annotation, @Immutable would be included in this list, as an immutable class is by definition
     * thread-safe.
     */
    public abstract Builder acceptedAnnotations(Iterable<String> acceptedAnnotations);

    /** An annotation which marks a generic parameter as a container type. */
    public abstract Builder containerOfAnnotation(Iterable<String> containerOfAnnotation);

    /** An annotation which, when found on a class, should suppress the test */
    public abstract Builder suppressAnnotation(Iterable<String> suppressAnnotation);

    /**
     * An annotation which, when found on a type parameter, indicates that the type parameter may
     * only be instantiated with thread-safe types.
     */
    public abstract Builder typeParameterAnnotation(Iterable<String> typeParameterAnnotation);

    /**
     * An annotation which, when found on a type parameter, indicates that the type parameter may
     * only be instantiated with thread-safe types.
     */
    public abstract Builder acceptedTypeParameterAnnotation(
        Iterable<String> acceptedTypeParameterAnnotation);

    abstract Builder visitorState(VisitorState state);

    public final ThreadSafety build(VisitorState state) {
      return visitorState(state).build();
    }

    abstract ThreadSafety build();
  }

  /**
   * The {@link ThreadSafety} utility class can be used by either the bug checker that enforces
   * immutability or by the bug checker that enforces thread-safety. Depending on which of these bug
   * checkers is using this utility, different messages become appropriate.
   */
  public enum Purpose {
    /** This is being used by the immutability bug checker */
    FOR_IMMUTABLE_CHECKER {
      @Override
      String mutableOrNonThreadSafe() {
        return "mutable";
      }

      @Override
      String mutableOrNotThreadSafe() {
        return "mutable";
      }
    },

    /** This is being used by the thread-safety bug checker */
    FOR_THREAD_SAFE_CHECKER {
      @Override
      String mutableOrNonThreadSafe() {
        return "non-thread-safe";
      }

      @Override
      String mutableOrNotThreadSafe() {
        return "not thread-safe";
      }
    },
    ;

    /**
     * Returns either the string {@code "mutable"} or {@code "non-thread-safe"} depending on the
     * purpose.
     */
    abstract String mutableOrNonThreadSafe();

    /**
     * Returns either the string {@code "mutable"} or {@code "not thread-safe"} depending on the
     * purpose.
     */
    abstract String mutableOrNotThreadSafe();
  }

  ThreadSafety(
      VisitorState visitorState,
      Purpose purpose,
      boolean markerAnnotationInherited,
      ThreadSafetyKnownTypes knownTypes,
      ImmutableSet<String> markerAnnotations,
      ImmutableSet<String> acceptedAnnotations,
      ImmutableSet<String> containerOfAnnotation,
      ImmutableSet<String> suppressAnnotation,
      ImmutableSet<String> typeParameterAnnotation,
      ImmutableSet<String> acceptedTypeParameterAnnotation) {
    this.state = visitorState;
    this.purpose = purpose;
    this.markerAnnotationInherited = markerAnnotationInherited;
    this.knownTypes = knownTypes;
    this.markerAnnotations = markerAnnotations;
    this.acceptedAnnotations = acceptedAnnotations;
    this.containerOfAnnotation = containerOfAnnotation;
    this.suppressAnnotation = suppressAnnotation;
    this.typeParameterAnnotation = typeParameterAnnotation;
    this.acceptedTypeParameterAnnotation = acceptedTypeParameterAnnotation;
  }

  /**
   * A human-friendly explanation of a thread safety violations.
   *
   * <p>An absent explanation indicates either an annotated type with no violations, or a type
   * without the annotation.
   *
   * @param path The list of steps in the explanation.
   *     <p>Example: ["Foo has field 'xs' of type 'int[]'", "arrays are not thread-safe"]
   */
  public record Violation(ConsPStack<String> path) {
    public static Violation create(ConsPStack<String> path) {
      return new Violation(path);
    }

    /** Returns true if a violation was found. */
    public boolean isPresent() {
      return !path().isEmpty();
    }

    /** Returns the explanation. */
    public String message() {
      return Joiner.on(", ").join(path());
    }

    /** Adds a step. */
    public Violation plus(String edge) {
      return create(path().plus(edge));
    }

    /** Creates an explanation with one step. */
    public static Violation of(String reason) {
      return create(ConsPStack.singleton(reason));
    }

    /** An empty explanation. */
    public static Violation absent() {
      return create(ConsPStack.empty());
    }
  }

  /**
   * Check that a type-use of an {@code @ThreadSafe}-annotated type is instantiated with threadsafe
   * type arguments where required by its annotation's containerOf element.
   *
   * @param containerTypeParameters the in-scope threadsafe type parameters, declared on some
   *     enclosing class.
   * @param annotation the type's {@code @ThreadSafe} info
   * @param type the type to check
   */
  public Violation threadSafeInstantiation(
      Set<String> containerTypeParameters, AnnotationInfo annotation, Type type) {
    return threadSafeInstantiation(containerTypeParameters, annotation, type, new HashSet<>());
  }

  public Violation threadSafeInstantiation(
      Set<String> containerTypeParameters,
      AnnotationInfo annotation,
      Type type,
      Set<TypeVariableSymbol> recursiveThreadSafeTypeParameter) {
    for (int i = 0; i < type.tsym.getTypeParameters().size(); i++) {
      TypeVariableSymbol typaram = type.tsym.getTypeParameters().get(i);
      boolean immutableTypeParameter = hasAcceptedThreadSafeTypeParameterAnnotation(typaram);
      if (annotation.containerOf().contains(typaram.getSimpleName().toString())
          || immutableTypeParameter) {
        if (type.getTypeArguments().isEmpty()) {
          return Violation.of(
              String.format(
                  "'%s' required instantiation of '%s' with type parameters, but was raw",
                  getPrettyName(type.tsym), typaram));
        }
        Type tyarg = type.getTypeArguments().get(i);
        // Don't check whether wildcard types' erasure is safe. Wildcards can only be used where
        // the type isn't being instantiated, and we can rely on instantiations all being checked.
        if (immutableTypeParameter && tyarg instanceof WildcardType) {
          continue;
        }
        if (suppressAnnotation != null
            && tyarg.getAnnotationMirrors().stream()
                .anyMatch(
                    a ->
                        suppressAnnotation.contains(
                            ((ClassSymbol) a.getAnnotationType().asElement())
                                .flatName()
                                .toString()))) {
          continue;
        }
        Violation info =
            isThreadSafeTypeInternal(
                !immutableTypeParameter,
                containerTypeParameters,
                tyarg,
                recursiveThreadSafeTypeParameter);
        if (info.isPresent()) {
          return info.plus(
              String.format(
                  "'%s' was instantiated with %s type for '%s'",
                  getPrettyName(type.tsym),
                  purpose.mutableOrNonThreadSafe(),
                  typaram.getSimpleName()));
        }
      }
    }
    return Violation.absent();
  }

  /**
   * Check that the super-type of a {@code @ThreadSafe}-annotated type is instantiated with
   * threadsafe type arguments where required by its annotation's containerOf element, and that any
   * type arguments that correspond to containerOf type parameters on the sub-type are also in the
   * super-type's containerOf spec.
   *
   * @param containerTypeParameters the in-scope threadsafe type parameters, declared on some
   *     enclosing class.
   * @param annotation the type's {@code @ThreadSafe} info
   * @param type the type to check
   */
  public Violation checkSuperInstantiation(
      Set<String> containerTypeParameters, AnnotationInfo annotation, Type type) {
    Violation info = threadSafeInstantiation(containerTypeParameters, annotation, type);
    if (info.isPresent()) {
      return info;
    }
    return Streams.zip(
            type.asElement().getTypeParameters().stream(),
            type.getTypeArguments().stream(),
            (typaram, argument) -> {
              if (containerOfSubtyping(containerTypeParameters, annotation, typaram, argument)) {
                return Violation.of(
                    String.format(
                        "'%s' is not a container of '%s'", annotation.typeName(), typaram));
              }
              return Violation.absent();
            })
        .filter(Violation::isPresent)
        .findFirst()
        .orElse(Violation.absent());
  }

  // Enforce strong behavioral subtyping for containers.
  // If:
  // (1) a generic super type is instantiated with a type argument that is a type variable
  //     declared by the current class, and
  // (2) the current class is a container of that type parameter, then
  // (3) require the super-class to also be a container of its corresponding type parameter.
  private boolean containerOfSubtyping(
      Set<String> containerTypeParameters,
      AnnotationInfo annotation,
      TypeVariableSymbol typaram,
      Type tyargument) {
    // (1)
    if (!tyargument.hasTag(TypeTag.TYPEVAR)) {
      return false;
    }
    // (2)
    if (!containerTypeParameters.contains(tyargument.asElement().getSimpleName().toString())
        || isTypeParameterThreadSafe(
            (TypeVariableSymbol) tyargument.asElement(), containerTypeParameters)) {
      return false;
    }
    // (3)
    if (annotation.containerOf().contains(typaram.getSimpleName().toString())) {
      return false;
    }
    return true;
  }

  /**
   * Returns an {@link Violation} explaining whether the type is threadsafe.
   *
   * @param allowContainerTypeParameters true when checking the instantiation of an {@code
   *     typeParameterAnnotation}-annotated type parameter; indicates that {@code
   *     containerTypeParameters} should be ignored
   * @param containerTypeParameters type parameters in enclosing elements' containerOf
   *     specifications
   * @param type to check for thread-safety
   */
  public Violation isThreadSafeType(
      boolean allowContainerTypeParameters, Set<String> containerTypeParameters, Type type) {
    return isThreadSafeTypeInternal(
        allowContainerTypeParameters, containerTypeParameters, type, new HashSet<>());
  }

  private Violation isThreadSafeTypeInternal(
      boolean allowContainerTypeParameters,
      Set<String> containerTypeParameters,
      Type type,
      Set<TypeVariableSymbol> recursiveThreadSafeTypeParameter) {
    return type.accept(
        new ThreadSafeTypeVisitor(
            allowContainerTypeParameters,
            containerTypeParameters,
            recursiveThreadSafeTypeParameter),
        null);
  }

  private class ThreadSafeTypeVisitor extends Types.SimpleVisitor<Violation, Void> {

    private final boolean allowContainerTypeParameters;
    private final Set<String> containerTypeParameters;
    private final Set<TypeVariableSymbol> recursiveThreadSafeTypeParameter;

    private ThreadSafeTypeVisitor(
        boolean allowContainerTypeParameters,
        Set<String> containerTypeParameters,
        Set<TypeVariableSymbol> recursiveThreadSafeTypeParameter) {
      this.allowContainerTypeParameters = allowContainerTypeParameters;
      this.recursiveThreadSafeTypeParameter = recursiveThreadSafeTypeParameter;
      this.containerTypeParameters =
          !allowContainerTypeParameters ? ImmutableSet.of() : containerTypeParameters;
    }

    @Override
    public Violation visitWildcardType(WildcardType type, Void s) {
      return state.getTypes().wildUpperBound(type).accept(this, null);
    }

    @Override
    public Violation visitArrayType(ArrayType t, Void s) {
      return Violation.of(String.format("arrays are %s", purpose.mutableOrNotThreadSafe()));
    }

    @Override
    public Violation visitTypeVar(TypeVar type, Void s) {
      TypeVariableSymbol tyvar = (TypeVariableSymbol) type.tsym;
      if (containerTypeParameters.contains(tyvar.getSimpleName().toString())) {
        return Violation.absent();
      }
      if (isTypeParameterThreadSafe(
          tyvar, containerTypeParameters, recursiveThreadSafeTypeParameter)) {
        return Violation.absent();
      }
      String message;
      if (!allowContainerTypeParameters) {
        message =
            String.format("'%s' is not annotated @ImmutableTypeParameter", tyvar.getSimpleName());
      } else if (!containerTypeParameters.isEmpty()) {
        message =
            String.format(
                "'%s' is a %s type variable (not in '%s')",
                tyvar.getSimpleName(),
                purpose.mutableOrNonThreadSafe(),
                Joiner.on(", ").join(containerTypeParameters));
      } else {
        message =
            String.format(
                "'%s' is a %s type variable",
                tyvar.getSimpleName(), purpose.mutableOrNonThreadSafe());
      }
      return Violation.of(message);
    }

    @Override
    public Violation visitType(Type type, Void s) {
      switch (type.tsym.getKind()) {
        case ANNOTATION_TYPE -> {
          // Annotations are always immutable
          // (https://errorprone.info/bugpattern/ImmutableAnnotationChecker)
          return Violation.absent();
        }
        case ENUM -> {
          // assume enums are always immutable
          // TODO(b/25630186): add enforcement
          return Violation.absent();
        }
        case INTERFACE, CLASS, RECORD -> {}
        default -> {
          throw new AssertionError(String.format("Unexpected type kind %s", type.tsym.getKind()));
        }
      }
      if (WellKnownMutability.isAnnotation(state, type)) {
        // Annotations are always immutable
        // (https://errorprone.info/bugpattern/ImmutableAnnotationChecker)
        return Violation.absent();
      }
      AnnotationInfo annotation = getMarkerOrAcceptedAnnotation(type.tsym, state);
      if (annotation != null) {
        return threadSafeInstantiation(
            containerTypeParameters, annotation, type, recursiveThreadSafeTypeParameter);
      }
      String nameStr = type.tsym.flatName().toString();
      if (knownTypes.getKnownUnsafeClasses().contains(nameStr)) {
        return Violation.of(
            String.format(
                "'%s' is %s", type.tsym.getSimpleName(), purpose.mutableOrNotThreadSafe()));
      }
      if (WellKnownMutability.isProto2MessageClass(state, type)) {
        if (WellKnownMutability.isProto2MutableMessageClass(state, type)) {
          return Violation.of(
              String.format("'%s' is a mutable proto message", type.tsym.getSimpleName()));
        }
        return Violation.absent();
      }
      if (WellKnownMutability.isProtoEnum(state, type)) {
        return Violation.absent();
      }
      return Violation.of(
          String.format(
              "the declaration of type '%s' is not annotated with %s",
              type,
              Streams.concat(markerAnnotations.stream(), acceptedAnnotations.stream())
                  .map(a -> "@" + a)
                  .collect(Collectors.joining(" or "))));
    }
  }

  /**
   * Returns whether the given type parameter's declaration is annotated with {@link
   * #typeParameterAnnotation} indicating it will only ever be instantiated with thread-safe types.
   */
  public boolean hasThreadSafeTypeParameterAnnotation(TypeVariableSymbol symbol) {
    return symbol.getAnnotationMirrors().stream()
        .anyMatch(t -> typeParameterAnnotation.contains(t.type.tsym.flatName().toString()));
  }

  /**
   * Returns whether the given type parameter's declaration is annotated with {@link
   * #typeParameterAnnotation} or another acceptable annotation.
   */
  public boolean hasAcceptedThreadSafeTypeParameterAnnotation(TypeVariableSymbol symbol) {
    Set<String> annotations = Sets.union(typeParameterAnnotation, acceptedTypeParameterAnnotation);
    return symbol.getAnnotationMirrors().stream()
        .anyMatch(t -> annotations.contains(t.type.tsym.flatName().toString()));
  }

  /**
   * Returns whether the given type parameter's declaration is annotated with {@link
   * #containerOfAnnotation} indicating its type-safety determines the type-safety of the outer
   * class.
   */
  public boolean hasThreadSafeElementAnnotation(TypeVariableSymbol symbol) {
    return symbol.getAnnotationMirrors().stream()
        .anyMatch(t -> containerOfAnnotation.contains(t.type.tsym.flatName().toString()));
  }

  /**
   * Returns whether a type parameter is thread-safe.
   *
   * <p>This is true if either the type parameter's declaration is annotated with {@link
   * #typeParameterAnnotation} (indicating it can only be instantiated with thread-safe types), or
   * the type parameter has a thread-safe upper bound (sub-classes of thread-safe types are also
   * thread-safe).
   *
   * <p>If a type has a recursive bound, we recursively assume that this type satisfies all
   * thread-safety constraints. Recursion can only happen with type variables that have recursive
   * type bounds. These type variables do not need to be called out in the "containerOf" attribute
   * or annotated with {@link #typeParameterAnnotation}.
   *
   * <p>Recursion does not apply to other kinds of types because all declared types must be
   * annotated thread-safe, which means that thread-safety checkers don't need to analyze all
   * referenced types recursively.
   */
  private boolean isTypeParameterThreadSafe(
      TypeVariableSymbol symbol, Set<String> containerTypeParameters) {
    return isTypeParameterThreadSafe(symbol, containerTypeParameters, new HashSet<>());
  }

  private boolean isTypeParameterThreadSafe(
      TypeVariableSymbol symbol,
      Set<String> containerTypeParameters,
      Set<TypeVariableSymbol> recursiveThreadSafeTypeParameter) {
    if (!recursiveThreadSafeTypeParameter.add(symbol)) {
      return true;
    }
    // TODO(b/77695285): Prevent type variables that are immutable because of an immutable upper
    // bound to be marked thread-safe via containerOf or typeParameterAnnotation.
    try {
      for (Type bound : symbol.getBounds()) {
        if (!isThreadSafeTypeInternal(
                true, containerTypeParameters, bound, recursiveThreadSafeTypeParameter)
            .isPresent()) {
          // A type variable is thread-safe if any upper bound is thread-safe.
          return true;
        }
      }
      return hasAcceptedThreadSafeTypeParameterAnnotation(symbol);
    } finally {
      recursiveThreadSafeTypeParameter.remove(symbol);
    }
  }

  /**
   * Gets the {@link Symbol}'s annotation info, either from a marker annotation on the symbol, from
   * an accepted annotation on the symbol, or from the list of well-known types.
   */
  public AnnotationInfo getMarkerOrAcceptedAnnotation(Symbol sym, VisitorState state) {
    String nameStr = sym.flatName().toString();
    AnnotationInfo known = knownTypes.getKnownSafeClasses().get(nameStr);
    if (known != null) {
      return known;
    }
    return getAnnotation(
        sym, ImmutableSet.copyOf(Sets.union(markerAnnotations, acceptedAnnotations)), state);
  }

  /** Returns an enclosing instance for the specified type if it is thread-safe. */
  public @Nullable Type mutableEnclosingInstance(Optional<ClassTree> tree, ClassType type) {
    if (tree.isPresent()
        && !CanBeStaticAnalyzer.referencesOuter(
            tree.get(), ASTHelpers.getSymbol(tree.get()), state)) {
      return null;
    }
    Type enclosing = type.getEnclosingType();
    while (!enclosing.getKind().equals(TypeKind.NONE)) {
      if (getMarkerOrAcceptedAnnotation(enclosing.tsym, state) == null
          && isThreadSafeType(
                  /* allowContainerTypeParameters= */ false,
                  /* containerTypeParameters= */ ImmutableSet.of(),
                  enclosing)
              .isPresent()) {
        return enclosing;
      }
      enclosing = enclosing.getEnclosingType();
    }
    return null;
  }

  /**
   * Gets the set of in-scope threadsafe type parameters from the containerOf specs on annotations.
   *
   * <p>Usually only the immediately enclosing declaration is searched, but it's possible to have
   * cases like:
   *
   * <pre>{@code
   * @MarkerAnnotation(containerOf="T") class C<T> {
   *   class Inner extends ThreadSafeCollection<T> {}
   * }
   * }</pre>
   */
  public Set<String> threadSafeTypeParametersInScope(Symbol sym) {
    if (sym == null) {
      return ImmutableSet.of();
    }
    ImmutableSet.Builder<String> result = ImmutableSet.builder();
    OUTER:
    for (Symbol s = sym; s.owner != null; s = s.owner) {
      switch (s.getKind()) {
        case INSTANCE_INIT -> {
          continue;
        }
        case PACKAGE -> {
          break OUTER;
        }
        default -> {}
      }
      AnnotationInfo annotation = getMarkerOrAcceptedAnnotation(s, state);
      if (annotation == null) {
        continue;
      }
      for (TypeVariableSymbol typaram : s.getTypeParameters()) {
        String name = typaram.getSimpleName().toString();
        if (annotation.containerOf().contains(name)) {
          result.add(name);
        }
      }
      if (isStatic(s)) {
        break;
      }
    }
    return result.build();
  }

  private @Nullable AnnotationInfo getAnnotation(
      Symbol sym, ImmutableSet<String> annotationsToCheck, VisitorState state) {
    if (sym == null) {
      return null;
    }
    Optional<Compound> attr =
        sym.getRawAttributes().stream()
            .filter(a -> annotationsToCheck.contains(a.type.tsym.getQualifiedName().toString()))
            .findAny();
    if (attr.isPresent()) {
      ImmutableList<String> containerElements = containerOf(state, attr.get());
      if (!containerOfAnnotation.isEmpty() && containerElements.isEmpty()) {
        containerElements =
            sym.getTypeParameters().stream()
                .filter(
                    p ->
                        p.getAnnotationMirrors().stream()
                            .anyMatch(
                                a ->
                                    containerOfAnnotation.contains(
                                        a.type.tsym.flatName().toString())))
                .map(p -> p.getSimpleName().toString())
                .collect(toImmutableList());
      }
      return AnnotationInfo.create(sym.getQualifiedName().toString(), containerElements);
    }
    // @ThreadSafe is inherited from supertypes
    if (!(sym instanceof ClassSymbol classSymbol)) {
      return null;
    }

    ImmutableSet<Type> superclasses =
        markerAnnotationInherited
            ? state.getTypes().closure(classSymbol.type).stream()
                .filter(c -> !c.tsym.equals(classSymbol))
                .collect(toImmutableSet())
            : ImmutableSet.of(classSymbol.getSuperclass());
    for (Type superClass : superclasses) {
      AnnotationInfo superAnnotation = getInheritedAnnotation(superClass.asElement(), state);
      if (superAnnotation == null) {
        continue;
      }
      // If an annotated super-type was found, look for any type arguments to the super-type that
      // are in the super-type's containerOf spec, and where the arguments are type parameters
      // of the current class.
      // E.g. for `Foo<X> extends Super<X>` if `Super<Y>` is annotated
      // `@ThreadSafeContainerAnnotation Y`
      // then `Foo<X>` is has X implicitly annotated `@ThreadSafeContainerAnnotation X`
      ImmutableList.Builder<String> containerOf = ImmutableList.builder();
      for (int i = 0; i < superClass.getTypeArguments().size(); i++) {
        Type arg = superClass.getTypeArguments().get(i);
        TypeVariableSymbol formal = superClass.asElement().getTypeParameters().get(i);
        if (!arg.hasTag(TypeTag.TYPEVAR)) {
          continue;
        }
        TypeSymbol argSym = arg.asElement();
        if (argSym.owner == sym
            && superAnnotation.containerOf().contains(formal.getSimpleName().toString())) {
          containerOf.add(argSym.getSimpleName().toString());
        }
      }
      return AnnotationInfo.create(superAnnotation.typeName(), containerOf.build());
    }
    return null;
  }

  /**
   * Gets the possibly inherited marker annotation on the given symbol, and reverse-propagates
   * containerOf spec's from super-classes.
   */
  public AnnotationInfo getInheritedAnnotation(Symbol sym, VisitorState state) {
    return getAnnotation(sym, markerAnnotations, state);
  }

  private static ImmutableList<String> containerOf(VisitorState state, Compound attr) {
    Attribute m = attr.member(CONTAINEROF.get(state));
    if (m == null) {
      return ImmutableList.of();
    }
    return MoreAnnotations.asStrings(m).collect(toImmutableList());
  }

  /** Gets a human-friendly name for the given {@link Symbol} to use in diagnostics. */
  public String getPrettyName(Symbol sym) {
    if (!sym.getSimpleName().isEmpty()) {
      return sym.getSimpleName().toString();
    }
    if (sym.getKind() == ElementKind.ENUM) {
      // anonymous classes for enum constants are identified by the enclosing constant
      // declaration
      return sym.owner.getSimpleName().toString();
    }
    // anonymous classes have an empty name, but a recognizable superclass or interface
    // e.g. refer to `new Runnable() { ... }` as "Runnable"
    Type superType = state.getTypes().supertype(sym.type);
    if (state.getTypes().isSameType(superType, state.getSymtab().objectType)) {
      superType = Iterables.getFirst(state.getTypes().interfaces(sym.type), superType);
    }
    return superType.tsym.getSimpleName().toString();
  }

  public Violation checkInstantiation(
      Collection<TypeVariableSymbol> typeParameters, Collection<Type> typeArguments) {
    return Streams.zip(
            typeParameters.stream(),
            typeArguments.stream(),
            (sym, type) -> checkInstantiation(sym, ImmutableList.of(type)))
        .filter(Violation::isPresent)
        .findFirst()
        .orElse(Violation.absent());
  }

  /** Checks that any thread-safe type parameters are instantiated with thread-safe types. */
  public Violation checkInstantiation(
      TypeVariableSymbol typeParameter, Collection<Type> instantiations) {
    if (!hasThreadSafeTypeParameterAnnotation(typeParameter)) {
      return Violation.absent();
    }
    for (Type instantiation : instantiations) {
      Violation info =
          isThreadSafeType(
              /* allowContainerTypeParameters= */ true,
              /* containerTypeParameters= */ ImmutableSet.of(),
              instantiation);
      if (info.isPresent()) {
        return info.plus(
            String.format(
                "instantiation of '%s' is %s", typeParameter, purpose.mutableOrNotThreadSafe()));
      }
    }
    return Violation.absent();
  }

  /** Checks the instantiation of any thread-safe type parameters in the current invocation. */
  public Violation checkInvocation(Type methodType, Symbol symbol) {
    if (methodType == null) {
      return Violation.absent();
    }
    List<TypeVariableSymbol> typeParameters = symbol.getTypeParameters();
    if (typeParameters.stream().noneMatch(this::hasThreadSafeTypeParameterAnnotation)) {
      // fast path
      return Violation.absent();
    }
    ImmutableListMultimap<TypeVariableSymbol, Type> instantiation =
        ASTHelpers.getTypeSubstitution(methodType, symbol);

    for (TypeVariableSymbol typeParameter : typeParameters) {
      Violation violation = checkInstantiation(typeParameter, instantiation.get(typeParameter));
      if (violation.isPresent()) {
        return violation;
      }
    }
    return Violation.absent();
  }

  private static final Supplier<Name> CONTAINEROF =
      VisitorState.memoize(state -> state.getName("containerOf"));
}
