/*
 * Copyright 2015 The Error Prone Authors.
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

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.MethodTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import javax.lang.model.element.Modifier;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    name = "MissingOverride",
    summary = "method overrides method in supertype; expected @Override",
    severity = WARNING,
    tags = StandardTags.STYLE)
public class MissingOverride extends BugChecker implements MethodTreeMatcher {

  /** if true, don't warn on missing {@code @Override} annotations inside interfaces */
  private final boolean ignoreInterfaceOverrides;

  public MissingOverride() {
    this(ErrorProneFlags.empty());
  }

  public MissingOverride(ErrorProneFlags flags) {
    this.ignoreInterfaceOverrides =
        flags.getBoolean("MissingOverride:IgnoreInterfaceOverrides").orElse(false);
  }

  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    Symbol sym = ASTHelpers.getSymbol(tree);
    if (sym.isStatic()) {
      return Description.NO_MATCH;
    }
    if (ASTHelpers.hasAnnotation(sym, Override.class, state)) {
      return Description.NO_MATCH;
    }
    MethodSymbol override = getFirstOverride(sym, state.getTypes());
    if (override == null) {
      return Description.NO_MATCH;
    }
    if (!ASTHelpers.getGeneratedBy(state).isEmpty()) {
      return Description.NO_MATCH;
    }
    if (ASTHelpers.hasAnnotation(override, Deprecated.class, state)) {
      // to allow deprecated methods to be removed non-atomically, we permit overrides of
      // @Deprecated to skip the annotation
      return Description.NO_MATCH;
    }
    return buildDescription(tree)
        .addFix(SuggestedFix.prefixWith(tree, "@Override "))
        .setMessage(
            String.format(
                "%s %s method in %s; expected @Override",
                sym.getSimpleName(),
                override.enclClass().isInterface()
                        || override.getModifiers().contains(Modifier.ABSTRACT)
                    ? "implements"
                    : "overrides",
                override.enclClass().getSimpleName()))
        .build();
  }

  /**
   * Returns the {@link MethodSymbol} of the first method that sym overrides in its supertype
   * closure, or {@code null} if no such method exists.
   */
  private MethodSymbol getFirstOverride(Symbol sym, Types types) {
    ClassSymbol owner = sym.enclClass();
    if (ignoreInterfaceOverrides && owner.isInterface()) {
      // pretend the method does not override anything
      return null;
    }
    for (Type s : types.closure(owner.type)) {
      if (types.isSameType(s, owner.type)) {
        continue;
      }
      for (Symbol m : s.tsym.members().getSymbolsByName(sym.name)) {
        if (!(m instanceof MethodSymbol)) {
          continue;
        }
        MethodSymbol msym = (MethodSymbol) m;
        if (msym.isStatic()) {
          continue;
        }
        if (sym.overrides(msym, owner, types, /* checkResult= */ false)) {
          return msym;
        }
      }
    }
    return null;
  }
}
