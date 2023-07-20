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

package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.fixes.SuggestedFixes.prettyType;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.enclosingClass;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.bugpatterns.threadsafety.WellKnownMutability;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.suppliers.Supplier;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import javax.inject.Inject;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(summary = "Fields in Guice modules should be final", severity = WARNING)
public class MutableGuiceModule extends BugChecker implements VariableTreeMatcher {

  private final WellKnownMutability wellKnownMutability;

  @Inject
  MutableGuiceModule(WellKnownMutability wellKnownMutability) {
    this.wellKnownMutability = wellKnownMutability;
  }

  @Override
  public Description matchVariable(VariableTree tree, VisitorState state) {
    if (state.errorProneOptions().isTestOnlyTarget()) {
      return NO_MATCH;
    }
    VarSymbol sym = getSymbol(tree);
    if (sym == null) {
      return NO_MATCH;
    }
    if (!sym.getKind().equals(ElementKind.FIELD)) {
      return NO_MATCH;
    }
    Symbol abstractModule = ABSTRACT_MODULE.get(state);
    if (abstractModule == null) {
      return NO_MATCH;
    }
    if (!enclosingClass(sym).isSubClass(abstractModule, state.getTypes())) {
      return NO_MATCH;
    }
    if (sym.isStatic()) {
      return NO_MATCH;
    }
    if (!tree.getModifiers().getFlags().contains(Modifier.FINAL)) {
      Description.Builder description = buildDescription(tree);
      SuggestedFixes.addModifiers(tree, state, Modifier.FINAL)
          .filter(f -> SuggestedFixes.compilesWithFix(f, state))
          .ifPresent(description::addFix);
      state.reportMatch(description.build());
    }
    Type type = getType(tree);
    String nameStr = type.tsym.flatName().toString();
    if (wellKnownMutability.getKnownMutableClasses().contains(nameStr)) {
      state.reportMatch(
          buildDescription(tree)
              .setMessage(
                  String.format(
                      "Fields in Guice modules should be immutable, but %s is mutable",
                      prettyType(type, state)))
              .build());
    }
    return NO_MATCH;
  }

  private static final Supplier<Symbol> ABSTRACT_MODULE =
      VisitorState.memoize(state -> state.getSymbolFromString("com.google.inject.AbstractModule"));
}
