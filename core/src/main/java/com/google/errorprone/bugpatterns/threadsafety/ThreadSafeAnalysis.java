/*
 * Copyright 2023 The Error Prone Authors.
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
import com.google.errorprone.annotations.concurrent.LazyInit;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.threadsafety.ThreadSafety.Violation;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.TypeVariableSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ClassType;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeKind;

/** Analyzes types for deep thread safety. */
public class ThreadSafeAnalysis {
  private final BugChecker bugChecker;
  private final VisitorState state;
  private final WellKnownThreadSafety wellKnownThreadSafety;
  private final ThreadSafety threadSafety;

  public ThreadSafeAnalysis(
      BugChecker bugChecker, VisitorState state, WellKnownThreadSafety wellKnownThreadSafety) {
    this.bugChecker = bugChecker;
    this.state = state;
    this.wellKnownThreadSafety = wellKnownThreadSafety;

    this.threadSafety = ThreadSafety.threadSafeBuilder(wellKnownThreadSafety).build(state);
  }

  boolean hasThreadSafeTypeParameterAnnotation(TypeVariableSymbol sym) {
    return threadSafety.hasThreadSafeTypeParameterAnnotation(sym);
  }

  boolean hasThreadSafeElementAnnotation(TypeVariableSymbol sym) {
    return threadSafety.hasThreadSafeElementAnnotation(sym);
  }

  Violation checkInstantiation(
      Collection<TypeVariableSymbol> classTypeParameters, Collection<Type> typeArguments) {
    return threadSafety.checkInstantiation(classTypeParameters, typeArguments);
  }

  public Violation checkInvocation(Type methodType, Symbol symbol) {
    return threadSafety.checkInvocation(methodType, symbol);
  }

  /**
   * Check that an {@code @ThreadSafe}-annotated class:
   *
   * <ul>
   *   <li>does not declare or inherit any fields which are not thread safe,
   *   <li>any threadsafe supertypes are instantiated with threadsafe type arguments as required by
   *       their containerOf spec, and
   *   <li>any enclosing instances are threadsafe.
   * </ul>
   *
   * requiring supertypes to be annotated threadsafe would be too restrictive.
   */
  public Violation checkForThreadSafety(
      Optional<ClassTree> tree, ImmutableSet<String> threadSafeTypeParams, ClassType type) {
    Violation info = areFieldsThreadSafe(tree, threadSafeTypeParams, type);
    if (info.isPresent()) {
      return info;
    }

    for (Type interfaceType : state.getTypes().interfaces(type)) {
      AnnotationInfo interfaceAnnotation =
          threadSafety.getMarkerOrAcceptedAnnotation(interfaceType.tsym, state);
      if (interfaceAnnotation == null) {
        continue;
      }
      Violation interfaceInfo =
          threadSafety.checkSuperInstantiation(
              threadSafeTypeParams, interfaceAnnotation, interfaceType);
      if (interfaceInfo.isPresent()) {
        return interfaceInfo.plus(
            String.format(
                "'%s' extends '%s'",
                threadSafety.getPrettyName(type.tsym),
                threadSafety.getPrettyName(interfaceType.tsym)));
      }
    }

    Violation superInfo = checkSuper(threadSafeTypeParams, type);
    if (superInfo.isPresent()) {
      return superInfo;
    }
    Type mutableEnclosing = threadSafety.mutableEnclosingInstance(tree, type);
    if (mutableEnclosing != null) {
      return Violation.of(
          String.format(
              "'%s' has non-thread-safe enclosing instance '%s'",
              threadSafety.getPrettyName(type.tsym), mutableEnclosing));
    }
    return Violation.absent();
  }

  private Violation checkSuper(ImmutableSet<String> threadSafeTypeParams, ClassType type) {
    ClassType superType = (ClassType) state.getTypes().supertype(type);
    if (superType.getKind() == TypeKind.NONE
        || state.getTypes().isSameType(state.getSymtab().objectType, superType)) {
      return Violation.absent();
    }
    if (WellKnownMutability.isAnnotation(state, type)) {
      // TODO(b/25630189): add enforcement
      return Violation.absent();
    }

    AnnotationInfo superannotation =
        threadSafety.getMarkerOrAcceptedAnnotation(superType.tsym, state);
    if (superannotation != null) {
      // If the superclass does happen to be threadsafe, we don't need to recursively
      // inspect it. We just have to check that it's instantiated correctly:
      Violation info =
          threadSafety.checkSuperInstantiation(threadSafeTypeParams, superannotation, superType);
      if (!info.isPresent()) {
        return Violation.absent();
      }
      return info.plus(
          String.format(
              "'%s' extends '%s'",
              threadSafety.getPrettyName(type.tsym), threadSafety.getPrettyName(superType.tsym)));
    }

    // Recursive case: check if the supertype is 'effectively' threadsafe.
    Violation info = checkForThreadSafety(Optional.empty(), threadSafeTypeParams, superType);
    if (!info.isPresent()) {
      return Violation.absent();
    }
    return info.plus(
        String.format(
            "'%s' extends '%s'",
            threadSafety.getPrettyName(type.tsym), threadSafety.getPrettyName(superType.tsym)));
  }

