/*
 * Copyright 2019 The Error Prone Authors.
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

package com.google.errorprone.matchers.method;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Streams;
import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.method.MethodInvocationMatcher.MethodKind;
import com.google.errorprone.matchers.method.MethodInvocationMatcher.Rule;
import com.google.errorprone.matchers.method.MethodInvocationMatcher.Token;
import com.google.errorprone.matchers.method.MethodInvocationMatcher.Token.DefinedIn;
import com.google.errorprone.matchers.method.MethodInvocationMatcher.Token.Kind;
import com.google.errorprone.matchers.method.MethodInvocationMatcher.Token.MethodName;
import com.google.errorprone.matchers.method.MethodInvocationMatcher.Token.ReceiverSupertype;
import com.google.errorprone.matchers.method.MethodInvocationMatcher.Token.ReceiverType;
import com.google.errorprone.matchers.method.MethodInvocationMatcher.TokenType;
import com.google.errorprone.matchers.method.MethodMatchers.AnyMethodMatcher;
import com.google.errorprone.matchers.method.MethodMatchers.ConstructorClassMatcher;
import com.google.errorprone.matchers.method.MethodMatchers.ConstructorMatcher;
import com.google.errorprone.matchers.method.MethodMatchers.InstanceMethodMatcher;
import com.google.errorprone.matchers.method.MethodMatchers.MethodClassMatcher;
import com.google.errorprone.matchers.method.MethodMatchers.MethodNameMatcher;
import com.google.errorprone.matchers.method.MethodMatchers.MethodSignatureMatcher;
import com.google.errorprone.matchers.method.MethodMatchers.ParameterMatcher;
import com.google.errorprone.matchers.method.MethodMatchers.StaticMethodMatcher;
import com.google.errorprone.predicates.TypePredicate;
import com.google.errorprone.predicates.TypePredicates;
import com.google.errorprone.suppliers.Supplier;
import com.google.errorprone.suppliers.Suppliers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.tools.javac.code.Type;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

final class MethodMatcherImpl
    implements InstanceMethodMatcher,
        StaticMethodMatcher,
        AnyMethodMatcher,
        MethodClassMatcher,
        MethodSignatureMatcher,
        MethodNameMatcher,
        ConstructorMatcher,
        ConstructorClassMatcher,
        ParameterMatcher {

  private static final class RulePart {
    public final TokenType type;
    public final ImmutableSet<Token> tokensAllowed;

    static Optional<RulePart> of(TokenType type, ImmutableSet<Token> tokensAllowed) {
      return Optional.of(new RulePart(type, tokensAllowed));
    }

    RulePart(TokenType type, ImmutableSet<Token> tokensAllowed) {
      this.type = type;
      this.tokensAllowed = tokensAllowed;
    }
  }

  /**
   * The fluent API methods in this class build up a list of constraints, which can either be used
   * as a predicate (by calling {@link #matches(MatchState, VisitorState)} on each Constraint in the
   * list), or exported as a rule set for {@link MethodInvocationMatcher#compile(Iterable)} (by
   * combining the results of calling {@link #asRulePart()} on each Constraint).
   */
  private interface Constraint {
    /** Tests whether this Constraint is satisfied with the method invocation we're checking. */
    boolean matches(MatchState m, VisitorState s);

    /**
     * Exports this Constraint as part of a ruleset for MethodInvocationMatcher.
     *
     * @return {@code Optional.empty()} if the constraint cannot be expressed as part of a Rule, or
     *     else a map entry to use as part of a Rule.
     */
    Optional<RulePart> asRulePart();
  }

  private abstract static class OpaqueConstraint implements Constraint {
    @Override
    public Optional<RulePart> asRulePart() {
      return Optional.empty();
    }
  }

  static final AnyMethodMatcher ANY_METHOD =
      new MethodMatcherImpl(
          BaseMethodMatcher.METHOD,
          ImmutableList.of(
              new Constraint() {
                @Override
                public boolean matches(MatchState m, VisitorState s) {
                  // Handled by base matcher.
                  return true;
                }

                @Override
                public Optional<RulePart> asRulePart() {
                  return RulePart.of(
                      TokenType.KIND,
                      ImmutableSet.of(
                          Kind.create(MethodKind.INSTANCE), Kind.create(MethodKind.STATIC)));
                }
              }));
  static final ConstructorMatcher CONSTRUCTOR =
      new MethodMatcherImpl(
          BaseMethodMatcher.CONSTRUCTOR,
          ImmutableList.of(
              new Constraint() {
                @Override
                public boolean matches(MatchState m, VisitorState s) {
                  return true;
                }

                @Override
                public Optional<RulePart> asRulePart() {
                  return RulePart.of(
                      TokenType.KIND, ImmutableSet.of(Kind.create(MethodKind.CONSTRUCTOR)));
                }
              }));
  static final StaticMethodMatcher STATIC_METHOD =
      new MethodMatcherImpl(
          BaseMethodMatcher.METHOD,
          ImmutableList.of(
              new Constraint() {
                @Override
                public boolean matches(MatchState m, VisitorState s) {
                  return m.sym().isStatic();
                }

                @Override
                public Optional<RulePart> asRulePart() {
                  return RulePart.of(
                      TokenType.KIND, ImmutableSet.of(Kind.create(MethodKind.STATIC)));
                }
              }));
  static final InstanceMethodMatcher INSTANCE_METHOD =
      new MethodMatcherImpl(
          BaseMethodMatcher.METHOD,
          ImmutableList.of(
              new Constraint() {
                @Override
                public boolean matches(MatchState m, VisitorState s) {
                  return !m.sym().isStatic();
                }

                @Override
                public Optional<RulePart> asRulePart() {
                  return RulePart.of(
                      TokenType.KIND, ImmutableSet.of(Kind.create(MethodKind.INSTANCE)));
                }
              }));

  private final BaseMethodMatcher baseMatcher;

  private final ImmutableList<Constraint> constraints;

  // All constructors private: only static final instances are legal starting points for chains.
  private MethodMatcherImpl(BaseMethodMatcher baseMatcher, ImmutableList<Constraint> matchers) {
    this.baseMatcher = baseMatcher;
    this.constraints = matchers;
  }

  private MethodMatcherImpl append(Constraint c) {
    return new MethodMatcherImpl(
        baseMatcher, ImmutableList.<Constraint>builder().addAll(this.constraints).add(c).build());
  }

  @Override
  public boolean matches(ExpressionTree tree, VisitorState state) {
    MatchState method = baseMatcher.match(tree);
    if (method == null) {
      return false;
    }
    for (Constraint constraint : constraints) {
      if (!constraint.matches(method, state)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public MethodClassMatcher onClass(TypePredicate predicate) {
    return append(
        new OpaqueConstraint() {
          @Override
          public boolean matches(MatchState m, VisitorState s) {
            return predicate.apply(m.ownerType(), s);
          }
        });
  }

  @Override
  public MethodClassMatcher onClass(String className) {
    TypePredicate pred = TypePredicates.isExactType(className);
    return append(
        new Constraint() {
          @Override
          public boolean matches(MatchState m, VisitorState s) {
            return pred.apply(m.ownerType(), s);
          }

          @Override
          public Optional<RulePart> asRulePart() {
            return RulePart.of(
                TokenType.RECEIVER_TYPE, ImmutableSet.of(ReceiverType.create(className)));
          }
        });
  }

  @Override
  public MethodClassMatcher onClass(Supplier<Type> classType) {
    return onClass(TypePredicates.isExactType(classType));
  }

  @Override
  public MethodClassMatcher onClassAny(Iterable<String> classNames) {
    TypePredicate pred = TypePredicates.isExactTypeAny(classNames);
    return append(
        new Constraint() {
          @Override
          public boolean matches(MatchState m, VisitorState s) {
            return pred.apply(m.ownerType(), s);
          }

          @Override
          public Optional<RulePart> asRulePart() {
            return RulePart.of(
                TokenType.RECEIVER_TYPE,
                Streams.stream(classNames)
                    .map(ReceiverType::create)
                    .collect(ImmutableSet.toImmutableSet()));
          }
        });
  }

  @Override
  public MethodClassMatcher onClassAny(String... classNames) {
    return onClassAny(ImmutableList.copyOf(classNames));
  }

  @Override
  public MethodClassMatcher onExactClass(String className) {
    return onClass(className);
  }

  @Override
  public MethodClassMatcher onExactClass(Supplier<Type> classType) {
    return onClass(classType);
  }

  @Override
  public MethodClassMatcher onDescendantOf(String className) {
    TypePredicate pred = TypePredicates.isDescendantOf(className);
    return append(
        new Constraint() {
          @Override
          public boolean matches(MatchState m, VisitorState s) {
            return pred.apply(m.ownerType(), s);
          }

          @Override
          public Optional<RulePart> asRulePart() {
            return RulePart.of(
                TokenType.RECEIVER_SUPERTYPE, ImmutableSet.of(ReceiverSupertype.create(className)));
          }
        });
  }

  @Override
  public MethodClassMatcher onDescendantOf(Supplier<Type> classType) {
    return onClass(TypePredicates.isDescendantOf(classType));
  }

  @Override
  public MethodClassMatcher onDescendantOfAny(String... classTypes) {
    return onDescendantOfAny(ImmutableList.copyOf(classTypes));
  }

  @Override
  public MethodClassMatcher onDescendantOfAny(Iterable<String> classTypes) {
    TypePredicate pred = TypePredicates.isDescendantOfAny(classTypes);
    return append(
        new Constraint() {
          @Override
          public boolean matches(MatchState m, VisitorState s) {
            return pred.apply(m.ownerType(), s);
          }

          @Override
          public Optional<RulePart> asRulePart() {
            return RulePart.of(
                TokenType.RECEIVER_SUPERTYPE,
                Streams.stream(classTypes)
                    .map(ReceiverSupertype::create)
                    .collect(ImmutableSet.toImmutableSet()));
          }
        });
  }

  @Override
  public MethodClassMatcher anyClass() {
    return this;
  }

  @Override
  public MethodNameMatcher named(String name) {
    checkArgument(
        !name.contains("(") && !name.contains(")"),
        "method name (%s) cannot contain parentheses; use \"foo\" instead of \"foo()\"",
        name);
    return append(
        new Constraint() {
          @Override
          public boolean matches(MatchState m, VisitorState s) {
            return m.sym().getSimpleName().contentEquals(name);
          }

          @Override
          public Optional<RulePart> asRulePart() {
            return RulePart.of(TokenType.METHOD_NAME, ImmutableSet.of(MethodName.create(name)));
          }
        });
  }

  @Override
  public MethodNameMatcher namedAnyOf(String... names) {
    return namedAnyOf(ImmutableSet.copyOf(names));
  }

  @Override
  public MethodNameMatcher namedAnyOf(Iterable<String> names) {
    Set<String> expected = ImmutableSet.copyOf(names);
    return append(
        new Constraint() {
          @Override
          public boolean matches(MatchState m, VisitorState s) {
            return expected.contains(m.sym().getSimpleName().toString());
          }

          @Override
          public Optional<RulePart> asRulePart() {
            return RulePart.of(
                TokenType.METHOD_NAME,
                Streams.stream(names)
                    .map(MethodName::create)
                    .collect(ImmutableSet.toImmutableSet()));
          }
        });
  }

  @Override
  public MethodNameMatcher withAnyName() {
    return this;
  }

  @Override
  public MethodNameMatcher withNameMatching(Pattern pattern) {
    return append(
        new OpaqueConstraint() {
          @Override
          public boolean matches(MatchState m, VisitorState s) {
            return pattern.matcher(m.sym().getSimpleName().toString()).matches();
          }
        });
  }

  @Override
  public MethodSignatureMatcher withSignature(String signature) {
    // TODO(cushon): build a way to match signatures (including varargs ones!) that doesn't
    // rely on MethodSymbol#toString().
    return append(
        new OpaqueConstraint() {
          @Override
          public boolean matches(MatchState m, VisitorState s) {
            return m.sym().getSimpleName().contentEquals(signature)
                || m.sym().toString().equals(signature);
          }
        });
  }

  @Override
  public ParameterMatcher withParameters(String... parameters) {
    return withParameters(ImmutableList.copyOf(parameters));
  }

  private ParameterMatcher withParameters(
      Iterable<Supplier<Type>> expected, Optional<Token> newConstraint) {
    return append(
        new Constraint() {
          @Override
          public boolean matches(MatchState method, VisitorState state) {
            List<Type> actual = method.paramTypes();
            if (actual.size() != Iterables.size(expected)) {
              return false;
            }
            Iterator<Type> ax = actual.iterator();
            Iterator<Supplier<Type>> bx = expected.iterator();
            while (ax.hasNext()) {
              if (!ASTHelpers.isSameType(ax.next(), bx.next().get(state), state)) {
                return false;
              }
            }
            return true;
          }

          @Override
          public Optional<RulePart> asRulePart() {
            return newConstraint.map(
                types -> new RulePart(TokenType.PARAMETER_TYPES, ImmutableSet.of(types)));
          }
        });
  }

  @Override
  public ParameterMatcher withParameters(Iterable<String> expected) {
    return withParameters(
        Suppliers.fromStrings(expected),
        Optional.of(Token.ParameterTypes.create(ImmutableList.copyOf(expected))));
  }

  @Override
  public ParameterMatcher withParametersOfType(Iterable<Supplier<Type>> expected) {
    return withParameters(expected, Optional.empty());
  }

  @Override
  public ConstructorClassMatcher forClass(TypePredicate predicate) {
    return append(
        new OpaqueConstraint() {
          @Override
          public boolean matches(MatchState m, VisitorState s) {
            return predicate.apply(m.ownerType(), s);
          }
        });
  }

  @Override
  public ConstructorClassMatcher forClass(String className) {
    return append(
        new Constraint() {
          @Override
          public boolean matches(MatchState m, VisitorState s) {
            return m.ownerType().asElement().getQualifiedName().contentEquals(className);
          }

          @Override
          public Optional<RulePart> asRulePart() {
            return RulePart.of(TokenType.DEFINED_IN, ImmutableSet.of(DefinedIn.create(className)));
          }
        });
  }

  @Override
  public ConstructorClassMatcher forClass(Supplier<Type> classType) {
    return forClass(TypePredicates.isExactType(classType));
  }

  @Override
  public Optional<Rule> asRule() {
    ImmutableMap.Builder<TokenType, Set<Token>> builder =
        ImmutableMap.builderWithExpectedSize(constraints.size());
    for (Constraint constraint : constraints) {
      Optional<RulePart> optionalPart = constraint.asRulePart();
      if (!optionalPart.isPresent()) {
        return Optional.empty();
      }
      RulePart rulePart = optionalPart.get();
      builder.put(rulePart.type, rulePart.tokensAllowed);
    }

    return Optional.of(Rule.create(builder.build()));
  }
}
