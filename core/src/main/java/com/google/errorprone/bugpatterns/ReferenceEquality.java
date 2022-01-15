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

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.suppliers.Supplier;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.tools.javac.code.Scope;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.util.Name;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    name = "ReferenceEquality",
    summary = "Comparison using reference equality instead of value equality",
    severity = WARNING,
    tags = StandardTags.FRAGILE_CODE)
public class ReferenceEquality extends AbstractReferenceEquality {

  @Override
  protected boolean matchArgument(ExpressionTree tree, VisitorState state) {
    Type type = ASTHelpers.getType(tree);
    if (!type.isReference()) {
      return false;
    }
    ClassTree classTree = ASTHelpers.findEnclosingNode(state.getPath(), ClassTree.class);
    if (classTree == null) {
      return false;
    }
    Type classType = ASTHelpers.getType(classTree);
    if (classType == null) {
      return false;
    }
    if (inComparisonMethod(classType, type, state)) {
      return false;
    }
    if (ASTHelpers.isSubtype(type, state.getSymtab().enumSym.type, state)) {
      return false;
    }
    if (ASTHelpers.isSubtype(type, state.getSymtab().classType, state)) {
      return false;
    }
    if (!implementsEquals(type, state)) {
      return false;
    }
    return true;
  }

  private static boolean inComparisonMethod(Type classType, Type type, VisitorState state) {
    Symtab symtab = state.getSymtab();
    // Check for lambdas implementing the Comparator.compare functional interface.
    LambdaExpressionTree lambdaTree =
        ASTHelpers.findEnclosingNode(state.getPath(), LambdaExpressionTree.class);
    if (lambdaTree != null) {
      return ASTHelpers.isSameType(ASTHelpers.getType(lambdaTree), symtab.comparatorType, state);
    }

    MethodTree methodTree = ASTHelpers.findEnclosingMethod(state);
    if (methodTree == null) {
      return false;
    }
    MethodSymbol sym = ASTHelpers.getSymbol(methodTree);
    if (sym == null || sym.isStatic()) {
      return false;
    }
    if (overridesMethodOnType(classType, sym, symtab.comparatorType, "compare", state)) {
      return true;
    }
    if (overridesMethodOnType(classType, sym, symtab.comparableType, "compareTo", state)
        || overridesMethodOnType(classType, sym, symtab.objectType, "equals", state)) {
      return ASTHelpers.isSameType(type, classType, state);
    }
    return false;
  }

  private static boolean overridesMethodOnType(
      Type classType,
      MethodSymbol methodSymbol,
      Type overriddenType,
      String overriddenMethodName,
      VisitorState state) {
    Symbol overriddenMethodSymbol = getOnlyMember(state, overriddenType, overriddenMethodName);
    return methodSymbol.getSimpleName().contentEquals(overriddenMethodName)
        && methodSymbol.overrides(
            overriddenMethodSymbol, classType.tsym, state.getTypes(), /* checkResult= */ false);
  }

  private static Symbol getOnlyMember(VisitorState state, Type type, String name) {
    return getOnlyElement(type.tsym.members().getSymbolsByName(state.getName(name)));
  }

  /** Check if the method declares or inherits an implementation of .equals() */
  public static boolean implementsEquals(Type type, VisitorState state) {
    Name equalsName = EQUALS.get(state);
    Symbol objectEquals = getOnlyMember(state, state.getSymtab().objectType, "equals");
    for (Type sup : state.getTypes().closure(type)) {
      if (sup.tsym.isInterface()) {
        continue;
      }
      if (ASTHelpers.isSameType(sup, state.getSymtab().objectType, state)) {
        return false;
      }
      Scope scope = sup.tsym.members();
      if (scope == null) {
        continue;
      }
      for (Symbol sym : scope.getSymbolsByName(equalsName)) {
        if (sym.overrides(objectEquals, type.tsym, state.getTypes(), /* checkResult= */ false)) {
          return true;
        }
      }
    }
    return false;
  }

  private static final Supplier<Name> EQUALS =
      VisitorState.memoize(state -> state.getName("equals"));
}
