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

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.bugpatterns.javadoc.MarkdownComment.Block;
import com.google.errorprone.bugpatterns.javadoc.MarkdownComment.Code;
import com.google.errorprone.bugpatterns.javadoc.MarkdownComment.CodeBlock;
import com.google.errorprone.bugpatterns.javadoc.MarkdownComment.Emphasis;
import com.google.errorprone.bugpatterns.javadoc.MarkdownComment.Entity;
import com.google.errorprone.bugpatterns.javadoc.MarkdownComment.Heading;
import com.google.errorprone.bugpatterns.javadoc.MarkdownComment.Html;
import com.google.errorprone.bugpatterns.javadoc.MarkdownComment.HtmlBlock;
import com.google.errorprone.bugpatterns.javadoc.MarkdownComment.Inline;
import com.google.errorprone.bugpatterns.javadoc.MarkdownComment.InlineTag;
import com.google.errorprone.bugpatterns.javadoc.MarkdownComment.Link;
import com.google.errorprone.bugpatterns.javadoc.MarkdownComment.ListBlock;
import com.google.errorprone.bugpatterns.javadoc.MarkdownComment.MarkdownInlineTag;
import com.google.errorprone.bugpatterns.javadoc.MarkdownComment.Paragraph;
import com.google.errorprone.bugpatterns.javadoc.MarkdownComment.ReferenceLink;
import com.google.errorprone.bugpatterns.javadoc.MarkdownComment.SoftBreak;
import com.google.errorprone.bugpatterns.javadoc.MarkdownComment.Text;
import com.google.errorprone.bugpatterns.javadoc.MarkdownCommentPrinter.PrintedComment;
import com.google.errorprone.bugpatterns.javadoc.MarkdownCommentPrinter.Region;
import java.util.List;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.BulletList;
import org.commonmark.node.Document;
import org.commonmark.node.FencedCodeBlock;
import org.commonmark.node.HtmlInline;
import org.commonmark.node.Image;
import org.commonmark.node.LinkReferenceDefinition;
import org.commonmark.node.ListItem;
import org.commonmark.node.Node;
import org.commonmark.node.OrderedList;
import org.commonmark.node.SoftLineBreak;
import org.commonmark.node.StrongEmphasis;
import org.commonmark.parser.InlineParserContext;
import org.commonmark.parser.Parser;
import org.commonmark.parser.beta.LinkInfo;
import org.commonmark.parser.beta.LinkResult;
import org.commonmark.parser.beta.Scanner;

/// Checks that the Markdown printed for a comment is parsed back into the intended structure.
///
/// This guards against mistakes in [MarkdownCommentPrinter] (e.g. a missing escape) changing the
/// meaning of a comment, and against `DocCommentParser` lexical hazards in `///` mode. Each region
/// of the comment is parsed the way javadoc parses it: inline tags have been replaced by a
/// placeholder character, and a link whose label is a reference to a program element (e.g.
/// `[Foo#bar]`) is a link to that program element.
final class MarkdownCommentValidator {

  /// The scheme of the destinations of links to program elements.
  private static final String REFERENCE_SCHEME = "javadoc-reference:";

  private static final Parser PARSER =
      Parser.builder()
          .extensions(ImmutableList.of(TablesExtension.create()))
          .linkProcessor(MarkdownCommentValidator::processLink)
          .build();

  /// A parser for decoding entities and link destinations.
  private static final Parser PLAIN_PARSER = Parser.builder().build();

  /// ASCII HTML whitespace (excluding non-breaking spaces like `\u00A0`).
  private static final CharMatcher WHITESPACE = CharMatcher.anyOf(" \t\n\r\f");

  private static final Splitter NEWLINE_SPLITTER = Splitter.on('\n');

  /// Standard inline tags whose bodies DocCommentParser parses as Markdown (`Phase.INLINE`).
  private static final ImmutableSet<String> MARKDOWN_INLINE_TAGS =
      ImmutableSet.of("index", "link", "linkplain", "return", "summary");

  /// Returns true if every region of the given comment parses into the intended structure and has
  /// no `DocCommentParser` lexical hazards.
  static boolean isValid(PrintedComment comment) {
    for (String line : comment.lines()) {
      if (line.indexOf('\n') >= 0 || line.indexOf('\r') >= 0) {
        return false;
      }
    }
    if (hasBackslashTripleAdvanceHazard(comment.lines())) {
      return false;
    }
    for (Region region : comment.regions()) {
      if (!hasValidHtmlBlocks(region.blocks())) {
        return false;
      }
      Signature expected = new Signature();
      expected.blocks(region.blocks());
      Signature actual = new Signature();
      actual.node(PARSER.parse(region.markdown()));
      if (!expected.build().equals(actual.build())) {
        return false;
      }
    }
    return true;
  }

