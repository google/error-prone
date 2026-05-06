/*
 * Copyright 2026 The Error Prone Authors.
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

package com.google.errorprone.matchers.field;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.errorprone.util.ASTHelpers.getSymbol;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.suppliers.Supplier;
import com.sun.source.tree.ExpressionTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.util.Name;
import java.util.function.BiPredicate;

/** Matchers for fields. */
public final class FieldMatchers {

  /** Matches instance fields. */
  public interface InstanceFieldMatcher {
    FieldClassMatcher onClass(String className);
  }

  /** Matches static fields. */
  public interface StaticFieldMatcher {
    FieldClassMatcher onClass(String className);
  }

  /** Matches fields by declaring class. */
  public interface FieldClassMatcher {
    FieldNameMatcher named(String name);

    FieldNameMatcher namedAnyOf(String first, String second, String... rest);

    FieldNameMatcher namedAnyOf(ImmutableSet<String> names);
  }

  /** Matches fields by name. */
  public interface FieldNameMatcher extends Matcher<ExpressionTree> {}

  private static FieldClassMatcher fieldClassMatcher(
      BiPredicate<VarSymbol, VisitorState> predicate) {
    return new FieldClassMatcher() {
      @Override
      public FieldNameMatcher named(String name) {
        return fieldNameMatcher(
            predicate.and((symbol, state) -> symbol.getSimpleName().contentEquals(name)));
      }

      @Override
      public FieldNameMatcher namedAnyOf(String first, String second, String... rest) {
        return namedAnyOf(ImmutableSet.copyOf(Lists.asList(first, second, rest)));
      }

      @Override
      public FieldNameMatcher namedAnyOf(ImmutableSet<String> names) {
        Supplier<ImmutableSet<Name>> nameSupplier =
            VisitorState.memoize(
                state -> names.stream().map(state::getName).collect(toImmutableSet()));
        return fieldNameMatcher(
            predicate.and(
                (symbol, state) -> nameSupplier.get(state).contains(symbol.getSimpleName())));
      }
    };
  }

  private static FieldNameMatcher fieldNameMatcher(BiPredicate<VarSymbol, VisitorState> predicate) {
    return (expressionTree, state) ->
        getSymbol(expressionTree) instanceof VarSymbol symbol && predicate.test(symbol, state);
  }

  public static InstanceFieldMatcher instanceField() {
    return (className) -> {
      Supplier<Symbol> symbol = VisitorState.memoize(state -> state.getSymbolFromString(className));
      return fieldClassMatcher(
          (sym, state) -> {
            Symbol classSymbol = symbol.get(state);
            return classSymbol != null && !sym.isStatic() && sym.owner == classSymbol;
          });
    };
  }

  public static StaticFieldMatcher staticField() {
    return className -> {
      Supplier<Symbol> symbol = VisitorState.memoize(state -> state.getSymbolFromString(className));
      return fieldClassMatcher(
          (sym, state) -> {
            Symbol classSymbol = symbol.get(state);
            return classSymbol != null && sym.isStatic() && sym.owner == classSymbol;
          });
    };
  }

  private FieldMatchers() {}
}
