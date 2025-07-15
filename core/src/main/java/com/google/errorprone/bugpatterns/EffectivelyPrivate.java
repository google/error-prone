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
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.hasAnnotation;
import static com.google.errorprone.util.ASTHelpers.isEffectivelyPrivate;
import static com.google.errorprone.util.ASTHelpers.streamSuperMethods;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import java.util.Collections;
import java.util.Optional;
import javax.inject.Inject;
import javax.lang.model.element.Modifier;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    summary = "This declaration has public or protected modifiers, but is effectively private.",
    severity = WARNING)
public final class EffectivelyPrivate extends BugChecker
    implements MethodTreeMatcher, VariableTreeMatcher, ClassTreeMatcher {

  private final WellKnownKeep wellKnownKeep;

  @Inject
  EffectivelyPrivate(WellKnownKeep wellKnownKeep) {
    this.wellKnownKeep = wellKnownKeep;
  }

  @Override
  public Description matchVariable(VariableTree tree, VisitorState state) {
    VarSymbol sym = getSymbol(tree);
    if (!sym.getKind().isField()) {
      return NO_MATCH;
    }
    return match(tree, tree.getModifiers(), state);
  }

  @Override
  public Description matchClass(ClassTree tree, VisitorState state) {
    return match(tree, tree.getModifiers(), state);
  }

  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    return match(tree, tree.getModifiers(), state);
  }

  private Description match(Tree tree, ModifiersTree modifiers, VisitorState state) {
    Symbol sym = getSymbol(tree);
    if (!isEffectivelyPrivate(sym)) {
      return NO_MATCH;
    }
    if (wellKnownKeep.shouldKeep(tree)) {
      return NO_MATCH;
    }
    if (sym instanceof MethodSymbol methodSymbol) {
      if (hasAnnotation(methodSymbol, "java.lang.Override", state)) {
        return NO_MATCH;
      }
      // TODO: cushon - technically this should only match final classes, otherwise it could break
      // a subclass that relies on inheriting a method of a particular visibility to fulfil and
      // interface contract. Skip that for now, since many classes don't rely on that and also
      // aren't explicitly final.
      if (streamSuperMethods(methodSymbol, state.getTypes()).findAny().isPresent()) {
        return NO_MATCH;
      }
    }
    if (Collections.disjoint(modifiers.getFlags(), MODIFIER_TO_REMOVE)) {
      return NO_MATCH;
    }
    Optional<SuggestedFix> fix = removeModifiers(modifiers, state, MODIFIER_TO_REMOVE);
    if (fix.isEmpty()) {
      // The fix may be empty for implicit modifiers, e.g. on enum constant fields
      return NO_MATCH;
    }
    return describeMatch(tree, fix.get());
  }

  private static final ImmutableSet<Modifier> MODIFIER_TO_REMOVE =
      ImmutableSet.of(Modifier.PUBLIC, Modifier.PROTECTED);
}
