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
import static com.google.common.collect.ImmutableSet.toImmutableSet;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.errorprone.ClassName;
import com.google.errorprone.VisitorState;
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
import com.sun.source.tree.NewClassTree;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.util.Name;
import java.util.List;
import java.util.function.Predicate;
import org.jspecify.annotations.Nullable;

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

  /** The kind of method or constructor being matched. */
  private enum Kind {
    ANY_METHOD,
    INSTANCE_METHOD,
    STATIC_METHOD,
    CONSTRUCTOR
  }

  private interface ParameterConstraint {
    boolean matches(MethodSymbol sym, VisitorState state);
  }

  static final AnyMethodMatcher ANY_METHOD = new MethodMatcherImpl(Kind.ANY_METHOD);
  static final ConstructorMatcher CONSTRUCTOR = new MethodMatcherImpl(Kind.CONSTRUCTOR);
  static final StaticMethodMatcher STATIC_METHOD = new MethodMatcherImpl(Kind.STATIC_METHOD);
  static final InstanceMethodMatcher INSTANCE_METHOD = new MethodMatcherImpl(Kind.INSTANCE_METHOD);

  // The fluent API only allows setting each of the following constraints once. They are evaluated
  // in order of increasing cost in matches(), regardless of the order they were specified in.
  private final Kind kind;
  private final @Nullable Supplier<ImmutableSet<Name>> names;
  private final int arity;
  private final @Nullable Predicate<String> namePredicate;
  private final @Nullable TypePredicate classConstraint;
  private final @Nullable ParameterConstraint parameterConstraint;

  // All constructors private: only static final instances are legal starting points for chains.
  private MethodMatcherImpl(Kind kind) {
    this(
        kind,
        /* names= */ null,
        /* arity= */ -1,
        /* namePredicate= */ null,
        /* classConstraint= */ null,
        /* parameterConstraint= */ null);
  }

  private MethodMatcherImpl(
      Kind kind,
      @Nullable Supplier<ImmutableSet<Name>> names,
      int arity,
      @Nullable Predicate<String> namePredicate,
      @Nullable TypePredicate classConstraint,
      @Nullable ParameterConstraint parameterConstraint) {
    this.kind = kind;
    this.names = names;
    this.arity = arity;
    this.namePredicate = namePredicate;
    this.classConstraint = classConstraint;
    this.parameterConstraint = parameterConstraint;
  }

  private MethodMatcherImpl withNames(Supplier<ImmutableSet<Name>> names) {
    return new MethodMatcherImpl(
        kind, names, arity, namePredicate, classConstraint, parameterConstraint);
  }

  private MethodMatcherImpl withNamePredicate(Predicate<String> namePredicate) {
    return new MethodMatcherImpl(
        kind, names, arity, namePredicate, classConstraint, parameterConstraint);
  }

  private MethodMatcherImpl withClassConstraint(TypePredicate classConstraint) {
    return new MethodMatcherImpl(
        kind, names, arity, namePredicate, classConstraint, parameterConstraint);
  }

  private MethodMatcherImpl withParameterConstraint(
      int arity, @Nullable ParameterConstraint parameterConstraint) {
    return new MethodMatcherImpl(
        kind, names, arity, namePredicate, classConstraint, parameterConstraint);
  }

  @Override
  public boolean matches(ExpressionTree tree, VisitorState state) {
    if (!(ASTHelpers.getSymbol(tree) instanceof MethodSymbol sym)) {
      return false;
    }
    boolean kindMatches =
        switch (kind) {
          case ANY_METHOD -> !(tree instanceof NewClassTree);
          case INSTANCE_METHOD -> !(tree instanceof NewClassTree) && !sym.isStatic();
          case STATIC_METHOD -> !(tree instanceof NewClassTree) && sym.isStatic();
          case CONSTRUCTOR ->
              switch (tree.getKind()) {
                case NEW_CLASS, METHOD_INVOCATION, MEMBER_REFERENCE -> sym.isConstructor();
                default -> false;
              };
        };
    if (!kindMatches) {
      return false;
    }
    if (names != null && !names.get(state).contains(sym.getSimpleName())) {
      return false;
    }
    if (arity >= 0 && sym.type.getParameterTypes().size() != arity) {
      return false;
    }
    if (namePredicate != null && !namePredicate.test(sym.getSimpleName().toString())) {
      return false;
    }
    if (classConstraint != null && !classConstraint.apply(ownerType(tree, sym), state)) {
      return false;
    }
    return parameterConstraint == null || parameterConstraint.matches(sym, state);
  }

  /** The type of the class in which a member method or constructor is declared. */
  private Type ownerType(ExpressionTree tree, MethodSymbol sym) {
    if (kind == Kind.CONSTRUCTOR) {
      return sym.owner.type;
    }
    // TODO: b/130658266 - should this be the symbol's owner type, not the receiver's owner type?
    return ASTHelpers.getReceiverType(tree);
  }

  @Override
  public MethodClassMatcher onClass(TypePredicate predicate) {
    return withClassConstraint(predicate);
  }

  @Override
  public MethodClassMatcher onClass(String className) {
    return onClass(TypePredicates.isExactType(className));
  }

  @Override
  public MethodClassMatcher onClass(ClassName className) {
    return onClass(TypePredicates.isExactType(className));
  }

  @Override
  public MethodClassMatcher onClass(Supplier<Type> classType) {
    return onClass(TypePredicates.isExactType(classType));
  }

  @Override
  public MethodClassMatcher onClassAny(Iterable<String> classNames) {
    return onClass(TypePredicates.isExactTypeAny(classNames));
  }

  @Override
  public MethodClassMatcher onClassAny(String... classNames) {
    return onClassAny(ImmutableList.copyOf(classNames));
  }

  @Override
  public MethodClassMatcher onClassAny(ClassName... classNames) {
    return onClass(TypePredicates.isExactTypeAny(classNames));
  }

  @Override
  public MethodClassMatcher onExactClass(String className) {
    return onClass(className);
  }

  @Override
  public MethodClassMatcher onExactClass(ClassName className) {
    return onClass(className);
  }

  @Override
  public MethodClassMatcher onExactClass(Supplier<Type> classType) {
    return onClass(classType);
  }

  @Override
  public MethodClassMatcher onExactClassAny(Iterable<String> classTypes) {
    return onClassAny(classTypes);
  }

  @Override
  public MethodClassMatcher onExactClassAny(String... classTypes) {
    return onClassAny(classTypes);
  }

  @Override
  public MethodClassMatcher onExactClassAny(ClassName... classTypes) {
    return onClassAny(classTypes);
  }

  @Override
  public MethodClassMatcher onDescendantOf(String className) {
    return onClass(TypePredicates.isDescendantOf(className));
  }

  @Override
  public MethodClassMatcher onDescendantOf(ClassName className) {
    return onClass(TypePredicates.isDescendantOf(className));
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
  public MethodClassMatcher onDescendantOfAny(ClassName... classTypes) {
    return onClass(TypePredicates.isDescendantOfAny(classTypes));
  }

  @Override
  public MethodClassMatcher onDescendantOfAny(Iterable<String> classTypes) {
    return onClass(TypePredicates.isDescendantOfAny(classTypes));
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
    return namedAnyOf(ImmutableSet.of(name));
  }

  @Override
  public MethodNameMatcher namedAnyOf(String... names) {
    return namedAnyOf(ImmutableSet.copyOf(names));
  }

  @Override
  public MethodNameMatcher namedAnyOf(Iterable<String> names) {
    ImmutableSet<String> expected = ImmutableSet.copyOf(names);
    Supplier<ImmutableSet<Name>> expectedNames =
        VisitorState.memoize(
            state -> expected.stream().map(state::getName).collect(toImmutableSet()));
    return withNames(expectedNames);
  }

  @Override
  public MethodNameMatcher withAnyName() {
    return this;
  }

  @Override
  public MethodNameMatcher withNameMatching(org.safere.Pattern pattern) {
    return withNamePredicate(pattern.asMatchPredicate());
  }

  @Override
  public MethodNameMatcher withNameMatching(java.util.regex.Pattern pattern) {
    return withNamePredicate(pattern.asMatchPredicate());
  }

  @Override
  public ParameterMatcher withNoParameters() {
    return withParameterConstraint(/* arity= */ 0, /* parameterConstraint= */ null);
  }

  @Override
  public ParameterMatcher withParameters(String first, String... rest) {
    return withParameters(Lists.asList(first, rest));
  }

  @Override
  public ParameterMatcher withParameters(Iterable<String> expected) {
    return withParametersOfType(Suppliers.fromStrings(expected));
  }

  @Override
  public ParameterMatcher withParametersOfType(Iterable<Supplier<Type>> expected) {
    ImmutableList<Supplier<Type>> expectedTypes = ImmutableList.copyOf(expected);
    return withParameterConstraint(
        expectedTypes.size(),
        (sym, state) -> {
          List<Type> actual = sym.type.getParameterTypes();
          for (int i = 0; i < expectedTypes.size(); i++) {
            if (!ASTHelpers.isSameType(actual.get(i), expectedTypes.get(i).get(state), state)) {
              return false;
            }
          }
          return true;
        });
  }

  @Override
  public ParameterMatcher withParametersOfType(Supplier<Type> first, Supplier<Type>... rest) {
    return withParametersOfType(Lists.asList(first, rest));
  }

  @Override
  public ParameterMatcher withParametersOfType(ClassName first, ClassName... rest) {
    ImmutableList<ClassName> expected = ImmutableList.copyOf(Lists.asList(first, rest));
    return withParameterConstraint(
        expected.size(),
        (sym, state) -> {
          List<Type> actual = sym.type.getParameterTypes();
          for (int i = 0; i < expected.size(); i++) {
            if (!ASTHelpers.isSameType(actual.get(i), expected.get(i), state)) {
              return false;
            }
          }
          return true;
        });
  }

  @Override
  public ParameterMatcher withParametersMatching(
      ParameterPredicate first, ParameterPredicate... rest) {
    return withParametersMatching(Lists.asList(first, rest));
  }

  @Override
  public ParameterMatcher withParametersMatching(Iterable<ParameterPredicate> expected) {
    ImmutableList<ParameterPredicate> expectedPredicates = ImmutableList.copyOf(expected);
    return withParameterConstraint(
        expectedPredicates.size(),
        (sym, state) -> {
          List<VarSymbol> actual = sym.getParameters();
          for (int i = 0; i < expectedPredicates.size(); i++) {
            VarSymbol parameter = actual.get(i);
            if (!expectedPredicates.get(i).matches(parameter, parameter.type, state)) {
              return false;
            }
          }
          return true;
        });
  }

  @Override
  public ConstructorClassMatcher forClass(TypePredicate predicate) {
    return withClassConstraint(predicate);
  }

  @Override
  public ConstructorClassMatcher forClass(String className) {
    Supplier<Name> expectedName = VisitorState.memoize(state -> state.getName(className));
    return forClass(
        (type, state) -> type.asElement().getQualifiedName().equals(expectedName.get(state)));
  }

  @Override
  public ConstructorClassMatcher forClass(ClassName className) {
    return forClass(TypePredicates.isExactType(className));
  }

  @Override
  public ConstructorClassMatcher forClass(Supplier<Type> classType) {
    return forClass(TypePredicates.isExactType(classType));
  }
}
