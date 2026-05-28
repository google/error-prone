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
import static com.google.errorprone.VisitorState.memoize;
import static com.google.errorprone.util.ASTHelpers.getUpperBound;
import static com.google.errorprone.util.ASTHelpers.isSameType;
import static com.google.errorprone.util.ASTHelpers.isSubtype;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.SEALED;

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
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.util.Name;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
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
    if (definitelyUsesReferenceEquality(type, state)) {
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
    if (sym.isStatic()) {
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

  /**
   * Returns {@code true} if an instance of {@code type} is guaranteed to have an {@code equals}
   * implementation that is equivalent to {@code ==}.
   *
   * <p>We can guarantee this for:
   *
   * <ul>
   *   <li>enum classes
   *   <li>{@code final} classes that inherit {@link Object#equals} instead of having a more
   *       specific implementation
   *   <li>{@code sealed} classes whose permitted subclasses all definitely use reference equality
   *       according to this method
   * </ul>
   */
  private static boolean definitelyUsesReferenceEquality(Type type, VisitorState state) {
    return definitelyUsesReferenceEquality(type, state, 0);
  }

  private static boolean definitelyUsesReferenceEquality(Type type, VisitorState state, int depth) {
    if (depth > 1000) {
      /*
       * javac should never generate classes that form a PermittedSubclasses cycle, but just in case
       * some system does, we bail out when we have seen a chain that is implausibly long.
       */
      return false;
    }

    /*
     * If a value has static type `Class`, for example, then it uses reference equality, since
     * `Class` is a `final` class that does not override `equals`. But we also want to cover cases
     * like those of a value whose static type is `T` if `T` is declared as `T extends Class<Foo>`.
     * To do so, we look at the upper bound of the static type, transitively resolving a chain of
     * bounds (e.g., `<T extends Class<?>, U extends T>`) until we reach a fixed point.
     */
    Type previous;
    do {
      previous = type;
      type = getUpperBound(type, state.getTypes());
    } while (!state.getTypes().isSameType(type, previous));
    if (type.tsym == null) {
      return false;
    }
    if (isSubtype(type, state.getSymtab().enumSym.type, state)) {
      return true;
    }
    if (implementsEquals(type, state)) {
      return false;
    }
    if (type.tsym.getModifiers().contains(FINAL)) {
      return true;
    }
    if (type.tsym.getModifiers().contains(SEALED)) {
      if (type.tsym instanceof ClassSymbol classSymbol) {
        for (Type sub : classSymbol.getPermittedSubclasses()) {
          if (!definitelyUsesReferenceEquality(sub, state, depth + 1)) {
            return false;
          }
        }
        return true;
      }
    }
    return false;
  }

  /**
   * Returns {@code true} if {@code type} declares or inherits an override of {@link Object#equals}.
   */
  private static boolean implementsEquals(Type type, VisitorState state) {
    Name equalsName = EQUALS.get(state);
    Symbol objectEquals = getOnlyMember(state, state.getSymtab().objectType, "equals");
    for (Type sup : state.getTypes().closure(type)) {
      if (isSameType(sup, state.getSymtab().objectType, state)) {
        continue;
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

  private static final Supplier<Name> EQUALS = memoize(state -> state.getName("equals"));
}