  /// Checks raw HTML blocks and inline HTML against `DocCommentParser` lexical hazards in `///`
  /// mode (where HTML comments and `<pre>` blocks are not parsed specially by `DocCommentParser`).
  private static boolean hasValidHtmlBlocks(List<Block> blocks) {
    for (Block block : blocks) {
      switch (block) {
        case HtmlBlock h -> {
          if (!isValidHtmlBlock(h.html())) {
            return false;
          }
        }
        case Paragraph p -> {
          if (!hasValidHtmlInlines(p.inlines())) {
            return false;
          }
        }
        case Heading h -> {
          if (!hasValidHtmlInlines(h.inlines())) {
            return false;
          }
        }
        case ListBlock l -> {
          for (ImmutableList<Block> item : l.items()) {
            if (!hasValidHtmlBlocks(item)) {
              return false;
            }
          }
        }
        case CodeBlock c -> {}
      }
    }
    return true;
  }

  private static boolean hasValidHtmlInlines(List<Inline> inlines) {
    for (Inline inline : inlines) {
      switch (inline) {
        case Html h -> {
          if (h.html().contains("*@/")
              || (h.html().startsWith("<!--") && h.html().contains("{@"))) {
            return false;
          }
        }
        case Emphasis e -> {
          if (!hasValidHtmlInlines(e.inlines())) {
            return false;
          }
        }
        case Link l -> {
          if (!hasValidHtmlInlines(l.inlines())) {
            return false;
          }
        }
        case ReferenceLink r when r.label() != null -> {
          if (!hasValidHtmlInlines(r.label())) {
            return false;
          }
        }
        case MarkdownInlineTag t -> {
          if (!hasValidHtmlBlocks(t.body())) {
            return false;
          }
        }
        default -> {}
      }
    }
    return true;
  }

  private static boolean isValidHtmlBlock(String html) {
    boolean hasInlineTag = html.contains("{@");
    boolean prevBlank = true;
    boolean inHtmlComment = false;
    for (String line : NEWLINE_SPLITTER.split(html)) {
      if (line.contains("*@/")) {
        return false;
      }
      int indent = 0;
      int first = 0;
      while (first < line.length()) {
        char c = line.charAt(first);
        if (c == ' ') {
          indent++;
          first++;
        } else if (c == '\t') {
          indent += 4 - (indent % 4);
          first++;
        } else {
          break;
        }
      }
      if (first == line.length()) {
        prevBlank = true;
        continue;
      }
      // `@` at line start begins a block tag (or `@@`/`@*` escape) in DocCommentParser even inside
      // HTML comments and raw HTML blocks.
      if (line.charAt(first) == '@') {
        return false;
      }
      // A 4-column indented line after a blank line triggers INDENTED_CODE in DocCommentParser,
      // which calls skipLine() and prints inline tags literally.
      if (prevBlank && indent >= 4 && line.contains("{@")) {
        return false;
      }
      // A code fence inside an HTML block triggers FENCED_CODE in DocCommentParser, suppressing
      // inline tags.
      if (indent < 4
          && hasInlineTag
          && (line.startsWith("```", first) || line.startsWith("~~~", first))) {
        return false;
      }
      // Inside `<!-- ... -->`, DocCommentParser in `///` mode expands `{@...}` tags that
      // traditional Javadoc ignores.
      if (inHtmlComment || line.contains("<!--")) {
        int i = 0;
        while (i < line.length()) {
          if (inHtmlComment) {
            if (line.startsWith("-->", i)) {
              inHtmlComment = false;
              i += 3;
            } else if (line.startsWith("{@", i)) {
              return false;
            } else {
              i++;
            }
          } else if (line.startsWith("<!--", i)) {
            inHtmlComment = true;
            i += 4;
          } else {
            i++;
          }
        }
      }
      // Backticks on a line in `///` mode trigger skipCode() in DocCommentParser, suppressing `{@`.
      if (!inHtmlComment && line.indexOf('`') >= 0 && line.contains("{@")) {
        return false;
      }
      prevBlank = false;
    }
    return true;
  }

