/*
 * Copyright 2018 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.nullness;

import com.google.errorprone.CompilationTestHelper;
import com.sun.tools.javac.main.Main.Result;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class UnsafeWildcardTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(UnsafeWildcard.class, getClass());

  @Test
  public void positiveExpressions() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.List;",
            "class Test<T> {",
            "  static class WithBound<U extends Number> {}",
            "  public WithBound<? super T> basic() {",
            "    // BUG: Diagnostic contains: Cast to wildcard type unsafe",
            "    return null;",
            "  }",
            "  public WithBound<? super T> inParens() {",
            "    // BUG: Diagnostic contains: Cast to wildcard type unsafe",
            "    return (null);",
            "  }",
            "  public WithBound<? super T> cast() {",
            "    // BUG: Diagnostic contains: Cast to wildcard type unsafe",
            "    return (WithBound<? super T>) null;",
            "  }",
            "  public WithBound<? super T> inTernary(boolean x, WithBound<? super T> dflt) {",
            "    // BUG: Diagnostic contains: Cast to wildcard type unsafe",
            "    return x ? null : dflt;",
            "  }",
            "  public WithBound<? super T> allNullTernary(boolean x) {",
            "    // BUG: Diagnostic contains: Cast to wildcard type unsafe",
            "    return x ? null : null;",
            "  }",
            "  public WithBound<? super T> parensInTernary(boolean x) {",
            "    // BUG: Diagnostic contains: Cast to wildcard type unsafe",
            "    return x ? (null) : null;",
            "  }",
            "  public WithBound<? super T> parensAroundTernary(boolean x) {",
            "    // BUG: Diagnostic contains: Cast to wildcard type unsafe",
            "    return (x ? null : null);",
            "  }",
            "  public List<WithBound<? super T>> nestedWildcard() {",
            "    // BUG: Diagnostic contains: Cast to wildcard type unsafe",
            "    return null;",
            "  }",
            "  public List<? extends WithBound<? super T>> extendsWildcard() {",
            "    // BUG: Diagnostic contains: Cast to wildcard type unsafe",
            "    return null;",
            "  }",
            "  public List<? super WithBound<? super T>> superWildcard() {",
            "    // BUG: Diagnostic contains: Cast to wildcard type unsafe",
            "    return null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeReturns() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.List;",
            "class Test {",
            "  public String basic() {",
            "    return null;",
            "  }",
            "  public String inParens() {",
            "    return (null);",
            "  }",
            "  public String inTernary(boolean x) {",
            "    return x ? null : \"foo\";",
            "  }",
            "  public String allNullTernary(boolean x) {",
            "    return x ? null : null;",
            "  }",
            "  public String parensInTernary(boolean x) {",
            "    return x ? (null) : \"foo\";",
            "  }",
            "  public String parensAroundTernary(boolean x) {",
            "    return (x ? null : \"foo\");",
            "  }",
            "  public List<String> typearg() {",
            "    return null;",
            "  }",
            "  public List<? extends String> extendsWildcard() {",
            "    return null;",
            "  }",
            "  public List<? super String> superWildcardNoImplicitBound() {",
            "    return null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeLambdas() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.List;",
            "import java.util.function.Function;",
            "class Test {",
            "  public Function<String, String> basic() {",
            "    return x -> null;",
            "  }",
            "  public Function<String, String> inParens() {",
            "    return x -> (null);",
            "  }",
            "  public Function<Boolean, String> inTernary() {",
            "    return x -> x ? null : \"foo\";",
            "  }",
            "  public Function<String, String> returnInLambda() {",
            "    return x -> { return null; };",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void lambdasWithTypeParameters() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.List;",
            "import java.util.function.Function;",
            "class Test {",
            "  class WithBound<T extends Number> {}",
            "  public Function<String, List<? super String>> contra() {",
            "    return s -> null;",
            "  }",
            "  public Function<Integer, WithBound<? super Integer>> implicitOk() {",
            "    return i -> null;",
            "  }",
            "  public <U> Function<U, WithBound<? super U>> implicitPositive() {",
            "    // BUG: Diagnostic contains: Cast to wildcard type unsafe",
            "    return u -> null;",
            "  }",
            "  public <U> Function<U, WithBound<? super U>> returnInLambda() {",
            "    // BUG: Diagnostic contains: Cast to wildcard type unsafe",
            "    return u -> { return null; };",
            "  }",
            "  public <U> Function<U, WithBound<? super U>> nestedWildcard() {",
            "    // BUG: Diagnostic contains: Cast to wildcard type unsafe",
            "    return null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void typeParameters() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.List;",
            "class Test<S> {",
            "  static class WithBound<U extends Number> {}",
            "  class WildcardBound<T extends WithBound<? super S>> {",
            "    T bad() {",
            "      // We allow this and instead check instantiations below",
            "      return null;",
            "    }",
            "  }",
            "  WildcardBound<WithBound<? super S>> diamond() {",
            "    // BUG: Diagnostic contains: Unsafe wildcard type argument",
            "    return new WildcardBound<>();",
            "  }",
            "  WildcardBound<WithBound<? super S>> create() {",
            "    // BUG: Diagnostic contains: Unsafe wildcard type argument",
            "    return new WildcardBound<WithBound<? super S>>();",
            "  }",
            "  WildcardBound<WithBound<? super S>> none() {",
            "    // BUG: Diagnostic contains: Cast to wildcard type unsafe",
            "    return null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void variables() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.List;",
            "class Test<T> {",
            "  class WithBound<T extends Number> {}",
            "  private String s;",
            "  private List<String> xs = null;",
            "  private List<? super String> ys;",
            "  private WithBound<? super Integer> zs = null;",
            "  // BUG: Diagnostic contains: Cast to wildcard type unsafe",
            "  private WithBound<? super T> initialized = null;",
            "  // BUG: Diagnostic contains: Cast to wildcard type unsafe",
            "  private final WithBound<? super T> initializedFinal = null;",
            "  // BUG: Diagnostic contains: Uninitialized field with unsafe wildcard",
            "  private WithBound<? super T> uninitialized;",
            "  private final WithBound<? super T> uninitializedFinal;",
            "  Test() {",
            "    // BUG: Diagnostic contains: Cast to wildcard type unsafe",
            "    uninitializedFinal = null;",
            "    // BUG: Diagnostic contains: Cast to wildcard type unsafe",
            "    uninitialized = null;",
            "    // BUG: Diagnostic contains: Cast to wildcard type unsafe",
            "    initialized = null;",
            "  }",
            "  public void foo() {",
            "    List<? extends String> covariant = null;",
            "    List<? super String> contravariant = null;",
            "    WithBound<? super Integer> inBounds = null;",
            "    WithBound<? super T> uninitializedLocal;",
            "    final WithBound<? super T> uninitializedFinalLocal;",
            "    // BUG: Diagnostic contains: Cast to wildcard type unsafe",
            "    WithBound<? super T> implicitBounds = null;",
            "    covariant = null;",
            "    contravariant = null;",
            "    inBounds = null;",
            "    // BUG: Diagnostic contains: Cast to wildcard type unsafe",
            "    uninitializedLocal = null;",
            "    // BUG: Diagnostic contains: Cast to wildcard type unsafe",
            "    uninitializedFinalLocal = null;",
            "    // BUG: Diagnostic contains: Cast to wildcard type unsafe",
            "    implicitBounds = null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void calls() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.List;",
            "class Test {",
            "  class WithBound<T extends Number> {}",
            "  public void foo(String s, List<String> xs, List<? super String> contra) {",
            "    foo(null, null, null);",
            "  }",
            "  public void negative(WithBound<Integer> xs, WithBound<? super Integer> contra) {",
            "    negative(null, null);",
            "  }",
            "  public <U> void positive(WithBound<? super U> implicit) {",
            "    // BUG: Diagnostic contains: Cast to wildcard type unsafe",
            "    this.<U>positive(null);",
            "    positive(null);", // ok b/c compiler uses U = Object and ? super Object is ok
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void inferredParamType_flaggedIfProblematic() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.List;",
            "class Test {",
            "  static class WithBound<T extends Number> {}",
            "  public <U> List<WithBound<? super U>> positive(WithBound<? super U> safe) {",
            "    // BUG: Diagnostic contains: Unsafe wildcard in inferred type argument",
            "    return List.of(safe,",
            "        // BUG: Diagnostic contains: Cast to wildcard type unsafe",
            "        null);", // implicitly upcast to WithBound<? super U>
            "  }",
            "  public List<WithBound<? super Integer>> negative(WithBound<Integer> safe) {",
            "    return List.of(safe, null);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void constructors() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test<U> {",
            "  class WithBound<T extends Number> {}",
            "  public Test() { this(null, null); }",
            "  public Test(WithBound<? super U> implicit) {}",
            "  public Test(WithBound<Integer> xs, WithBound<? super Integer> contra) {",
            "    // BUG: Diagnostic contains: Cast to wildcard type unsafe",
            "    this(null);",
            "  }",
            "  class Sub<S> extends Test<S> {",
            "    Sub(WithBound<? super U> implicit) {",
            "      // BUG: Diagnostic contains: Cast to wildcard type unsafe",
            "      super(null);",
            "    }",
            "  }",
            "  static <U> Test<U> newClass() {",
            "    new Test<U>(null, null);",
            "    // BUG: Diagnostic contains: Cast to wildcard type unsafe",
            "    new Test<U>(null);",
            "    // BUG: Diagnostic contains: Cast to wildcard type unsafe",
            "    return new Test<>(null);",
            "  }",
            "  static <U> Test<U> anonymous() {",
            "    // BUG: Diagnostic contains: Cast to wildcard type unsafe",
            "    new Test<U>(null) {};",
            "    return null;",
            "  }",
            "  void inner() {",
            "    // BUG: Diagnostic contains: Cast to wildcard type unsafe",
            "    new Sub<U>(null);",
            "    // BUG: Diagnostic contains: Cast to wildcard type unsafe",
            "    new Sub<U>(null) {};",
            "    // BUG: Diagnostic contains: Cast to wildcard type unsafe",
            "    this.new Sub<U>(null) {};",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void supertypes_problematicWildcards_flagged() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.io.Serializable;",
            "import java.util.AbstractList;",
            "import java.util.List;",
            "class Test {",
            "  class WithBound<T extends Number> {}",
            "  // BUG: Diagnostic contains: Unsafe wildcard type",
            "  abstract class BadList<U> extends AbstractList<WithBound<? super U>> {}",
            "  abstract class BadListImpl<U> implements Serializable,",
            "      // BUG: Diagnostic contains: Unsafe wildcard type",
            "      List<WithBound<? super U>> {}",
            "  interface BadListItf<U> extends Serializable,",
            "      // BUG: Diagnostic contains: Unsafe wildcard type",
            "      List<WithBound<? super U>> {}",
            "}")
        .doTest();
  }

  @Test
  public void varargs() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test<T> {",
            "  static class WithBound<T extends Number> {}",
            "  Test(WithBound<Integer> xs, WithBound<? super T>... args) {}",
            "  static <U> void hasVararg(WithBound<Integer> xs, WithBound<? super U>... args) {}",
            "  static <U> void nullVarargs(WithBound<? super U> xs) {",
            "    Test.<U>hasVararg(",
            "        null,", // fine: target type is safe
            "        // BUG: Diagnostic contains: Cast to wildcard type unsafe",
            "        null,",
            "        xs,",
            "        // BUG: Diagnostic contains: Cast to wildcard type unsafe",
            "        null);",
            "    new Test<U>(",
            "        null,",
            "        // BUG: Diagnostic contains: Cast to wildcard type unsafe",
            "        null,",
            "        xs,",
            "        // BUG: Diagnostic contains: Cast to wildcard type unsafe",
            "        null);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void arrays() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  class WithBound<T extends Number> {}",
            // Generic array creation is a compilation error, and non-generic arrays are ok
            "  Object[] simpleInitializer = { null };",
            "  Object[][] nestedInitializer = { { null }, { null } };",
            "  <U> void nulls() {",
            "    String[][] stringMatrix = null;",
            "    WithBound<? super Integer>[] implicitBound = null;",
            "    // BUG: Diagnostic contains: Cast to wildcard type unsafe",
            "    WithBound<? super U>[] simpleNull = null;",
            "    // BUG: Diagnostic contains: Cast to wildcard type unsafe",
            "    WithBound<? super U>[][] nestedNull = null;",
            "  }",
            "}")
        .doTest();
  }

  /**
   * Regresion test demonstrating that generic array creation is a compiler error. If it wasn't,
   * we'd want to check element types.
   */
  @Test
  public void genericArrays_isCompilerError() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  class WithBound<T extends Number> {}",
            "  // BUG: Diagnostic matches: X",
            "  WithBound<? super Integer>[] simpleInitializer = { null };",
            "  // BUG: Diagnostic matches: X",
            "  WithBound<? super Integer>[][] nestedInitializer = { { null }, { null } };",
            "  // BUG: Diagnostic matches: X",
            "  WithBound<? super Integer>[][] emptyInitializer = {};",
            "  void newArrays() {",
            "    // BUG: Diagnostic matches: X",
            "    Object[] a1 = new WithBound<? super Integer>[] {};",
            "    // BUG: Diagnostic matches: X",
            "    Object[] a2 = new WithBound<? super Integer>[0];",
            "    // BUG: Diagnostic matches: X",
            "    Object[] a3 = new WithBound<? super Integer>[][] {};",
            "    // BUG: Diagnostic matches: X",
            "    Object[] a4 = new WithBound<? super Integer>[0][];",
            "  }",
            "}")
        .expectResult(Result.ERROR)
        .matchAllDiagnostics()
        .expectErrorMessage("X", msg -> msg.contains("generic array creation"))
        .doTest();
  }

  @Test
  public void arrays_rawTypes_futureWork() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  class WithBound<T extends Number> {}",
            "  <U> void problematic() {",
            // The following implicitly create problematic types even absent null values (though
            // problematic non-empty arrays containing all-null values can be created just as
            // easily with [N] where N > 0). The compiler issues raw and unchecked warnings here,
            // but we might want to flag assignments as well.
            "    WithBound<? super U> raw = new WithBound();",
            "    WithBound<? super U>[] array = new WithBound[0];",
            "    WithBound<? super U>[][] nested = new WithBound[0][];",
            "  }",
            "}")
        .doTest();
  }

  /**
   * Regression test to ignore {@code null} assignment to wildcard whose lower bound is a type
   * variable with non-trivial upper bound. The compiler rejects potentially dangerous wildcards on
   * its own in this case, but simple subtype checks between lower and upper bound can fail and lead
   * to false positives if involved type variables' upper bounds capture another type variable.
   */
  @Test
  public void boundedTypeVar_validLowerBound_isIgnored() {
    compilationHelper
        .addSourceLines(
            "MyIterable.java",
            "import java.util.List;",
            "interface MyIterable<E, T extends Iterable<E>> {",
            "  static class Test<F, S extends List<F>> implements MyIterable<F, S> {",
            "    MyIterable<F, ? super S> parent;",
            "    public Test() {",
            "      this.parent = null;",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void boundedTypeVar_questionableLowerBound_isCompilerError() {
    compilationHelper
        .addSourceLines(
            "MyIterable.java",
            "import java.util.List;",
            "interface MyIterable<E, T extends List<E>> {",
            "  // BUG: Diagnostic matches: X",
            "  static class Test<F, S extends Iterable<F>> implements MyIterable<F, S> {",
            "    MyIterable<F, ? super S> parent;",
            "    public Test() {",
            "      this.parent = null;",
            "    }",
            "  }",
            "}")
        .expectResult(Result.ERROR)
        .matchAllDiagnostics()
        .expectErrorMessage(
            "X", msg -> msg.contains("type argument S is not within bounds of type-variable T"))
        .doTest();
  }

  /**
   * Regression test to ignore {@code null} assignment to wildcard whose lower bound is a concrete
   * type and whose implicit upper bound is F-bounded. The compiler rejects potentially dangerous
   * wildcards on its own in this case, but simple subtype checks between lower and upper bound can
   * fail and lead to false positives.
   */
  @Test
  public void fBoundedImplicitUpperBound_validLowerBound_isIgnored() {
    compilationHelper
        .addSourceLines(
            "FBounded.java",
            "abstract class FBounded<T extends FBounded<T>> {",
            "  public static final class Coll<E> extends FBounded<Coll<E>> {}",
            "  public interface Listener<U extends FBounded<U>> {}",
            "  public static <K> void shouldWork() {",
            "    Listener<? super Coll<K>> validListener = null;",
            "    // BUG: Diagnostic contains: Cast to wildcard type unsafe",
            "    Listener<? super K> invalidListener = null;",
            "    // BUG: Diagnostic contains: Cast to wildcard type unsafe",
            "    Iterable<Listener<? super K>> invalidListeners = java.util.List.of(null, null);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void fBoundedImplicitUpperBound_invalidLowerBound_isCompilerError() {
    compilationHelper
        .addSourceLines(
            "FBounded.java",
            "abstract class FBounded<T extends FBounded<T>> {",
            "  public static final class Coll<E> extends FBounded<Coll<E>> {}",
            "  public interface Listener<U extends FBounded<U>> {}",
            "  public static <K> void shouldWork() {",
            "    // BUG: Diagnostic matches: X",
            "    Listener<? super String> listener = null;",
            "  }",
            "}")
        .expectResult(Result.ERROR)
        .matchAllDiagnostics()
        .expectErrorMessage(
            "X", msg -> msg.contains("String is not within bounds of type-variable U"))
        .doTest();
  }
}
