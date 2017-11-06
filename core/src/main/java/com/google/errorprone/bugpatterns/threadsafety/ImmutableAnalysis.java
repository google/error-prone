/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

import com.google.auto.value.AutoValue;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.errorprone.VisitorState;
import com.google.errorprone.annotations.Immutable;
import com.google.errorprone.annotations.concurrent.LazyInit;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.CanBeStaticAnalyzer;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Attribute.Compound;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.code.Symbol.TypeVariableSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ArrayType;
import com.sun.tools.javac.code.Type.ClassType;
import com.sun.tools.javac.code.Type.TypeVar;
import com.sun.tools.javac.code.Type.WildcardType;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.util.Filter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.SimpleAnnotationValueVisitor8;
import org.pcollections.ConsPStack;

/** Analyzes types for deep immutability. */
public class ImmutableAnalysis {

  /**
   * A human-friendly explanation of an immutability violations.
   *
   * <p>An absent explanation indicates either an @Immutable-annotated type with no violations, or a
   * type without the annotation.
   */
  @AutoValue
  abstract static class Violation {

    private static Violation create(ConsPStack<String> path) {
      return new AutoValue_ImmutableAnalysis_Violation(path);
    }

    /** @return true if a violation was found */
    boolean isPresent() {
      return !path().isEmpty();
    }

    /** @return the explanation */
    String message() {
      return Joiner.on(", ").join(path());
    }

    /**
     * The list of steps in the explanation.
     *
     * <p>Example: ["Foo has field 'xs' of type 'int[]'", "arrays are mutable"]
     */
    abstract ConsPStack<String> path();

    /** Adds a step. */
    Violation plus(String edge) {
      return create(path().plus(edge));
    }

    /** Creates an explanation with one step. */
    static Violation of(String reason) {
      return create(ConsPStack.singleton(reason));
    }

    /** An empty explanation. */
    static Violation absent() {
      return create(ConsPStack.<String>empty());
    }
  }

  private final BugChecker bugChecker;
  private final VisitorState state;
  private final WellKnownMutability wellKnownMutability;

  private final String nonFinalFieldMessage;
  private final String mutableFieldMessage;

  public ImmutableAnalysis(
      BugChecker bugChecker,
      VisitorState state,
      WellKnownMutability wellKnownMutability,
      String nonFinalFieldMessage,
      String mutableFieldMessage) {
    this.bugChecker = bugChecker;
    this.state = state;
    this.wellKnownMutability = wellKnownMutability;
    this.nonFinalFieldMessage = nonFinalFieldMessage;
    this.mutableFieldMessage = mutableFieldMessage;
  }

  /**
   * Check that an {@code @Immutable}-annotated class:
   *
   * <ul>
   *   <li>does not declare or inherit any mutable fields,
   *   <li>any immutable supertypes are instantiated with immutable type arguments as required by
   *       their containerOf spec, and
   *   <li>any enclosing instances are immutable.
   * </ul>
   *
   * requiring supertypes to be annotated immutable would be too restrictive.
   */
  Violation checkForImmutability(
      Optional<ClassTree> tree, ImmutableSet<String> immutableTyParams, ClassType type) {
    Violation info = areFieldsImmutable(tree, immutableTyParams, type);
    if (info.isPresent()) {
      return info;
    }

    for (Type interfaceType : state.getTypes().interfaces(type)) {
      AnnotationInfo interfaceAnnotation = getImmutableAnnotation(interfaceType.tsym, state);
      if (interfaceAnnotation == null) {
        continue;
      }
      info = immutableInstantiation(immutableTyParams, interfaceAnnotation, interfaceType);
      if (info.isPresent()) {
        return info.plus(
            String.format(
                "'%s' extends '%s'", getPrettyName(type.tsym), getPrettyName(interfaceType.tsym)));
      }
    }

    if (!type.asElement().isEnum()) {
      // don't check enum super types here to avoid double-reporting errors
      info = checkSuper(immutableTyParams, type);
      if (info.isPresent()) {
        return info;
      }
    }
    Type mutableEnclosing = mutableEnclosingInstance(tree, type);
    if (mutableEnclosing != null) {
      return info.plus(
          String.format(
              "'%s' has mutable enclosing instance '%s'",
              getPrettyName(type.tsym), mutableEnclosing));
    }
    return Violation.absent();
  }

  private Type mutableEnclosingInstance(Optional<ClassTree> tree, ClassType type) {
    if (tree.isPresent()
        && !CanBeStaticAnalyzer.referencesOuter(
            tree.get(), ASTHelpers.getSymbol(tree.get()), state)) {
      return null;
    }
    Type enclosing = type.getEnclosingType();
    while (!Type.noType.equals(enclosing)) {
      if (getImmutableAnnotation(enclosing.tsym, state) == null
          && isImmutableType(ImmutableSet.of(), enclosing).isPresent()) {
        return enclosing;
      }
      enclosing = enclosing.getEnclosingType();
    }
    return null;
  }

