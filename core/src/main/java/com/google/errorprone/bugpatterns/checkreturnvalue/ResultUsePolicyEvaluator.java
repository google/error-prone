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

import static com.google.common.collect.ImmutableListMultimap.toImmutableListMultimap;
import static com.google.errorprone.bugpatterns.checkreturnvalue.ResultUsePolicy.OPTIONAL;
import static com.google.errorprone.bugpatterns.checkreturnvalue.ResultUseRule.RuleScope.ENCLOSING_ELEMENTS;
import static com.google.errorprone.bugpatterns.checkreturnvalue.ResultUseRule.RuleScope.GLOBAL;
import static com.google.errorprone.bugpatterns.checkreturnvalue.ResultUseRule.RuleScope.METHOD;
import static java.util.Map.entry;

import com.google.common.collect.ImmutableListMultimap;
import com.google.errorprone.VisitorState;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.bugpatterns.checkreturnvalue.ResultUseRule.Evaluation;
import com.google.errorprone.bugpatterns.checkreturnvalue.ResultUseRule.RuleScope;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Stream;
import javax.lang.model.element.ElementKind;

/**
 * Evaluates methods and their enclosing classes and packages to determine a {@link ResultUsePolicy}
 * for the methods.
 */
public final class ResultUsePolicyEvaluator {

  /** Creates a new {@link ResultUsePolicyEvaluator} using the given {@code rules}. */
  public static ResultUsePolicyEvaluator create(ResultUseRule... rules) {
    return create(Arrays.asList(rules));
  }

  /** Creates a new {@link ResultUsePolicyEvaluator} using the given {@code rules}. */
  public static ResultUsePolicyEvaluator create(Iterable<? extends ResultUseRule> rules) {
    return builder().addRules(rules).build();
  }

  /** Returns a new {@link Builder} for creating a {@link ResultUsePolicyEvaluator}. */
  public static ResultUsePolicyEvaluator.Builder builder() {
    return new Builder();
  }

  /** Map of method symbol kinds to the scopes that should be evaluated for that kind of symbol. */
  private static final ImmutableListMultimap<ElementKind, RuleScope> SCOPES =
      ImmutableListMultimap.<ElementKind, RuleScope>builder()
          .putAll(ElementKind.METHOD, METHOD, ENCLOSING_ELEMENTS, GLOBAL)
          // TODO(cgdecker): Constructors in particular (though really all methods I think) should
          //  not be able to get a policy of OPTIONAL from enclosing elements. Only defaults should
          //  come from enclosing elements, and there should only be one default policy (EXPECTED).
          .putAll(ElementKind.CONSTRUCTOR, METHOD, ENCLOSING_ELEMENTS, GLOBAL)
          .build();

  /** All the rules for this evaluator, indexed by the scopes they apply to. */
  private final ImmutableListMultimap<RuleScope, ResultUseRule> rules;

  private ResultUsePolicyEvaluator(Builder builder) {
    this.rules =
        builder.rules.stream()
            .flatMap(rule -> rule.scopes().stream().map(scope -> entry(scope, rule)))
            .collect(toImmutableListMultimap(Entry::getKey, Entry::getValue));
  }

  /**
   * Evaluates the given {@code method} and returns a single {@link ResultUsePolicy} that should
   * apply to it.
   */
  public ResultUsePolicy evaluate(MethodSymbol method, VisitorState state) {
    return policies(method, state).findFirst().orElse(OPTIONAL);
  }

  private Stream<ResultUsePolicy> policies(MethodSymbol method, VisitorState state) {
    return SCOPES.get(method.getKind()).stream()
        .flatMap(scope -> scope.policies(method, state, rules));
  }

  /**
   * Returns a stream of {@link Evaluation}s made by rules starting from the given {@code method}.
   */
  public Stream<Evaluation> evaluations(MethodSymbol method, VisitorState state) {
    return SCOPES.get(method.getKind()).stream()
        .flatMap(scope -> scope.evaluations(method, state, rules));
  }

  /** Builder for {@link ResultUsePolicyEvaluator}. */
  public static final class Builder {
    private final List<ResultUseRule> rules = new ArrayList<>();

    private Builder() {}

    /** Adds the given {@code rule}. */
    @CanIgnoreReturnValue
    public Builder addRule(ResultUseRule rule) {
      this.rules.add(rule);
      return this;
    }

    /** Adds all the given {@code rules}. */
    @CanIgnoreReturnValue
    public Builder addRules(ResultUseRule... rules) {
      return addRules(Arrays.asList(rules));
    }

    /** Adds all the given {@code rules}. */
    @CanIgnoreReturnValue
    public Builder addRules(Iterable<? extends ResultUseRule> rules) {
      rules.forEach(this::addRule);
      return this;
    }

    /** Builds a new {@link ResultUsePolicyEvaluator}. */
    public ResultUsePolicyEvaluator build() {
      return new ResultUsePolicyEvaluator(this);
    }
  }
}
