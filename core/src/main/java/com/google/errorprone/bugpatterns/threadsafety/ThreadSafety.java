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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.auto.value.AutoValue;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.errorprone.VisitorState;
import com.google.errorprone.annotations.ImmutableTypeParameter;
import com.google.errorprone.bugpatterns.CanBeStaticAnalyzer;
import com.google.errorprone.util.ASTHelpers;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ElementKind;
import javax.lang.model.util.SimpleAnnotationValueVisitor8;
import org.pcollections.ConsPStack;

/**
 * A class which gives information about the annotation of types; if a type isn't annotated, {@link
 * Violation} gives information as to why it is not.
 */
public final class ThreadSafety {
  private final VisitorState state;
  private final KnownTypes knownTypes;
  private final ImmutableSet<String> markerAnnotations;
  private final ImmutableSet<String> acceptedAnnotations;
  private final Class<? extends Annotation> containerOfAnnotation;
  private final Class<? extends Annotation> suppressAnnotation;

  /**
   * @param markerAnnotations annotations that will cause a class to be tested with this {@link
   *     ThreadSafety} instance; for example, when testing a class for immutability, this should
   *     be @Immutable.
   * @param acceptedAnnotations annotations that do *not* cause a class to be tested, but which are
   *     treated as valid annotations to pass the test; for example, if @ThreadSafe is the marker
   *     annotation, @Immutable would be included in this list, as an immutable class is by
   *     definition thread-safe.
   * @param containerOfAnnotation an annotation which marks a generic parameter as a container type
   * @param suppressAnnotation an annotation which, when found on a class, should suppress the test
   */
  public ThreadSafety(
      VisitorState state,
      KnownTypes knownTypes,
      Set<String> markerAnnotations,
      Set<String> acceptedAnnotations,
      @Nullable Class<? extends Annotation> containerOfAnnotation,
      @Nullable Class<? extends Annotation> suppressAnnotation) {
    this.state = checkNotNull(state);
    this.knownTypes = checkNotNull(knownTypes);
    this.markerAnnotations = ImmutableSet.copyOf(checkNotNull(markerAnnotations));
    this.acceptedAnnotations = ImmutableSet.copyOf(checkNotNull(acceptedAnnotations));
    this.containerOfAnnotation = containerOfAnnotation;
    this.suppressAnnotation = suppressAnnotation;
  }

  /** Information about known types and whether they're known to be safe or unsafe. */
  public interface KnownTypes {
    /**
     * Types that are known to be safe even if they're not annotated with an expected annotation.
     */
    public Map<String, AnnotationInfo> getKnownSafeClasses();

    /** Types that are known to be unsafe and don't need testing. */
    public Set<String> getKnownUnsafeClasses();
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
     * <p>Example: ["Foo has field 'xs' of type 'int[]'", "arrays are mutable"]
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
   * @param threadSafeTypeParams the in-scope threadsafe type parameters, declared on some enclosing
   *     class.
   * @param annotation the type's {@code @ThreadSafe} info
   * @param type the type to check
   */
  public Violation threadSafeInstantiation(
      Set<String> threadSafeTypeParams, AnnotationInfo annotation, Type type) {
    if (!annotation.containerOf().isEmpty()
        && type.tsym.getTypeParameters().size() != type.getTypeArguments().size()) {
      return Violation.of(
          String.format(
              "'%s' required instantiation of '%s' with type parameters, but was raw",
              getPrettyName(type.tsym), Joiner.on(", ").join(annotation.containerOf())));
    }
    for (int i = 0; i < type.tsym.getTypeParameters().size(); i++) {
      TypeVariableSymbol typaram = type.tsym.getTypeParameters().get(i);
      if (annotation.containerOf().contains(typaram.getSimpleName().toString())) {
        Type tyarg = type.getTypeArguments().get(i);
        if (suppressAnnotation != null
            && tyarg
                .getAnnotationMirrors()
                .stream()
                .anyMatch(
                    a ->
                        ((ClassSymbol) a.getAnnotationType().asElement())
                            .flatName()
                            .toString()
                            .equals(suppressAnnotation.getName()))) {
          continue;
        }
        Violation info = isThreadSafeType(threadSafeTypeParams, tyarg);
        if (info.isPresent()) {
          return info.plus(
              String.format(
                  "'%s' was instantiated with mutable type for '%s'",
                  getPrettyName(type.tsym), typaram.getSimpleName()));
        }
      }
    }
    return Violation.absent();
  }

