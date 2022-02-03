/*
 * Copyright 2022 The Error Prone Authors.
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

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.common.collect.Sets.immutableEnumSet;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.MethodTree;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import javax.lang.model.element.Modifier;

/** Bugpattern for incorrect overloads of main. */
@BugPattern(summary = "'main' methods must be public, static, and void", severity = WARNING)
public final class IncorrectMainMethod extends BugChecker implements MethodTreeMatcher {

  private static final ImmutableSet<Modifier> REQUIRED_MODIFIERS =
      immutableEnumSet(Modifier.PUBLIC, Modifier.STATIC);

  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    if (!tree.getName().contentEquals("main")) {
      return NO_MATCH;
    }
    MethodSymbol sym = getSymbol(tree);
    if (sym.getParameters().size() != 1) {
      return NO_MATCH;
    }
    Type type = getOnlyElement(sym.getParameters()).asType();
    Types types = state.getTypes();
    Symtab symtab = state.getSymtab();
    if (!types.isArray(type) || !types.isSameType(types.elemtype(type), symtab.stringType)) {
      return NO_MATCH;
    }
    if (sym.getModifiers().containsAll(REQUIRED_MODIFIERS)
        && types.isSameType(getType(tree.getReturnType()), symtab.voidType)) {
      return NO_MATCH;
    }
    SuggestedFix.Builder fix = SuggestedFix.builder().replace(tree.getReturnType(), "void");
    SuggestedFixes.addModifiers(tree, tree.getModifiers(), state, REQUIRED_MODIFIERS)
        .ifPresent(fix::merge);
    return describeMatch(tree, fix.build());
  }
}
