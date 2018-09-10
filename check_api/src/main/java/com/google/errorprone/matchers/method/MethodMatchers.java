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
import com.google.errorprone.predicates.TypePredicate;
import com.google.errorprone.suppliers.Supplier;
import com.sun.source.tree.ExpressionTree;
import com.sun.tools.javac.code.Type;
import java.util.regex.Pattern;

public class MethodMatchers {

  // Language definition for fluent method matchers.

  public interface InstanceMethodMatcher extends Matcher<ExpressionTree> {
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

  public interface StaticMethodMatcher extends Matcher<ExpressionTree> {
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

  public interface AnyMethodMatcher extends Matcher<ExpressionTree> {
    /** Match the given type exactly. */
    MethodClassMatcher onClass(TypePredicate predicate);

    /** Match on any class. */
    MethodClassMatcher anyClass();
  }

  public interface MethodClassMatcher extends Matcher<ExpressionTree> {
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

  public interface MethodSignatureMatcher extends Matcher<ExpressionTree> {}

  public interface MethodNameMatcher extends Matcher<ExpressionTree> {
    /** Match methods whose formal parameters have the given types. */
    ParameterMatcher withParameters(String... parameters);

    /** Match methods whose formal parameters have the given types. */
    ParameterMatcher withParameters(Iterable<String> parameters);
  }

  public interface ConstructorMatcher extends Matcher<ExpressionTree> {
    /** Match on types that satisfy the given predicate. */
    ConstructorClassMatcher forClass(TypePredicate predicate);

    /** Match on the given type exactly. */
    ConstructorClassMatcher forClass(String className);

    /** Match on the given type exactly. */
    ConstructorClassMatcher forClass(Supplier<Type> classType);
  }

  public interface ConstructorClassMatcher extends Matcher<ExpressionTree> {
    /** Match constructors whose formal parameters have the given types. */
    ParameterMatcher withParameters(String... parameters);

    /** Match constructors whose formal parameters have the given types. */
    ParameterMatcher withParameters(Iterable<String> parameters);

    /** Match constructors whose formal parameters have the given types. */
    ParameterMatcher withParametersOfType(Iterable<Supplier<Type>> parameters);
  }

  public interface ParameterMatcher extends Matcher<ExpressionTree> {}

  // Method matcher factories

  public static StaticMethodMatcher staticMethod() {
    return new StaticMethodMatcherImpl();
  }

  public static InstanceMethodMatcher instanceMethod() {
    return new InstanceMethodMatcherImpl();
  }

  public static AnyMethodMatcher anyMethod() {
    return new AnyMethodMatcherImpl();
  }

  public static ConstructorMatcher constructor() {
    return new ConstructorMatcherImpl();
  }
}