  /** Returns an {@link Violation} explaining whether the type is threadsafe. */
  public Violation isThreadSafeType(Set<String> threadSafeTypeParams, Type type) {
    return type.accept(new ThreadSafeTypeVisitor(threadSafeTypeParams), null);
  }

  private class ThreadSafeTypeVisitor extends Types.SimpleVisitor<Violation, Void> {

    private final Set<String> threadSafeTypeParams;

    private ThreadSafeTypeVisitor(Set<String> threadSafeTypeParams) {
      this.threadSafeTypeParams = threadSafeTypeParams;
    }

    @Override
    public Violation visitWildcardType(WildcardType type, Void s) {
      return state.getTypes().wildUpperBound(type).accept(this, null);
    }

    @Override
    public Violation visitArrayType(ArrayType t, Void s) {
      return Violation.of("arrays are mutable");
    }

    @Override
    public Violation visitTypeVar(TypeVar type, Void s) {
      TypeVariableSymbol tyvar = (TypeVariableSymbol) type.tsym;
      if (threadSafeTypeParams.contains(tyvar.getSimpleName().toString())) {
        return Violation.absent();
      }
      Symbol immutableTypeParameter =
          state.getSymbolFromString(ImmutableTypeParameter.class.getName());
      if (tyvar
          .getAnnotationMirrors()
          .stream()
          .anyMatch(t -> t.getAnnotationType().asElement().equals(immutableTypeParameter))) {
        return Violation.absent();
      }
      String message;
      if (threadSafeTypeParams.isEmpty()) {
        message = String.format("'%s' is a mutable type variable", tyvar.getSimpleName());
      } else {
        message =
            String.format(
                "'%s' is a mutable type variable (not in '%s')",
                tyvar.getSimpleName(), Joiner.on(", ").join(threadSafeTypeParams));
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
        return threadSafeInstantiation(threadSafeTypeParams, annotation, type);
      }
      String nameStr = type.tsym.flatName().toString();
      if (knownTypes.getKnownUnsafeClasses().contains(nameStr)) {
        return Violation.of(String.format("'%s' is mutable", type.tsym.getSimpleName()));
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
              markerAnnotations.stream().map(a -> "@" + a).collect(Collectors.joining(" or "))));
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
    while (!Type.noType.equals(enclosing)) {
      if (getMarkerOrAcceptedAnnotation(enclosing.tsym, state) == null
          && isThreadSafeType(ImmutableSet.of(), enclosing).isPresent()) {
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
    Symbol annosym = state.getSymbolFromString(annotation);
    Optional<Compound> attr =
        sym.getAnnotationMirrors()
            .stream()
            .filter(a -> a.getAnnotationType().asElement().equals(annosym))
            .findAny();
    if (attr.isPresent()) {
      ImmutableList<String> containerElements = containerOf(state, attr.get());
      if (elementAnnotation != null && containerElements.isEmpty()) {
        containerElements =
            sym.getTypeParameters()
                .stream()
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
    ImmutableList.Builder<String> containerOf = ImmutableList.builder();
    m.accept(
        new SimpleAnnotationValueVisitor8<Void, Void>() {
          @Override
          public Void visitString(String s, Void unused) {
            containerOf.add(s);
            return null;
          }

          @Override
          public Void visitArray(List<? extends AnnotationValue> list, Void unused) {
            for (AnnotationValue value : list) {
              value.accept(this, null);
            }
            return null;
          }
        },
        null);
    return containerOf.build();
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
}