  private Violation checkSuper(ImmutableSet<String> immutableTyParams, ClassType type) {
    ClassType superType = (ClassType) state.getTypes().supertype(type);
    if (superType.getKind() == TypeKind.NONE
        || state.getTypes().isSameType(state.getSymtab().objectType, superType)) {
      return Violation.absent();
    }
    if (WellKnownMutability.isAnnotation(state, type)) {
      // TODO(b/25630189): add enforcement
      return Violation.absent();
    }

    AnnotationInfo superannotation = getImmutableAnnotation(superType.tsym, state);
    if (superannotation != null) {
      // If the superclass does happen to be immutable, we don't need to recursively
      // inspect it. We just have to check that it's instantiated correctly:
      Violation info = immutableInstantiation(immutableTyParams, superannotation, superType);
      if (!info.isPresent()) {
        return Violation.absent();
      }
      return info.plus(
          String.format(
              "'%s' extends '%s'", getPrettyName(type.tsym), getPrettyName(superType.tsym)));
    }

    // Recursive case: check if the supertype is 'effectively' immutable.
    Violation info =
        checkForImmutability(Optional.<ClassTree>empty(), immutableTyParams, superType);
    if (!info.isPresent()) {
      return Violation.absent();
    }
    return info.plus(
        String.format(
            "'%s' extends '%s'", getPrettyName(type.tsym), getPrettyName(superType.tsym)));
  }

  /**
   * Check a single class' fields for immutability.
   *
   * @param immutableTyParams the in-scope immutable type parameters
   * @param classType the type to check the fields of
   */
  Violation areFieldsImmutable(
      Optional<ClassTree> tree, ImmutableSet<String> immutableTyParams, ClassType classType) {
    ClassSymbol classSym = (ClassSymbol) classType.tsym;
    if (classSym.members() == null) {
      return Violation.absent();
    }
    Filter<Symbol> instanceFieldFilter =
        new Filter<Symbol>() {
          @Override
          public boolean accepts(Symbol symbol) {
            return symbol.getKind() == ElementKind.FIELD && !symbol.isStatic();
          }
        };
    Map<Symbol, Tree> declarations = new HashMap<>();
    if (tree.isPresent()) {
      for (Tree member : tree.get().getMembers()) {
        Symbol sym = ASTHelpers.getSymbol(member);
        if (sym != null) {
          declarations.put(sym, member);
        }
      }
    }
    // javac gives us members in reverse declaration order
    // handling them in declaration order leads to marginally better diagnostics
    List<Symbol> members =
        ImmutableList.copyOf(classSym.members().getSymbols(instanceFieldFilter)).reverse();
    for (Symbol member : members) {
      Optional<Tree> memberTree = Optional.ofNullable(declarations.get(member));
      Violation info =
          isFieldImmutable(memberTree, immutableTyParams, classSym, classType, (VarSymbol) member);
      if (info.isPresent()) {
        return info;
      }
    }
    return Violation.absent();
  }

  /** Check a single field for immutability. */
  private Violation isFieldImmutable(
      Optional<Tree> tree,
      ImmutableSet<String> immutableTyParams,
      ClassSymbol classSym,
      ClassType classType,
      VarSymbol var) {
    if (bugChecker.isSuppressed(var)) {
      return Violation.absent();
    }
    if (ASTHelpers.hasAnnotation(var, LazyInit.class, state)) {
      return Violation.absent();
    }
    if (!var.getModifiers().contains(Modifier.FINAL)) {
      if (tree.isPresent()) {
        // If we have a tree to attach diagnostics to, report the error immediately instead of
        // accumulating the path to the error from the top-level class being checked
        state.reportMatch(
            BugChecker.buildDescriptionFromChecker(tree.get(), bugChecker)
                .setMessage(nonFinalFieldMessage)
                .addFix(SuggestedFixes.addModifiers(tree.get(), state, Modifier.FINAL))
                .build());
        return Violation.absent();
      }
      return Violation.of(
          String.format(
              "'%s' has non-final field '%s'", getPrettyName(classSym), var.getSimpleName()));
    }
    Type varType = state.getTypes().memberType(classType, var);
    Violation info = isImmutableType(immutableTyParams, varType);
    if (info.isPresent()) {
      if (tree.isPresent()) {
        // If we have a tree to attach diagnostics to, report the error immediately instead of
        // accumulating the path to the error from the top-level class being checked
        state.reportMatch(
            BugChecker.buildDescriptionFromChecker(tree.get(), bugChecker)
                .setMessage(info.plus(mutableFieldMessage).message())
                .build());
        return Violation.absent();
      }
      return info.plus(
          String.format(
              "'%s' has field '%s' of type '%s'",
              getPrettyName(classSym), var.getSimpleName(), varType));
    }
    return Violation.absent();
  }

