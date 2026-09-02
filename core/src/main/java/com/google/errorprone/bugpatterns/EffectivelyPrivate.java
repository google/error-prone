/*
 * Copyright 2025 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.fixes.SuggestedFixes.removeModifiers;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.enclosingClass;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.hasAnnotation;
import static com.google.errorprone.util.ASTHelpers.isCanonicalRecordConstructor;
import static com.google.errorprone.util.ASTHelpers.isEffectivelyPrivate;
import static com.google.errorprone.util.ASTHelpers.streamSuperMethods;
import static javax.lang.model.element.Modifier.PROTECTED;
import static javax.lang.model.element.Modifier.PUBLIC;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.CompilationUnitTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import java.util.Collections;
import javax.inject.Inject;
import javax.lang.model.element.Modifier;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    summary = "This declaration has public or protected modifiers, but is effectively private.",
    severity = WARNING)
public final class EffectivelyPrivate extends BugChecker implements CompilationUnitTreeMatcher {
  private final WellKnownKeep wellKnownKeep;

  @Inject
  EffectivelyPrivate(WellKnownKeep wellKnownKeep) {
    this.wellKnownKeep = wellKnownKeep;
  }

  @Override
  public Description matchCompilationUnit(CompilationUnitTree tree, VisitorState state) {
    ImmutableSet<ClassSymbol> hasVisibleSubclass = findClassesWithVisibleSubclasses(tree, state);
    new SuppressibleTreePathScanner<Void, Void>(state) {
      @Override
      public Void visitVariable(VariableTree tree, Void unused) {
        VarSymbol sym = getSymbol(tree);
        if (sym.getKind().isField()) {
          match(tree, tree.getModifiers(), hasVisibleSubclass, state);
        }
        return super.visitVariable(tree, null);
      }

      @Override
      public Void visitClass(ClassTree tree, Void unused) {
        match(tree, tree.getModifiers(), hasVisibleSubclass, state);
        return super.visitClass(tree, null);
      }

      @Override
      public Void visitMethod(MethodTree tree, Void unused) {
        match(tree, tree.getModifiers(), hasVisibleSubclass, state);
        return super.visitMethod(tree, null);
      }
    }.scan(tree, null);
    return NO_MATCH;
  }

  private void match(
      Tree tree,
      ModifiersTree modifiers,
      ImmutableSet<ClassSymbol> hasVisibleSubclass,
      VisitorState state) {
    Symbol sym = getSymbol(tree);
    if (!isEffectivelyPrivate(sym)) {
      return;
    }
    if (wellKnownKeep.shouldKeep(tree)) {
      return;
    }
    if (sym instanceof MethodSymbol methodSymbol) {
      if (hasAnnotation(methodSymbol, "java.lang.Override", state)) {
        return;
      }
      if (methodSymbol.isConstructor()) {
        ClassSymbol enclosingClass = enclosingClass(methodSymbol);
        if (isCanonicalRecordConstructor(methodSymbol, state)) {
          if (enclosingClass.getModifiers().contains(PUBLIC)
              || enclosingClass.getModifiers().contains(PROTECTED)) {
            // Canonical constructors are required to be at least as visible as the record itself.
            return;
          }
        }
      }
      // TODO: cushon - technically this should only match final classes, otherwise it could break
      // a subclass that relies on inheriting a method of a particular visibility to fulfil and
      // interface contract. Skip that for now, since many classes don't rely on that and also
      // aren't explicitly final.
      if (streamSuperMethods(methodSymbol, state.getTypes()).findAny().isPresent()) {
        return;
      }
    }
    var enclosingClass = enclosingClass(sym);
    if (hasVisibleSubclass.contains(enclosingClass)) {
      return;
    }
    if (Collections.disjoint(modifiers.getFlags(), MODIFIER_TO_REMOVE)) {
      return;
    }
    removeModifiers(modifiers, state, MODIFIER_TO_REMOVE)
        // The fix may be empty for implicit modifiers, e.g. on enum constant fields
        .ifPresent(fix -> state.reportMatch(describeMatch(tree, fix)));
  }

  private static final ImmutableSet<Modifier> MODIFIER_TO_REMOVE =
      ImmutableSet.of(Modifier.PUBLIC, Modifier.PROTECTED);

  private static ImmutableSet<ClassSymbol> findClassesWithVisibleSubclasses(
      CompilationUnitTree compilationUnit, VisitorState state) {
    ImmutableSet.Builder<ClassSymbol> hasVisibleSubclass = ImmutableSet.builder();
    new TreePathScanner<Void, Void>() {
      @Override
      public Void visitClass(ClassTree tree, Void unused) {
        ClassSymbol sym = getSymbol(tree);
        if (!isEffectivelyPrivate(sym)) {
          for (Type superType : state.getTypes().closure(sym.type)) {
            if (superType.tsym instanceof ClassSymbol classSymbol) {
              hasVisibleSubclass.add(classSymbol);
            }
          }
        }
        return super.visitClass(tree, null);
      }
    }.scan(compilationUnit, null);
    return hasVisibleSubclass.build();
  }
}
