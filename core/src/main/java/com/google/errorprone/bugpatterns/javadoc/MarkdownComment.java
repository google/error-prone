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

import com.google.common.collect.ImmutableList;
import org.jspecify.annotations.Nullable;

/// The Markdown documentation comment that [TraditionalJavadocToMarkdown] produces for a
/// traditional Javadoc comment.
///
/// This is the intermediate representation between [MarkdownCommentBuilder] (which decides _what_
/// to emit) and [MarkdownCommentPrinter] (which decides how to spell it, including escaping and
/// indentation). [MarkdownCommentValidator] checks that the printed Markdown parses back into the
/// same structure.
///
/// @param body the main description
/// @param blockTags the block tags, such as `@param` and `@return`
record MarkdownComment(
    ImmutableList<MarkdownComment.Block> body, ImmutableList<MarkdownComment.BlockTag> blockTags) {

  /// A block tag, such as `@param x description`.
  sealed interface BlockTag {}

  /// A block tag whose description is converted to Markdown.
  ///
  /// @param head the tag name and any arguments, e.g. `@param x` or `@throws IOException`
  /// @param description the description, which may be empty
  record ConvertedBlockTag(String head, ImmutableList<Block> description) implements BlockTag {}

  /// A block tag that is emitted as-is, without being converted or validated, e.g. `@see`.
  record VerbatimBlockTag(String text) implements BlockTag {}

  /// A Markdown block, such as a paragraph or a list.
  sealed interface Block {}

  /// A paragraph of inline content.
  record Paragraph(ImmutableList<Inline> inlines) implements Block {}

  /// An ATX heading, e.g. `## Heading`.
  record Heading(int level, ImmutableList<Inline> inlines) implements Block {}

  /// A bullet list or an ordered list.
  ///
  /// @param start the number of the first item of an ordered list
  /// @param items the items, each of which is a list of blocks
  record ListBlock(boolean ordered, int start, ImmutableList<ImmutableList<Block>> items)
      implements Block {}

  /// A fenced code block.
  ///
  /// @param info the info string (typically the language), possibly empty
  /// @param literal the contents of the code block, without a trailing newline
  record CodeBlock(String info, String literal) implements Block {}

  /// A block of raw HTML, which may also contain traditional inline tags such as `{@code ...}`.
  record HtmlBlock(String html) implements Block {}

  /// Markdown inline content.
  sealed interface Inline {}

  /// Literal text, which is escaped as necessary when printed.
  record Text(String literal) implements Inline {}

  /// A code span, e.g. `` `foo` ``.
  record Code(String literal) implements Inline {}

  /// Emphasis (e.g. `*foo*`) or strong emphasis (e.g. `**foo**`).
  ///
  /// @param tag the name of the HTML element that the emphasis was converted from (`i`, `em`, `b`
  ///   or `strong`), which is used if the emphasis can't be written with delimiters
  /// @param delimiter the delimiter chosen by [MarkdownCommentPrinter] (`*` or `_`), or `\0` before
  ///   resolution
  record Emphasis(String tag, ImmutableList<Inline> inlines, char delimiter) implements Inline {
    Emphasis(String tag, ImmutableList<Inline> inlines) {
      this(tag, inlines, '\0');
    }

    boolean strong() {
      return tag.equals("b") || tag.equals("strong");
    }
  }

  /// An inline link, e.g. `[text](https://example.com)`.
  record Link(String destination, ImmutableList<Inline> inlines) implements Inline {}

  /// A link to a program element, e.g. `[Foo#bar]` or `[label][Foo#bar]`.
  ///
  /// @param reference the reference, e.g. `Foo#bar(int[])`
  /// @param label the label, or `null` to use the reference as the label
  record ReferenceLink(String reference, @Nullable ImmutableList<Inline> label) implements Inline {}

  /// A Javadoc inline tag that is emitted as-is, e.g. `{@inheritDoc}` or `{@literal <T>}`.
  record InlineTag(String text) implements Inline {}

  /// A Javadoc inline tag whose body javadoc parses as Markdown, e.g. `{@summary ...}` or a kept
  /// `{@link ref label}`.
  ///
  /// @param head the tag name and any non-Markdown prefix, without leading `@`, e.g. `summary`,
  ///   `index "term"`, or `link Foo`
  /// @param body the Markdown body of the tag, which may be empty
  record MarkdownInlineTag(String head, ImmutableList<Block> body) implements Inline {}

  /// Raw inline HTML, e.g. `<sup>` or `<!-- comment -->`.
  record Html(String html) implements Inline {}

  /// An HTML entity, e.g. `&lt;`.
  record Entity(String text) implements Inline {}

  /// A line break within a paragraph.
  enum SoftBreak implements Inline {
    INSTANCE
  }
}
