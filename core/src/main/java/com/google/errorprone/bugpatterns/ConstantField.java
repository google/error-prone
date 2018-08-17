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

package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;

import com.google.common.base.CaseFormat;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.names.NamingConventions;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
    name = "ConstantField",
    category = JDK,
    summary = "Field name is CONSTANT_CASE, but field is not static and final",
    severity = SUGGESTION,
    providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION
    // TODO(glorioso): This feels like a Style change, but we suggest adding static and final
    // to a field which may not compile if we do. We'll want to be more aggressive about not making
    // breaking changes before we consider this a Style change.
    // tags = StandardTags.STYLE
    )
public class ConstantField extends BugChecker implements VariableTreeMatcher {

  @Override
  public Description matchVariable(VariableTree tree, VisitorState state) {
    Symbol.VarSymbol sym = ASTHelpers.getSymbol(tree);
    if (sym == null || sym.getKind() != ElementKind.FIELD) {
      return Description.NO_MATCH;
    }
    String name = sym.getSimpleName().toString();
    if (sym.isStatic() && sym.getModifiers().contains(Modifier.FINAL)) {
      return checkImmutable(tree, state, sym, name);
    }
    if (!name.equals(name.toUpperCase())) {
      return Description.NO_MATCH;
    }

    Description.Builder descriptionBuilder = buildDescription(tree);
    if (canBecomeStaticMember(sym)) {
      descriptionBuilder.addFix(
          SuggestedFixes.addModifiers(tree, state, Modifier.FINAL, Modifier.STATIC)
              .map(
                  f ->
                      SuggestedFix.builder()
                          .setShortDescription("make static and final")
                          .merge(f)
                          .build()));
    }
    return descriptionBuilder
        .addFix(
            SuggestedFix.builder()
                .setShortDescription("change to camelcase")
                .merge(
                    SuggestedFixes.renameVariable(
                        tree, CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, name), state))
                .build())
        .build();
  }

  private static boolean canBecomeStaticMember(VarSymbol sym) {
    // JLS 8.1.3: It is a compile-time error if an inner class declares a member that is
    // explicitly or implicitly static, unless the member is a constant variable (§4.12.4).

    // We could try and figure out if the declaration *would* be a compile time constant if made
    // static, but that's a bit much to keep adding this fix.
    ClassSymbol owningClass = sym.enclClass();

    // Enum anonymous classes aren't considered isInner() even though they can't hold static fields
    switch (owningClass.getNestingKind()) {
      case LOCAL:
      case ANONYMOUS:
        return false;
      default:
        return !owningClass.isInner();
    }
  }

  private Description checkImmutable(
      VariableTree tree, VisitorState state, VarSymbol sym, String name) {
    Type type = sym.type;
    if (type == null) {
      return Description.NO_MATCH;
    }
    switch (name) {
      case "serialVersionUID":
        // mandated by the Serializable API
        return Description.NO_MATCH;
      default:
        break;
    }
    if (name.toUpperCase().equals(name)) {
      return Description.NO_MATCH;
    }
    if (state.getTypes().unboxedTypeOrType(type).isPrimitive()
        || ASTHelpers.isSameType(type, state.getSymtab().stringType, state)
        || type.tsym.getKind() == ElementKind.ENUM) {
      String constName = upperCaseReplace(name);
      return buildDescription(tree)
          .setMessage(
              String.format(
                  "%ss are immutable, field should be named '%s'",
                  sym.type.tsym.getSimpleName(), constName))
          .addFix(SuggestedFixes.renameVariable(tree, constName, state))
          .build();
    }
    return Description.NO_MATCH;
  }

  private static String upperCaseReplace(String name) {
    String constName;
    if (name.contains("_")) {
      constName = name.toUpperCase();
    } else {
      constName = NamingConventions.convertToLowerUnderscore(name).toUpperCase();
    }

    // C++-style constants like kFooBar should become FOO_BAR, not K_FOO_BAR
    if (constName.startsWith("K_")) {
      constName = constName.substring("K_".length());
    }
    return constName;
  }
}
