/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;

import com.google.auto.value.AutoValue;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.annotations.Immutable;
import com.google.errorprone.annotations.concurrent.LazyInit;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.TypeVariableSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ArrayType;
import com.sun.tools.javac.code.Type.ClassType;
import com.sun.tools.javac.code.Type.TypeVar;
import com.sun.tools.javac.code.Type.WildcardType;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.util.Filter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeKind;
import org.pcollections.ConsPStack;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
  name = "Immutable",
  summary = "Type declaration annotated with @Immutable is not immutable",
  category = JDK,
  severity = ERROR
)
public class ImmutableChecker extends BugChecker implements BugChecker.ClassTreeMatcher {

  /**
   * A human-friendly explanation of an immutability violations.
   *
   * An absent explanation indicates either an @Immutable-annotated type with no violations, or a
   * type without the annotation.
   */
  @AutoValue
  abstract static class Violation {

    private static Violation create(ConsPStack<String> path) {
      return new AutoValue_ImmutableChecker_Violation(path);
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

  @Override
  public Description matchClass(ClassTree tree, VisitorState state) {
    if (tree.getSimpleName().length() == 0) {
      // anonymous classes have empty names
      // TODO(cushon): once Java 8 happens, require @Immutable on anonymous classes
      return handleAnonymousClass(tree, state);
    }

    ImmutableAnnotationInfo annotation = getImmutableAnnotation(tree);
    if (annotation == null) {
      // If the type isn't annotated we don't check for immutability, but we do
      // report an error if it extends/implements any @Immutable-annotated types.
      return checkSubtype(tree, state);
    }

    // Special-case visiting declarations of known-immutable types; these uses
    // of the annotation are "trusted".
    if (WellKnownMutability.KNOWN_IMMUTABLE.containsValue(annotation)) {
      return Description.NO_MATCH;
    }

    // Check that the types in containerOf actually exist
    Set<String> typarams = new HashSet<>();
    for (TypeParameterTree typaram : tree.getTypeParameters()) {
      typarams.add(typaram.getName().toString());
    }
    SetView<String> difference = Sets.difference(annotation.containerOf(), typarams);
    if (!difference.isEmpty()) {
      String message =
          String.format(
              "could not find type(s) referenced by containerOf: %s",
              Joiner.on("', '").join(difference));
      return buildDescription(tree).setMessage(message).build();
    }

    // Main path for @Immutable-annotated types:
    //
    // Check that the fields (including inherited fields) are immutable, and
    // validate the type hierarchy superclass.
    Violation info =
        checkForImmutability(
            Optional.of(tree),
            immutableTypeParametersInScope(ASTHelpers.getSymbol(tree)),
            ASTHelpers.getType(tree),
            state);

    if (!info.isPresent()) {
      return Description.NO_MATCH;
    }

    String message =
        "type annotated with @Immutable could not be proven immutable: " + info.message();
    return buildDescription(tree).setMessage(message).build();
  }

  /**
   * Check that an {@code @Immutable}-annotated class:
   *
   * <ul>
   * <li>does not declare or inherit any mutable fields,
   * <li>any immutable supertypes are instantiated with immutable type arguments as required by
   *     their containerOf spec, and
   * <li>any enclosing instances are immutable.
   * </ul>
   *
   * requiring supertypes to be annotated immutable would be too restrictive.
   */
  private Violation checkForImmutability(
      Optional<ClassTree> tree,
      ImmutableSet<String> immutableTyParams,
      ClassType type,
      VisitorState state) {
    Violation info = areFieldsImmutable(tree, immutableTyParams, type, state);
    if (info.isPresent()) {
      return info;
    }

    for (Type interfaceType : state.getTypes().interfaces(type)) {
      ImmutableAnnotationInfo interfaceAnnotation = getImmutableAnnotation(interfaceType.tsym);
      if (interfaceAnnotation == null) {
        continue;
      }
      info = immutableInstantiation(immutableTyParams, interfaceAnnotation, interfaceType, state);
      if (info.isPresent()) {
        return info.plus(
            String.format(
                "'%s' extends '%s'",
                getPrettyName(type.tsym, state),
                getPrettyName(interfaceType.tsym, state)));
      }
    }

    info = checkSuper(immutableTyParams, type, state);
    if (info.isPresent()) {
      return info;
    }

    Type enclosing = type.getEnclosingType();
    while (!Type.noType.equals(enclosing)) {
      // require the enclosing instance to be annotated @Immutable
      // don't worry about containerOf, this isn't an explicit type use
      if (getImmutableAnnotation(enclosing.tsym) == null) {
        return info.plus(
            String.format(
                "'%s' has mutable enclosing instance '%s'",
                getPrettyName(type.tsym, state), getPrettyName(enclosing.tsym, state)));
      }
      enclosing = enclosing.getEnclosingType();
    }

    return Violation.absent();
  }

  private Violation checkSuper(
      ImmutableSet<String> immutableTyParams, ClassType type, VisitorState state) {
    ClassType superType = (ClassType) state.getTypes().supertype(type);
    if (superType.getKind() == TypeKind.NONE
        || state.getTypes().isSameType(state.getSymtab().objectType, superType)) {
      return Violation.absent();
    }

    ImmutableAnnotationInfo superannotation = getImmutableAnnotation(superType.tsym);
    if (superannotation != null) {
      // If the superclass does happen to be immutable, we don't need to recursively
      // inspect it. We just have to check that it's instantiated correctly:
      Violation info = immutableInstantiation(immutableTyParams, superannotation, superType, state);
      if (!info.isPresent()) {
        return Violation.absent();
      }
      return info.plus(
          String.format(
              "'%s' extends '%s'",
              getPrettyName(type.tsym, state),
              getPrettyName(superType.tsym, state)));
    }

    // Recursive case: check if the supertype is 'effectively' immutable.
    Violation info =
        checkForImmutability(Optional.<ClassTree>absent(), immutableTyParams, superType, state);
    if (!info.isPresent()) {
      return Violation.absent();
    }
    return info.plus(
        String.format(
            "'%s' extends '%s'",
            getPrettyName(type.tsym, state),
            getPrettyName(superType.tsym, state)));
  }

  /**
   * Check a single class' fields for immutability.
   *
   * @param immutableTyParams the in-scope immutable type parameters
   * @param classType the type to check the fields of
   */
  private Violation areFieldsImmutable(
      Optional<ClassTree> tree,
      ImmutableSet<String> immutableTyParams,
      ClassType classType,
      VisitorState state) {
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
      Optional<Tree> memberTree = Optional.fromNullable(declarations.get(member));
      Violation info =
          isFieldImmutable(
              memberTree, immutableTyParams, classSym, classType, (VarSymbol) member, state);
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
      VarSymbol var,
      VisitorState state) {
    SuppressWarnings suppression = ASTHelpers.getAnnotation(var, SuppressWarnings.class);
    if (suppression != null
        && !Collections.disjoint(Arrays.asList(suppression.value()), allNames())) {
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
            buildDescription(tree.get())
                .setMessage("@Immutable classes cannot have non-final fields")
                .addFix(SuggestedFixes.addModifiers(tree.get(), state, Modifier.FINAL))
                .build());
        return Violation.absent();
      }
      return Violation.of(
          String.format(
              "'%s' has non-final field '%s'",
              getPrettyName(classSym, state),
              var.getSimpleName()));
    }
    Type varType = state.getTypes().memberType(classType, var);
    Violation info = ImmutableTypeVisitor.isImmutableType(immutableTyParams, varType, state);
    if (info.isPresent()) {
      if (tree.isPresent()) {
        // If we have a tree to attach diagnostics to, report the error immediately instead of
        // accumulating the path to the error from the top-level class being checked
        state.reportMatch(
            buildDescription(tree.get())
                .setMessage(info.plus("@Immutable class has mutable field").message())
                .build());
        return Violation.absent();
      }
      return info.plus(
          String.format(
              "'%s' has field '%s' of type '%s'",
              getPrettyName(classSym, state),
              var.getSimpleName(),
              varType));
    }
    return Violation.absent();
  }

