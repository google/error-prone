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
import static com.google.errorprone.util.ASTHelpers.enclosingElements;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import com.google.errorprone.VisitorState;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.PackageSymbol;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/** A rule for determining {@link ResultUsePolicy} for methods and/or constructors. */
public abstract class ResultUseRule {

  // TODO(cgdecker): Switching to a model where scopes can only either be in a "marked" or
  //  "unmarked" state and only methods can have a specific policy will simplify all of this a lot.

  private ResultUseRule() {} // only allow the subclasses below

  /** An ID for uniquely identifying this rule. */
  public abstract String id();

  /** The scopes this rule applies to. */
  public abstract ImmutableSet<RuleScope> scopes();

  /** Evaluates the given {@code symbol} and optionally returns a {@link ResultUsePolicy} for it. */
  public abstract Optional<ResultUsePolicy> evaluate(Symbol symbol, VisitorState state);

  /** Evaluates the given symbol and optionally returns an {@link Evaluation} of it. */
  public final Optional<Evaluation> evaluate(RuleScope scope, Symbol symbol, VisitorState state) {
    return evaluate(symbol, state).map(policy -> Evaluation.create(this, scope, symbol, policy));
  }

  @Override
  public final String toString() {
    return id();
  }

  /**
   * A rule that evaluates methods and constructors to determine a {@link ResultUsePolicy} for them.
   */
  public abstract static class MethodRule extends ResultUseRule {
    private static final ImmutableSet<RuleScope> SCOPES = ImmutableSet.of(METHOD);

    @Override
    public final ImmutableSet<RuleScope> scopes() {
      return SCOPES;
    }

    /**
     * Evaluates the given {@code method} and optionally returns a {@link ResultUsePolicy} for it.
     */
    public abstract Optional<ResultUsePolicy> evaluateMethod(
        MethodSymbol method, VisitorState state);

    @Override
    public final Optional<ResultUsePolicy> evaluate(Symbol symbol, VisitorState state) {
      return symbol instanceof MethodSymbol
          ? evaluateMethod((MethodSymbol) symbol, state)
          : Optional.empty();
    }
  }

  /**
   * A rule that evaluates symbols of any kind to determine a {@link ResultUsePolicy} to associate
   * with them.
   */
  public abstract static class SymbolRule extends ResultUseRule {
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
   */
  public abstract static class GlobalRule extends ResultUseRule {
    private static final ImmutableSet<RuleScope> SCOPES = ImmutableSet.of(GLOBAL);

    @Override
    public final ImmutableSet<RuleScope> scopes() {
      return SCOPES;
    }

    /** Optionally returns a global policy for methods or constructors. */
    public abstract Optional<ResultUsePolicy> evaluate(boolean constructor, VisitorState state);

    @Override
    public final Optional<ResultUsePolicy> evaluate(Symbol symbol, VisitorState state) {
      return evaluate(symbol.isConstructor(), state);
    }
  }

  /** Scope to which a rule may apply. */
  public enum RuleScope {
    /** The specific method or constructor for which a {@link ResultUsePolicy} is being chosen. */
    METHOD {
      @Override
      Stream<Symbol> members(MethodSymbol method) {
        return Stream.of(method);
      }
    },
    /**
     * Classes and package that enclose a <i>method</i> for which a {@link ResultUsePolicy} is being
     * chosen.
     */
    ENCLOSING_ELEMENTS {
      @Override
      Stream<Symbol> members(MethodSymbol method) {
        return enclosingElements(method)
            .filter(s -> s instanceof ClassSymbol || s instanceof PackageSymbol);
      }
    },
    /** The global scope. */
    GLOBAL {
      @Override
      Stream<Symbol> members(MethodSymbol method) {
        return Stream.of(method);
      }
    };

    /** Returns an ordered stream of elements in this scope relative to the given {@code method}. */
    abstract Stream<Symbol> members(MethodSymbol method);

    /** Returns an ordered stream of policies from rules in this scope. */
    final Stream<ResultUsePolicy> policies(
        MethodSymbol method, VisitorState state, ListMultimap<RuleScope, ResultUseRule> rules) {
      List<ResultUseRule> scopeRules = rules.get(this);
      return members(method)
          .flatMap(symbol -> scopeRules.stream().map(rule -> rule.evaluate(symbol, state)))
          .flatMap(Optional::stream);
    }

    /** Returns an ordered stream of evaluations in this scope. */
    final Stream<Evaluation> evaluations(
        MethodSymbol method, VisitorState state, ListMultimap<RuleScope, ResultUseRule> rules) {
      List<ResultUseRule> scopeRules = rules.get(this);
      return members(method)
          .flatMap(symbol -> scopeRules.stream().map(rule -> rule.evaluate(this, symbol, state)))
          .flatMap(Optional::stream);
    }
  }

  /** An evaluation that a rule makes. */
  @AutoValue
  public abstract static class Evaluation {
    /** Creates a new {@link Evaluation}. */
    public static Evaluation create(
        ResultUseRule rule, RuleScope scope, Symbol element, ResultUsePolicy policy) {
      return new AutoValue_ResultUseRule_Evaluation(rule, scope, element, policy);
    }

    /** The rule that made this evaluation. */
    public abstract ResultUseRule rule();
    /** The scope at which the evaluation was made. */
    public abstract RuleScope scope();
    /** The specific element in the scope for which the evaluation was made. */
    public abstract Symbol element();
    /** The policy the rule selected. */
    public abstract ResultUsePolicy policy();
  }
}