  /// Checks for OpenJDK 25's `DocCommentParser` `case '\\'` triple-advance bug in `MARKDOWN` mode,
  /// where `\` consumes `\`, the escaped character, and the following character without inspecting
  /// that third character. If that third character is EOF (appending `\u001a`), `{`, `}`, `` ` ``,
  /// `~`, or `\n` before a block tag/marker, the parsed doc tree is corrupted.
  private static boolean hasBackslashTripleAdvanceHazard(ImmutableList<String> lines) {
    String text = String.join("\n", lines);
    int len = text.length();
    int i = 0;
    boolean atLineStart = true;
    char openFenceChar = 0;
    int openFenceLen = 0;

    while (i < len) {
      if (atLineStart) {
        atLineStart = false;
        int lineEnd = text.indexOf('\n', i);
        int end = lineEnd < 0 ? len : lineEnd;
        int pos = skipListAndSpacePrefix(text, i, end);
        if (openFenceChar != 0) {
          if (isClosingFence(text, pos, end, openFenceChar, openFenceLen)) {
            openFenceChar = 0;
            openFenceLen = 0;
          }
          i = lineEnd < 0 ? len : lineEnd + 1;
          atLineStart = true;
          continue;
        }
        int fenceLen = openingFenceLength(text, pos, end);
        if (fenceLen >= 3) {
          openFenceChar = text.charAt(pos);
          openFenceLen = fenceLen;
          i = lineEnd < 0 ? len : lineEnd + 1;
          atLineStart = true;
          continue;
        }
        // Quoted `@see "..."` is parsed as Kind.TEXT, not MARKDOWN.
        if (text.startsWith("@see \"", pos)) {
          i = lineEnd < 0 ? len : lineEnd + 1;
          atLineStart = true;
          continue;
        }
      }

      char c = text.charAt(i);
      if (c == '\n') {
        atLineStart = true;
        i++;
        continue;
      }
      if (c == '`') {
        int runEnd = i;
        while (runEnd < len && text.charAt(runEnd) == '`') {
          runEnd++;
        }
        int runLen = runEnd - i;
        int close = findClosingBackticks(text, runEnd, runLen);
        if (close >= 0) {
          i = close + runLen;
          continue;
        }
        i = runEnd;
        continue;
      }
      if (c == '{'
          && i + 1 < len
          && text.charAt(i + 1) == '@'
          && (i + 2 >= len || text.charAt(i + 2) != '@')) {
        int nameStart = i + 2;
        int nameEnd = nameStart;
        while (nameEnd < len
            && (Character.isUnicodeIdentifierPart(text.charAt(nameEnd))
                || text.charAt(nameEnd) == '.'
                || text.charAt(nameEnd) == '-'
                || text.charAt(nameEnd) == ':')) {
          nameEnd++;
        }
        String tagName = text.substring(nameStart, nameEnd);
        if (!MARKDOWN_INLINE_TAGS.contains(tagName)) {
          // Non-Markdown inline tags ({@code}, {@literal}, {@snippet}, {@value}, unknown tags) are
          // parsed as Kind.TEXT, so `case '\\'` does not run inside them.
          i = skipBracedTag(text, nameEnd);
          continue;
        }
      }
      if (c == '\\') {
        if (i + 2 >= len) {
          // Swallows EOI (`\u001a`) into the comment text.
          return true;
        }
        char third = text.charAt(i + 2);
        if (third == '{' || third == '}' || third == '`' || third == '~') {
          return true;
        }
        if (third == '\n' && isHazardousNextLine(text, i + 3)) {
          return true;
        }
        i += 3;
        continue;
      }
      i++;
    }
    return false;
  }

  private static int skipListAndSpacePrefix(String text, int start, int end) {
    int p = start;
    while (p < end) {
      while (p < end && (text.charAt(p) == ' ' || text.charAt(p) == '\t')) {
        p++;
      }
      if (p < end
          && (text.charAt(p) == '-' || text.charAt(p) == '+' || text.charAt(p) == '*')
          && p + 1 < end
          && (text.charAt(p + 1) == ' ' || text.charAt(p + 1) == '\t')) {
        p += 2;
        continue;
      }
      int digits = p;
      while (digits < end && text.charAt(digits) >= '0' && text.charAt(digits) <= '9') {
        digits++;
      }
      if (digits > p
          && digits - p <= 9
          && digits + 1 < end
          && (text.charAt(digits) == '.' || text.charAt(digits) == ')')
          && (text.charAt(digits + 1) == ' ' || text.charAt(digits + 1) == '\t')) {
        p = digits + 2;
        continue;
      }
      break;
    }
    return p;
  }