  /**
   * Check that a type-use of an {@code @Immutable}-annotated type is instantiated
   * with immutable type arguments where required by its annotation's containerOf
   * element.
   *
   * @param immutableTyParams the in-scope immutable type parameters, declared on
   *                          some enclosing class.
   * @param annotation the type's {@code @Immutable} info
   * @param type the type to check
   */
  private static Violation immutableInstantiation(
      ImmutableSet<String> immutableTyParams,
      ImmutableAnnotationInfo annotation,
      Type type,
      VisitorState state) {
    if (type.tsym.getTypeParameters().size() != type.getTypeArguments().size()) {
      return Violation.of(
          String.format(
              "'%s' required immutable instantiation of '%s', but was raw",
              getPrettyName(type.tsym, state),
              Joiner.on(", ").join(annotation.containerOf())));
    }
    for (int i = 0; i < type.tsym.getTypeParameters().size(); i++) {
      TypeVariableSymbol typaram = type.tsym.getTypeParameters().get(i);
      if (annotation.containerOf().contains(typaram.getSimpleName().toString())) {
        Type tyarg = type.getTypeArguments().get(i);
        Violation info = ImmutableTypeVisitor.isImmutableType(immutableTyParams, tyarg, state);
        if (info.isPresent()) {
          return info.plus(
              String.format(
                  "'%s' was instantiated with mutable type for '%s'",
                  getPrettyName(type.tsym, state),
                  typaram.getSimpleName()));
        }
      }
    }
    return Violation.absent();
  }

