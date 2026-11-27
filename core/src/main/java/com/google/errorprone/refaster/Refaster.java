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

import com.google.errorprone.annotations.CanIgnoreReturnValue;

/// Static utilities to indicate special handling in Refaster templates.
///
/// @author lowasser@google.com (Louis Wasserman)
// We need to call these methods from templates that aren't run as normal Java code.
@SuppressWarnings("DoNotCallSuggester")
public final class Refaster {
  private Refaster() {}

  /// Indicates that Refaster should treat this `@Repeated` argument specifically as a varargs
  /// argument.
  ///
  /// For example, you might write
  ///
  /// ```
  /// @BeforeTemplate void something(@Repeated T arg) {
  ///  Stream.of(asVarargs(arg)); // doesn't use the Stream.of(T) overload, but Stream.of(T...)
  /// }
  /// ```
  public static <T> T[] asVarargs(T arg) {
    throw new UnsupportedOperationException();
  }

  /// Indicates that Refaster should attempt to match a target expression against each of the
  /// specified template expressions, in order, and succeed at the first match.
  ///
  /// This method should only be used in Refaster templates, but should never actually be run.
  ///
  /// For example, instead of writing
  ///
  /// ```
  /// @BeforeTemplate <E> List<E> copyOfSingleton(E element) {
  ///   return ImmutableList.copyOf(Collections.singletonList(element));
  /// }
  /// @BeforeTemplate <E> List<E> copyOfArrayList(E element) {
  ///   return ImmutableList.copyOf(Lists.newArrayList(element));
  /// }
  /// ```
  ///
  /// one could alternately write
  ///
  /// ```
  /// @BeforeTemplate <E> List<E> singleton(E element) {
  ///   return ImmutableList.copyOf(Refaster.anyOf(
  ///     Collections.singletonList(element),
  ///     Lists.newArrayList(element)));
  /// }
  /// ```
  @SafeVarargs
  @CanIgnoreReturnValue
  public static <T> T anyOf(T... expressions) {
    throw new UnsupportedOperationException();
  }

  /// This is a placeholder for the Java instanceof operator that can be used with Refaster type
  /// variables. The type argument must always be specified explicitly, e.g.
  /// `Refaster.<String>isInstance(o)`.
  ///
  /// For example, instead of writing the broken
  ///
  /// ```
  /// @AfterTemplate <T> boolean instanceOf(Object o) {
  ///   return o instanceof T; // you want to match this, but it won't compile
  /// }
  /// ```
  ///
  /// you would instead write
  ///
  /// ```
  /// @AfterTemplate <T> boolean instanceOf(Object o) {
  ///   return Refaster.<T>isInstance(o); // generates the replacement "o instanceof T"
  /// }
  /// ```
  ///
  /// @throws IllegalArgumentException if T is not specified explicitly.
  @SuppressWarnings("UnusedTypeParameter") // The type parameter supports extralinguistic magic
  public static <T> boolean isInstance(Object o) {
    // real code wouldn't have an unused type parameter (T) or an unused argument (o)
    throw new UnsupportedOperationException(o.toString());
  }

  /// This is a placeholder for `new T[size]`. The type argument must always be specified
  /// explicitly, e.g. `Refaster.<String>newArray(10)`.
  ///
  /// For example, instead of writing the broken
  ///
  /// ```
  /// @AfterTemplate <T> T[] newTArray(int size) {
  ///   return new T[size]; // you want to generate this code, but it won't compile
  /// }
  /// ```
  ///
  /// you would instead write
  ///
  /// ```
  /// @AfterTemplate <T> T[] newTArray(int size) {
  ///   return Refaster.<T>newArray(size);
  /// }
  /// ```
  ///
  /// @throws IllegalArgumentException if T is not specified explicitly.
  public static <T> T[] newArray(int size) {
    throw new UnsupportedOperationException(Integer.toString(size));
  }

  /// This is a placeholder for the expression T.class. The type argument must always be specified
  /// explicitly, e.g. `Refaster.<String>clazz()`.
  ///
  /// For example, instead of writing the broken
  ///
  /// ```
  /// @AfterTemplate <T> T[] getEnumConstants() {
  ///   return T.class.getEnumConstants(); // you want to inline this, but it won't compile
  /// }
  /// ```
  ///
  /// you would instead write
  ///
  /// ```
  /// @AfterTemplate <T> T[] getEnumConstants() {
  ///   return Refaster.<T>clazz().getEnumConstants();
  /// }
  /// ```
  ///
  /// @throws IllegalArgumentException if T is not specified explicitly.
  public static <T> Class<T> clazz() {
    throw new UnsupportedOperationException();
  }

