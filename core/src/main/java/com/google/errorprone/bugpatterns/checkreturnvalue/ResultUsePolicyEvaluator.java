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
import static com.google.errorprone.bugpatterns.checkreturnvalue.ResultUsePolicy.UNSPECIFIED;
import static com.google.errorprone.bugpatterns.checkreturnvalue.ResultUseRule.RuleScope.ENCLOSING_ELEMENTS;
import static com.google.errorprone.bugpatterns.checkreturnvalue.ResultUseRule.RuleScope.GLOBAL;
import static java.util.Map.entry;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.bugpatterns.checkreturnvalue.ResultUseRule.Evaluation;
import com.google.errorprone.bugpatterns.checkreturnvalue.ResultUseRule.RuleScope;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Evaluates methods and their enclosing classes and packages to determine a {@link ResultUsePolicy}
 * for the methods.
 *
 * @param <C> the type of the context object used during evaluation
 * @param <S> the type of symbols
 * @param <M> the type of method symbols
 */
public final class ResultUsePolicyEvaluator<C, S, M extends S> {

  /**
   * Returns a new {@link Builder} for creating a {@link ResultUsePolicyEvaluator}.
   *
   * @param <C> the type of the context object used during evaluation
   * @param <S> the type of symbols
   * @param <M> the type of method symbols
   */
  public static <C, S, M extends S> ResultUsePolicyEvaluator.Builder<C, S, M> builder(
      MethodInfo<C, S, M> methodInfo) {
    return new Builder<>(methodInfo);
  }

  /**
   * Delegate to return information about a method symbol.
   *
   * @param <C> the type of the context object used during evaluation
   * @param <S> the type of symbols
   * @param <M> the type of method symbols
   */
  public interface MethodInfo<C, S, M extends S> {
    /** Returns an ordered stream of elements in this scope relative to the given {@code method}. */
    Stream<S> scopeMembers(RuleScope scope, M method, C context);

    /** Returns the kind of the given method. */
    MethodKind getMethodKind(M method);

    /** Returns the scopes that apply for the given method. */
    default ImmutableList<RuleScope> scopes(M method) {
      return getMethodKind(method).scopes;
    }

    /** What kind a method symbol is, and what scopes apply to it. */
    enum MethodKind {
      /** An actual method, not a constructor. */
      METHOD(RuleScope.METHOD, ENCLOSING_ELEMENTS, GLOBAL),

      /** A constructor. */
      // TODO(cgdecker): Constructors in particular (though really all methods I think) should
      //  not be able to get a policy of OPTIONAL from enclosing elements. Only defaults should
      //  come from enclosing elements, and there should only be one default policy (EXPECTED).
      CONSTRUCTOR(RuleScope.METHOD, ENCLOSING_ELEMENTS, GLOBAL),

      /** Neither a method nor a constructor. */
      OTHER(),
      ;

      private final ImmutableList<RuleScope> scopes;

      MethodKind(RuleScope... scopes) {
        this.scopes = ImmutableList.copyOf(scopes);
      }
    }
  }

  /** All the rules for this evaluator, indexed by the scopes they apply to. */
  private final ImmutableListMultimap<RuleScope, ResultUseRule<C, S>> rules;

  private final MethodInfo<C, S, M> methodInfo;

  private ResultUsePolicyEvaluator(Builder<C, S, M> builder) {
    this.rules =
        builder.rules.stream()
            .flatMap(rule -> rule.scopes().stream().map(scope -> entry(scope, rule)))
            .collect(toImmutableListMultimap(Entry::getKey, Entry::getValue));
    this.methodInfo = builder.methodInfo;
  }

  /**
   * Evaluates the given {@code method} and returns a single {@link ResultUsePolicy} that should
   * apply to it.
   */
  public ResultUsePolicy evaluate(M method, C state) {
    // Equivalent to evaluations(method, state).findFirst().map(Evaluation::policy), but avoids the
    // overhead of streams, since this is called for every invocation whose result is unused. Rules
    // are evaluated in the same order, stopping at the first one that returns a policy.
    for (RuleScope scope : methodInfo.scopes(method)) {
      ImmutableList<ResultUseRule<C, S>> scopeRules = rules.get(scope);
      Iterator<S> members = methodInfo.scopeMembers(scope, method, state).iterator();
      while (members.hasNext()) {
        S symbol = members.next();
        for (ResultUseRule<C, S> rule : scopeRules) {
          Optional<ResultUsePolicy> policy = rule.evaluate(symbol, state);
          if (policy.isPresent()) {
            return policy.get();
          }
        }
      }
    }
    return UNSPECIFIED;
  }

  /**
   * Returns a stream of {@link Evaluation}s made by rules starting from the given {@code method}.
   */
  public Stream<Evaluation<S>> evaluations(M method, C state) {
    return methodInfo.scopes(method).stream()
        .flatMap(scope -> evaluateForScope(method, state, scope));
  }

  /**
   * Evaluates all rules in a {@code scope} for each member of the {@code scope} for the {@code
   * method}.
   */
  private Stream<Evaluation<S>> evaluateForScope(M method, C state, RuleScope scope) {
    ImmutableList<ResultUseRule<C, S>> scopeRules = rules.get(scope);
    return methodInfo
        .scopeMembers(scope, method, state)
        .flatMap(symbol -> scopeRules.stream().map(rule -> rule.evaluate(scope, symbol, state)))
        .flatMap(Optional::stream);
  }

  /**
   * Builder for {@link ResultUsePolicyEvaluator}.
   *
   * @param <C> the type of the context object used during evaluation
   * @param <S> the type of symbols
   * @param <M> the type of method symbols
   */
  // TODO(dpb): Consider using @AutoBuilder.
  public static final class Builder<C, S, M extends S> {
    private final List<ResultUseRule<C, S>> rules = new ArrayList<>();
    private final MethodInfo<C, S, M> methodInfo;

    private Builder(MethodInfo<C, S, M> methodInfo) {
      this.methodInfo = methodInfo;
    }

    /** Adds the given {@code rule}. */
    @CanIgnoreReturnValue
    public Builder<C, S, M> addRule(ResultUseRule<C, S> rule) {
      this.rules.add(rule);
      return this;
    }

    /** Adds all the given {@code rules}. */
    @CanIgnoreReturnValue
    public Builder<C, S, M> addRules(ResultUseRule<C, S>... rules) {
      return addRules(Arrays.asList(rules));
    }

    /** Adds all the given {@code rules}. */
    @CanIgnoreReturnValue
    public Builder<C, S, M> addRules(Iterable<? extends ResultUseRule<C, S>> rules) {
      rules.forEach(this::addRule);
      return this;
    }

    /** Builds a new {@link ResultUsePolicyEvaluator}. */
    public ResultUsePolicyEvaluator<C, S, M> build() {
      return new ResultUsePolicyEvaluator<>(this);
    }
  }
}