  private static class ImmutableTypeVisitor extends Types.SimpleVisitor<Violation, Void> {

    /**
     * Returns an {@link ImmutableChecker.Violation} explaining whether the type is immutable.
     */
    static ImmutableChecker.Violation isImmutableType(
        ImmutableSet<String> immutableTyParams, Type type, VisitorState state) {
      return type.accept(new ImmutableChecker.ImmutableTypeVisitor(immutableTyParams, state), null);
    }

    private final ImmutableSet<String> immutableTyParams;
    private final VisitorState state;

    private ImmutableTypeVisitor(ImmutableSet<String> immutableTyParams, VisitorState state) {
      this.immutableTyParams = immutableTyParams;
      this.state = state;
    }

    @Override
    public Violation visitWildcardType(WildcardType type, Void s) {
      return state.getTypes().wildUpperBound(type).accept(this, null);
    }

    @Override
    public Violation visitArrayType(ArrayType t, Void s) {
      return Violation.of(String.format("arrays are mutable"));
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
                tyvar.getSimpleName(),
                Joiner.on(", ").join(immutableTyParams));
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
      ImmutableAnnotationInfo annotation = getImmutableAnnotation(type.tsym);
      if (annotation != null) {
        return immutableInstantiation(immutableTyParams, annotation, type, state);
      }
      String nameStr = type.tsym.flatName().toString();
      if (WellKnownMutability.KNOWN_UNSAFE.contains(nameStr)) {
        return Violation.of(
            String.format("'%s' is known to be mutable", type.tsym.getSimpleName()));
      }
      if (WellKnownMutability.isProto2MessageClass(state, type)) {
        if (WellKnownMutability.isProto2MutableMessageClass(state, type)) {
          return Violation.of(
              String.format("'%s' is a mutable proto message", type.tsym.getSimpleName()));
        }
        return Violation.absent();
      }
      return Violation.of(String.format("'%s' is not annotated @Immutable", type));
    }
  }

  // Anonymous classes

  /** Check anonymous implementations of {@code @Immutable} types. */
  private Description handleAnonymousClass(ClassTree tree, VisitorState state) {
    ClassSymbol sym = ASTHelpers.getSymbol(tree);
    if (sym == null) {
      return Description.NO_MATCH;
    }
    Type superType = immutableSupertype(sym, state);
    if (superType == null) {
      return Description.NO_MATCH;
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
    ImmutableSet<String> typarams = immutableTypeParametersInScope(sym);
    Violation info =
        areFieldsImmutable(Optional.of(tree), typarams, ASTHelpers.getType(tree), state);
    if (!info.isPresent()) {
      return Description.NO_MATCH;
    }
    String reason = Joiner.on(", ").join(info.path());
    String message = String.format(
        "Class extends @Immutable type %s, but is not immutable: %s", superType, reason);
    return buildDescription(tree).setMessage(message).build();
  }

  // Strong behavioural subtyping

  /** Check for classes without {@code @Immutable} that have immutable supertypes. */
  private Description checkSubtype(ClassTree tree, VisitorState state) {
    ClassSymbol sym = ASTHelpers.getSymbol(tree);
    if (sym == null) {
      return Description.NO_MATCH;
    }
    Type superType = immutableSupertype(sym, state);
    if (superType == null) {
      return Description.NO_MATCH;
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
  private static Type immutableSupertype(Symbol sym, VisitorState state) {
    for (Type superType : state.getTypes().closure(sym.type)) {
      if (superType.equals(sym.type)) {
        continue;
      }
      // Don't use getImmutableAnnotation here: subtypes of trusted types are
      // also trusted, only check for explicitly annotated supertypes.
      if (ASTHelpers.hasAnnotation(superType.tsym, Immutable.class, state)) {
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

  // utilities

  /**
   * Gets the{@link Symbol}'s {@code @Immutable} annotation info, either from an annotation on
   * the symbol or from the list of well-known immutable types.
   */
  static ImmutableAnnotationInfo getImmutableAnnotation(Symbol sym) {
    String nameStr = sym.flatName().toString();
    ImmutableAnnotationInfo known = WellKnownMutability.KNOWN_IMMUTABLE.get(nameStr);
    if (known != null) {
      return known;
    }
    Immutable immutable = ASTHelpers.getAnnotation(sym, Immutable.class);
    if (immutable == null) {
      return null;
    }
    return ImmutableAnnotationInfo.create(
        sym.getQualifiedName().toString(), ImmutableList.copyOf(immutable.containerOf()));
  }

  /**
   * Gets the {@link Tree}'s {@code @Immutable} annotation info, either from an annotation on
   * the symbol or from the list of well-known immutable types.
   */
  static ImmutableAnnotationInfo getImmutableAnnotation(Tree tree) {
    Symbol sym = ASTHelpers.getSymbol(tree);
    return sym == null ? null : getImmutableAnnotation(sym);
  }

  /**
   * Gets the set of in-scope immutable type parameters from the containerOf specs
   * on {@code @Immutable} annotations.
   *
   * <p>Usually only the immediately enclosing declaration is searched, but it's
   * possible to have cases like:
   *
   * <pre>
   * @Immutable(containerOf="T") class C<T> {
   *   class Inner extends ImmutableCollection<T> {}
   * }
   * </pre>
   */
  private static ImmutableSet<String> immutableTypeParametersInScope(Symbol sym) {
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
      ImmutableAnnotationInfo annotation = getImmutableAnnotation(s);
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

  /** Gets a human-friendly name for the given {@link Symbol} to use in diagnostics. */
  private static String getPrettyName(Symbol sym, VisitorState state) {
    if (!sym.getSimpleName().isEmpty()) {
      return sym.getSimpleName().toString();
    }
    if (sym.getKind() == ElementKind.ENUM) {
      // anonymous classes for enum constants are identified by the enclosing constant
      // declaration
      return sym.owner.getSimpleName().toString();
    }
    // anonymous classes have an empty name, but a recognizable superclass
    // e.g. refer to `new Runnable() { ... }` as "Runnable"
    return state.getTypes().supertype(sym.type).tsym.getSimpleName().toString();
  }
}
