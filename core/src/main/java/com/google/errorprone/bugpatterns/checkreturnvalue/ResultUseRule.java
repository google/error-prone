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

import static com.google.errorprone.bugpatterns.checkreturnvalue.ResultUseRule.RuleScope.ENCLOSING_ELEMENTS;
import static com.google.errorprone.bugpatterns.checkreturnvalue.ResultUseRule.RuleScope.GLOBAL;
import static com.google.errorprone.bugpatterns.checkreturnvalue.ResultUseRule.RuleScope.METHOD;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;

/**
 * A rule for determining {@link ResultUsePolicy} for methods and/or constructors.
 *
 * @param <C> the type of the context object used during evaluation
 * @param <S> the type of symbols
 */
public abstract class ResultUseRule<C, S> {

  // TODO(cgdecker): Switching to a model where scopes can only either be in a "marked" or
  //  "unmarked" state and only methods can have a specific policy will simplify all of this a lot.

  /** An ID for uniquely identifying this rule. */
  public abstract String id();

  /** The scopes this rule applies to. */
  public abstract ImmutableSet<RuleScope> scopes();

  // TODO(dpb): Reorder parameters for the following methods so they have the same initial
  // parameters.

  /** Evaluates the given {@code symbol} and optionally returns a {@link ResultUsePolicy} for it. */
  public abstract Optional<ResultUsePolicy> evaluate(S symbol, C context);

  /** Evaluates the given symbol and optionally returns an {@link Evaluation} of it. */
  public final Optional<Evaluation<S>> evaluate(RuleScope scope, S symbol, C context) {
    return evaluate(symbol, context).map(policy -> Evaluation.create(this, scope, symbol, policy));
  }

  @Override
  public final String toString() {
    return id();
  }

  /**
   * A rule that evaluates methods and constructors to determine a {@link ResultUsePolicy} for them.
   *
   * @param <C> the type of the context object used during evaluation
   * @param <S> the type of symbols
   * @param <M> the type of method symbols
   */
  public abstract static class MethodRule<C, S, M extends S> extends ResultUseRule<C, S> {
    private static final ImmutableSet<RuleScope> SCOPES = ImmutableSet.of(METHOD);
    private final Class<M> methodSymbolClass;

    protected MethodRule(Class<M> methodSymbolClass) {
      this.methodSymbolClass = methodSymbolClass;
    }

    @Override
    public final ImmutableSet<RuleScope> scopes() {
      return SCOPES;
    }

    /**
     * Evaluates the given {@code method} and optionally returns a {@link ResultUsePolicy} for it.
     */
    public abstract Optional<ResultUsePolicy> evaluateMethod(M method, C context);

    @Override
    public final Optional<ResultUsePolicy> evaluate(S symbol, C context) {
      return methodSymbolClass.isInstance(symbol)
          ? evaluateMethod(methodSymbolClass.cast(symbol), context)
          : Optional.empty();
    }
  }

  /**
   * A rule that evaluates symbols of any kind to determine a {@link ResultUsePolicy} to associate
   * with them.
   *
   * @param <C> the type of the context object used during evaluation
   * @param <S> the type of symbols
   */
  public abstract static class SymbolRule<C, S> extends ResultUseRule<C, S> {
    private static final ImmutableSet<RuleScope> SCOPES =
        ImmutableSet.of(METHOD, ENCLOSING_ELEMENTS);

    @Override
    public final ImmutableSet<RuleScope> scopes() {
      return SCOPES;
    }
  }

  /**
   * A global rule that is evaluated when none of the more specific rules determine a {@link
   * ResultUsePolicy} for a method.
   *
   * @param <C> the type of the context object used during evaluation
   * @param <S> the type of symbols
   */
  public abstract static class GlobalRule<C, S> extends ResultUseRule<C, S> {
    private static final ImmutableSet<RuleScope> SCOPES = ImmutableSet.of(GLOBAL);

    @Override
    public final ImmutableSet<RuleScope> scopes() {
      return SCOPES;
    }
  }

  /** Scope to which a rule may apply. */
  public enum RuleScope {
    /** The specific method or constructor for which a {@link ResultUsePolicy} is being chosen. */
    METHOD,

    /**
     * Classes and package that enclose a <i>method</i> for which a {@link ResultUsePolicy} is being
     * chosen.
     */
    ENCLOSING_ELEMENTS,

    /** The global scope. */
    GLOBAL,
  }

  /**
   * An evaluation that a rule makes.
   *
   * @param <S> the type of symbols
   */
  @AutoValue
  public abstract static class Evaluation<S> {
    /** Creates a new {@link Evaluation}. */
    public static <S> Evaluation<S> create(
        ResultUseRule<?, S> rule, RuleScope scope, S element, ResultUsePolicy policy) {
      return new AutoValue_ResultUseRule_Evaluation<>(rule, scope, element, policy);
    }

    /** The rule that made this evaluation. */
    public abstract ResultUseRule<?, S> rule();

    /** The scope at which the evaluation was made. */
    public abstract RuleScope scope();

    /** The specific element in the scope for which the evaluation was made. */
    public abstract S element();

    /** The policy the rule selected. */
    public abstract ResultUsePolicy policy();
  }
}
