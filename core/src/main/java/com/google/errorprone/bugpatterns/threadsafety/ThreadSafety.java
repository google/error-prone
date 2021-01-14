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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.auto.value.AutoValue;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.common.collect.Streams;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.CanBeStaticAnalyzer;
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
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ElementKind;
import javax.lang.model.type.TypeKind;
import org.pcollections.ConsPStack;

/**
 * A class which gives information about the annotation of types; if a type isn't annotated, {@link
 * Violation} gives information as to why it is not.
 */
public final class ThreadSafety {
  private final VisitorState state;
  private final Purpose purpose;
  private final KnownTypes knownTypes;
  private final ImmutableSet<String> markerAnnotations;
  private final ImmutableSet<String> acceptedAnnotations;
  private final Class<? extends Annotation> containerOfAnnotation;
  @Nullable private final Class<? extends Annotation> suppressAnnotation;
  @Nullable private final Class<? extends Annotation> typeParameterAnnotation;

  /** Stores recursive invocations of {@link #isTypeParameterThreadSafe} */
  private final Set<TypeVariableSymbol> recursiveThreadSafeTypeParameter = new HashSet<>();

  public static Builder builder() {
    return new Builder();
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
      String immutableOrThreadSafe() {
        return "immutable";
      }

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
      String immutableOrThreadSafe() {
        return "thread-safe";
      }

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
     * Returns either the string {@code "immutable"} or {@code "thread-safe"} depending on the
     * purpose.
     */
    abstract String immutableOrThreadSafe();

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

  /** {@link ThreadSafety}Builder */
  public static class Builder {

    private Builder() {}

    private Purpose purpose = Purpose.FOR_IMMUTABLE_CHECKER;
    private KnownTypes knownTypes;
    private ImmutableSet<String> markerAnnotations;
    private ImmutableSet<String> acceptedAnnotations = ImmutableSet.of();
    @Nullable private Class<? extends Annotation> containerOfAnnotation;
    @Nullable private Class<? extends Annotation> suppressAnnotation;
    @Nullable private Class<? extends Annotation> typeParameterAnnotation;

    /** See {@link Purpose}. */
    public Builder setPurpose(Purpose purpose) {
      this.purpose = purpose;
      return this;
    }

    /** Information about known types and whether they're known to be safe or unsafe. */
    public Builder knownTypes(KnownTypes knownTypes) {
      this.knownTypes = knownTypes;
      return this;
    }

    /**
     * Annotations that will cause a class to be tested with this {@link ThreadSafety} instance; for
     * example, when testing a class for immutability, this should be @Immutable.
     */
    public Builder markerAnnotations(Set<String> markerAnnotations) {
      return markerAnnotations(ImmutableSet.copyOf(markerAnnotations));
    }

    // TODO(ringwalt): Remove this constructor. We need it for binary compatibility.
    public Builder markerAnnotations(ImmutableSet<String> markerAnnotations) {
      checkNotNull(markerAnnotations);
      this.markerAnnotations = markerAnnotations;
      return this;
    }

    /**
     * Annotations that do *not* cause a class to be tested, but which are treated as valid
     * annotations to pass the test; for example, if @ThreadSafe is the marker
     * annotation, @Immutable would be included in this list, as an immutable class is by definition
     * thread-safe.
     */
    public Builder acceptedAnnotations(Set<String> acceptedAnnotations) {
      return acceptedAnnotations(ImmutableSet.copyOf(acceptedAnnotations));
    }

    // TODO(ringwalt): Remove this constructor. We need it for binary compatibility.
    public Builder acceptedAnnotations(ImmutableSet<String> acceptedAnnotations) {
      checkNotNull(acceptedAnnotations);
      this.acceptedAnnotations = acceptedAnnotations;
      return this;
    }

    /** An annotation which marks a generic parameter as a container type. */
    public Builder containerOfAnnotation(Class<? extends Annotation> containerOfAnnotation) {
      checkNotNull(containerOfAnnotation);
      this.containerOfAnnotation = containerOfAnnotation;
      return this;
    }

    /** An annotation which, when found on a class, should suppress the test */
    public Builder suppressAnnotation(Class<? extends Annotation> suppressAnnotation) {
      checkNotNull(suppressAnnotation);
      this.suppressAnnotation = suppressAnnotation;
      return this;
    }

    /**
     * An annotation which, when found on a type parameter, indicates that the type parameter may
     * only be instantiated with thread-safe types.
     */
    public Builder typeParameterAnnotation(Class<? extends Annotation> typeParameterAnnotation) {
      checkNotNull(typeParameterAnnotation);
      checkArgument(
          Arrays.stream(typeParameterAnnotation.getAnnotation(Target.class).value())
              .anyMatch(ElementType.TYPE_PARAMETER::equals),
          "%s must be applicable to type parameters",
          typeParameterAnnotation);
      this.typeParameterAnnotation = typeParameterAnnotation;
      return this;
    }

    public ThreadSafety build(VisitorState state) {
      checkNotNull(knownTypes);
      checkNotNull(markerAnnotations);
      return new ThreadSafety(
          state,
          purpose,
          knownTypes,
          markerAnnotations,
          acceptedAnnotations,
          containerOfAnnotation,
          suppressAnnotation,
          typeParameterAnnotation);
    }
  }

  /** Use {@link #builder()} instead. */
  @Deprecated
  public ThreadSafety(
      VisitorState state,
      KnownTypes knownTypes,
      Set<String> markerAnnotations,
      Set<String> acceptedAnnotations,
      @Nullable Class<? extends Annotation> containerOfAnnotation,
      @Nullable Class<? extends Annotation> suppressAnnotation) {
    this(
        state,
        knownTypes,
        markerAnnotations,
        acceptedAnnotations,
        containerOfAnnotation,
        suppressAnnotation,
        /* typeParameterAnnotation= */ null);
  }

  /** Use {@link #builder()} instead. */
  @Deprecated
  public ThreadSafety(
      VisitorState state,
      KnownTypes knownTypes,
      Set<String> markerAnnotations,
      Set<String> acceptedAnnotations,
      @Nullable Class<? extends Annotation> containerOfAnnotation,
      @Nullable Class<? extends Annotation> suppressAnnotation,
      @Nullable Class<? extends Annotation> typeParameterAnnotation) {
    this(
        state,
        Purpose.FOR_IMMUTABLE_CHECKER,
        knownTypes,
        markerAnnotations,
        acceptedAnnotations,
        containerOfAnnotation,
        suppressAnnotation,
        typeParameterAnnotation);
  }

  private ThreadSafety(
      VisitorState state,
      Purpose purpose,
      KnownTypes knownTypes,
      Set<String> markerAnnotations,
      Set<String> acceptedAnnotations,
      @Nullable Class<? extends Annotation> containerOfAnnotation,
      @Nullable Class<? extends Annotation> suppressAnnotation,
      @Nullable Class<? extends Annotation> typeParameterAnnotation) {
    this.state = checkNotNull(state);
    this.purpose = purpose;
    this.knownTypes = checkNotNull(knownTypes);
    this.markerAnnotations = ImmutableSet.copyOf(checkNotNull(markerAnnotations));
    this.acceptedAnnotations = ImmutableSet.copyOf(checkNotNull(acceptedAnnotations));
    this.containerOfAnnotation = containerOfAnnotation;
    this.suppressAnnotation = suppressAnnotation;
    this.typeParameterAnnotation = typeParameterAnnotation;
  }

  /** Information about known types and whether they're known to be safe or unsafe. */
  public interface KnownTypes {
    /**
     * Types that are known to be safe even if they're not annotated with an expected annotation.
     */
    Map<String, AnnotationInfo> getKnownSafeClasses();

    /** Types that are known to be unsafe and don't need testing. */
    Set<String> getKnownUnsafeClasses();
  }

  /**
   * A human-friendly explanation of a thread safety violations.
   *
   * <p>An absent explanation indicates either an annotated type with no violations, or a type
   * without the annotation.
   */
  @AutoValue
  public abstract static class Violation {

    public static Violation create(ConsPStack<String> path) {
      return new AutoValue_ThreadSafety_Violation(path);
    }

    /** @return true if a violation was found */
    public boolean isPresent() {
      return !path().isEmpty();
    }

    /** @return the explanation */
    public String message() {
      return Joiner.on(", ").join(path());
    }

    /**
     * The list of steps in the explanation.
     *
     * <p>Example: ["Foo has field 'xs' of type 'int[]'", "arrays are not thread-safe"]
     */
    public abstract ConsPStack<String> path();

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
    if (!annotation.containerOf().isEmpty()
        && type.tsym.getTypeParameters().size() != type.getTypeArguments().size()) {
      return Violation.of(
          String.format(
              "'%s' required instantiation of '%s' with type parameters, but was raw",
              getPrettyName(type.tsym), Joiner.on(", ").join(annotation.containerOf())));
    }
    for (int i = 0; i < type.tsym.getTypeParameters().size(); i++) {
      TypeVariableSymbol typaram = type.tsym.getTypeParameters().get(i);
      boolean immutableTypeParameter = hasThreadSafeTypeParameterAnnotation(typaram);
      if (annotation.containerOf().contains(typaram.getSimpleName().toString())
          || immutableTypeParameter) {
        Type tyarg = type.getTypeArguments().get(i);
        if (suppressAnnotation != null
            && tyarg.getAnnotationMirrors().stream()
                .anyMatch(
                    a ->
                        ((ClassSymbol) a.getAnnotationType().asElement())
                            .flatName()
                            .contentEquals(suppressAnnotation.getName()))) {
          continue;
        }
        Violation info = isThreadSafeType(!immutableTypeParameter, containerTypeParameters, tyarg);
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

  /** @deprecated use {@link #isThreadSafeType(boolean, Set, Type)} instead. */
  @Deprecated
  public Violation isThreadSafeType(Set<String> containerTypeParameters, Type type) {
    return isThreadSafeType(
        /* allowContainerTypeParameters= */ true, containerTypeParameters, type);
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
    return type.accept(
        new ThreadSafeTypeVisitor(allowContainerTypeParameters, containerTypeParameters), null);
  }

  private class ThreadSafeTypeVisitor extends Types.SimpleVisitor<Violation, Void> {

    private final boolean allowContainerTypeParameters;
    private final Set<String> containerTypeParameters;

    private ThreadSafeTypeVisitor(
        boolean allowContainerTypeParameters, Set<String> containerTypeParameters) {
      this.allowContainerTypeParameters = allowContainerTypeParameters;
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
      if (isTypeParameterThreadSafe(tyvar, containerTypeParameters)) {
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
        case ANNOTATION_TYPE:
          // assume annotations are always immutable
          // TODO(b/25630189): add enforcement
          return Violation.absent();
        case ENUM:
          // assume enums are always immutable
          // TODO(b/25630186): add enforcement
          return Violation.absent();
        case INTERFACE:
        case CLASS:
          break;
        default:
          throw new AssertionError(String.format("Unexpected type kind %s", type.tsym.getKind()));
      }
      if (WellKnownMutability.isAnnotation(state, type)) {
        // annotation implementations may not have ANNOTATION_TYPE kind, assume they are immutable
        // TODO(b/25630189): add enforcement
        return Violation.absent();
      }
      AnnotationInfo annotation = getMarkerOrAcceptedAnnotation(type.tsym, state);
      if (annotation != null) {
        return threadSafeInstantiation(containerTypeParameters, annotation, type);
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
   * Returns true if the given type parameter's declaration is annotated with {@link
   * #typeParameterAnnotation} indicated it will only ever be instantiated with thread-safe types.
   */
  public boolean hasThreadSafeTypeParameterAnnotation(TypeVariableSymbol symbol) {
    return typeParameterAnnotation != null
        && symbol.getAnnotationMirrors().stream()
            .anyMatch(t -> t.type.tsym.flatName().contentEquals(typeParameterAnnotation.getName()));
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
    if (!recursiveThreadSafeTypeParameter.add(symbol)) {
      return true;
    }
    // TODO(b/77695285): Prevent type variables that are immutable because of an immutable upper
    // bound to be marked thread-safe via containerOf or typeParameterAnnotation.
    try {
      for (Type bound : symbol.getBounds()) {
        if (!isThreadSafeType(true, containerTypeParameters, bound).isPresent()) {
          // A type variable is thread-safe if any upper bound is thread-safe.
          return true;
        }
      }
      return hasThreadSafeTypeParameterAnnotation(symbol);
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
  public Type mutableEnclosingInstance(Optional<ClassTree> tree, ClassType type) {
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
   * <pre>
   * {@literal @}MarkerAnnotation(containerOf="T") class C&lt;T&gt; {
   *   class Inner extends ThreadSafeCollection&lt;T&gt; {}
   * }
   * </pre>
   */
  public Set<String> threadSafeTypeParametersInScope(Symbol sym) {
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
      if (s.isStatic()) {
        break;
      }
    }
    return result.build();
  }

  private AnnotationInfo getAnnotation(
      Symbol sym, ImmutableSet<String> annotationsToCheck, VisitorState state) {
    for (String annotation : annotationsToCheck) {
      AnnotationInfo info = getAnnotation(sym, state, annotation, containerOfAnnotation);
      if (info != null) {
        return info;
      }
    }
    return null;
  }

  private AnnotationInfo getAnnotation(
      Symbol sym,
      VisitorState state,
      String annotation,
      @Nullable Class<? extends Annotation> elementAnnotation) {
    if (sym == null) {
      return null;
    }
    Optional<Compound> attr =
        sym.getRawAttributes().stream()
            .filter(a -> a.type.tsym.getQualifiedName().contentEquals(annotation))
            .findAny();
    if (attr.isPresent()) {
      ImmutableList<String> containerElements = containerOf(state, attr.get());
      if (elementAnnotation != null && containerElements.isEmpty()) {
        containerElements =
            sym.getTypeParameters().stream()
                .filter(p -> p.getAnnotation(elementAnnotation) != null)
                .map(p -> p.getSimpleName().toString())
                .collect(toImmutableList());
      }
      return AnnotationInfo.create(sym.getQualifiedName().toString(), containerElements);
    }
    // @ThreadSafe is inherited from supertypes
    if (!(sym instanceof ClassSymbol)) {
      return null;
    }
    Type superClass = ((ClassSymbol) sym).getSuperclass();
    AnnotationInfo superAnnotation = getInheritedAnnotation(superClass.asElement(), state);
    if (superAnnotation == null) {
      return null;
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

  /**
   * Gets the possibly inherited marker annotation on the given symbol, and reverse-propagates
   * containerOf spec's from super-classes.
   */
  public AnnotationInfo getInheritedAnnotation(Symbol sym, VisitorState state) {
    return getAnnotation(sym, markerAnnotations, state);
  }

  private static ImmutableList<String> containerOf(VisitorState state, Compound attr) {
    Attribute m = attr.member(state.getName("containerOf"));
    if (m == null) {
      return ImmutableList.of();
    }
    return MoreAnnotations.asStrings((AnnotationValue) m).collect(toImmutableList());
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
    ImmutableMultimap<TypeVariableSymbol, Type> instantiation = getInstantiation(methodType);

    for (TypeVariableSymbol typeParameter : typeParameters) {
      Violation violation = checkInstantiation(typeParameter, instantiation.get(typeParameter));
      if (violation.isPresent()) {
        return violation;
      }
    }
    return Violation.absent();
  }

  private static ImmutableMultimap<TypeVariableSymbol, Type> getInstantiation(Type methodType) {
    List<Type> to = new ArrayList<>();
    ArrayList<Type> from = new ArrayList<>();
    getSubst(getMapping(methodType), from, to);
    ImmutableMultimap.Builder<TypeVariableSymbol, Type> mapping = ImmutableMultimap.builder();
    Streams.forEachPair(
        from.stream(), to.stream(), (f, t) -> mapping.put((TypeVariableSymbol) f.asElement(), t));
    return mapping.build();
  }

  private static Type getMapping(Type type) {
    if (type == null) {
      return null;
    }
    try {
      // Reflectively extract the mapping from Type.createMethodTypeWithReturn
      Field valField = type.getClass().getDeclaredField("val$t");
      valField.setAccessible(true);
      return (Type) valField.get(type);
    } catch (ReflectiveOperationException e) {
      return type;
    }
  }

  @SuppressWarnings("unchecked")
  private static void getSubst(Type m, List<Type> from, List<Type> to) {
    try {
      // Reflectively extract the mapping from an enclosing instance of Types.Subst
      Field substField = m.getClass().getDeclaredField("this$0");
      substField.setAccessible(true);
      Object subst = substField.get(m);
      Field fromField = subst.getClass().getDeclaredField("from");
      Field toField = subst.getClass().getDeclaredField("to");
      fromField.setAccessible(true);
      toField.setAccessible(true);
      from.addAll((Collection<Type>) fromField.get(subst));
      to.addAll((Collection<Type>) toField.get(subst));
    } catch (ReflectiveOperationException e) {
      return;
    }
  }
}