  /**
   * Check that a type-use of an {@code @Immutable}-annotated type is instantiated with immutable
   * type arguments where required by its annotation's containerOf element.
   *
   * @param immutableTyParams the in-scope immutable type parameters, declared on some enclosing
   *     class.
   * @param annotation the type's {@code @Immutable} info
   * @param type the type to check
   */
  Violation immutableInstantiation(
      ImmutableSet<String> immutableTyParams, AnnotationInfo annotation, Type type) {
    if (!annotation.containerOf().isEmpty()
        && type.tsym.getTypeParameters().size() != type.getTypeArguments().size()) {
      return Violation.of(
          String.format(
              "'%s' required immutable instantiation of '%s', but was raw",
              getPrettyName(type.tsym), Joiner.on(", ").join(annotation.containerOf())));
    }
    for (int i = 0; i < type.tsym.getTypeParameters().size(); i++) {
      TypeVariableSymbol typaram = type.tsym.getTypeParameters().get(i);
      if (annotation.containerOf().contains(typaram.getSimpleName().toString())) {
        Type tyarg = type.getTypeArguments().get(i);
        Violation info = isImmutableType(immutableTyParams, tyarg);
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

  /** Returns an {@link Violation} explaining whether the type is immutable. */
  Violation isImmutableType(ImmutableSet<String> immutableTyParams, Type type) {
    return type.accept(new ImmutableTypeVisitor(immutableTyParams), null);
  }

  private class ImmutableTypeVisitor extends Types.SimpleVisitor<Violation, Void> {

    private final ImmutableSet<String> immutableTyParams;

    private ImmutableTypeVisitor(ImmutableSet<String> immutableTyParams) {
      this.immutableTyParams = immutableTyParams;
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
      if (immutableTyParams != null
          && immutableTyParams.contains(tyvar.getSimpleName().toString())) {
        return Violation.absent();
      }
      String message;
      if (immutableTyParams.isEmpty()) {
        message = String.format("'%s' is a mutable type variable", tyvar.getSimpleName());
      } else {
        message =
            String.format(
                "'%s' is a mutable type variable (not in '%s')",
                tyvar.getSimpleName(), Joiner.on(", ").join(immutableTyParams));
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
      AnnotationInfo annotation = getImmutableAnnotation(type.tsym, state);
      if (annotation != null) {
        return immutableInstantiation(immutableTyParams, annotation, type);
      }
      String nameStr = type.tsym.flatName().toString();
      if (wellKnownMutability.getKnownUnsafeClasses().contains(nameStr)) {
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
              "the declaration of type '%s' is not annotated"
                  + " @com.google.errorprone.annotations.Immutable",
              type));
    }
  }

  /**
   * Gets the {@link Symbol}'s {@code @Immutable} annotation info, either from an annotation on the
   * symbol or from the list of well-known immutable types.
   */
  AnnotationInfo getImmutableAnnotation(Symbol sym, VisitorState state) {
    String nameStr = sym.flatName().toString();
    AnnotationInfo known = wellKnownMutability.getKnownImmutableClasses().get(nameStr);
    if (known != null) {
      return known;
    }
    return getInheritedAnnotation(sym, state);
  }

  /**
   * Gets the possibly inherited {@code @Immutable} annotation on the given symbol, and
   * reverse-propagates containerOf spec's from super-classes.
   */
  static AnnotationInfo getInheritedAnnotation(Symbol sym, VisitorState state) {
    if (!(sym instanceof ClassSymbol)) {
      return null;
    }
    Compound attr = sym.attribute(state.getSymbolFromString(Immutable.class.getName()));
    if (attr != null) {
      return AnnotationInfo.create(sym.getQualifiedName().toString(), containerOf(state, attr));
    }
    // @Immutable is inherited from supertypes
    Type superClass = ((ClassSymbol) sym).getSuperclass();
    AnnotationInfo superAnnotation = getInheritedAnnotation(superClass.asElement(), state);
    if (superAnnotation == null) {
      return null;
    }
    // If an annotated super-type was found, look for any type arguments to the super-type that
    // are in the super-type's containerOf spec, and where the arguments are type parameters
    // of the current class.
    // E.g. for `Foo<X> extends Super<X>` if `Super<Y>` is annotated `@Immutable(containerOf={"Y"})`
    // then `Foo<X>` is implicitly annotated `@Immutable(containerOf={"X"})`.
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
    return AnnotationInfo.create(sym.getQualifiedName().toString(), containerOf.build());
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

  /**
   * Gets the {@link Tree}'s {@code @Immutable} annotation info, either from an annotation on the
   * symbol or from the list of well-known immutable types.
   */
  AnnotationInfo getImmutableAnnotation(Tree tree, VisitorState state) {
    Symbol sym = ASTHelpers.getSymbol(tree);
    return sym == null ? null : getImmutableAnnotation(sym, state);
  }

  /** Gets a human-friendly name for the given {@link Symbol} to use in diagnostics. */
  private String getPrettyName(Symbol sym) {
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