  private static int openingFenceLength(String text, int pos, int end) {
    if (pos >= end) {
      return 0;
    }
    char c = text.charAt(pos);
    if (c != '`' && c != '~') {
      return 0;
    }
    int p = pos;
    while (p < end && text.charAt(p) == c) {
      p++;
    }
    int count = p - pos;
    if (count < 3) {
      return 0;
    }
    if (c == '`') {
      for (int k = p; k < end; k++) {
        if (text.charAt(k) == '`') {
          return 0;
        }
      }
    }
    return count;
  }

  private static boolean isClosingFence(
      String text, int pos, int end, char fenceChar, int fenceLen) {
    int p = pos;
    while (p < end && text.charAt(p) == fenceChar) {
      p++;
    }
    if (p - pos < fenceLen) {
      return false;
    }
    while (p < end) {
      if (text.charAt(p) != ' ' && text.charAt(p) != '\t') {
        return false;
      }
      p++;
    }
    return true;
  }

  private static int findClosingBackticks(String text, int from, int length) {
    int i = from;
    int len = text.length();
    while (i < len) {
      char c = text.charAt(i);
      if (c == '\n' && i + 1 < len && text.charAt(i + 1) == '\n') {
        return -1;
      }
      if (c != '`') {
        i++;
        continue;
      }
      int runEnd = i;
      while (runEnd < len && text.charAt(runEnd) == '`') {
        runEnd++;
      }
      if (runEnd - i == length) {
        return i;
      }
      i = runEnd;
    }
    return -1;
  }

  private static int skipBracedTag(String text, int from) {
    int depth = 1;
    int i = from;
    while (i < text.length() && depth > 0) {
      char c = text.charAt(i++);
      if (c == '{') {
        depth++;
      } else if (c == '}') {
        depth--;
      }
    }
    return i;
  }

  /// Returns true if skipping `initMarkdownLine()` / `newline = true` at `nextLineStart` would
  /// misparse the next line (e.g. swallowing a `@blockTag`, code fence, list marker, or indented
  /// code line).
  private static boolean isHazardousNextLine(String text, int nextLineStart) {
    int len = text.length();
    if (nextLineStart >= len) {
      return false;
    }
    int indent = 0;
    int p = nextLineStart;
    while (p < len) {
      char c = text.charAt(p);
      if (c == ' ') {
        indent++;
        p++;
      } else if (c == '\t') {
        indent += 4 - (indent % 4);
        p++;
      } else {
        break;
      }
    }
    if (p >= len || text.charAt(p) == '\n') {
      // Blank line: the following '\n' will reset newline and call initMarkdownLine().
      return false;
    }
    if (indent >= 4) {
      return true;
    }
    char first = text.charAt(p);
    return first == '@'
        || first == '`'
        || first == '~'
        || first == '#'
        || first == '>'
        || first == '-'
        || first == '+'
        || first == '*'
        || first == '='
        || first == '_'
        || (first >= '0' && first <= '9');
  }

  /// Handles links that javadoc treats as links to program elements: reference links (e.g.
  /// `[Foo#bar]` or `[text][Foo#bar]`) without a link reference definition, whose label is a
  /// reference.
  private static LinkResult processLink(
      LinkInfo linkInfo, Scanner scanner, InlineParserContext context) {
    if (linkInfo.destination() != null) {
      return LinkResult.none();
    }
    String label =
        linkInfo.label() != null && !linkInfo.label().isEmpty()
            ? linkInfo.label()
            : linkInfo.text();
    if (context.getDefinition(LinkReferenceDefinition.class, label) != null) {
      return LinkResult.none();
    }
    // javadoc removes the backslashes from escaped brackets in references, e.g. `int\[\]`.
    String reference = label.replace("\\[\\]", "[]");
    if (!MarkdownCommentPrinter.isReferenceLike(reference)) {
      return LinkResult.none();
    }
    String destination = REFERENCE_SCHEME + normalizeReference(reference);
    return linkInfo.marker() != null
        ? LinkResult.wrapTextIn(new Image(destination, null), scanner.position()).includeMarker()
        : LinkResult.wrapTextIn(
            new org.commonmark.node.Link(destination, null), scanner.position());
  }

