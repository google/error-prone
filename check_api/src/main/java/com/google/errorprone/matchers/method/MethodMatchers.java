/*
 * Copyright 2015 The Error Prone Authors.
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

import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.method.MethodInvocationMatcher.Rule;
import com.google.errorprone.predicates.TypePredicate;
import com.google.errorprone.suppliers.Supplier;
import com.sun.source.tree.ExpressionTree;
import com.sun.tools.javac.code.Type;
import java.util.Optional;
import java.util.regex.Pattern;

public class MethodMatchers {

  /** @deprecated use {@code Matcher<ExpressionTree>} instead of referring directly to this type. */
  @Deprecated
  public interface MethodMatcher extends Matcher<ExpressionTree> {

    /**
     * A rule for expressing this matcher as a MethodInvocationMatcher, if possible. If this matcher
     * uses predicates not supported by the MethodInvocationMatcher evaluator, this method will
     * return empty().
     */
    Optional<Rule> asRule();
  }

  // Language definition for fluent method matchers.

  /** @deprecated use {@code Matcher<ExpressionTree>} instead of referring directly to this type. */
  @Deprecated
  public interface InstanceMethodMatcher extends MethodMatcher {
    /** Match on types that satisfy the given predicate. */
    MethodClassMatcher onClass(TypePredicate predicate);

    /** Match on types with the given fully-qualified name. (e.g. java.lang.String) */
    MethodClassMatcher onExactClass(String className);

    /** Match on the given type exactly. */
    MethodClassMatcher onExactClass(Supplier<Type> classType);

    /** Match on descendants of the given fully-qualified type name. */
    MethodClassMatcher onDescendantOf(String className);

    /** Match on descendants of the given type. */
    MethodClassMatcher onDescendantOf(Supplier<Type> classType);

    /** Match on types that are descendants of any of the given types. */
    MethodClassMatcher onDescendantOfAny(String... classTypes);

    /** Match on types that are descendants of any of the given types. */
    MethodClassMatcher onDescendantOfAny(Iterable<String> classTypes);

    /** Match on any class. */
    MethodClassMatcher anyClass();
  }

  /** @deprecated use {@code Matcher<ExpressionTree>} instead of referring directly to this type. */
  @Deprecated
  public interface StaticMethodMatcher extends MethodMatcher {
    /** Match on types that satisfy the given predicate. */
    MethodClassMatcher onClass(TypePredicate predicate);

    /** Match on types with the given fully-qualified name. (e.g. {@code java.lang.String} */
    MethodClassMatcher onClass(String className);

    /** Match on the given type exactly. */
    MethodClassMatcher onClass(Supplier<Type> classType);

    /** Match on types that are equal to any of the given types. */
    MethodClassMatcher onClassAny(Iterable<String> classNames);

    /** Match on types that are equal to any of the given types. */
    MethodClassMatcher onClassAny(String... classNames);

    /** Match on any class. */
    MethodClassMatcher anyClass();
  }

  /** @deprecated use {@code Matcher<ExpressionTree>} instead of referring directly to this type. */
  @Deprecated
  public interface AnyMethodMatcher extends MethodMatcher {
    /** Match the given type exactly. */
    MethodClassMatcher onClass(TypePredicate predicate);

    /** Match on types with the given fully-qualified name. (e.g. {@code java.lang.String} */
    MethodClassMatcher onClass(String className);

    /** Match on any class. */
    MethodClassMatcher anyClass();
  }

  /** @deprecated use {@code Matcher<ExpressionTree>} instead of referring directly to this type. */
  @Deprecated
  public interface MethodClassMatcher extends MethodMatcher {
    /** Match methods with the given name. (e.g. {@code toString}) */
    MethodNameMatcher named(String name);

    /** Match methods with any of the given names. */
    MethodNameMatcher namedAnyOf(String... names);

    /** Match methods with any of the given names. */
    MethodNameMatcher namedAnyOf(Iterable<String> names);

    /** Match methods with any name. */
    MethodNameMatcher withAnyName();

    /** Match methods with a name that matches the given regular expression. */
    MethodNameMatcher withNameMatching(Pattern pattern);

    /**
     * Match methods with the given signature. The implementation uses javac internals to
     * pretty-print the signatures, and the signature format is not well-specified. This matcher
     * should be used with caution.
     *
     * <p>Example: {@code format(java.lang.String,java.lang.Object...)}
     */
    MethodSignatureMatcher withSignature(String signature);
  }

  /** @deprecated use {@code Matcher<ExpressionTree>} instead of referring directly to this type. */
  @Deprecated
  public interface MethodSignatureMatcher extends MethodMatcher {}

  /** @deprecated use {@code Matcher<ExpressionTree>} instead of referring directly to this type. */
  @Deprecated
  public interface MethodNameMatcher extends MethodMatcher {
    /** Match methods whose formal parameters have the given types. */
    ParameterMatcher withParameters(String... parameters);

    /** Match methods whose formal parameters have the given types. */
    ParameterMatcher withParameters(Iterable<String> parameters);

    /** Match constructors whose formal parameters have the given types. */
    ParameterMatcher withParametersOfType(Iterable<Supplier<Type>> parameters);
  }

  /** @deprecated use {@code Matcher<ExpressionTree>} instead of referring directly to this type. */
  @Deprecated
  public interface ConstructorMatcher extends MethodMatcher {
    /** Match on types that satisfy the given predicate. */
    ConstructorClassMatcher forClass(TypePredicate predicate);

    /** Match on the given type exactly. */
    ConstructorClassMatcher forClass(String className);

    /** Match on the given type exactly. */
    ConstructorClassMatcher forClass(Supplier<Type> classType);
  }

  /** @deprecated use {@code Matcher<ExpressionTree>} instead of referring directly to this type. */
  @Deprecated
  public interface ConstructorClassMatcher extends MethodMatcher {
    /** Match constructors whose formal parameters have the given types. */
    ParameterMatcher withParameters(String... parameters);

    /** Match constructors whose formal parameters have the given types. */
    ParameterMatcher withParameters(Iterable<String> parameters);

    /** Match constructors whose formal parameters have the given types. */
    ParameterMatcher withParametersOfType(Iterable<Supplier<Type>> parameters);
  }

  /** @deprecated use {@code Matcher<ExpressionTree>} instead of referring directly to this type. */
  @Deprecated
  public interface ParameterMatcher extends MethodMatcher {}

  // Method matcher factories

  public static StaticMethodMatcher staticMethod() {
    return MethodMatcherImpl.STATIC_METHOD;
  }

  public static InstanceMethodMatcher instanceMethod() {
    return MethodMatcherImpl.INSTANCE_METHOD;
  }

  public static AnyMethodMatcher anyMethod() {
    return MethodMatcherImpl.ANY_METHOD;
  }

  public static ConstructorMatcher constructor() {
    return MethodMatcherImpl.CONSTRUCTOR;
  }
}
