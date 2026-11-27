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

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.TruthJUnit.assume;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.bugpatterns.javadoc.MarkdownComment.CodeBlock;
import com.google.errorprone.bugpatterns.javadoc.MarkdownComment.HtmlBlock;
import com.google.errorprone.bugpatterns.javadoc.MarkdownComment.Paragraph;
import com.google.errorprone.bugpatterns.javadoc.MarkdownComment.Text;
import com.google.errorprone.bugpatterns.javadoc.MarkdownCommentPrinter.PrintedComment;
import com.google.errorprone.bugpatterns.javadoc.MarkdownCommentPrinter.Region;
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
        .addOutputLines(
            "Test.java",
            """
            /// # Real Section
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
        // The label of a {@link} is rendered in code font, which Markdown can only express if the
        // label is plain text, so this {@link} is kept as-is.
        .addOutputLines(
            "Test.java",
            """
            /// # Real Section
            ///
            /// {@link Foo <h1>Not A Real Heading</h1>}
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
        // Markdown headings can't have attributes, so the headings are kept as HTML. (All of them,
        // to preserve their relative levels.)
        .addOutputLines(
            "Test.java",
            """
            /// <h3 id="section" class="title">Section Title</h3>
            ///
            /// <h4>Subsection</h4>
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
    // HTML isn't recognized in a ``` block, so a <pre> block that contains HTML is kept as-is.
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
            /// <pre>
            /// This really is <i>italic</i> and <b>bold</b>.
            /// Optional&lt;T&gt;
            /// </pre>
            public class Test {}
            """)
        .doTest();
  }

  @Test
  public void preWithLink() {
    assume().that(EXTRA_MARKDOWN_SPACE_GONE).isTrue();
    // HTML isn't recognized in a ``` block, so a <pre> block that contains a link is kept as-is.
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
            /// <pre>
            /// See <a href="http://example.com">this link</a>
            /// </pre>
            public class Test {}
            """)
        .doTest();
  }

  @Test
  public void preWithLineBreak() {
    assume().that(EXTRA_MARKDOWN_SPACE_GONE).isTrue();
    // In a <pre> block, <br> tags render as line breaks.
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
            /// Line 1
            /// Line 2
            /// ```
            public class Test {}
            """)
        .doTest();
  }

  @Test
  public void preWithBareEntities() {
    assume().that(EXTRA_MARKDOWN_SPACE_GONE).isTrue();
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
            /// A < B
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
            @Deprecated
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
            @Deprecated
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
              /// - This is a linkplain to {@linkplain #bar()} with trailing parens.
              /// - This is a linkplain to {@linkplain #bar} without trailing parens.
              /// - This is a link to [Object#toString()] with trailing parens.
              /// - This is a link to [Object#toString] without trailing parens.
              /// - This is a linkplain to {@linkplain Object#toString()} with trailing parens.
              /// - This is a linkplain to {@linkplain Object#toString} without trailing parens.
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
            /// @see SeeTag the class representing the \\\\@see tag
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
        .addOutputLines(
            "Test.java",
            """
            /// Foo `bar baz`.
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
        // An unclosed <b> can't be written as Markdown emphasis.
        .addOutputLines(
            "Test.java",
            """
            /// <b>outer start
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
            /// <b>bold text</div>
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

  @Test
  public void codeTagWithBackticksAndEmpty() {
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * Empty {@code} and backticks {@code `ClassName.methodName()`} and {@code foo`bar}.
             */
            public class Test {}
            """)
        .addOutputLines(
            "Test.java",
            """
            /// Empty  and backticks `` `ClassName.methodName()` `` and ``foo`bar``.
            public class Test {}
            """)
        .doTest();
  }

  @Test
  public void bannerCommentIgnored() {
    helper
        .addInputLines(
            "Test.java",
            """
            /************************************************************************
             * Section banner
             ************************************************************************/
            public class Test {}
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void linkWithTrailingSpaceInLabel() {
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * See {@link #foo foo = "bar" }.
             */
            public class Test {
              public void foo() {}
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            /// See [`foo = "bar"`][#foo].
            public class Test {
              public void foo() {}
            }
            """)
        .doTest();
  }

  @Test
  public void lineBreakInCodeTagWithSpaceAndInList() {
    assume().that(EXTRA_MARKDOWN_SPACE_GONE).isTrue();
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * Foo {@code\s
             * bar}.
             *
             * <ul>
             *   <li>{@code
             *     baz}
             * </ul>
             */
            public class Test {}
            """)
        .addOutputLines(
            "Test.java",
            """
            /// Foo `bar`.
            ///
            /// - `baz`
            public class Test {}
            """)
        .doTest();
  }

  @Test
  public void lineBreakInCodeTagInsideLinkAndBold() {
    assume().that(EXTRA_MARKDOWN_SPACE_GONE).isTrue();
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * See <a href="https://example.com">{@code
             * Foo}</a> and <b>{@code
             * Bar}</b>.
             */
            public class Test {}
            """)
        .addOutputLines(
            "Test.java",
            """
            /// See [`Foo`](https://example.com) and **`Bar`**.
            public class Test {}
            """)
        .doTest();
  }

  @Test
  public void linkWithBackticksInLabel() {
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * See {@link #foo `bar`}.
             */
            public class Test {
              public void foo() {}
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            /// See [`` `bar` ``][#foo].
            public class Test {
              public void foo() {}
            }
            """)
        .doTest();
  }

  @Test
  public void preWithTrailingBlankLine() {
    assume().that(EXTRA_MARKDOWN_SPACE_GONE).isTrue();
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * <pre>{@code
             * int x = 1;
             *
             * }</pre>
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
  public void nestedListsOlUlOl() {
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * <ol>
             *   <li>Outer 1
             *     <ul>
             *       <li>Middle bullet
             *         <ol>
             *           <li>Inner numbered</li>
             *         </ol>
             *       </li>
             *     </ul>
             *   </li>
             *   <li>Outer 2</li>
             * </ol>
             */
            public class Test {}
            """)
        .addOutputLines(
            "Test.java",
            """
            /// 1. Outer 1
            ///
            ///    - Middle bullet
            ///
            ///      1. Inner numbered
            ///
            /// 1. Outer 2
            public class Test {}
            """)
        .doTest();
  }

  @Test
  public void nestedListsOlOlUl() {
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * <ol>
             *   <li>Outer
             *     <ol>
             *       <li>Middle numbered
             *         <ul>
             *           <li>Inner bullet</li>
             *         </ul>
             *       </li>
             *     </ol>
             *   </li>
             * </ol>
             */
            public class Test {}
            """)
        .addOutputLines(
            "Test.java",
            """
            /// 1. Outer
            ///
            ///    1. Middle numbered
            ///
            ///       - Inner bullet
            public class Test {}
            """)
        .doTest();
  }

  @Test
  public void preWithEntities() {
    assume().that(EXTRA_MARKDOWN_SPACE_GONE).isTrue();
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * <pre>
             * &#64;Override
             * &#064;Nullable
             * &#x40;Deprecated
             * List&lt;String&gt; xs = a &amp;&amp; b ? &quot;x&quot; : &apos;y&apos;;
             * /* comment *&#47;
             * &bogus;
             * </pre>
             *
             * <pre><code>
             * Map&lt;String, Integer&gt; m;
             * </code></pre>
             */
            public class Test {}
            """)
        .addOutputLines(
            "Test.java",
            """
            /// ```
            /// @Override
            /// @Nullable
            /// @Deprecated
            /// List<String> xs = a && b ? "x" : 'y';
            /// /* comment */
            /// &bogus;
            /// ```
            ///
            /// ```
            /// Map<String, Integer> m;
            /// ```
            public class Test {}
            """)
        .doTest();
  }

  @Test
  public void codeTagWithEntities() {
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * See <code>List&lt;String&gt;</code>, <code>a &amp;&amp; b</code>,
             * <code>&#64;Nullable</code>, <code>/* c *&#47;</code>, and <code>&bogus;</code>.
             */
            public class Test {}
            """)
        .addOutputLines(
            "Test.java",
            """
            /// See `List<String>`, `a && b`, `@Nullable`, `/* c */`, and `&bogus;`.
            public class Test {}
            """)
        .doTest();
  }

  @Test
  public void linkLabelWithEntities() {
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * See {@link Test Test&lt;String&gt;} and {@linkplain Test Test&lt;String&gt;}.
             *
             * @see <code>A &lt; B</code>
             */
            public class Test<T> {}
            """)
        .addOutputLines(
            "Test.java",
            """
            /// See [`Test<String>`][Test] and [Test&lt;String&gt;][Test].
            ///
            /// @see <code>A &lt; B</code>
            public class Test<T> {}
            """)
        .doTest();
  }

  @Test
  public void escapesMarkdownSyntax() {
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * Computes a*b*c and __init__ for args[i] in [0, 1].
             * 1. This is not a list item.
             * # This is not a heading.
             */
            public class Test {}
            """)
        .addOutputLines(
            "Test.java",
            """
            /// Computes a\\*b\\*c and \\_\\_init\\_\\_ for args\\[i] in [0, 1].
            /// 1\\. This is not a list item.
            /// \\# This is not a heading.
            public class Test {}
            """)
        .doTest();
  }

  @Test
  public void emphasisThatCannotUseDelimiters() {
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * Some foo<b>(bar)</b>baz, and <b>bold</b><i>italic</i>.
             */
            public class Test {}
            """)
        // `foo**(bar)**baz` would not be parsed as emphasis, so the HTML is kept.
        .addOutputLines(
            "Test.java",
            """
            /// Some foo<b>(bar)</b>baz, and __bold__*italic*.
            public class Test {}
            """)
        .doTest();
  }

  @Test
  public void seeTagWithCodeLabel() {
    helper
        .addInputLines(
            "Test.java",
            """
            public class Test {
              /**
               * Does something.
               *
               * @see #bar() Test.bar()
               */
              public void foo() {}

              public void bar() {}
            }
            """)
        // javadoc renders this label in code font in a traditional comment, but not in a Markdown
        // comment.
        .addOutputLines(
            "Test.java",
            """
            public class Test {
              /// Does something.
              ///
              /// @see #bar() `Test.bar()`
              public void foo() {}

              public void bar() {}
            }
            """)
        .doTest();
  }

  @Test
  public void backslashBeforeU() {
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * Escapes like \\\\uXXXX are written as-is.
             */
            public class Test {}
            """)
        // Each backslash is escaped, since an odd number of backslashes before `u` would be a
        // Unicode escape.
        .addOutputLines(
            "Test.java",
            """
            /// Escapes like \\\\\\\\uXXXX are written as-is.
            public class Test {}
            """)
        .doTest();
  }

  @Test
  public void codeOnSameLineAsComment() {
    helper
        .addInputLines(
            "Test.java",
            """
            public class Test {
              int x; /** The y. */ int y;
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            public class Test {
              int x;
              /// The y.
              int y;
            }
            """)
        .doTest();
  }

  @Test
  public void codeBlockInListItem() {
    assume().that(EXTRA_MARKDOWN_SPACE_GONE).isTrue();
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * <ul>
             *   <li>An item with code:
             *       <pre>{@code
             *       int x = 1;
             *       }</pre>
             * </ul>
             */
            public class Test {}
            """)
        .addOutputLines(
            "Test.java",
            """
            /// - An item with code:
            ///
            ///   ```
            ///         int x = 1;
            ///   ```
            public class Test {}
            """)
        .doTest();
  }

  @Test
  public void linkAtStartOfParagraphFollowedByColon() {
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * {@link Test}: a class.
             */
            public class Test {}
            """)
        // `[Test]: a class.` would be a link reference definition.
        .addOutputLines(
            "Test.java",
            """
            /// [Test][]: a class.
            public class Test {}
            """)
        .doTest();
  }

  @Test
  public void unicodeEscapeInComment() {
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * A range of ['\\u0000', '\\u007F'].
             */
            public class Test {}
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void unconvertibleComment() {
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * <b>Bold <p>paragraph</b>
             */
            public class Test {}
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void lineDocCommentOnPreviousLine() {
    // javac merges a `///` comment with a `///` comment on the next line.
    helper
        .addInputLines(
            "Test.java",
            """
            public class Test {
              /// A Markdown note.
              /** Not converted. */
              int a;

              ////////////////////////////////////////
              /** Not converted. */
              int b;

              int c; /// A trailing note.
              /** Not converted. */
              int d;

              // A line comment.
              /** Converted. */
              int e;

              /// A separate note.

              /** Converted. */
              int f;
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            public class Test {
              /// A Markdown note.
              /** Not converted. */
              int a;

              ////////////////////////////////////////
              /** Not converted. */
              int b;

              int c; /// A trailing note.
              /** Not converted. */
              int d;

              // A line comment.
              /// Converted.
              int e;

              /// A separate note.

              /// Converted.
              int f;
            }
            """)
        .doTest();
  }

  @Test
  public void deprecatedTagWithoutAnnotation() {
    // javac only treats `@deprecated` in a `/** ... */` comment as deprecating the declaration.
    helper
        .addInputLines(
            "Test.java",
            """
            public class Test {
              /**
               * Not converted.
               *
               * @deprecated Use something else.
               */
              public void notAnnotated() {}

              /**
               * Converted.
               *
               * @deprecated Use something else.
               */
              @Deprecated
              public void annotated() {}

              /**
               * Converted.
               *
               * @deprecated Use something else.
               */
              @java.lang.Deprecated
              public void qualifiedAnnotation() {}
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            public class Test {
              /**
               * Not converted.
               *
               * @deprecated Use something else.
               */
              public void notAnnotated() {}

              /// Converted.
              ///
              /// @deprecated Use something else.
              @Deprecated
              public void annotated() {}

              /// Converted.
              ///
              /// @deprecated Use something else.
              @java.lang.Deprecated
              public void qualifiedAnnotation() {}
            }
            """)
        .doTest();
  }

  @Test
  public void deprecatedTagWithCustomDeprecatedAnnotation() {
    helper
        .addInputLines(
            "Deprecated.java",
            """
            package custom;

            public @interface Deprecated {}
            """)
        .expectUnchanged()
        .addInputLines(
            "Test.java",
            """
            import custom.Deprecated;

            public class Test {
              /**
               * Not converted because the annotation is not java.lang.Deprecated.
               *
               * @deprecated Use something else.
               */
              @Deprecated
              public void customDeprecated() {}
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void deprecatedInCodeOrCommentWithoutAnnotation() {
    helper
        .addInputLines(
            "Test.java",
            """
            public class Test {
              /**
               * Example:
               * {@code
               * @deprecated
               * }
               */
              public void inCode() {}

              /**
               * <!--
               * @deprecated
               * -->
               * Still deprecated by JavaTokenizer.
               */
              public void inComment() {}
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void deprecatedTagWithoutAnnotation_nonMethods() {
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * Class doc.
             *
             * @deprecated Use another class.
             */
            public enum Test {
              /**
               * Constant doc.
               *
               * @deprecated Use another constant.
               */
              A;

              /**
               * Field doc.
               *
               * @deprecated Use another field.
               */
              int field;
            }
            """)
        .expectUnchanged()
        .addInputLines(
            "package-info.java",
            """
            /**
             * Package doc.
             *
             * @deprecated Use another package.
             */
            package test;
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void packageAndModuleJavadoc() {
    helper
        .addInputLines(
            "package-info.java",
            """
            /**
             * Package doc with {@code code}.
             *
             * @deprecated Use another package.
             */
            @Deprecated
            package test;
            """)
        .addOutputLines(
            "package-info.java",
            """
            /// Package doc with `code`.
            ///
            /// @deprecated Use another package.
            @Deprecated
            package test;
            """)
        .doTest();

    BugCheckerRefactoringTestHelper.newInstance(TraditionalJavadocToMarkdown.class, getClass())
        .addInputLines(
            "module-info.java",
            """
            /**
             * Module doc.
             *
             * @deprecated Use another module.
             */
            module my.module {}
            """)
        .expectUnchanged()
        .doTest();

    BugCheckerRefactoringTestHelper.newInstance(TraditionalJavadocToMarkdown.class, getClass())
        .addInputLines(
            "module-info.java",
            """
            /**
             * Module doc with {@code code}.
             *
             * @deprecated Use another module.
             */
            @Deprecated
            module my.module {}
            """)
        .addOutputLines(
            "module-info.java",
            """
            /// Module doc with `code`.
            ///
            /// @deprecated Use another module.
            @Deprecated
            module my.module {}
            """)
        .doTest();
  }

  @Test
  public void commentBodyStartsWithJavadocOpener() {
    helper
        .addInputLines(
            "Test.java",
            """
            public class Test {
              /** /** example */
              int a;

              /**
               * /** example
               */
              int b;
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            public class Test {
              /// /\\*\\* example
              int a;

              /// /\\*\\* example
              int b;
            }
            """)
        .doTest();
  }

  @Test
  public void embeddedNewlinesDoNotEmitUncommentedJava() {
    assume().that(EXTRA_MARKDOWN_SPACE_GONE).isTrue();
    helper
        .addInputLines(
            "Test.java",
            """
            import java.io.Serializable;
            import java.util.Map;

            public class Test implements Serializable {
              /**
               * <h2>Title {@literal
               * bar
               * }</h2>
               */
              int a;

              /**
               * {@snippet lang="a
               * b" :
               * int x;
               * }
               */
              int b;

              /**
               * @serialField m java.util.Map<
               * String, Integer> desc
               */
              private static final long serialVersionUID = 1L;
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.io.Serializable;
            import java.util.Map;

            public class Test implements Serializable {
              /// <h2>Title {@literal
              /// bar
              /// }</h2>
              int a;

              /// {@snippet lang="a
              /// b" :
              /// int x;
              /// }
              int b;

              /// @serialField m java.util.Map< String, Integer> desc
              private static final long serialVersionUID = 1L;
            }
            """)
        .doTest();
  }

  @Test
  public void valuelessHtmlAndSnippetAttributes() {
    assume().that(EXTRA_MARKDOWN_SPACE_GONE).isTrue();
    helper
        .addInputLines(
            "Test.java",
            """
            public class Test {
              /** <p id>text</p> */
              int a;

              /** <a href>text</a> */
              int b;

              /** <ol start><li>a</li></ol> */
              int c;

              /**
               * {@snippet lang :
               * int x;
               * }
               */
              int d;
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            public class Test {
              /// <p id>text</p>
              int a;

              /// <a href>text</a>
              int b;

              /// <ol start><li>a</li></ol>
              int c;

              /// {@snippet lang :
              /// int x;
              /// }
              int d;
            }
            """)
        .doTest();
  }

  @Test
  public void nullableDocTreeFields() {
    helper
        .addInputLines(
            "Test.java",
            """
            import java.io.Serializable;

            public class Test implements Serializable {
              /** {@link} and {@linkplain} */
              int a;

              /** @throws } */
              void b() {}

              /** @exception } */
              void c() {}

              /** @serialField count } */
              int d;

              /** @provides } */
              int e;

              /** @uses } */
              int f;

              /** @serialField count int*/
              int g;
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.io.Serializable;

            public class Test implements Serializable {
              /// {@link} and {@linkplain}
              int a;

              /** @throws } */
              void b() {}

              /** @exception } */
              void c() {}

              /** @serialField count } */
              int d;

              /** @provides } */
              int e;

              /** @uses } */
              int f;

              /// @serialField count int
              int g;
            }
            """)
        .doTest();
  }

  @Test
  public void nonBreakingSpacePreservedAndDoesNotCrash() {
    helper
        .addInputLines(
            "Test.java",
            """
            public class Test {
              /** <b>\u00A0</b> */
              int a;

              /** <code>&nbsp;</code> */
              int b;

              /** {@link String foo&nbsp;bar} */
              int c;
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            public class Test {
              /// <b>\u00A0</b>
              int a;

              /// `\u00A0`
              int b;

              /// [`foo\u00A0bar`][String]
              int c;
            }
            """)
        .doTest();
  }

  @Test
  public void html5TableWithoutOptionalClosingTags() {
    // This test fails on JDK23 for some reason.
    // https://github.com/google/error-prone/actions/runs/45210389889/job/137817313495
    assume().that(EXTRA_MARKDOWN_SPACE_GONE).isTrue();
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * <table>
             *   <caption>Title
             *   <tr><th>A<th>B
             *   <tr><td>1<td>2
             * </table>
             *
             * <p>After {@link String}.
             */
            public class Test {}
            """)
        .addOutputLines(
            "Test.java",
            """
            /// <table>
            ///   <caption>Title
            ///   <tr><th>A<th>B
            ///   <tr><td>1<td>2
            /// </table>
            ///
            /// After [String].
            public class Test {}
            """)
        .doTest();
  }

  @Test
  public void seeTagEscapesMarkdownAndHandlesAdjacentCode() {
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * @see Test *important* note
             * @see Test args[i]
             * @see Test see {@link String}(x)
             * @see Test {@code a}{@code b}
             * @see "Quoted *not* markdown"
             */
            public class Test {}
            """)
        .addOutputLines(
            "Test.java",
            """
            /// @see Test \\*important\\* note
            /// @see Test args\\[i]
            /// @see Test see [String][](x)
            /// @see Test `a`<code>b</code>
            /// @see "Quoted *not* markdown"
            public class Test {}
            """)
        .doTest();
  }

  @Test
  public void inlineTagBodiesEscapedAtLineStart() {
    assume().that(EXTRA_MARKDOWN_SPACE_GONE).isTrue();
    helper
        .addInputLines(
            "Test.java",
            """
            public class Test {
              /** {@summary # foo} */
              int a;

              /** {@summary - item} */
              int b;

              /** {@summary 1. first} */
              int c;

              /** {@summary {@link String}: https://example.com} */
              int d;

              /** {@return # value} */
              int e() { return 0; }

              /** {@index term - desc} */
              int f;
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            public class Test {
              /// {@summary \\# foo}
              int a;

              /// {@summary \\- item}
              int b;

              /// {@summary 1\\. first}
              int c;

              /// {@summary [String][]: https://example.com}
              int d;

              /// {@return \\# value}
              int e() { return 0; }

              /// {@index term \\- desc}
              int f;
            }
            """)
        .doTest();
  }

  @Test
  public void unknownInlineTagAndInheritDocSupertype() {
    helper
        .addInputLines(
            "Super.java",
            """
            interface Super {
              void m();
            }
            """)
        .expectUnchanged()
        .addInputLines(
            "Test.java",
            """
            public class Test implements Super {
              /** Uses {@math a * b} and {@custom}. */
              int a;

              /** {@inheritDoc Super} */
              @Override
              public void m() {}
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            public class Test implements Super {
              /// Uses {@math a * b} and {@custom}.
              int a;

              /// {@inheritDoc Super}
              @Override
              public void m() {}
            }
            """)
        .doTest();
  }

  @Test
  public void inlineEdgeWhitespaceAndLiteralLeadingSpace() {
    helper
        .addInputLines(
            "Test.java",
            """
            public class Test {
              /**
               * Foo<b>
               * bar</b>
               */
              int a;

              /** See<a href="https://example.com"> here </a>now. */
              int b;

              /** Call<code> foo </code>bar. */
              int c;

              /** A<code> </code>B */
              int d;

              /** {@literal  foo} */
              int e;
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            public class Test {
              /// Foo **bar**
              int a;

              /// See [here](https://example.com) now.
              int b;

              /// Call `foo` bar.
              int c;

              /// A` `B
              int d;

              /// {@literal  foo}
              int e;
            }
            """)
        .doTest();
  }

  @Test
  public void snippetDirectivesAndStripIndent() {
    assume().that(EXTRA_MARKDOWN_SPACE_GONE).isTrue();
    helper
        .addInputLines(
            "Test.java",
            """
            public class Test {
              /**
               * {@snippet :
               * // @highlight substring="x"
               * int x = 1;
               * }
               */
              int a;

              /**
               * {@snippet :
               *     int x = 1;
               *     int y = 2;
               *     }
               */
              int b;
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            public class Test {
              /// {@snippet :
              /// // @highlight substring="x"
              /// int x = 1;
              /// }
              int a;

              /// ```
              /// int x = 1;
              /// int y = 2;
              /// ```
              int b;
            }
            """)
        .doTest();
  }

  @Test
  public void midParagraphHtmlCommentDoesNotSplitParagraph() {
    helper
        .addInputLines(
            "Test.java",
            """
            /**
             * Returns the
             * <!-- internal note -->
             * value.
             */
            public class Test {}
            """)
        .addOutputLines(
            "Test.java",
            """
            /// Returns the <!-- internal note -->
            /// value.
            public class Test {}
            """)
        .doTest();
  }

  @Test
  public void htmlAttributesAndBlankLinesPreserved() {
    assume().that(EXTRA_MARKDOWN_SPACE_GONE).isTrue();
    helper
        .addInputLines(
            "Test.java",
            """
            public class Test {
              /** <pre id="example">code</pre> */
              int a;

              /** <code id="x">foo</code> */
              int b;

              /** <p class="note">Note</p> */
              int c;

              /** <p id='a"b'>Text</p> */
              int d;

              /**
               * <textarea>
               * a
               *
               * b
               * </textarea>
               */
              int e;

              /**
               * <div>
               * {@snippet :
               * a
               *
               * b
               * }
               * </div>
               */
              int f;
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            public class Test {
              /// <pre id="example">code</pre>
              int a;

              /// <code id="x">foo</code>
              int b;

              /// <p class="note">Note</p>
              int c;

              /// <a id="a&quot;b"></a>Text
              int d;

              /// <textarea>
              /// a
              ///
              /// b
              /// </textarea>
              int e;

              /**
               * <div>
               * {@snippet :
               * a
               *
               * b
               * }
               * </div>
               */
              int f;
            }
            """)
        .doTest();
  }

  @Test
  public void nestedListDirectlyInListItem() {
    assume().that(EXTRA_MARKDOWN_SPACE_GONE).isTrue();
    helper
        .addInputLines(
            "Test.java",
            """
            public class Test {
              /**
               * <ul><li><ul><li>foo</li></ul></li></ul>
               */
              int a;

              /**
               * <ol><li><ul><li>foo</li></ul></li></ol>
               */
              int b;
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            public class Test {
              /// -
              ///   - foo
              int a;

              /// 1.
              ///    - foo
              int b;
            }
            """)
        .doTest();
  }

  @Test
  public void starAtSlashEscapeAndBackslashTripleAdvanceHazards() {
    helper
        .addInputLines(
            "Test.java",
            """
            public class Test {
              /** <b>foo</b>@/bar */
              int a;

              /**
               * Method.
               *
               * @param x foo*
               * @return bar
               */
              int b(int x) { return x; }

              /** {@summary Matches foo*} rest */
              int c;

              /** a*{@docRoot} */
              int d;

              /** a* */
              int e;
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            public class Test {
              /// **foo**\\@/bar
              int a;

              /**
               * Method.
               *
               * @param x foo*
               * @return bar
               */
              int b(int x) { return x; }

              /** {@summary Matches foo*} rest */
              int c;

              /** a*{@docRoot} */
              int d;

              /** a* */
              int e;
            }
            """)
        .doTest();
  }

  @Test
  public void linkplainMatchingLabelAndShortcutLinkWithMarkdownChars() {
    helper
        .addInputLines(
            "Test.java",
            """
            import java.util.List;

            public class Test {
              /** See {@linkplain java.lang.String java.lang.String}. */
              int a;

              /** See {@link #_a_()} and {@link List<String>}. */
              int b;

              void _a_() {}
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.util.List;

            public class Test {
              /// See {@linkplain java.lang.String java.lang.String}.
              int a;

              /// See {@link #_a_()} and {@link List<String>}.
              int b;

              void _a_() {}
            }
            """)
        .doTest();
  }

  @Test
  public void escapesAdditionalCommonMarkConstructs() {
    assume().that(EXTRA_MARKDOWN_SPACE_GONE).isTrue();
    helper
        .addInputLines(
            "Test.java",
            """
            public class Test {
              /**
               * Refs [##foo bar], [Foo##bar baz], and [@Nullable Foo].
               * Nested [a [1 2] b](url) and email <123@example.com> and emoji 🚀_foo_🚀.
               *
               * <p>a | b
               * --- | ---
               *
               * <h2>Title\t##</h2>
               *
               * <p><span title="a>b">
               * foo</span>
               *
               * {@snippet lang="~`foo" :
               * x
               * }
               */
              int a;
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            public class Test {
              /// Refs \\[##foo bar], \\[Foo##bar baz], and \\[@Nullable Foo].
              /// Nested \\[a [1 2] b](url) and email \\<123@example.com> and emoji 🚀\\_foo\\_🚀.
              ///
              /// a | b
              /// \\--- | ---
              ///
              /// # Title\t\\##
              ///
              /// <span title="a>b"> foo</span>
              ///
              /// ~~~ ~`foo
              /// x
              /// ~~~
              int a;
            }
            """)
        .doTest();
  }

  @Test
  public void htmlBlockDocCommentParserHazardsLeftUnchanged() {
    helper
        .addInputLines(
            "Test.java",
            """
            public class Test {
              /**
               * <!--
               * @foo
               * -->
               */
              int a;

              /**
               * <pre>
               * foo
               *
               *     {@link String}
               * </pre>
               */
              int b;

              /**
               * <!-- {@link String} -->
               */
              int c;
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void postConversionUnicodeEscapeAndBacktickPairs() {
    helper
        .addInputLines(
            "Test.java",
            """
            public class Test {
              /** <code>&#92;u0041</code> */
              int a;

              /** Inline `foo` and ``a ` b`` and lone ` here. */
              int b;
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            public class Test {
              /** <code>&#92;u0041</code> */
              int a;

              /// Inline `foo` and ``a ` b`` and lone \\` here.
              int b;
            }
            """)
        .doTest();
  }

  @Test
  public void backtickPairsAcrossHtmlAndLineBreaks() {
    helper
        .addInputLines(
            "Test.java",
            """
            public class Test {
              /** (`foo/<stage>/<outcome>/<reason>` and `foo/<stage>/<outcome>/<reason>/users`) */
              int acrossUnknownTags;

              /** Use `foo`, `bar <em>baz</em>`, and `qux`. */
              int acrossEmphasis;

              /**
               * (`"a"` or `"b
               * c"`). Defaults to `"a"` when blank.
               */
              int acrossLineBreak;

              /** `foo {@code bar}` and `baz`. */
              int acrossInlineTag;

              /** `a&lt;b` and `c`. */
              int acrossEntity;

              /**
               * <h3>`foo <em>bar</em>` and `baz`.</h3>
               */
              static class InHeading {}

              /** {@summary `foo <em>bar</em>` and `baz`.} */
              int inInlineTag;
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            public class Test {
              /// (\\`foo/<stage>/<outcome>/<reason>\\` and \\`foo/<stage>/<outcome>/<reason>/users\\`)
              int acrossUnknownTags;

              /// Use `foo`, \\`bar *baz*\\`, and `qux`.
              int acrossEmphasis;

              /// (`"a"` or `"b c"`). Defaults to `"a"` when blank.
              int acrossLineBreak;

              /// \\`foo `bar`\\` and `baz`.
              int acrossInlineTag;

              /// \\`a&lt;b\\` and `c`.
              int acrossEntity;

              /// # \\`foo *bar*\\` and `baz`.
              static class InHeading {}

              /// {@summary \\`foo *bar*\\` and `baz`.}
              int inInlineTag;
            }
            """)
        .doTest();
  }

  @Test
  public void backtickPairsEdgeCases() {
    helper
        .addInputLines(
            "Test.java",
            """
            public class Test {
              /** <b>`foo`</b> and {@linkplain Object `bar`}. */
              int insideInlineContainers;

              /** A lone ` backtick. */
              int loneBacktick;

              /** <a href="https://example.com">`baz`</a>. */
              int insideLink;

              /** {@link String} and `foo`. */
              int alongsideLabelLessReferenceLink;

              /** <b>`foo` bar</b> and <b>`foo` bar</b>. */
              int duplicateEqualContainers;

              /**
               * `
               * ` and `foo`.
               */
              int blankAcrossLineBreak;

              /**
               * `
               * foo
               * `
               */
              int paddedAcrossLineBreak;

              /** ``a `b` <em>c</em>`` and `d`. */
              int multiBacktickAcrossHtml;

              /** `a <b>b` c</b> and `d`. */
              int openOutsideCloseInside;

              /** <b>a `b</b> c` and `d`. */
              int openInsideCloseOutside;
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            public class Test {
              /// **`foo`** and [`bar`][Object].
              int insideInlineContainers;

              /// A lone \\` backtick.
              int loneBacktick;

              /// [`baz`](https://example.com).
              int insideLink;

              /// [String] and `foo`.
              int alongsideLabelLessReferenceLink;

              /// **`foo` bar** and **`foo` bar**.
              int duplicateEqualContainers;

              /// \\` \\` and `foo`.
              int blankAcrossLineBreak;

              /// `foo`
              int paddedAcrossLineBreak;

              /// \\`\\`a \\`b\\` *c*\\`\\` and `d`.
              int multiBacktickAcrossHtml;

              /// \\`a **b\\` c** and `d`.
              int openOutsideCloseInside;

              /// **a \\`b** c\\` and `d`.
              int openInsideCloseOutside;
            }
            """)
        .doTest();
  }

  @Test
  public void validatorRejectsStructuralMismatchesAndHazards() {
    Paragraph plainStar = new Paragraph(ImmutableList.of(new Text("*foo*")));
    assertThat(
            MarkdownCommentValidator.isValid(
                new PrintedComment(
                    ImmutableList.of("\\*foo\\*"),
                    ImmutableList.of(new Region(ImmutableList.of(plainStar), "\\*foo\\*")))))
        .isFalse(); // trailing \* hits EOF triple-advance hazard
    assertThat(
            MarkdownCommentValidator.isValid(
                new PrintedComment(
                    ImmutableList.of("\\*foo\\*."),
                    ImmutableList.of(
                        new Region(
                            ImmutableList.of(new Paragraph(ImmutableList.of(new Text("*foo*.")))),
                            "\\*foo\\*.")))))
        .isTrue();
    // Unescaped emphasis fails structural comparison.
    assertThat(
            MarkdownCommentValidator.isValid(
                new PrintedComment(
                    ImmutableList.of("*foo*."),
                    ImmutableList.of(
                        new Region(
                            ImmutableList.of(new Paragraph(ImmutableList.of(new Text("*foo*.")))),
                            "*foo*.")))))
        .isFalse();
    // Unescaped javadoc reference fails structural comparison.
    assertThat(
            MarkdownCommentValidator.isValid(
                new PrintedComment(
                    ImmutableList.of("args[i]."),
                    ImmutableList.of(
                        new Region(
                            ImmutableList.of(new Paragraph(ImmutableList.of(new Text("args[i].")))),
                            "args[i].")))))
        .isFalse();
    // Embedded newline in printed lines is rejected.
    assertThat(
            MarkdownCommentValidator.isValid(
                new PrintedComment(
                    ImmutableList.of("line1\nline2"),
                    ImmutableList.of(
                        new Region(
                            ImmutableList.of(
                                new Paragraph(ImmutableList.of(new Text("line1 line2")))),
                            "line1\nline2")))))
        .isFalse();
    // CodeBlock info/literal newline collision is rejected.
    CodeBlock multilineInfo = new CodeBlock("a\nb", "c");
    assertThat(
            MarkdownCommentValidator.isValid(
                new PrintedComment(
                    ImmutableList.of("```a", "b", "c", "```"),
                    ImmutableList.of(
                        new Region(ImmutableList.of(multilineInfo), "```a\nb\nc\n```")))))
        .isFalse();
    // Indented code block does not match fenced CodeBlock.
    assertThat(
            MarkdownCommentValidator.isValid(
                new PrintedComment(
                    ImmutableList.of("    x"),
                    ImmutableList.of(
                        new Region(ImmutableList.of(new CodeBlock("", "x")), "    x")))))
        .isFalse();
    // HtmlBlock with line-start @foo is rejected.
    HtmlBlock commentWithAt = new HtmlBlock("<!--\n@foo\n-->");
    assertThat(
            MarkdownCommentValidator.isValid(
                new PrintedComment(
                    ImmutableList.of("<!--", "@foo", "-->"),
                    ImmutableList.of(
                        new Region(ImmutableList.of(commentWithAt), "<!--\n@foo\n-->")))))
        .isFalse();
  }
}
