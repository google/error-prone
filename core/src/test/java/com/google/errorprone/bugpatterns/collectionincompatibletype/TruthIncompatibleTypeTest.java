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

package com.google.errorprone.bugpatterns.collectionincompatibletype;

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link TruthIncompatibleType}Test */
@RunWith(JUnit4.class)
public class TruthIncompatibleTypeTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(TruthIncompatibleType.class, getClass());

  @Test
  public void positive() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "public class Test {",
            "  static final class A {}",
            "  static final class B {}",
            "  public void f(A a, B b) {",
            "    // BUG: Diagnostic contains:",
            "    assertThat(a).isEqualTo(b);",
            "    // BUG: Diagnostic contains:",
            "    assertThat(a).isNotEqualTo(b);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void assume() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import static com.google.common.truth.TruthJUnit.assume;",
            "public class Test {",
            "  static final class A {}",
            "  static final class B {}",
            "  public void f(A a, B b) {",
            "    // BUG: Diagnostic contains:",
            "    assume().that(a).isEqualTo(b);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "public class Test {",
            "  static final class A {}",
            "  static final class B {}",
            "  public void f(A a, B b) {",
            "    assertThat(a).isEqualTo(a);",
            "    assertThat(b).isEqualTo(b);",
            "    assertThat(\"a\").isEqualTo(\"b\");",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void mixedNumberTypes_noMatch() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "public class Test {",
            "  public void f() {",
            "    assertThat(2L).isEqualTo(2);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void mixedBoxedNumberTypes_noMatch() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "public class Test {",
            "  public void f() {",
            "    assertThat(Byte.valueOf((byte) 2)).isEqualTo(2);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void chainedThrowAssertion_noMatch() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "public class Test {",
            "  public void f(Exception e) {",
            "    assertThat(e).hasMessageThat().isEqualTo(\"foo\");",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void clazz() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "public class Test {",
            "  public void f(Class<InterruptedException> a, Class<? extends Throwable> b) {",
            "    try {",
            "    } catch (Exception e) {",
            "      assertThat(e.getCause().getClass()).isEqualTo(IllegalArgumentException.class);",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void containment() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "public class Test {",
            "  public void f(Iterable<Long> xs, String x) {",
            "    // BUG: Diagnostic contains:",
            "    assertThat(xs).contains(x);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void containment_noMatch() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "public class Test {",
            "  public void f(Iterable<Long> xs, Number x) {",
            "    assertThat(xs).contains(x);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void vectorContainment() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "import com.google.common.collect.ImmutableList;",
            "public class Test {",
            "  public void f(Iterable<Long> xs, String x) {",
            "    // BUG: Diagnostic contains:",
            "    assertThat(xs).containsExactlyElementsIn(ImmutableList.of(x));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void vectorContainment_noMatch() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "import com.google.common.collect.ImmutableList;",
            "public class Test {",
            "  public void f(Iterable<Long> xs, Number x) {",
            "    assertThat(xs).containsExactlyElementsIn(ImmutableList.of(x));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void variadicCall_noMatch() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "import com.google.common.collect.ImmutableList;",
            "public class Test {",
            "  public void f(Iterable<Long> xs, Number... x) {",
            "    assertThat(xs).containsExactly((Object[]) x);",
            "  }",
            "}")
        .ignoreJavacErrors()
        .doTest();
  }

  @Test
  public void variadicCall_checked() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "import com.google.common.collect.ImmutableList;",
            "public class Test {",
            "  public void f(Iterable<Long> xs, String... x) {",
            "    // BUG: Diagnostic contains:",
            "    assertThat(xs).containsExactly(x);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void variadicCall_primitiveArray() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "import com.google.common.collect.ImmutableList;",
            "public class Test {",
            "  public void f(Iterable<byte[]> xs, byte[] ys) {",
            "    assertThat(xs).containsExactly(ys);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void containsExactlyElementsIn_withArray_match() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "import com.google.common.collect.ImmutableList;",
            "public class Test {",
            "  public void f(Iterable<String> xs, Object... x) {",
            "    assertThat(xs).containsExactlyElementsIn(x);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void containsExactlyElementsIn_withArray_mismatched() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "import com.google.common.collect.ImmutableList;",
            "public class Test {",
            "  public void f(Iterable<Long> xs, String... x) {",
            "    // BUG: Diagnostic contains:",
            "    assertThat(xs).containsExactlyElementsIn(x);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void containsExactlyElementsIn_numericTypes_notSpecialCased() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "import com.google.common.collect.ImmutableList;",
            "public class Test {",
            "  public void f(Iterable<Long> xs, ImmutableList<Integer> ys) {",
            "    // BUG: Diagnostic contains:",
            "    assertThat(xs).containsExactlyElementsIn(ys);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void comparingElementsUsing_typeMismatch() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "import com.google.common.collect.ImmutableList;",
            "import com.google.common.truth.Correspondence;",
            "public class Test {",
            "  public void f(Iterable<Long> xs, Correspondence<Integer, String> c) {",
            "    // BUG: Diagnostic contains:",
            "    assertThat(xs).comparingElementsUsing(c).doesNotContain(\"\");",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void comparingElementsUsing_typesMatch() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "import com.google.common.collect.ImmutableList;",
            "import com.google.common.truth.Correspondence;",
            "public class Test {",
            "  public void f(Iterable<Long> xs, Correspondence<Long, String> c) {",
            "    assertThat(xs).comparingElementsUsing(c).doesNotContain(\"\");",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void mapContainsExactlyEntriesIn_keyTypesDiffer() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "import java.util.Map;",
            "public class Test {",
            "  public void f(Map<String, Long> xs, Map<Long, Long> ys) {",
            "    // BUG: Diagnostic contains:",
            "    assertThat(xs).containsExactlyEntriesIn(ys);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void mapContainsExactlyEntriesIn_valueTypesDiffer() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "import java.util.Map;",
            "public class Test {",
            "  public void f(Map<String, Long> xs, Map<String, String> ys) {",
            "    // BUG: Diagnostic contains:",
            "    assertThat(xs).containsExactlyEntriesIn(ys);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void mapContainsExactly() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "import java.util.Map;",
            "public class Test {",
            "  public void f(Map<String, Long> xs) {",
            "    // BUG: Diagnostic contains:",
            "    assertThat(xs).containsExactly(\"\", 1L, \"foo\", 2L, \"bar\", 3);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void mapContainsExactly_varargs() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "import java.util.Map;",
            "public class Test {",
            "  public void f(Map<String, Long> xs, String a, Long b, Object... rest) {",
            "    assertThat(xs).containsExactly(a, b, rest);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void multimapContainsExactly() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "import com.google.common.collect.Multimap;",
            "public class Test {",
            "  public void f(Multimap<String, Long> xs) {",
            "    // BUG: Diagnostic contains:",
            "    assertThat(xs).containsExactly(\"\", 1L, \"foo\", 2L, \"bar\", 3);",
            "  }",
            "}")
        .doTest();
  }
}
