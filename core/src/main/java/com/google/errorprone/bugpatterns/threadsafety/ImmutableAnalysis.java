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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.VisitorState;
import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.annotations.Immutable;
import com.google.errorprone.annotations.ImmutableTypeParameter;
import com.google.errorprone.annotations.concurrent.LazyInit;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.threadsafety.ThreadSafety.Purpose;
import com.google.errorprone.bugpatterns.threadsafety.ThreadSafety.Violation;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.TypeVariableSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ClassType;
import com.sun.tools.javac.util.Filter;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeKind;

/** Analyzes types for deep immutability. */
public class ImmutableAnalysis {

  private final BugChecker bugChecker;
  private final VisitorState state;
  private final WellKnownMutability wellKnownMutability;
  private final ThreadSafety threadSafety;

  ImmutableAnalysis(
      BugChecker bugChecker,
      VisitorState state,
      WellKnownMutability wellKnownMutability,
      ImmutableSet<String> immutableAnnotations) {
    this.bugChecker = bugChecker;
    this.state = state;
    this.wellKnownMutability = wellKnownMutability;
    this.threadSafety =
        ThreadSafety.builder()
            .setPurpose(Purpose.FOR_IMMUTABLE_CHECKER)
            .knownTypes(wellKnownMutability)
            .markerAnnotations(immutableAnnotations)
            .typeParameterAnnotation(ImmutableTypeParameter.class)
            .build(state);
  }

  public ImmutableAnalysis(
      BugChecker bugChecker, VisitorState state, WellKnownMutability wellKnownMutability) {
    this(bugChecker, state, wellKnownMutability, ImmutableSet.of(Immutable.class.getName()));
  }

  Violation isThreadSafeType(
      boolean allowContainerTypeParameters, Set<String> containerTypeParameters, Type type) {
    return threadSafety.isThreadSafeType(
        allowContainerTypeParameters, containerTypeParameters, type);
  }

  boolean hasThreadSafeTypeParameterAnnotation(TypeVariableSymbol sym) {
    return threadSafety.hasThreadSafeTypeParameterAnnotation(sym);
  }

  Violation checkInstantiation(
      Collection<TypeVariableSymbol> classTypeParameters, Collection<Type> typeArguments) {
    return threadSafety.checkInstantiation(classTypeParameters, typeArguments);
  }

  public Violation checkInvocation(Type methodType, Symbol symbol) {
    return threadSafety.checkInvocation(methodType, symbol);
  }

  /** Accepts {@link Violation violations} that are found during the analysis. */
  @FunctionalInterface
  public interface ViolationReporter {
    Description.Builder describe(Tree tree, Violation info);

