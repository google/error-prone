/*
 * Copyright 2026 The Error Prone Authors.
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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@SuppressWarnings("MisformattedTestData")
@RunWith(JUnit4.class)
public final class TraditionalJavadocToMarkdownTest {
  private static final boolean MARKDOWN_JAVADOC_SUPPORTED = Runtime.version().feature() >= 23;
  private static final boolean EXTRA_MARKDOWN_SPACE_GONE = Runtime.version().feature() >= 25;
  private final BugCheckerRefactoringTestHelper helper =
      BugCheckerRefactoringTestHelper.newInstance(TraditionalJavadocToMarkdown.class, getClass());

  @Before
  public void setUp() {
    assume().that(MARKDOWN_JAVADOC_SUPPORTED).isTrue();
  }

  @Test
  public void enumJavadoc() {
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * An enum with Javadoc.
             */
            public enum Test {
              FOO, BAR;
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            /// An enum with Javadoc.
            public enum Test {
              FOO, BAR;
            }
            """)
        .doTest();
  }

  @Test
  public void enumConstantJavadoc() {
    helper
        .addInputLines(
            "Test.java",
            """
            public enum Test {
              /**
               * Javadoc for {@code FOO}.
               */
              FOO,
              /**
               * Javadoc for {@code BAR}.
               */
              BAR;
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            public enum Test {
              /// Javadoc for `FOO`.
              FOO,
              /// Javadoc for `BAR`.
              BAR;
            }
            """)
        .doTest();
  }

  @Test
  public void enumConstantWithBodyJavadoc() {
    helper
        .addInputLines(
            "Test.java",
            """
            public enum Test {
              /**
               * Javadoc for FOO.
               */
              FOO {
                /**
                 * Returns the string representation of {@code FOO}.
                 */
                @Override
                public String toString() {
                  return "foo";
                }
              },
              /**
               * Javadoc for {@code BAR}.
               */
              BAR;
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            public enum Test {
              /// Javadoc for FOO.
              FOO {
                /// Returns the string representation of `FOO`.
                @Override
                public String toString() {
                  return "foo";
                }
              },
              /// Javadoc for `BAR`.
              BAR;
            }
            """)
        .doTest();
  }

  @Test
  public void simpleComment() {
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * A simple comment.
             */
            public class Test {}
            """)
        .addOutputLines(
            "Test.java",
            """
            /// A simple comment.
            public class Test {}
            """)
        .doTest();
  }

  @Test
  public void inlineTags() {
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * A comment with {@code code} and {@link String} and {@literal literal}.
             *
             * <p>A {@link String
             * link} that spans multiple lines.
             */
            public class Test {}
            """)
        .addOutputLines(
            "Test.java",
            """
            /// A comment with `code` and [String] and {@literal literal}.
            ///
            /// A [`link`][String] that spans multiple lines.
            public class Test {}
            """)
        .doTest();
  }

  @Test
  public void preWithNestedCode() {
    assume().that(EXTRA_MARKDOWN_SPACE_GONE).isTrue();
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * <pre><code>
             * int x = 1;
             * </code></pre>
             */
            public class Test {}
            """)
        .addOutputLines(
            "Test.java",
            """
            /// ```
            /// int x = 1;
            /// ```
            public class Test {}
            """)
        .doTest();
  }

  @Test
  public void preWithNestedCode_mockitoExample() {
    assume().that(EXTRA_MARKDOWN_SPACE_GONE).isTrue();
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * <pre class="code"><code class="java">
             * MockitoAnnotations.openMocks(testClass);
             * </code></pre>
             */
            public class Test {}
            """)
        .addOutputLines(
            "Test.java",
            """
            /// ```
            /// MockitoAnnotations.openMocks(testClass);
            /// ```
            public class Test {}
            """)
        .doTest();
  }

  @Test
  public void linkWithLabel() {
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * A comment with {@link String label} and {@linkplain Integer plain}.
             */
            public class Test {}
            """)
        .addOutputLines(
            "Test.java",
            """
            /// A comment with [`label`][String] and [plain][Integer].
            public class Test {}
            """)
        .doTest();
  }

  @Test
  public void htmlTags() {
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * <b>Bold</b> and <i>italic</i>.
             * <p>New paragraph.
             * <ul><li>Item</li></ul>
             */
            public class Test {}
            """)
        .addOutputLines(
            "Test.java",
            """
            /// **Bold** and *italic*.
            ///
            /// New paragraph.
            ///
            /// - Item
            public class Test {}
            """)
        .doTest();
  }

  @Test
  public void blockTags() {
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * Foo.
             * @param x description
             * @return nothing
             */
            public class Test {
              public void foo(int x) {}
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            /// Foo.
            ///
            /// @param x description
            /// @return nothing
            public class Test {
              public void foo(int x) {}
            }
            """)
        .doTest();
  }

  @Test
  public void headings() {
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * <h1>Header 1</h1>
             * <h2>Header 2</h2>
             * <h3>Header 3</h3>
             * <h4>Header 4</h4>
             * <h5>Header 5</h5>
             * <h6>Header 6</h6>
             */
            public class Test {}
            """)
        .addOutputLines(
            "Test.java",
            """
            /// # Header 1
            ///
            /// ## Header 2
            ///
            /// ### Header 3
            ///
            /// #### Header 4
            ///
            /// ##### Header 5
            ///
            /// ###### Header 6
            public class Test {}
            """)
        .doTest();
  }

  @Test
  public void headingsNormalized_startingAtH2() {
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * <h2>Header 2</h2>
             * <h3>Header 3</h3>
             * <h4>Header 4</h4>
             */
            public class Test {}
            """)
        .addOutputLines(
            "Test.java",
            """
            /// # Header 2
            ///
            /// ## Header 3
            ///
            /// ### Header 4
            public class Test {}
            """)
        .doTest();
  }

  @Test
  public void headingsNormalized_startingAtH3() {
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * <h3>Header 3</h3>
             * <h4>Header 4</h4>
             * <h5>Header 5</h5>
             * <h6>Header 6</h6>
             */
            public class Test {}
            """)
        .addOutputLines(
            "Test.java",
            """
            /// # Header 3
            ///
            /// ## Header 4
            ///
            /// ### Header 5
            ///
            /// #### Header 6
            public class Test {}
            """)
        .doTest();
  }

  @Test
  public void headingsNormalized_startingAtH4() {
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * <h4>Header 4</h4>
             * <h5>Header 5</h5>
             */
            public class Test {}
            """)
        .addOutputLines(
            "Test.java",
            """
            /// # Header 4
            ///
            /// ## Header 5
            public class Test {}
            """)
        .doTest();
  }

  @Test
  public void headingsNormalized_skippedLevels() {
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * <h3>Section 1</h3>
             * <h5>Sub-subsection 1.1.1</h5>
             */
            public class Test {}
            """)
        .addOutputLines(
            "Test.java",
            """
            /// # Section 1
            ///
            /// ### Sub-subsection 1.1.1
            public class Test {}
            """)
        .doTest();
  }

  @Test
  public void headingsNormalized_multipleSameLevel() {
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * <h3>First Section</h3>
             * <h4>First Subsection</h4>
             * <h3>Second Section</h3>
             * <h4>Second Subsection</h4>
             */
            public class Test {}
            """)
        .addOutputLines(
            "Test.java",
            """
            /// # First Section
            ///
            /// ## First Subsection
            ///
            /// # Second Section
            ///
            /// ## Second Subsection
            public class Test {}
            """)
        .doTest();
  }

  @Test
  public void headingsNormalized_outOfOrder() {
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * <h4>Subsection</h4>
             * <h3>Main Section</h3>
             */
            public class Test {}
            """)
        .addOutputLines(
            "Test.java",
            """
            /// ## Subsection
            ///
            /// # Main Section
            public class Test {}
            """)
        .doTest();
  }

  @Test
  public void headingsNormalized_inSeeDoesNotAffectMinLevel() {
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * <h3>Real Section</h3>
             *
             * @see <h1>Not A Real Heading</h1>
             */
            public class Test {}
            """)
        // NOTE: technically this is a bug because we (incorrectly) identify the <h1> inside the
        // @see as a heading, but that's unlikely to occur in practice.
        .addOutputLines(
            "Test.java",
            """
            /// ### Real Section
            ///
            /// @see <h1>Not A Real Heading</h1>
            public class Test {}
            """)
        .doTest();
  }

  @Test
  public void headingsNormalized_inLinkDoesNotAffectMinLevel() {
    assume().that(EXTRA_MARKDOWN_SPACE_GONE).isTrue();
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * <h3>Real Section</h3>
             *
             * {@link Foo <h1>Not A Real Heading</h1>}
             * @see {@link Foo <h1>Not A Real Heading</h1>}
             */
            public class Test {}
            """)
        // NOTE: technically this is a bug because we (incorrectly) identify the <h1> inside the
        // {@link} as a heading, but that's unlikely to occur in practice.
        .addOutputLines(
            "Test.java",
            """
            /// ### Real Section
            ///
            /// [`# Not A Real Heading`][Foo]
            ///
            /// @see {@link Foo <h1>Not A Real Heading</h1>}
            public class Test {}
            """)
        .doTest();
  }

  @Test
  public void headingsNormalized_inCodeDoesNotAffectMinLevel() {
    assume().that(EXTRA_MARKDOWN_SPACE_GONE).isTrue();
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * <h3>Real Section</h3>
             *
             * {@code <h1>Not A Real Heading</h1>}
             * {@literal <h2>Also Not A Heading</h2>}
             */
            public class Test {}
            """)
        .addOutputLines(
            "Test.java",
            """
            /// # Real Section
            ///
            /// `<h1>Not A Real Heading</h1>`
            /// {@literal <h2>Also Not A Heading</h2>}
            public class Test {}
            """)
        .doTest();
  }

  @Test
  public void headingsNormalized_startingAtH5() {
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * <h5>Header 5</h5>
             * <h6>Header 6</h6>
             */
            public class Test {}
            """)
        .addOutputLines(
            "Test.java",
            """
            /// # Header 5
            ///
            /// ## Header 6
            public class Test {}
            """)
        .doTest();
  }

  @Test
  public void headingsNormalized_startingAtH6() {
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * <h6>Header 6</h6>
             */
            public class Test {}
            """)
        .addOutputLines(
            "Test.java",
            """
            /// # Header 6
            public class Test {}
            """)
        .doTest();
  }

  @Test
  public void headingsNormalized_withAttributes() {
    // TODO(kak): Add support for attributes.
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * <h3 id="section" class="title">Section Title</h3>
             * <h4>Subsection</h4>
             */
            public class Test {}
            """)
        .addOutputLines(
            "Test.java",
            """
            /// # Section Title
            ///
            /// ## Subsection
            public class Test {}
            """)
        .doTest();
  }

  @Test
  public void methodJavadoc() {
    helper
        .addInputLines(
            "Test.java",
            """
            public class Test {
              /**
               * Returns the sum of x and y.
               *
               * @param x the first value
               * @param y the second value
               * @return the sum
               */
              public int add(int x, int y) {
                return x + y;
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            public class Test {
              /// Returns the sum of x and y.
              ///
              /// @param x the first value
              /// @param y the second value
              /// @return the sum
              public int add(int x, int y) {
                return x + y;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void complexMethodJavadoc() {
    assume().that(EXTRA_MARKDOWN_SPACE_GONE).isTrue();
    helper
        .addInputLines(
            "Test.java",
            """
            import java.time.Duration;
            public class Test {
              /**
               * Returns the number of seconds of the given duration as a {@code double}. This method should be
               * used to accommodate APIs that <b>only</b> accept durations as {@code double} values.
               *
               * <p>This conversion may lose precision.
               *
               * <p>If you need the number of seconds in this duration as a {@code long} (not a {@code double}),
               * simply use {@code duration.getSeconds()}.
               */
              @SuppressWarnings("DurationSecondsToDouble") // that's the whole point of this method...
              public static double toSecondsAsDouble(Duration duration) {
                return duration.getSeconds() + duration.getNano() / 1e9;
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.time.Duration;
            public class Test {
              /// Returns the number of seconds of the given duration as a `double`. This method should be
              /// used to accommodate APIs that **only** accept durations as `double` values.
              ///
              /// This conversion may lose precision.
              ///
              /// If you need the number of seconds in this duration as a `long` (not a `double`),
              /// simply use `duration.getSeconds()`.
              @SuppressWarnings("DurationSecondsToDouble") // that's the whole point of this method...
              public static double toSecondsAsDouble(Duration duration) {
                return duration.getSeconds() + duration.getNano() / 1e9;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void authorTag() {
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * An object which accepts requests to put the current thread to sleep.
             *
             * @author kak@google.com (Kurt Alfred Kluever)
             */
            public class Test {}
            """)
        .addOutputLines(
            "Test.java",
            """
            /// An object which accepts requests to put the current thread to sleep.
            ///
            /// @author kak@google.com (Kurt Alfred Kluever)
            public class Test {}
            """)
        .doTest();
  }

  @Test
  public void snippetTag() {
    assume().that(EXTRA_MARKDOWN_SPACE_GONE).isTrue();
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * A builder for a cache.
             *
             * <p>Usage example:
             *
             * {@snippet :
             * int maxSize = 10000;
             *
             * LoadingCache<Key, Graph> graphs = CacheBuilder.newBuilder()
             *     .maximumSize(maxSize)
             *     .build(
             *         new CacheLoader<Key, Graph>() {
             *           public Graph load(Key key) throws AnyException {
             *             return createExpensiveGraph(key);
             *           }
             *         });
             * }
             */
            public class Test {}
            """)
        .addOutputLines(
            "Test.java",
            """
            /// A builder for a cache.
            ///
            /// Usage example:
            ///
            /// ```
            /// int maxSize = 10000;
            ///
            /// LoadingCache<Key, Graph> graphs = CacheBuilder.newBuilder()
            ///     .maximumSize(maxSize)
            ///     .build(
            ///         new CacheLoader<Key, Graph>() {
            ///           public Graph load(Key key) throws AnyException {
            ///             return createExpensiveGraph(key);
            ///           }
            ///         });
            /// ```
            public class Test {}
            """)
        .doTest();
  }

  @Test
  public void snippetTag_withLangAttribute() {
    assume().that(EXTRA_MARKDOWN_SPACE_GONE).isTrue();
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * {@snippet lang=java :
             * int age = 42;
             * }
             */
            public class Test {}
            """)
        .addOutputLines(
            "Test.java",
            """
            /// ```java
            /// int age = 42;
            /// ```
            public class Test {}
            """)
        .doTest();
  }

  @Test
  public void snippetTag_withOtherAttributes() {
    assume().that(EXTRA_MARKDOWN_SPACE_GONE).isTrue();
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * {@snippet id="ref-1" :
             * int age = 42;
             * }
             */
            public class Test {}
            """)
        .addOutputLines(
            "Test.java",
            """
            /// {@snippet id="ref-1" :
            /// int age = 42;
            /// }
            public class Test {}
            """)
        .doTest();
  }

  @Test
  public void snippetTag_withLangAndOtherAttributes() {
    assume().that(EXTRA_MARKDOWN_SPACE_GONE).isTrue();
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * {@snippet lang=java id="ref-1" :
             * int age = 42;
             * }
             */
            public class Test {}
            """)
        .addOutputLines(
            "Test.java",
            """
            /// {@snippet lang=java id="ref-1" :
            /// int age = 42;
            /// }
            public class Test {}
            """)
        .doTest();
  }

  @Test
  public void snippetTag_external() {
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * {@snippet class="ExternalFile.java" region="example"}
             */
            public class Test {}
            """)
        .addOutputLines(
            "Test.java",
            """
            /// {@snippet class="ExternalFile.java" region="example"}
            public class Test {}
            """)
        .doTest();
  }

  @Test
  public void preCodeTag() {
    assume().that(EXTRA_MARKDOWN_SPACE_GONE).isTrue();
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * A builder for a cache.
             *
             * <pre>{@code
             * Optional<T> foo = maybeFoo();
             * foo.ifPresent(x -> handle(x, 1));
             * }</pre>
             */
            public class Test {}
            """)
        .addOutputLines(
            "Test.java",
            """
            /// A builder for a cache.
            ///
            /// ```
            /// Optional<T> foo = maybeFoo();
            /// foo.ifPresent(x -> handle(x, 1));
            /// ```
            public class Test {}
            """)
        .doTest();
  }

  @Test
  public void preWithHtmlAndEscapedGenerics() {
    assume().that(EXTRA_MARKDOWN_SPACE_GONE).isTrue();
    // Note: Within a <pre> block, newlines are significant and text is rendered with a code font.
    // Within a ``` block, both of those things are true, but also Markdown and HTML constructs
    // are not recognized. This is difficult to fix though, but hopefully fairly uncommon.
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * <pre>
             * This really is <i>italic</i> and <b>bold</b>.
             * Optional&lt;T&gt;
             * </pre>
             */
            public class Test {}
            """)
        .addOutputLines(
            "Test.java",
            """
            /// ```
            /// This really is *italic* and **bold**.
            /// Optional&lt;T&gt;
            /// ```
            public class Test {}
            """)
        .doTest();
  }

  @Test
  public void preWithLink() {
    assume().that(EXTRA_MARKDOWN_SPACE_GONE).isTrue();
    // Note: In traditional Javadoc <pre>, <a> tags render as clickable links.
    // In Markdown Javadoc ``` blocks, HTML tags and Markdown links are rendered as literal text.
    // Ideally, links inside <pre> should either be preserved as clickable (if it's not actually
    // code) or converted to plain text if it is code.
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * <pre>
             * See <a href="http://example.com">this link</a>
             * </pre>
             */
            public class Test {}
            """)
        .addOutputLines(
            "Test.java",
            """
            /// ```
            /// See [this link](http://example.com)
            /// ```
            public class Test {}
            """)
        .doTest();
  }

  @Test
  public void preWithLineBreak() {
    assume().that(EXTRA_MARKDOWN_SPACE_GONE).isTrue();
    // Note: In traditional Javadoc <pre>, <br> tags render as newlines.
    // Ideally, these should be converted to actual newlines in the Markdown output.
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * <pre>
             * Line 1<br>Line 2
             * </pre>
             */
            public class Test {}
            """)
        .addOutputLines(
            "Test.java",
            """
            /// ```
            /// Line 1<br>Line 2
            /// ```
            public class Test {}
            """)
        .doTest();
  }

  @Test
  public void preWithBareEntities() {
    assume().that(EXTRA_MARKDOWN_SPACE_GONE).isTrue();
    // Note: In traditional Javadoc <pre>, &lt; renders as <.
    // In Markdown Javadoc ``` blocks, HTML entities are rendered literally (as &lt;).
    // Ideally, HTML entities should be decoded (e.g., to <) when placed inside a Markdown code
    // block.
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * <pre>
             * A &lt; B
             * </pre>
             */
            public class Test {}
            """)
        .addOutputLines(
            "Test.java",
            """
            /// ```
            /// A &lt; B
            /// ```
            public class Test {}
            """)
        .doTest();
  }

  @Test
  public void otherBlockTags() {
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * Some text.
             *
             * @since 1.0
             * @version 2.0
             * @deprecated Use something else.
             * @see java.util.List
             * @custom Custom tag content.
             */
            public class Test {}
            """)
        .addOutputLines(
            "Test.java",
            """
            /// Some text.
            ///
            /// @since 1.0
            /// @version 2.0
            /// @deprecated Use something else.
            /// @see java.util.List
            /// @custom Custom tag content.
            public class Test {}
            """)
        .doTest();
  }

  @Test
  public void anchorTag() {
    assume().that(EXTRA_MARKDOWN_SPACE_GONE).isTrue();
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * When you want a zone-dependent clock, you may want to consider using {@link java.time.Clock}.
             * However, please read <a href="http://www.google.com">this link</a>.
             */
            public class Test {}
            """)
        .addOutputLines(
            "Test.java",
            """
            /// When you want a zone-dependent clock, you may want to consider using [java.time.Clock].
            /// However, please read [this link](http://www.google.com).
            public class Test {}
            """)
        .doTest();
  }

  @Test
  public void listAndParagraphs() {
    assume().that(EXTRA_MARKDOWN_SPACE_GONE).isTrue();
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * Static utility methods pertaining to {@link Duration} instances.
             *
             * <p>Use the following methods to create a {@link Duration}:
             *
             * <ul>
             *   <li>{@link Duration#ofDays(long)} if you have a long of days
             *   <li>{@link Duration#ofHours(long)} if you have a long of hours
             *   <li>{@link Duration#ofMinutes(long)} if you have a long of minutes
             *   <li>{@link Duration#ofSeconds(long)} if you have a long of seconds
             *   <li>{@link Duration#ofSeconds(long, long)} if you have a long of seconds and a nanosecond
             *       adjustment
             *   <li>{@link Durations#ofSeconds(double)} if you have a double of seconds
             *   <li>{@link Duration#ofMillis(long)} if you have a long of milliseconds
             *   <li>{@link Durations#ofMicros(long)} if you have a long of microseconds
             *   <li>{@link Duration#ofNanos(long)} if you have a long of nanoseconds
             * </ul>
             */
            public class Test {}
            """)
        .addOutputLines(
            "Test.java",
            """
            /// Static utility methods pertaining to [Duration] instances.
            ///
            /// Use the following methods to create a [Duration]:
            ///
            /// - [Duration#ofDays(long)] if you have a long of days
            /// - [Duration#ofHours(long)] if you have a long of hours
            /// - [Duration#ofMinutes(long)] if you have a long of minutes
            /// - [Duration#ofSeconds(long)] if you have a long of seconds
            /// - [Duration#ofSeconds(long, long)] if you have a long of seconds and a nanosecond
            ///       adjustment
            /// - [Durations#ofSeconds(double)] if you have a double of seconds
            /// - [Duration#ofMillis(long)] if you have a long of milliseconds
            /// - [Durations#ofMicros(long)] if you have a long of microseconds
            /// - [Duration#ofNanos(long)] if you have a long of nanoseconds
            public class Test {}
            """)
        .doTest();
  }

  @Test
  public void methodJavadocWithParagraphAndThrows() {
    assume().that(EXTRA_MARKDOWN_SPACE_GONE).isTrue();
    helper
        .addInputLines(
            "Test.java",
            """
            public class Test {
              /**
               * Returns the number of microseconds of the given duration. If that number is too large to fit in
               * a long, then an exception is thrown.
               *
               * <p>If the given duration has greater than microsecond precision, then the conversion will drop
               * any excess precision information as though the amount in nanoseconds was subject to integer
               * division by one thousand.
               *
               * @throws ArithmeticException if numeric overflow occurs during conversion
               */
              public static long toMicros(java.time.Duration duration) {
                return 0;
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            public class Test {
              /// Returns the number of microseconds of the given duration. If that number is too large to fit in
              /// a long, then an exception is thrown.
              ///
              /// If the given duration has greater than microsecond precision, then the conversion will drop
              /// any excess precision information as though the amount in nanoseconds was subject to integer
              /// division by one thousand.
              ///
              /// @throws ArithmeticException if numeric overflow occurs during conversion
              public static long toMicros(java.time.Duration duration) {
                return 0;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void splitLinkTag() {
    assume().that(EXTRA_MARKDOWN_SPACE_GONE).isTrue();
    helper
        .addInputLines(
            "Test.java",
            """
            public class Test {
              /**
               * Returns a {@link Duration} representing the given number of seconds, positive or negative.
               *
               * <p><b>Note:</b> If {@code seconds} is {@link Double#POSITIVE_INFINITY} or larger than the
               * maximum capacity of a duration, {@link Durations#MAX} is returned. If {@code seconds} is {@link
               * Double#NEGATIVE_INFINITY} or smaller than the minimum capacity of a duration, the smallest
               * representable duration is returned.
               *
               * @throws ArithmeticException if {@code seconds} is {@link Double#NaN}
               */
              public static long toSeconds(java.time.Duration duration) {
                return 0;
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            public class Test {
              /// Returns a [Duration] representing the given number of seconds, positive or negative.
              ///
              /// **Note:** If `seconds` is [Double#POSITIVE_INFINITY] or larger than the
              /// maximum capacity of a duration, [Durations#MAX] is returned. If `seconds` is [Double#NEGATIVE_INFINITY] or smaller than the minimum capacity of a duration, the smallest
              /// representable duration is returned.
              ///
              /// @throws ArithmeticException if `seconds` is [Double#NaN]
              public static long toSeconds(java.time.Duration duration) {
                return 0;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void hundredColumnBug() {
    assume().that(EXTRA_MARKDOWN_SPACE_GONE).isTrue();
    // This line is exactly 100 characters long (including indentation).
    // It ends with "{@link" at column 100.
    helper
        .addInputLines(
            "Test.java",
            """
            package test;
            import java.time.Duration;
            public class Test {
              /**
               * maximum capacity of a duration, {@link Duration} is returned. If {@code seconds} is {@link
               * Double#NEGATIVE_INFINITY} or smaller than the minimum capacity of a duration, the smallest
               * representable duration is returned.
               */
              public void test() {}
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            package test;
            import java.time.Duration;
            public class Test {
              /// maximum capacity of a duration, [Duration] is returned. If `seconds` is [Double#NEGATIVE_INFINITY] or smaller than the minimum capacity of a duration, the smallest
              /// representable duration is returned.
              public void test() {}
            }
            """)
        .doTest();
  }

  @Test
  public void linkTags() {
    helper
        .addInputLines(
            "Test.java",
            """
            package test;

            public class Test {
              /**
               * Test a few link tags:
               *
               * <ul>
               *   <li>This is a link to {@link #bar()} with trailing parens.
               *   <li>This is a link to {@link #bar} without trailing parens.
               *   <li>This is a linkplain to {@linkplain #bar()} with trailing parens.
               *   <li>This is a linkplain to {@linkplain #bar} without trailing parens.
               *   <li>This is a link to {@link Object#toString()} with trailing parens.
               *   <li>This is a link to {@link Object#toString} without trailing parens.
               *   <li>This is a linkplain to {@linkplain Object#toString()} with trailing parens.
               *   <li>This is a linkplain to {@linkplain Object#toString} without trailing parens.
               * </ul>
               */
              public void foo() {}

              public void bar() {}
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            package test;

            public class Test {
              /// Test a few link tags:
              ///
              /// - This is a link to [#bar()] with trailing parens.
              /// - This is a link to [#bar] without trailing parens.
              /// - This is a linkplain to [#bar()] with trailing parens.
              /// - This is a linkplain to [#bar] without trailing parens.
              /// - This is a link to [Object#toString()] with trailing parens.
              /// - This is a link to [Object#toString] without trailing parens.
              /// - This is a linkplain to [Object#toString()] with trailing parens.
              /// - This is a linkplain to [Object#toString] without trailing parens.
              public void foo() {}

              public void bar() {}
            }
            """)
        .doTest();
  }

  @Test
  public void linkWithArray() {
    // From
    // https://docs.oracle.com/en/java/javase/23/javadoc/using-markdown-documentation-comments.html
    // To create a reference link to a method that has array parameters, you must escape the square
    // brackets within the reference. For example, here is a reference link to the method
    // String.copyValueOf(char[]):
    // [String#copyValueOf(char\[\])]
    helper
        .addInputLines(
            "Test.java",
            """
            package test;

            /**
             * This is a link to {@link String#copyValueOf(char[])}.
             */
            public class Test {
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            package test;

            /// This is a link to [String#copyValueOf(char\\[\\])].
            public class Test {
            }
            """)
        .doTest();
  }

  @Test
  public void recordJavadoc() {
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * Javadoc for a record.
             *
             * @param foo a string
             * @param bar an int
             */
            public record Test(String foo, int bar) {}
            """)
        .addOutputLines(
            "Test.java",
            """
            /// Javadoc for a record.
            ///
            /// @param foo a string
            /// @param bar an int
            public record Test(String foo, int bar) {}
            """)
        .doTest();
  }

  @Test
  public void seeTagInClassDocs() {
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * Some javadoc.
             *
             * @see http://google.com
             */
            public final class Test {}
            """)
        .addOutputLines(
            "Test.java",
            """
            /// Some javadoc.
            ///
            /// @see http://google.com
            public final class Test {}
            """)
        .doTest();
  }

  @Test
  public void seeTagAfterParam() {
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * Some javadoc.
             *
             * @param foo a string
             * @see http://google.com
             */
            public final class Test {
              public void foo(String foo) {}
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            /// Some javadoc.
            ///
            /// @param foo a string
            /// @see http://google.com
            public final class Test {
              public void foo(String foo) {}
            }
            """)
        .doTest();
  }

  @Test
  public void seeTagWithReference() {
    // You cannot use markdown links in @see tags. See https://bugs.openjdk.org/browse/JDK-8381678
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * @see <a href="//en.wikipedia.org/wiki/Function_composition">function composition</a>
             */
            public class Test {}
            """)
        .addOutputLines(
            "Test.java",
            """
            /// @see <a href="//en.wikipedia.org/wiki/Function_composition">function composition</a>
            public class Test {}
            """)
        .doTest();
  }

  @Test
  public void seeTagWithHtmlAndInlineTags() {
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * @see <a href="http://example.com"><b>Bold</b> and {@code code}</a>
             */
            public class Test {}
            """)
        .addOutputLines(
            "Test.java",
            """
            /// @see <a href="http://example.com"><b>Bold</b> and `code`</a>
            public class Test {}
            """)
        .doTest();
  }

  @Test
  public void seeTagWithArrayParameter() {
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * @see #foo(int[])
             */
            public class Test {}
            """)
        .addOutputLines(
            "Test.java",
            """
            /// @see #foo(int[])
            public class Test {}
            """)
        .doTest();
  }

  @Test
  public void seeTagWithLinkAndLiteral() {
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * @see java.util.List label with {@link java.util.Map} and {@linkplain java.util.Set#contains(Object[]) set} and {@code code} and {@literal literal}
             */
            public class Test {}
            """)
        .addOutputLines(
            "Test.java",
            """
            /// @see java.util.List label with [java.util.Map] and [set][java.util.Set#contains(Object\\[\\])] and `code` and {@literal literal}
            public class Test {}
            """)
        .doTest();
  }

  @Test
  public void seeTagWithEscape() {
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * @see SeeTag the class representing the \\@see tag
             */
            public class Test {}
            """)
        .addOutputLines(
            "Test.java",
            """
            /// @see SeeTag the class representing the \\@see tag
            public class Test {}
            """)
        .doTest();
  }

  @Test
  public void seeTagWithLabel() {
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * @see #findOriginalEntityById full version
             */
            public class Test {}
            """)
        .addOutputLines(
            "Test.java",
            """
            /// @see #findOriginalEntityById full version
            public class Test {}
            """)
        .doTest();
  }

  @Test
  public void inheritDocTag() {
    helper
        .addInputLines(
            "Test.java",
            """
            public class Test {
              /**
               * {@inheritDoc}
               */
              @Override
              public String toString() {
                return "";
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            public class Test {
              /// {@inheritDoc}
              @Override
              public String toString() {
                return "";
              }
            }
            """)
        .doTest();
  }

  @Test
  public void inheritDocTagWithAdditionalDocs() {
    helper
        .addInputLines(
            "Test.java",
            """
            public class Test {
              /**
               * {@inheritDoc}
               *
               * <p>This method is final!
               */
              @Override
              public final boolean equals(Object o) {
                return super.equals(o);
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            public class Test {
              /// {@inheritDoc}
              ///
              /// This method is final!
              @Override
              public final boolean equals(Object o) {
                return super.equals(o);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void htmlComment() {
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * Before <!-- comment --> After
             */
            public class Test {}
            """)
        .addOutputLines(
            "Test.java",
            """
            /// Before <!-- comment --> After
            public class Test {}
            """)
        .doTest();
  }

  @Test
  public void docRootTag() {
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * See {@docRoot}/index.html
             */
            public class Test {}
            """)
        .addOutputLines(
            "Test.java",
            """
            /// See {@docRoot}/index.html
            public class Test {}
            """)
        .doTest();
  }

  @Test
  public void htmlEntities() {
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * A &lt; B &amp; C &gt; D
             */
            public class Test {}
            """)
        .addOutputLines(
            "Test.java",
            """
            /// A &lt; B &amp; C &gt; D
            public class Test {}
            """)
        .doTest();
  }

  @Test
  public void escapeSequences() {
    assume().that(EXTRA_MARKDOWN_SPACE_GONE).isTrue();
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * An email: user@@google.com
             *
             * @@deprecated this is not a tag
             */
            public class Test {}
            """)
        .addOutputLines(
            "Test.java",
            """
            /// An email: user@@google.com
            ///
            /// \\@deprecated this is not a tag
            public class Test {}
            """)
        .doTest();
  }

  @Test
  public void hiddenTag() {
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * Some docs.
             *
             * @hidden
             */
            public class Test {}
            """)
        .addOutputLines(
            "Test.java",
            """
            /// Some docs.
            ///
            /// @hidden
            public class Test {}
            """)
        .doTest();
  }

  @Test
  public void indexTag() {
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * This is {@index "search term" description}.
             */
            public class Test {}
            """)
        .addOutputLines(
            "Test.java",
            """
            /// This is {@index "search term" description}.
            public class Test {}
            """)
        .doTest();
  }

  @Test
  public void moduleTags() {
    helper
        .addInputLines(
            "module-info.java",
            """
            /**
             * Javadoc for module.
             *
             * @provides java.lang.Runnable description
             * @provides java.lang.Runnable
             * @uses java.lang.Runnable
             */
            module my.module {
            }
            """)
        .addOutputLines(
            "module-info.java",
            """
            /// Javadoc for module.
            ///
            /// @provides java.lang.Runnable description
            /// @provides java.lang.Runnable
            /// @uses java.lang.Runnable
            module my.module {
            }
            """)
        .doTest();
  }

  @Test
  public void serializationTags() {
    helper
        .addInputLines(
            "Test.java",
            """
            import java.io.ObjectStreamField;
            public class Test {
              /**
               * @serial description
               * @serialData data description
               * @serialField name String field description
               */
              private static final ObjectStreamField[] serialPersistentFields = {};
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.io.ObjectStreamField;
            public class Test {
              /// @serial description
              /// @serialData data description
              /// @serialField name String field description
              private static final ObjectStreamField[] serialPersistentFields = {};
            }
            """)
        .doTest();
  }

  @Test
  public void specTag() {
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * @spec http://example.com Title
             *
             * @spec http://example.com
             */
            public class Test {}
            """)
        .addOutputLines(
            "Test.java",
            """
            /// @spec http://example.com Title
            /// @spec http://example.com
            public class Test {}
            """)
        .doTest();
  }

  @Test
  public void summaryTag() {
    assume().that(EXTRA_MARKDOWN_SPACE_GONE).isTrue();
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * {@summary A summary.} More text.
             *
             * {@summary}
             *
             * {@summary }
             */
            public class Test {}
            """)
        .addOutputLines(
            "Test.java",
            """
            /// {@summary A summary.} More text.
            ///
            /// {@summary}
            ///
            /// {@summary}
            public class Test {}
            """)
        .doTest();
  }

  @Test
  public void systemPropertyTag() {
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * The {@systemProperty java.home} property.
             */
            public class Test {}
            """)
        .addOutputLines(
            "Test.java",
            """
            /// The {@systemProperty java.home} property.
            public class Test {}
            """)
        .doTest();
  }

  @Test
  public void valueTag() {
    helper
        .addInputLines(
            "Test.java",
            """
            public class Test {
              public static final int CONST = 1;
              /**
               * Value is {@value #CONST}.
               */
              public void foo() {}
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            public class Test {
              public static final int CONST = 1;
              /// Value is {@value #CONST}.
              public void foo() {}
            }
            """)
        .doTest();
  }

  @Test
  public void lineBreakInCodeTag() {
    assume().that(EXTRA_MARKDOWN_SPACE_GONE).isTrue();
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * Foo {@code
             * bar baz}.
             */
            public class Test {}
            """)
        // TODO(b/532216390): should be "/// Foo\n/// `bar baz`."
        .addOutputLines(
            "Test.java",
            """
            /// Foo `
            /// bar baz`.
            public class Test {}
            """)
        .doTest();
  }

  @Test
  public void supTag() {
    helper
        .addInputLines(
            "Test.java",
            """
            public class Test {
              /**
               * Returns a value between 0 and 2<sup>32</sup>-1 inclusive.
               */
              public void foo() {}
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            public class Test {
              /// Returns a value between 0 and 2<sup>32</sup>-1 inclusive.
              public void foo() {}
            }
            """)
        .doTest();
  }

  @Test
  public void tableTag() {
    assume().that(EXTRA_MARKDOWN_SPACE_GONE).isTrue();
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * <table>
             * <tr><td>Foo</td></tr>
             * </table>
             */
            public class Test {}
            """)
        .addOutputLines(
            "Test.java",
            """
            /// <table>
            /// <tr><td>Foo</td></tr>
            /// </table>
            public class Test {}
            """)
        .doTest();
  }

  @Test
  public void htmlTagWithAttributes() {
    helper
        .addInputLines(
            "Test.java",
            """
            public class Test {
              /**
               * <span class="foo" id='bar' data-attr>Text</span>
               */
              public void foo() {}
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            public class Test {
              /// <span class="foo" id='bar' data-attr>Text</span>
              public void foo() {}
            }
            """)
        .doTest();
  }

  @Test
  public void selfClosingTag() {
    helper
        .addInputLines(
            "Test.java",
            """
            public class Test {
              /**
               * Line 1<br/>Line 2<hr/><img src="foo.png" alt="Foo"/>
               */
              public void foo() {}
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            public class Test {
              /// Line 1<br/>Line 2<hr/><img src="foo.png" alt="Foo"/>
              public void foo() {}
            }
            """)
        .doTest();
  }

  @Test
  public void anchorTagsWithNamesAndIds() {
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * This is the first paragraph.
             *
             * <p><a id="anchor-id"></a>This is the second paragraph.
             *
             * <p><a name="anchor-name"></a>This is the third paragraph.
             */
            public class Test {}
            """)
        .addOutputLines(
            "Test.java",
            """
            /// This is the first paragraph.
            ///
            /// <a id="anchor-id"></a>This is the second paragraph.
            ///
            /// <a name="anchor-name"></a>This is the third paragraph.
            public class Test {}
            """)
        .doTest();
  }

  @Test
  public void anchorTagsWithNamesAndIds_selfClosing() {
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * This is the first paragraph.
             *
             * <p><a id="anchor-id"/>This is the second paragraph.
             *
             * <p><a name="anchor-name"/>This is the third paragraph.
             */
            public class Test {}
            """)
        .addOutputLines(
            "Test.java",
            """
            /// This is the first paragraph.
            ///
            /// <a id="anchor-id"/>This is the second paragraph.
            ///
            /// <a name="anchor-name"/>This is the third paragraph.
            public class Test {}
            """)
        .doTest();
  }

  @Test
  public void anchorTagsWithHref_selfClosing() {
    // it's a bit odd to have a self-closing tag _with_ an href, but...
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * This is the first paragraph.
             *
             * <p><a href="http://google.com"/>This is the second paragraph.
             */
            public class Test {}
            """)
        .addOutputLines(
            "Test.java",
            """
            /// This is the first paragraph.
            ///
            /// <a href="http://google.com"/>This is the second paragraph.
            public class Test {}
            """)
        .doTest();
  }

  @Test
  public void nestedPreTags() {
    assume().that(EXTRA_MARKDOWN_SPACE_GONE).isTrue();
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * <pre>
             * outer code
             * <pre>
             * inner code
             * </pre>
             * outer code again
             * </pre>
             */
            public class Test {}
            """)
        .addOutputLines(
            "Test.java",
            """
            /// ```
            /// outer code
            ///
            /// inner code
            ///
            /// outer code again
            /// ```
            public class Test {}
            """)
        .doTest();
  }

  @Test
  public void nestedListIndentation() {
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * <ul>
             *   <li>Item 1
             *     <ul>
             *       <li>Subitem A</li>
             *       <li>Subitem B</li>
             *     </ul>
             *   </li>
             *   <li>Item 2</li>
             * </ul>
             */
            public class Test {}
            """)
        .addOutputLines(
            "Test.java",
            """
            /// - Item 1
            ///
            ///   - Subitem A
            ///   - Subitem B
            ///
            /// - Item 2
            public class Test {}
            """)
        .doTest();
  }

  @Test
  public void nestedFormattingTags() {
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * <b>outer <b>inner</b> outer</b>
             * <code>outer <code>inner</code> outer</code>
             */
            public class Test {}
            """)
        .addOutputLines(
            "Test.java",
            """
            /// **outer inner outer** `outer inner outer`
            public class Test {}
            """)
        .doTest();
  }

  @Test
  public void htmlTagInsideSeeDoesNotPopOuterScopeTag() {
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * <b>outer start
             * @see Foo <b>inner tag</b>
             */
            public class Test {}
            """)
        .addOutputLines(
            "Test.java",
            """
            /// **outer start
            ///
            /// @see Foo <b>inner tag</b>
            public class Test {}
            """)
        .doTest();
  }

  @Test
  public void unmatchedClosingTag() {
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * <b>bold text</div>
             */
            public class Test {}
            """)
        .addOutputLines(
            "Test.java",
            """
            /// **bold text</div>
            public class Test {}
            """)
        .doTest();
  }

  @Test
  public void nestedItalicFormattingTags() {
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * <i>outer <i>inner</i> outer</i>
             * <em>outer <em>inner</em> outer</em>
             */
            public class Test {}
            """)
        .addOutputLines(
            "Test.java",
            """
            /// *outer inner outer* *outer inner outer*
            public class Test {}
            """)
        .doTest();
  }

  @Test
  public void seeTagWithLink() {
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * @see <a href="http://google.com">{@link Integer#signum}</a>
             */
            public class Test {}
            """)
        .addOutputLines(
            "Test.java",
            """
            /// @see <a href="http://google.com">[Integer#signum]</a>
            public class Test {}
            """)
        .doTest();
  }

  @Test
  public void orderedListMultipleItems() {
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * <ol>
             *   <li>First item</li>
             *   <li>Second item</li>
             *   <li>Third item</li>
             * </ol>
             */
            public class Test {}
            """)
        .addOutputLines(
            "Test.java",
            """
            /// 1. First item
            /// 1. Second item
            /// 1. Third item
            public class Test {}
            """)
        .doTest();
  }

  @Test
  public void nestedListsOlInsideUl() {
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * <ul>
             *   <li>Bullet item
             *     <ol>
             *       <li>Numbered 1</li>
             *       <li>Numbered 2</li>
             *     </ol>
             *   </li>
             * </ul>
             */
            public class Test {}
            """)
        .addOutputLines(
            "Test.java",
            """
            /// - Bullet item
            ///
            ///   1. Numbered 1
            ///   1. Numbered 2
            public class Test {}
            """)
        .doTest();
  }
}
