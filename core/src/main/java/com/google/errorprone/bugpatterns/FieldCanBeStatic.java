/*
 * Copyright 2020 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;
import static com.google.errorprone.matchers.Description.NO_MATCH;

import com.google.common.base.Ascii;
import com.google.common.base.CaseFormat;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;

/** @author andrew@gaul.org (Andrew Gaul) */
@BugPattern(
    name = "FieldCanBeStatic",
    summary =
        "A final field initialized at compile-time with an instance of an immutable type can be"
            + " static.",
    severity = SUGGESTION)
public class FieldCanBeStatic extends BugChecker implements VariableTreeMatcher {

  @Override
  public Description matchVariable(VariableTree tree, VisitorState state) {
    VarSymbol sym = ASTHelpers.getSymbol(tree);
    if (!canBeStatic(sym)) {
      return NO_MATCH;
    }
    SuggestedFix.Builder fix = SuggestedFix.builder();
    SuggestedFixes.addModifiers(tree, state, Modifier.STATIC).ifPresent(fix::merge);
    String name = tree.getName().toString();
    if (!name.equals(Ascii.toUpperCase(name))) {
      String renamed = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, name);
      fix.merge(SuggestedFixes.renameVariable(tree, renamed, state));
    }
    return describeMatch(tree.getModifiers(), fix.build());
  }

  private static boolean canBeStatic(VarSymbol sym) {
    if (sym == null) {
      return false;
    }
    if (!sym.getKind().equals(ElementKind.FIELD)) {
      return false;
    }
    if (!sym.getModifiers().contains(Modifier.FINAL)) {
      return false;
    }
    if (!sym.isPrivate()) {
      return false;
    }
    if (sym.isStatic()) {
      return false;
    }
    if (sym.getConstantValue() == null) {
      return false;
    }
    if (sym.hasAnnotations()) {
      return false;
    }
    return true;
  }
}
