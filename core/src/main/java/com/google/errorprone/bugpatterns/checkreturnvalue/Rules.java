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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.errorprone.util.ASTHelpers.hasDirectAnnotationWithSimpleName;

import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.checkreturnvalue.ResultUseRule.GlobalRule;
import com.google.errorprone.bugpatterns.checkreturnvalue.ResultUseRule.SymbolRule;
import com.sun.tools.javac.code.Symbol;
import java.util.Optional;
import java.util.function.BiPredicate;

/** Factories for common kinds {@link ResultUseRule}s. */
public final class Rules {

  private Rules() {}

  /**
   * Returns a simple global rule that always returns the given defaults for methods and
   * constructors.
   */
  public static ResultUseRule globalDefault(
      Optional<ResultUsePolicy> methodDefault, Optional<ResultUsePolicy> constructorDefault) {
    return new SimpleGlobalRule("GLOBAL_DEFAULT", methodDefault, constructorDefault);
  }

  /**
   * Returns a {@link ResultUseRule} that maps annotations with the given {@code simpleName} to the
   * given {@code policy}.
   */
  public static ResultUseRule mapAnnotationSimpleName(String simpleName, ResultUsePolicy policy) {
    return new SimpleRule(
        "ANNOTATION @" + simpleName,
        (sym, st) -> hasDirectAnnotationWithSimpleName(sym, simpleName),
        policy);
  }

  private static final class SimpleRule extends SymbolRule {
    private final String name;
    private final BiPredicate<Symbol, VisitorState> predicate;
    private final ResultUsePolicy policy;

    private SimpleRule(
        String name, BiPredicate<Symbol, VisitorState> predicate, ResultUsePolicy policy) {
      this.name = name;
      this.predicate = predicate;
      this.policy = policy;
    }

    @Override
    public String id() {
      return name;
    }

    @Override
    public Optional<ResultUsePolicy> evaluate(Symbol symbol, VisitorState state) {
      return predicate.test(symbol, state) ? Optional.of(policy) : Optional.empty();
    }
  }

  private static final class SimpleGlobalRule extends GlobalRule {
    private final String id;
    private final Optional<ResultUsePolicy> methodDefault;
    private final Optional<ResultUsePolicy> constructorDefault;

    private SimpleGlobalRule(
        String id,
        Optional<ResultUsePolicy> methodDefault,
        Optional<ResultUsePolicy> constructorDefault) {
      this.id = checkNotNull(id);
      this.methodDefault = methodDefault;
      this.constructorDefault = constructorDefault;
    }

    @Override
    public String id() {
      return id;
    }

    @Override
    public Optional<ResultUsePolicy> evaluate(boolean constructor, VisitorState state) {
      return constructor ? constructorDefault : methodDefault;
    }
  }
}
