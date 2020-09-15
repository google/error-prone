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

package com.google.errorprone.bugpatterns.javadoc;

import static org.junit.Assume.assumeFalse;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.BugCheckerRefactoringTestHelper.TestMode;
import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.util.RuntimeVersion;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link UnescapedEntity} bug pattern. */
@RunWith(JUnit4.class)
public final class UnescapedEntityTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(UnescapedEntity.class, getClass());
  private final BugCheckerRefactoringTestHelper refactoring =
      BugCheckerRefactoringTestHelper.newInstance(new UnescapedEntity(), getClass());

  @Test
  public void positive() {
    refactoring
        .addInputLines(
            "Test.java", //
            "/** List<Foo>, Map<Foo, Bar> */",
            "interface Test {}")
        .addOutputLines(
            "Test.java", //
            "/** {@code List<Foo>}, {@code Map<Foo, Bar>} */",
            "interface Test {}")
        .doTest(TestMode.AST_MATCH);
  }

  @Test
  public void negative() {
    helper
        .addSourceLines(
            "Test.java", //
            "/** {@code List<Foo>, Map<Foo, Bar>} */",
            "interface Test {}")
        .doTest();
  }

  @Test
  public void unescapedEntities_off() {
    refactoring
        .addInputLines(
            "Test.java", //
            "/** Foo & bar < */",
            "interface Test {}")
        .expectUnchanged()
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void withinPre() {
    refactoring
        .addInputLines(
            "Test.java",
            "/**",
            "  * <pre>Foo</pre>",
            "  * <pre>Use an ImmutableMap<String,Object> please</pre>",
            "  * <pre>bar</pre>",
            "  */",
            "interface Test {}")
        .addOutputLines(
            "Test.java",
            "/**",
            "  * <pre>Foo</pre>",
            "  * <pre>{@code Use an ImmutableMap<String,Object> please}</pre>",
            "  * <pre>bar</pre>",
            "  */",
            "interface Test {}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void withinPre_singleChar() {
    refactoring
        .addInputLines(
            "Test.java", //
            "/**",
            "  * <pre>n < 3</pre>",
            "  */",
            "interface Test {}")
        .addOutputLines(
            "Test.java", //
            "/**",
            "  * <pre>{@code n < 3}</pre>",
            "  */",
            "interface Test {}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void withinPre_alreadyEscaped() {
    assumeFalse(RuntimeVersion.isAtLeast15()); // https://bugs.openjdk.java.net/browse/JDK-8241780
    refactoring
        .addInputLines(
            "Test.java",
            "/**",
            "  * <pre>Use an ImmutableMap<String, Object> not a Map&lt;String, Object&gt;</pre>",
            "  */",
            "interface Test {}")
        .addOutputLines(
            "Test.java",
            "/**",
            "  * <pre>Use an ImmutableMap&lt;String, Object&gt; not a"
                + " Map&lt;String, Object&gt;</pre>",
            "  */",
            "interface Test {}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void withinPre_hasAnnotations() {
    assumeFalse(RuntimeVersion.isAtLeast15()); // https://bugs.openjdk.java.net/browse/JDK-8241780
    refactoring
        .addInputLines(
            "Test.java",
            "/**",
            "  * Foo",
            "  *",
            "  * <pre>",
            "  *   @Override",
            "  *   ImmutableMap<String, Object>",
            "  * </pre>",
            "  */",
            "interface Test {}")
        .addOutputLines(
            "Test.java",
            "/**",
            "  * Foo",
            "  *",
            "  * <pre>",
            "  *   @Override",
            "  *   ImmutableMap&lt;String, Object&gt;",
            "  * </pre>",
            "  */",
            "interface Test {}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void escapesWithoutAddingCodeBlock_withinPreBlockWithAnnotation() {
    assumeFalse(RuntimeVersion.isAtLeast15()); // https://bugs.openjdk.java.net/browse/JDK-8241780
    refactoring
        .addInputLines(
            "Test.java",
            "/**",
            "  * Foo",
            "  *",
            "  * <pre>",
            "  *  {@literal @}Override",
            "  *   ImmutableMap<String, Object>",
            "  * </pre>",
            "  */",
            "interface Test {}")
        .addOutputLines(
            "Test.java",
            "/**",
            "  * Foo",
            "  *",
            "  * <pre>",
            "  *  {@literal @}Override",
            "  *   ImmutableMap&lt;String, Object&gt;",
            "  * </pre>",
            "  */",
            "interface Test {}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void correctFindingPosition_withinPreBlock() {
    helper
        .addSourceLines(
            "Test.java",
            "/**",
            "  * Foo",
            "  *",
            "  * <pre>",
            "  *  {@literal @}Override",
            "  // BUG: Diagnostic contains: UnescapedEntity",
            "  *   ImmutableMap<String, Object>",
            "  * </pre>",
            "  */",
            "interface Test {}")
        .doTest();
  }

  @Test
  public void withinLink() {
    helper
        .addSourceLines(
            "Test.java", //
            "/** {@link List<Foo>} */",
            "interface Test {}")
        .doTest();
  }

  @Test
  public void withinSee() {
    helper
        .addSourceLines(
            "Test.java", //
            "import java.util.List;",
            "interface Test {",
            "  /** @see #foo(List<Integer>) */",
            "  void foo(List<Integer> foos);",
            "}")
        .doTest();
  }

  @Test
  public void badSee() {
    helper
        .addSourceLines(
            "Test.java", //
            "import java.util.List;",
            "interface Test {",
            "  /** @see <a href=\"http://google.com\">google</a> */",
            "  void foo(List<Integer> foos);",
            "}")
        .doTest();
  }

  @Test
  public void extraClosingTag() {
    refactoring
        .addInputLines(
            "Test.java", //
            "/** <pre>Foo List<Foo> bar</pre></pre> */",
            "interface Test {}")
        .addOutputLines(
            "Test.java", //
            "/** <pre>{@code Foo List<Foo> bar}</pre></pre> */",
            "interface Test {}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void nestedGenericType_properlyEscaped() {
    refactoring
        .addInputLines(
            "Test.java", //
            "/** List<List<Integer>> */",
            "interface Test {}")
        .addOutputLines(
            "Test.java", //
            "/** {@code List<List<Integer>>} */",
            "interface Test {}")
        .doTest(TestMode.TEXT_MATCH);
  }
}
