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

import static com.google.common.truth.TruthJUnit.assume;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link UnescapedEntity} bug pattern. */
@RunWith(JUnit4.class)
public final class UnescapedEntityTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(UnescapedEntity.class, getClass());
  private final BugCheckerRefactoringTestHelper refactoring =
      BugCheckerRefactoringTestHelper.newInstance(UnescapedEntity.class, getClass());

  @Test
  public void positive() {
    refactoring
        .addInputLines(
            "Test.java",
            """
            /** List<Foo>, Map<Foo, Bar> */
            interface Test {}
            """)
        .addOutputLines(
            "Test.java",
            """
            /** {@code List<Foo>}, {@code Map<Foo, Bar>} */
            interface Test {}
            """)
        .doTest();
  }

  @Test
  public void negative() {
    helper
        .addSourceLines(
            "Test.java",
            """
            /** {@code List<Foo>, Map<Foo, Bar>} */
            interface Test {}
            """)
        .doTest();
  }

  @Test
  public void unescapedEntities_off() {
    refactoring
        .addInputLines(
            "Test.java",
            """
            /** Foo & bar < */
            interface Test {}
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void withinPre() {
    refactoring
        .addInputLines(
            "Test.java",
            """
            /**
             * <pre>Foo</pre>
             *
             * <pre>Use an ImmutableMap<String,Object> please</pre>
             *
             * <pre>bar</pre>
             */
            interface Test {}
            """)
        .addOutputLines(
            "Test.java",
            """
            /**
             * <pre>Foo</pre>
             *
             * <pre>{@code Use an ImmutableMap<String,Object> please}</pre>
             *
             * <pre>bar</pre>
             */
            interface Test {}
            """)
        .doTest();
  }

  @Test
  public void withinPre_singleChar() {
    refactoring
        .addInputLines(
            "Test.java",
            """
            /** <pre>n < 3</pre> */
            interface Test {}
            """)
        .addOutputLines(
            "Test.java",
            """
            /** <pre>{@code n < 3}</pre> */
            interface Test {}
            """)
        .doTest();
  }

  @Test
  public void withinPre_alreadyEscaped() {
    assume()
        .that(Runtime.version().feature())
        .isLessThan(15); // https://bugs.openjdk.java.net/browse/JDK-8241780
    refactoring
        .addInputLines(
            "Test.java",
            """
            /** <pre>Use an ImmutableMap<String, Object> not a Map&lt;String, Object&gt;</pre> */
            interface Test {}
            """)
        .addOutputLines(
            "Test.java",
            """
            /** <pre>Use an ImmutableMap&lt;String, Object&gt; not a Map&lt;String, Object&gt;</pre> */
            interface Test {}
            """)
        .doTest();
  }

  @Test
  public void withinPre_hasAnnotations() {
    assume()
        .that(Runtime.version().feature())
        .isLessThan(15); // https://bugs.openjdk.java.net/browse/JDK-8241780
    refactoring
        .addInputLines(
            "Test.java",
            """
            /**
             * Foo
             *
             * <pre>
             *   @Override
             *   ImmutableMap<String, Object>
             * </pre>
             */
            interface Test {}
            """)
        .addOutputLines(
            "Test.java",
            """
            /**
             * Foo
             *
             * <pre>
             *   @Override
             *   ImmutableMap&lt;String, Object&gt;
             * </pre>
             */
            interface Test {}
            """)
        .doTest();
  }

  @Test
  public void escapesWithoutAddingCodeBlock_withinPreBlockWithAnnotation() {
    assume()
        .that(Runtime.version().feature())
        .isLessThan(15); // https://bugs.openjdk.java.net/browse/JDK-8241780
    refactoring
        .addInputLines(
            "Test.java",
            """
            /**
             * Foo
             *
             * <pre>
             *  {@literal @}Override
             *   ImmutableMap<String, Object>
             * </pre>
             */
            interface Test {}
            """)
        .addOutputLines(
            "Test.java",
            """
            /**
             * Foo
             *
             * <pre>
             *  {@literal @}Override
             *   ImmutableMap&lt;String, Object&gt;
             * </pre>
             */
            interface Test {}
            """)
        .doTest();
  }

  @Test
  public void correctFindingPosition_withinPreBlock() {
    helper
        .addSourceLines(
            "Test.java",
            """
            /**
             * Foo
             *
             * <pre>
             *  {@literal @}Override
             * // BUG: Diagnostic contains: UnescapedEntity
             *   ImmutableMap<String, Object>
             * </pre>
             */
            interface Test {}
            """)
        .doTest();
  }

  @Test
  public void withinLink() {
    helper
        .addSourceLines(
            "Test.java",
            """
            /** {@link List<Foo>} */
            interface Test {}
            """)
        .doTest();
  }

  @Test
  public void withinSee() {
    helper
        .addSourceLines(
            "Test.java",
            """
            import java.util.List;

            interface Test {
              /**
               * @see #foo(List<Integer>)
               */
              void foo(List<Integer> foos);
            }
            """)
        .doTest();
  }

  @Test
  public void badSee() {
    helper
        .addSourceLines(
            "Test.java",
            """
            import java.util.List;

            interface Test {
              /**
               * @see <a href="https://google.com">google</a>
               */
              void foo(List<Integer> foos);
            }
            """)
        .doTest();
  }

  @Test
  public void extraClosingTag() {
    refactoring
        .addInputLines(
            "Test.java",
            """
            /**
             * <pre>Foo List<Foo> bar</pre>
             *
             * </pre>
             */
            interface Test {}
            """)
        .addOutputLines(
            "Test.java",
            """
            /**
             * <pre>{@code Foo List<Foo> bar}</pre>
             *
             * </pre>
             */
            interface Test {}
            """)
        .doTest();
  }

  @Test
  public void nestedGenericType_properlyEscaped() {
    refactoring
        .addInputLines(
            "Test.java",
            """
            /** List<List<Integer>> */
            interface Test {}
            """)
        .addOutputLines(
            "Test.java",
            """
            /** {@code List<List<Integer>>} */
            interface Test {}
            """)
        .doTest();
  }

  @Test
  public void markdownJavadoc() {
    assume().that(Runtime.version().feature()).isAtLeast(23); // Markdown Javadoc is JDK 23+
    refactoring
        .addInputLines(
            "Test.java",
            """
            /// A command-line flag that parses arguments into a byte size represented as a `Flag<Long>`.
            interface Test {}
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void markdownJavadocCodeBlock() {
    assume().that(Runtime.version().feature()).isAtLeast(23); // Markdown Javadoc is JDK 23+
    refactoring
        .addInputLines(
            "Test.java",
            """
            /// ```java
            /// List<String> list;
            /// ```
            interface Test {}
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void markdownJavadocUnmatchedBacktick() {
    assume().that(Runtime.version().feature()).isAtLeast(23); // Markdown Javadoc is JDK 23+
    refactoring
        .addInputLines(
            "Test.java",
            """
            /// List<String> ` unmatched
            interface Test {}
            """)
        // TODO(b/530215233): This should suggest wrapping in backticks:
        // /// `List<String>` ` unmatched
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void markdownJavadocTildeCodeBlock() {
    assume().that(Runtime.version().feature()).isAtLeast(23); // Markdown Javadoc is JDK 23+
    refactoring
        .addInputLines(
            "Test.java",
            """
            /// ~~~java
            /// List<String> list;
            /// ~~~
            interface Test {}
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void markdownJavadocRawHtml() {
    assume().that(Runtime.version().feature()).isAtLeast(23); // Markdown Javadoc is JDK 23+
    refactoring
        .addInputLines(
            "Test.java",
            """
            /// <code>List<String></code>
            interface Test {}
            """)
        // TODO(b/530215233): This should suggest wrapping in {@code}:
        // /// <code>{@code List<String>}</code>
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void markdownJavadocSingleTildeNotCode() {
    assume().that(Runtime.version().feature()).isAtLeast(23); // Markdown Javadoc is JDK 23+
    refactoring
        .addInputLines(
            "Test.java",
            """
            /// ~List<String>~
            interface Test {}
            """)
        // TODO(b/530215233): This should suggest wrapping in backticks:
        // /// ~`List<String>`~
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void markdownJavadocMultipleCodeSpans() {
    assume().that(Runtime.version().feature()).isAtLeast(23); // Markdown Javadoc is JDK 23+
    refactoring
        .addInputLines(
            "Test.java",
            """
            /// `List<String>` and `Map<String, Integer>`
            interface Test {}
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void markdownJavadocMixedCodeSpanAndRawGeneric() {
    assume().that(Runtime.version().feature()).isAtLeast(23); // Markdown Javadoc is JDK 23+
    refactoring
        .addInputLines(
            "Test.java",
            """
            /// `List<String>` but raw Map<String, Integer>
            interface Test {}
            """)
        // TODO(b/530215233): This should suggest wrapping in backticks:
        // /// `List<String>` but raw `Map<String, Integer>`
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void markdownJavadocEscapedBacktick() {
    assume().that(Runtime.version().feature()).isAtLeast(23); // Markdown Javadoc is JDK 23+
    refactoring
        .addInputLines(
            "Test.java",
            """
            /// A literal backtick \\` and raw List<String> \\` more
            interface Test {}
            """)
        // TODO(b/530215233): This should suggest wrapping in backticks:
        // /// A literal backtick \\` and raw `List<String>` \\` more
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void nestedHtmlTagsInCodeHtml() {
    refactoring
        .addInputLines(
            "Test.java",
            """
            /** <code>List<String> <b>bold</b></code> */
            interface Test {}
            """)
        // TODO(b/530215233): This should be expectUnchanged() because we shouldn't wrap
        // nested HTML tags in {@code} which would escape them.
        .addOutputLines(
            "Test.java",
            """
            /** <code>{@code List<String> <b>bold</b>}</code> */
            interface Test {}
            """)
        .doTest();
  }

  @Test
  public void markdownJavadocNestedHtmlTagsInCodeHtml() {
    assume().that(Runtime.version().feature()).isAtLeast(23); // Markdown Javadoc is JDK 23+
    refactoring
        .addInputLines(
            "Test.java",
            """
            /// <code>List<String> <b>bold</b></code>
            interface Test {}
            """)
        .expectUnchanged()
        .doTest();
  }
}