    @CheckReturnValue
    default Description report(Tree tree, Violation info, Optional<SuggestedFix> suggestedFix) {
      Description.Builder description = describe(tree, info);
      suggestedFix.ifPresent(description::addFix);
      return description.build();
    }
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
  public Violation checkForImmutability(
      Optional<ClassTree> tree,
      ImmutableSet<String> immutableTyParams,
      ClassType type,
      ViolationReporter reporter) {
    Violation info = areFieldsImmutable(tree, immutableTyParams, type, reporter);
    if (info.isPresent()) {
      return info;
    }

    for (Type interfaceType : state.getTypes().interfaces(type)) {
      AnnotationInfo interfaceAnnotation = getImmutableAnnotation(interfaceType.tsym, state);
      if (interfaceAnnotation == null) {
        continue;
      }
      info =
          threadSafety.checkSuperInstantiation(
              immutableTyParams, interfaceAnnotation, interfaceType);
      if (info.isPresent()) {
        return info.plus(
            String.format(
                "'%s' extends '%s'",
                threadSafety.getPrettyName(type.tsym),
                threadSafety.getPrettyName(interfaceType.tsym)));
      }
    }

    if (!type.asElement().isEnum()) {
      // don't check enum super types here to avoid double-reporting errors
      info = checkSuper(immutableTyParams, type);
      if (info.isPresent()) {
        return info;
      }
    }
    Type mutableEnclosing = threadSafety.mutableEnclosingInstance(tree, type);
    if (mutableEnclosing != null) {
      return info.plus(
          String.format(
              "'%s' has mutable enclosing instance '%s'",
              threadSafety.getPrettyName(type.tsym), mutableEnclosing));
    }
    return Violation.absent();
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
    String message =
        String.format(
            "'%s' extends '%s'",
            threadSafety.getPrettyName(type.tsym), threadSafety.getPrettyName(superType.tsym));
    if (superannotation != null) {
      // If the superclass does happen to be immutable, we don't need to recursively
      // inspect it. We just have to check that it's instantiated correctly:
      Violation info =
          threadSafety.checkSuperInstantiation(immutableTyParams, superannotation, superType);
      if (!info.isPresent()) {
        return Violation.absent();
      }
      return info.plus(message);
    }

    // Recursive case: check if the supertype is 'effectively' immutable.
    Violation info =
        checkForImmutability(
            Optional.<ClassTree>empty(),
            immutableTyParams,
            superType,
            new ViolationReporter() {
              @Override
              public Description.Builder describe(Tree tree, Violation info) {
                return BugChecker.buildDescriptionFromChecker(tree, bugChecker)
                    .setMessage(info.plus(info.message()).message());
              }
            });
    if (!info.isPresent()) {
      return Violation.absent();
    }
    return info.plus(message);
  }

  /**
   * Check a single class' fields for immutability.
   *
   * @param immutableTyParams the in-scope immutable type parameters
   * @param classType the type to check the fields of
   */
  Violation areFieldsImmutable(
      Optional<ClassTree> tree,
      ImmutableSet<String> immutableTyParams,
      ClassType classType,
      ViolationReporter reporter) {
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
          isFieldImmutable(
              memberTree, immutableTyParams, classSym, classType, (VarSymbol) member, reporter);
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
      ViolationReporter reporter) {
    if (bugChecker.isSuppressed(var)) {
      return Violation.absent();
    }
    if (!var.getModifiers().contains(Modifier.FINAL)
        && !ASTHelpers.hasAnnotation(var, LazyInit.class, state)) {

      Violation info =
          Violation.of(
              String.format(
                  "'%s' has non-final field '%s'",
                  threadSafety.getPrettyName(classSym), var.getSimpleName()));
      if (tree.isPresent()) {
        // If we have a tree to attach diagnostics to, report the error immediately instead of
        // accumulating the path to the error from the top-level class being checked
        state.reportMatch(
            reporter.report(
                tree.get(), info, SuggestedFixes.addModifiers(tree.get(), state, Modifier.FINAL)));
        return Violation.absent();
      }
      return info;
    }
    Type varType = state.getTypes().memberType(classType, var);
    Violation info =
        threadSafety.isThreadSafeType(
            /* allowContainerTypeParameters= */ true, immutableTyParams, varType);
    if (info.isPresent()) {
      info =
          info.plus(
              String.format(
                  "'%s' has field '%s' of type '%s'",
                  threadSafety.getPrettyName(classSym), var.getSimpleName(), varType));
      if (tree.isPresent()) {
        // If we have a tree to attach diagnostics to, report the error immediately instead of
        // accumulating the path to the error from the top-level class being checked
        state.reportMatch(reporter.report(tree.get(), info, Optional.empty()));
        return Violation.absent();
      }
      return info;
    }
    return Violation.absent();
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
    return threadSafety.getInheritedAnnotation(sym, state);
  }

  /**
   * Gets the {@link Tree}'s {@code @Immutable} annotation info, either from an annotation on the
   * symbol or from the list of well-known immutable types.
   */
  AnnotationInfo getImmutableAnnotation(Tree tree, VisitorState state) {
    Symbol sym = ASTHelpers.getSymbol(tree);
    return sym == null ? null : threadSafety.getMarkerOrAcceptedAnnotation(sym, state);
  }
}
