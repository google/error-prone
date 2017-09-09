/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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
import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;
import static com.google.errorprone.fixes.SuggestedFixes.addModifiers;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.isSameType;
import static com.google.errorprone.util.ASTHelpers.isSubtype;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.MethodTree;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import javax.lang.model.element.Modifier;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
  name = "MethodCanBeStatic",
  altNames = "static-method",
  summary = "A private method that does not reference the enclosing instance can be static",
  category = JDK,
  severity = SUGGESTION,
  providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION
)
public class MethodCanBeStatic extends BugChecker implements MethodTreeMatcher {

  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    MethodSymbol sym = ASTHelpers.getSymbol(tree);
    if (sym == null) {
      return NO_MATCH;
    }
    if (sym.isStatic() || sym.isConstructor() || sym.getModifiers().contains(Modifier.NATIVE)) {
      return NO_MATCH;
    }
    if (!sym.isPrivate()) {
      // Methods that override other methods, or that are overridden, can't be static.
      // We conservatively warn only for private methods.
      return NO_MATCH;
    }
    switch (sym.owner.enclClass().getNestingKind()) {
      case TOP_LEVEL:
        break;
      case MEMBER:
        if (sym.owner.enclClass().hasOuterInstance()) {
          return NO_MATCH;
        }
        break;
      case LOCAL:
      case ANONYMOUS:
        return NO_MATCH;
    }
    if (CanBeStaticAnalyzer.referencesOuter(tree, sym, state)) {
      return NO_MATCH;
    }
    if (isSubtype(sym.owner.enclClass().type, state.getSymtab().serializableType, state)) {
      switch (sym.getSimpleName().toString()) {
        case "readObject":
          if (sym.getParameters().size() == 1
              && isSameType(
                  getOnlyElement(sym.getParameters()).type,
                  state.getTypeFromString("java.io.ObjectInputStream"),
                  state)) {
            return NO_MATCH;
          }
          break;
        case "writeObject":
          if (sym.getParameters().size() == 1
              && isSameType(
                  getOnlyElement(sym.getParameters()).type,
                  state.getTypeFromString("java.io.ObjectOutputStream"),
                  state)) {
            return NO_MATCH;
          }
          break;
        case "readObjectNoData":
          if (sym.getParameters().size() == 0) {
            return NO_MATCH;
          }
          break;
        default: // fall out
      }
    }
    return describeMatch(tree.getModifiers(), addModifiers(tree, state, Modifier.STATIC));
  }
}