  /// This is a placeholder for the expression E.valueOf(string). The type argument must always be
  /// specified explicitly, e.g. `Refaster.<RoundingMode>valueOf(string)`.
  ///
  /// For example, instead of writing the broken
  ///
  /// ```
  /// @BeforeTemplate <E extends Enum<E>> E valueOf(String str) {
  ///   return E.valueOf(str);
  /// }
  /// ```
  ///
  /// you would instead write
  ///
  /// ```
  /// @BeforeTemplate <E extends Enum<E>> E valueOf(String str) {
  ///   return Refaster.<E>enumValueOf(str);
  /// }
  /// ```
  ///
  /// @throws IllegalArgumentException if E is not specified explicitly.
  public static <E extends Enum<E>> E enumValueOf(String string) {
    throw new UnsupportedOperationException();
  }

  /// This is a special method to emit a comment before an expression. The comment argument must
  /// always be a string literal. This method cannot be static imported.
  ///
  /// For example, instead of writing
  ///
  /// ```
  /// @AfterTemplate int lengthWithComment(String str) {
  ///   return /* comment */ str.length();
  /// }
  /// ```
  ///
  /// you would instead write
  ///
  /// ```
  /// @AfterTemplate int lengthWithComment(String str) {
  ///   return Refaster.emitCommentBefore("comment", str.length());
  /// }
  /// ```
  public static <T> T emitCommentBefore(String literal, T expression) {
    throw new UnsupportedOperationException();
  }

  /// This is a special method to emit a one-line comment. The comment argument must always be a
  /// string literal. This method cannot be static imported.
  ///
  /// For example, instead of writing
  ///
  /// ```
  /// @AfterTemplate void printWithComment(String str) {
  ///   // comment
  ///   System.out.println(str);
  /// }
  /// ```
  ///
  /// you would instead write
  ///
  /// ```
  /// @AfterTemplate void printWithComment(String str) {
  ///   Refaster.emitComment("comment");
  ///   System.out.println(str);
  /// }
  /// ```
  public static void emitComment(String literal) {
    throw new UnsupportedOperationException();
  }

  /**
   * Indicates that Refaster should attempt to concatenate the given expressions, joining literals
   * if possible.
   *
   * <p>For example, instead of writing
   *
   * <pre><code>
   * {@literal @}AfterTemplate void exampleConcatLiterals(boolean b, String msg, Object o) {
   *     Preconditions.checkArgument(b, msg + "%s", o);
   *  }
   * </code></pre>
   *
   * <p>you could write
   *
   * <pre><code>
   * {@literal @}AfterTemplate String exampleConcatLiterals(boolean b, String msg, Object o) {
   *    Preconditions.checkArgument(b, Refaster.concatLiterals(msg, "%s"), o);
   * }
   * </code></pre>
   *
   * Since the plus operator is left-associative, the first or second operand must be a string to
   * ensure that all operands are string-concatenated. (E.g., if the first two operands were
   * numerical, addition would be performed.) This method enforces that the first operand is a
   * string. For the other case, see {@link #concatLiterals(Object, String)}.
   *
   * @see #concatLiterals(Object, String)
   */
  public static String concatLiterals(String str, Object... expressions) {
    throw new UnsupportedOperationException();
  }

  /**
   * Indicates that Refaster should attempt to concatenate the given expressions, joining literals
   * if possible.
   *
   * <p>For example, instead of writing
   *
   * <pre><code>
   * {@literal @}AfterTemplate void exampleConcatLiterals(boolean b, String msg, Object o) {
   *     Preconditions.checkArgument(b, msg + "%s", o1);
   *  }
   * </code></pre>
   *
   * <p>you could write
   *
   * <pre><code>
   * {@literal @}AfterTemplate String exampleConcatLiterals(boolean b, String msg, Object o) {
   *    Preconditions.checkArgument(b, Refaster.concatLiterals(msg, "%s"), o);
   * }
   * </code></pre>
   *
   * Since the plus operator is left-associative, the first or second operand must be a string to
   * ensure that all operands are string-concatenated. (E.g., if the first two operands were
   * numerical, addition would be performed.) This method enforces that the second operand is a
   * string. For the other case, see {@link #concatLiterals(String, Object...)}.
   *
   * @see #concatLiterals(String, Object...)
   */
  public static String concatLiterals(Object o, String str) {
    throw new UnsupportedOperationException();
  }
}