  private static String normalizeReference(String reference) {
    return WHITESPACE.trimFrom(WHITESPACE.collapseFrom(reference, ' '));
  }

  /// Returns the destination that CommonMark parses for a link whose destination is the given text.
  private static String expectedDestination(String destination) {
    Node document =
        PLAIN_PARSER.parse(
            "[x](<"
                + destination.replace("\\", "\\\\").replace("<", "\\<").replace(">", "\\>")
                + ">)");
    if (document.getFirstChild() instanceof org.commonmark.node.Paragraph paragraph
        && paragraph.getFirstChild() instanceof org.commonmark.node.Link link) {
      return link.getDestination();
    }
    // There is no valid link with this destination, so the signatures won't match.
    return "\0" + destination;
  }

  /// Returns the text that CommonMark parses for the given entity.
  private static String decodeEntity(String entity) {
    StringBuilder text = new StringBuilder();
    PLAIN_PARSER
        .parse(entity)
        .accept(
            new AbstractVisitor() {
              @Override
              public void visit(org.commonmark.node.Text node) {
                text.append(node.getLiteral());
              }
            });
    return text.toString();
  }

  /// A string that describes the structure of a Markdown document, ignoring differences that don't
  /// affect how it is rendered (e.g. whether a list is tight or loose, or how text is split into
  /// nodes).
  private static final class Signature {
    private final StringBuilder signature = new StringBuilder();
    private final StringBuilder text = new StringBuilder();
    private final StringBuilder html = new StringBuilder();

    /// Whether the next text starts a block, so that leading whitespace is insignificant.
    private boolean atBlockStart = true;

    String build() {
      flush(/* atBlockEnd= */ true);
      return signature.toString();
    }

    // The structure of a MarkdownComment.

    void blocks(List<Block> blocks) {
      for (Block block : blocks) {
        block(block);
      }
    }

    private void block(Block block) {
      switch (block) {
        case Paragraph p -> {
          if (p.inlines().stream().allMatch(Signature::isHtmlOrWhitespace)) {
            StringBuilder content = new StringBuilder();
            for (Inline inline : p.inlines()) {
              content.append(inline instanceof Html h ? h.html() : " ");
            }
            htmlBlock(content.toString());
          } else {
            open("P");
            inlines(p.inlines());
            close();
          }
        }
        case Heading h -> {
          open("H" + h.level());
          inlines(h.inlines());
          close();
        }
        case ListBlock l -> {
          open(l.ordered() ? "OL" + l.start() : "UL");
          for (ImmutableList<Block> item : l.items()) {
            open("LI");
            blocks(item);
            close();
          }
          close();
        }
        case CodeBlock c -> codeBlock(c.info(), c.literal());
        case HtmlBlock h -> htmlBlock(h.html());
      }
    }

    private void inlines(List<Inline> inlines) {
      for (Inline inline : inlines) {
        inline(inline);
      }
    }

    private void inline(Inline inline) {
      switch (inline) {
        case Text t -> text(t.literal());
        case Code c -> leaf("CODE", c.literal());
        case Emphasis e -> {
          open(e.strong() ? "STRONG" : "EM");
          inlines(e.inlines());
          close();
        }
        case Link l -> {
          open("LINK" + quote(expectedDestination(l.destination())));
          inlines(l.inlines());
          close();
        }
        case ReferenceLink r -> {
          open("LINK" + quote(REFERENCE_SCHEME + normalizeReference(r.reference())));
          if (r.label() == null) {
            text(r.reference());
          } else {
            inlines(r.label());
          }
          close();
        }
        case InlineTag t -> text(String.valueOf(MarkdownCommentPrinter.PLACEHOLDER));
        case MarkdownInlineTag t -> text(String.valueOf(MarkdownCommentPrinter.PLACEHOLDER));
        case Html h -> html(h.html());
        case Entity e -> text(decodeEntity(e.text()));
        case SoftBreak s -> text(" ");
      }
    }

    // The structure of a parsed CommonMark document.