  /**
   * Check a single class' fields for thread safety.
   *
   * @param threadSafeTypeParams the in-scope threadsafe type parameters
   * @param classType the type to check the fields of
   */
  Violation areFieldsThreadSafe(
      Optional<ClassTree> tree, ImmutableSet<String> threadSafeTypeParams, ClassType classType) {
    ClassSymbol classSym = (ClassSymbol) classType.tsym;
    if (classSym.members() == null) {
      return Violation.absent();
    }
    Predicate<Symbol> instanceFieldFilter = symbol -> symbol.getKind() == ElementKind.FIELD;
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
        ImmutableList.copyOf(ASTHelpers.scope(classSym.members()).getSymbols(instanceFieldFilter))
            .reverse();
    for (Symbol member : members) {
      Optional<Tree> memberTree = Optional.ofNullable(declarations.get(member));
      Violation info =
          isFieldThreadSafe(
              memberTree, threadSafeTypeParams, classSym, classType, (VarSymbol) member);
      if (info.isPresent()) {
        return info;
      }
    }
    return Violation.absent();
  }

  /** Check a single field for thread safe. */
  private Violation isFieldThreadSafe(
      Optional<Tree> tree,
      Set<String> threadSafeTypeParams,
      ClassSymbol classSym,
      ClassType classType,
      VarSymbol var) {
    if (bugChecker.isSuppressed(var)
        || bugChecker.customSuppressionAnnotations().stream()
            .map(a -> ASTHelpers.hasAnnotation(var, a, state))
            .anyMatch(v -> v)) {
      return Violation.absent();
    }
    if (var.getModifiers().contains(Modifier.STATIC)) {
      return Violation.absent();
    }
    if (!GuardedByUtils.getGuardValues(var).isEmpty()) {
      return Violation.absent();
    }

    if (!var.getModifiers().contains(Modifier.FINAL)
        && !ASTHelpers.hasAnnotation(var, LazyInit.class, state)) {
      return processModifier(tree, classSym, var, Modifier.FINAL, "'%s' has non-final field '%s'");
    }
    Type varType = state.getTypes().memberType(classType, var);
    Violation info =
        threadSafety.isThreadSafeType(
            /* allowContainerTypeParameters= */ true, threadSafeTypeParams, varType);
    if (info.isPresent()) {
      if (tree.isPresent()) {
        // If we have a tree to attach diagnostics to, report the error immediately instead of
        // accumulating the path to the error from the top-level class being checked
        state.reportMatch(
            bugChecker
                .buildDescription(tree.get())
                .setMessage(info.plus("@ThreadSafe class has non-thread-safe field").message())
                .build());
        return Violation.absent();
      }
      return info.plus(
          String.format(
              "'%s' has field '%s' of type '%s'",
              threadSafety.getPrettyName(classSym), var.getSimpleName(), varType));
    }
    return Violation.absent();
  }

  private Violation processModifier(
      Optional<Tree> tree, ClassSymbol classSym, VarSymbol var, Modifier modifier, String message) {

    if (tree.isPresent()) {
      // If we have a tree to attach diagnostics to, report the error immediately instead of
      // accumulating the path to the error from the top-level class being checked
      state.reportMatch(
          bugChecker
              .buildDescription(tree.get())
              .setMessage(
                  "@ThreadSafe class fields should be final or annotated with @GuardedBy. See "
                      + "https://errorprone.info/bugpattern/ThreadSafe for details.")
              .addFix(SuggestedFixes.addModifiers(tree.get(), state, modifier))
              .build());
      return Violation.absent();
    }
    return Violation.of(
        String.format(message, threadSafety.getPrettyName(classSym), var.getSimpleName()));
  }

  /**
   * Gets the {@link Tree}'s {@code @ThreadSafe} annotation info, either from an annotation on the
   * symbol or from the list of well-known immutable types.
   */
  AnnotationInfo getThreadSafeAnnotation(Tree tree, VisitorState state) {
    Symbol sym = ASTHelpers.getSymbol(tree);
    return getThreadSafeAnnotation(sym, state);
  }

  AnnotationInfo getThreadSafeAnnotation(Symbol sym, VisitorState state) {
    String nameStr = sym.flatName().toString();
    AnnotationInfo known = wellKnownThreadSafety.getKnownThreadSafeClasses().get(nameStr);
    if (known != null) {
      return known;
    }
    return threadSafety.getInheritedAnnotation(sym, state);
  }

  public ImmutableSet<String> threadSafeTypeParametersInScope(Symbol sym) {
    return ImmutableSet.copyOf(threadSafety.threadSafeTypeParametersInScope(sym));
  }
}
