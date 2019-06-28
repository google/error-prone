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
import com.google.common.collect.Iterables;
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
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.util.Name;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiPredicate;
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

  private final BaseMethodMatcher baseMatcher;

  private final ImmutableList<BiPredicate<MatchState, VisitorState>> matchers;

  public MethodMatcherImpl(BaseMethodMatcher baseMatcher) {
    this(baseMatcher, ImmutableList.of());
  }

  public MethodMatcherImpl(
      BaseMethodMatcher baseMatcher, BiPredicate<MatchState, VisitorState> predicate) {
    this(baseMatcher, ImmutableList.of(predicate));
  }

  private MethodMatcherImpl(
      BaseMethodMatcher baseMatcher,
      ImmutableList<BiPredicate<MatchState, VisitorState>> matchers) {
    this.baseMatcher = baseMatcher;
    this.matchers = matchers;
  }

  private MethodMatcherImpl append(BiPredicate<MatchState, VisitorState> matcher) {
    return new MethodMatcherImpl(
        baseMatcher,
        ImmutableList.<BiPredicate<MatchState, VisitorState>>builder()
            .addAll(this.matchers)
            .add(matcher)
            .build());
  }

  @Override
  public boolean matches(ExpressionTree tree, VisitorState state) {
    MatchState method = baseMatcher.match(tree, state);
    if (method == null) {
      return false;
    }
    for (BiPredicate<MatchState, VisitorState> matcher : matchers) {
      if (!matcher.test(method, state)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public MethodClassMatcher onClass(TypePredicate predicate) {
    return append((method, state) -> predicate.apply(method.ownerType(), state));
  }

  @Override
  public MethodClassMatcher onClass(String className) {
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
  public MethodClassMatcher onExactClass(String className) {
    return onClass(TypePredicates.isExactType(className));
  }

  @Override
  public MethodClassMatcher onExactClass(Supplier<Type> classType) {
    return onClass(TypePredicates.isExactType(classType));
  }

  @Override
  public MethodClassMatcher onDescendantOf(String className) {
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
    return append((method, state) -> method.sym().getSimpleName().contentEquals(name));
  }

  @Override
  public MethodNameMatcher namedAnyOf(String... names) {
    return namedAnyOf(ImmutableList.copyOf(names));
  }

  @Override
  public MethodNameMatcher namedAnyOf(Iterable<String> names) {
    return append(
        (method, state) -> {
          Name methodName = method.sym().getSimpleName();
          for (String name : names) {
            if (methodName.contentEquals(name)) {
              return true;
            }
          }
          return false;
        });
  }

  @Override
  public MethodNameMatcher withAnyName() {
    return this;
  }

  @Override
  public MethodNameMatcher withNameMatching(Pattern pattern) {
    return append(
        (method, state) -> pattern.matcher(method.sym().getSimpleName().toString()).matches());
  }

  @Override
  public MethodSignatureMatcher withSignature(String signature) {
    // TODO(cushon): build a way to match signatures (including varargs ones!) that doesn't
    // rely on MethodSymbol#toString().
    return append(
        (method, state) ->
            method.sym().getSimpleName().contentEquals(signature)
                || method.sym().toString().equals(signature));
  }

  @Override
  public ParameterMatcher withParameters(String... parameters) {
    return withParameters(ImmutableList.copyOf(parameters));
  }

  @Override
  public ParameterMatcher withParameters(Iterable<String> expected) {
    return withParametersOfType(Suppliers.fromStrings(expected));
  }

  @Override
  public ParameterMatcher withParametersOfType(Iterable<Supplier<Type>> expected) {
    return append(
        (method, state) -> {
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
        });
  }

  @Override
  public ConstructorClassMatcher forClass(TypePredicate predicate) {
    return append((method, state) -> predicate.apply(method.ownerType(), state));
  }

  @Override
  public ConstructorClassMatcher forClass(String className) {
    return append(
        (method, state) ->
            method.ownerType().asElement().getQualifiedName().contentEquals(className));
  }

  @Override
  public ConstructorClassMatcher forClass(Supplier<Type> classType) {
    return forClass(TypePredicates.isExactType(classType));
  }
}
