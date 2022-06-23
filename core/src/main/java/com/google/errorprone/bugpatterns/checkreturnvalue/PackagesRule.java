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

package com.google.errorprone.bugpatterns.checkreturnvalue;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.checkreturnvalue.ResultUseRule.SymbolRule;
import com.google.errorprone.suppliers.Supplier;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.PackageSymbol;
import com.sun.tools.javac.util.Name;
import java.util.Optional;

/**
 * A rule that enables checking for methods belonging to a set of packages or any of their
 * subpackages.
 */
public final class PackagesRule extends SymbolRule {

  /**
   * Returns a new rule using the given package {@code patterns}. Each pattern string must either be
   * the fully qualified name of a package (to enable checking for methods in that package and its
   * subpackages) or a {@code -} character followed by the fully qualified name of a package (to
   * disable checking for methods in that package and its subpackages).
   */
  public static PackagesRule fromPatterns(Iterable<String> patterns) {
    return new PackagesRule(ImmutableList.copyOf(patterns));
  }

  private final Supplier<ImmutableMap<Name, Boolean>> packagesSupplier;

  private PackagesRule(ImmutableList<String> patterns) {
    this.packagesSupplier =
        VisitorState.memoize(
            state -> {
              ImmutableMap.Builder<Name, Boolean> builder = ImmutableMap.builder();
              for (String pattern : patterns) {
                if (pattern.charAt(0) == '-') {
                  builder.put(state.getName(pattern.substring(1)), false);
                } else {
                  builder.put(state.getName(pattern), true);
                }
              }
              return builder.buildOrThrow();
            });
  }

  @Override
  public final String id() {
    return "Packages";
  }

  @Override
  public Optional<ResultUsePolicy> evaluate(Symbol symbol, VisitorState state) {
    while (symbol instanceof PackageSymbol) {
      Boolean value = packagesSupplier.get(state).get(((PackageSymbol) symbol).fullname);
      if (value != null) {
        return value
            ? Optional.of(ResultUsePolicy.EXPECTED)
            // stop evaluating if the package matched a negative pattern
            : Optional.empty();
      }
      symbol = symbol.owner;
    }
    return Optional.empty();
  }
}
