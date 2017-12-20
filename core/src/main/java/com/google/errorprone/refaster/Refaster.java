/*
 * Copyright 2013 The Error Prone Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.errorprone.refaster;

/**
 * Static utilities to indicate special handling in Refaster templates.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
public class Refaster {
  private Refaster() {}

  /**
   * Indicates that Refaster should treat this {@code @Repeated} argument specifically as a varargs
   * argument.
   *
   * <p>For example, you might write
   *
   * <pre><code>
   *    {@literal @}BeforeTemplate void something(@Repeated T arg) {
   *     Stream.of(asVarargs(arg)); // doesn't use the Stream.of(T) overload, but Stream.of(T...)
   *    }
   * </code></pre>
   */
  public static <T> T[] asVarargs(T arg) {
    throw new UnsupportedOperationException();
  }

  /**
   * Indicates that Refaster should attempt to match a target expression against each of the
   * specified template expressions, in order, and succeed at the first match.
   *
   * <p>This method should only be used in Refaster templates, but should never actually be run.
   *
   * <p>For example, instead of writing
   *
   * <pre><code>
   * {@literal @}BeforeTemplate &lt;E&gt; List&lt;E&gt; copyOfSingleton(E element) {
   *   return ImmutableList.copyOf(Collections.singletonList(element));
   * }
   * {@literal @}BeforeTemplate &lt;E&gt; List&lt;E&gt; copyOfArrayList(E element) {
   *   return ImmutableList.copyOf(Lists.newArrayList(element));
   * }
   * </code></pre>
   *
   * <p>one could alternately write
   *
   * <pre><code>
   * {@literal @}BeforeTemplate &lt;E&gt; List&lt;E&gt; singleton(E element) {
   *   return ImmutableList.copyOf(Refaster.anyOf(
   *     Collections.singletonList(element),
   *     Lists.newArrayList(element)));
   * }
   * </code></pre>
   */
  @SafeVarargs
  public static <T> T anyOf(T... expressions) {
    throw new UnsupportedOperationException();
  }

  /**
   * This is a placeholder for the Java instanceof operator that can be used with Refaster type
   * variables. The type argument must always be specified explicitly, e.g. {@code
   * Refaster.<String>isInstance(o)}.
   *
   * <p>For example, instead of writing the broken
   *
   * <pre><code>
   * {@literal @}AfterTemplate &lt;T&gt; boolean instanceOf(Object o) {
   *   return o instanceof T; // you want to match this, but it won't compile
   * }
   * </code></pre>
   *
   * <p>you would instead write
   *
   * <pre><code>
   * {@literal @}AfterTemplate &lt;T&gt; boolean instanceOf(Object o) {
   *   return Refaster.&lt;T&gt;isInstance(o); // generates the replacement "o instanceof T"
   * }
   * </code></pre>
   *
   * @throws IllegalArgumentException if T is not specified explicitly.
   */
  public static <T> boolean isInstance(Object o) {
    // real code wouldn't have an unused type parameter (T) or an unused argument (o)
    throw new UnsupportedOperationException(o.toString());
  }

  /**
   * This is a placeholder for {@code new T[size]}. The type argument must always be specified
   * explicitly, e.g. {@code Refaster.<String>newArray(10)}.
   *
   * <p>For example, instead of writing the broken
   *
   * <pre><code>
   * {@literal @}AfterTemplate &lt;T&gt; T[] newTArray(int size) {
   *   return new T[size]; // you want to generate this code, but it won't compile
   * }
   * </code></pre>
   *
   * <p>you would instead write
   *
   * <pre><code>
   * {@literal @}AfterTemplate &lt;T&gt; T[] newTArray(int size) {
   *   return Refaster.&lt;T&gt;newArray(size);
   * }
   * </code></pre>
   *
   * @throws IllegalArgumentException if T is not specified explicitly.
   */
  public static <T> T[] newArray(int size) {
    throw new UnsupportedOperationException(Integer.toString(size));
  }

  /**
   * This is a placeholder for the expression T.class. The type argument must always be specified
   * explicitly, e.g. {@code Refaster.<String>clazz()}.
   *
   * <p>For example, instead of writing the broken
   *
   * <pre><code>
   * {@literal @}AfterTemplate &lt;T&gt; T[] getEnumConstants() {
   *   return T.class.getEnumConstants(); // you want to inline this, but it won't compile
   * }
   * </code></pre>
   *
   * you would instead write
   *
   * <pre><code>
   * {@literal @}AfterTemplate &lt;T&gt; T[] getEnumConstants() {
   *   return Refaster.&lt;T&gt;clazz().getEnumConstants();
   * }
   * </code></pre>
   *
   * @throws IllegalArgumentException if T is not specified explicitly.
   */
  public static <T> Class<T> clazz() {
    throw new UnsupportedOperationException();
  }

  /**
   * This is a placeholder for the expression E.valueOf(string). The type argument must always be
   * specified explicitly, e.g. {@code Refaster.<RoundingMode>valueOf(string)}.
   *
   * <p>For example, instead of writing the broken
   *
   * <pre><code>
   * {@literal @}BeforeTemplate &lt;E extends Enum&lt;E&gt;&gt; E valueOf(String str) {
   *   return E.valueOf(str);
   * }
   * </code></pre>
   *
   * <p>you would instead write
   *
   * <pre><code>
   * {@literal @}BeforeTemplate &lt;E extends Enum&lt;E&gt;&gt; E valueOf(String str) {
   *   return Refaster.&lt;E&gt;enumValueOf(str);
   * }
   * </code></pre>
   *
   * @throws IllegalArgumentException if E is not specified explicitly.
   */
  public static <E extends Enum<E>> E enumValueOf(String string) {
    throw new UnsupportedOperationException();
  }

  /**
   * This is a special method to emit a comment before an expression. The comment argument must
   * always be a string literal. This method cannot be static imported.
   *
   * <p>For example, instead of writing
   *
   * <pre><code>
   * {@literal @}AfterTemplate int lengthWithComment(String str) {
   *   return /* comment \*\/ str.length();
   * }
   * </code></pre>
   *
   * <p>you would instead write
   *
   * <pre><code>
   * {@literal @}AfterTemplate int lengthWithComment(String str) {
   *   return Refaster.emitCommentBefore("comment", str.length());
   * }
   * </code></pre>
   */
  public static <T> T emitCommentBefore(String literal, T expression) {
    throw new UnsupportedOperationException();
  }

  /**
   * This is a special method to emit a one-line comment. The comment argument must always be a
   * string literal. This method cannot be static imported.
   *
   * <p>For example, instead of writing
   *
   * <pre><code>
   * {@literal @}AfterTemplate void printWithComment(String str) {
   *   // comment
   *   System.out.println(str);
   * }
   * </code></pre>
   *
   * <p>you would instead write
   *
   * <pre><code>
   * {@literal @}AfterTemplate void printWithComment(String str) {
   *   Refaster.emitComment("comment");
   *   System.out.println(str);
   * }
   * </code></pre>
   */
  public static void emitComment(String literal) {
    throw new UnsupportedOperationException();
  }
}