    void node(Node node) {
      switch (node) {
        case Document d -> children(d);
        case org.commonmark.node.Paragraph p -> {
          if (isHtmlOrWhitespace(p)) {
            StringBuilder content = new StringBuilder();
            for (Node child = p.getFirstChild(); child != null; child = child.getNext()) {
              content.append(child instanceof HtmlInline h ? h.getLiteral() : " ");
            }
            htmlBlock(content.toString());
          } else {
            open("P");
            children(p);
            close();
          }
        }
        case org.commonmark.node.Heading h -> {
          open("H" + h.getLevel());
          children(h);
          close();
        }
        case BulletList l -> {
          open("UL");
          children(l);
          close();
        }
        case OrderedList l -> {
          open("OL" + l.getMarkerStartNumber());
          children(l);
          close();
        }
        case ListItem i -> {
          open("LI");
          children(i);
          close();
        }
        case FencedCodeBlock c -> codeBlock(c.getInfo(), withoutFinalNewline(c));
        case org.commonmark.node.HtmlBlock h -> htmlBlock(h.getLiteral());
        case org.commonmark.node.Text t -> text(t.getLiteral());
        case org.commonmark.node.Code c -> leaf("CODE", c.getLiteral());
        case org.commonmark.node.Emphasis e -> {
          open("EM");
          children(e);
          close();
        }
        case StrongEmphasis e -> {
          open("STRONG");
          children(e);
          close();
        }
        case org.commonmark.node.Link l -> {
          open("LINK" + quote(l.getDestination()));
          children(l);
          close();
        }
        case HtmlInline h -> html(h.getLiteral());
        case SoftLineBreak b -> text(" ");
        // Anything else, e.g. a block quote, indented code block, or hard line break, is never
        // intended.
        default -> {
          open(node.getClass().getSimpleName());
          children(node);
          close();
        }
      }
    }

    private static boolean isHtmlOrWhitespace(Inline inline) {
      return inline instanceof Html
          || inline instanceof SoftBreak
          || (inline instanceof Text t && WHITESPACE.matchesAllOf(t.literal()));
    }

    private static boolean isHtmlOrWhitespace(org.commonmark.node.Paragraph paragraph) {
      for (Node child = paragraph.getFirstChild(); child != null; child = child.getNext()) {
        if (!(child instanceof HtmlInline
            || child instanceof SoftLineBreak
            || (child instanceof org.commonmark.node.Text t
                && WHITESPACE.matchesAllOf(t.getLiteral())))) {
          return false;
        }
      }
      return true;
    }

    private static String withoutFinalNewline(FencedCodeBlock codeBlock) {
      String literal = codeBlock.getLiteral();
      return literal.endsWith("\n") ? literal.substring(0, literal.length() - 1) : literal;
    }

    private void children(Node node) {
      for (Node child = node.getFirstChild(); child != null; child = child.getNext()) {
        node(child);
      }
    }

    // Building the signature.

    private void open(String name) {
      flush(/* atBlockEnd= */ false);
      signature.append(name).append('[');
      atBlockStart = true;
    }

    private void close() {
      flush(/* atBlockEnd= */ true);
      signature.append(']');
      atBlockStart = false;
    }

    private void leaf(String name, String content) {
      flush(/* atBlockEnd= */ false);
      signature.append(name).append(quote(content));
      atBlockStart = false;
    }

    private void codeBlock(String info, String literal) {
      flush(/* atBlockEnd= */ false);
      signature.append("CODE").append(quote(info)).append(quote(literal));
      atBlockStart = false;
    }

    private void htmlBlock(String content) {
      leaf("HTMLBLOCK", WHITESPACE.trimFrom(WHITESPACE.collapseFrom(content, ' ')));
    }

    private void text(String s) {
      flushHtml();
      text.append(s);
    }

    private void html(String s) {
      flushText(/* atBlockEnd= */ false);
      html.append(s);
    }

    private void flush(boolean atBlockEnd) {
      flushText(atBlockEnd);
      flushHtml();
    }

    private void flushText(boolean atBlockEnd) {
      if (text.isEmpty()) {
        return;
      }
      String s = WHITESPACE.collapseFrom(text, ' ');
      text.setLength(0);
      if (atBlockStart) {
        s = WHITESPACE.trimLeadingFrom(s);
      }
      if (atBlockEnd) {
        s = WHITESPACE.trimTrailingFrom(s);
      }
      if (!s.isEmpty()) {
        signature.append(quote(s));
        atBlockStart = false;
      }
    }

    private void flushHtml() {
      if (html.isEmpty()) {
        return;
      }
      signature.append("HTML").append(quote(WHITESPACE.collapseFrom(html, ' ')));
      html.setLength(0);
      atBlockStart = false;
    }

    private static String quote(String s) {
      return '"' + s.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
    }
  }

  private MarkdownCommentValidator() {}
}
