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

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.bugpatterns.javadoc.MarkdownComment.Block;
import com.google.errorprone.bugpatterns.javadoc.MarkdownComment.BlockTag;
import com.google.errorprone.bugpatterns.javadoc.MarkdownComment.Code;
import com.google.errorprone.bugpatterns.javadoc.MarkdownComment.CodeBlock;
import com.google.errorprone.bugpatterns.javadoc.MarkdownComment.ConvertedBlockTag;
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
import com.google.errorprone.bugpatterns.javadoc.MarkdownComment.VerbatimBlockTag;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/// Prints a [MarkdownComment] as the text of a Markdown documentation comment.
///
/// The printer decides how each construct is spelled. It escapes text that would otherwise be
/// interpreted as Markdown or as a Javadoc tag, indents the content of list items, and chooses the
/// delimiters for emphasis. Where CommonMark's rules don't allow delimiters in a particular context
/// (e.g. `foo<b>(bar)</b>`), emphasis is printed as HTML instead.
final class MarkdownCommentPrinter {

  /// The character that javadoc substitutes for inline tags before parsing Markdown.
  static final char PLACEHOLDER = '\uFFFC';

  /// The names of the HTML elements that start an HTML block when they appear at the start of a
  /// line. See https://spec.commonmark.org/0.31.2/#html-blocks (start conditions 1 and 6).
  static final ImmutableSet<String> HTML_BLOCK_ELEMENTS =
      ImmutableSet.of(
          "address",
          "article",
          "aside",
          "base",
          "basefont",
          "blockquote",
          "body",
          "caption",
          "center",
          "col",
          "colgroup",
          "dd",
          "details",
          "dialog",
          "dir",
          "div",
          "dl",
          "dt",
          "fieldset",
          "figcaption",
          "figure",
          "footer",
          "form",
          "frame",
          "frameset",
          "h1",
          "h2",
          "h3",
          "h4",
          "h5",
          "h6",
          "head",
          "header",
          "hr",
          "html",
          "iframe",
          "legend",
          "li",
          "link",
          "main",
          "menu",
          "menuitem",
          "nav",
          "noframes",
          "ol",
          "optgroup",
          "option",
          "p",
          "param",
          "pre",
          "script",
          "search",
          "section",
          "style",
          "summary",
          "table",
          "tbody",
          "td",
          "textarea",
          "tfoot",
          "th",
          "thead",
          "title",
          "tr",
          "track",
          "ul");

  /// A printed comment.
  ///
  /// @param lines the lines of the comment, without the leading `///`
  /// @param regions the parts of the comment that javadoc parses as separate Markdown documents
  record PrintedComment(ImmutableList<String> lines, ImmutableList<Region> regions) {}

  /// A part of a comment that javadoc parses as a separate Markdown document: the main description,
  /// the description of a block tag, or the body of a Markdown-bearing inline tag.
  ///
  /// @param blocks the content of the region, in which any emphasis or adjacent code span that is
  ///   printed as HTML has been replaced by that HTML
  /// @param markdown the printed Markdown, with inline tags replaced by [#PLACEHOLDER] (as javadoc
  ///   does before parsing the Markdown)
  record Region(ImmutableList<Block> blocks, String markdown) {}

  /// Prints the given comment.
  static PrintedComment print(MarkdownComment comment) {
    MarkdownCommentPrinter printer = new MarkdownCommentPrinter();
    ImmutableList<Block> body = printer.resolveBlocks(comment.body());
    ImmutableList.Builder<Region> regions = ImmutableList.builder();
    printer.addRegions(body, regions);
    List<String> lines = new ArrayList<>(printer.printBlocks(body, /* placeholders= */ false));
    if (!lines.isEmpty() && !comment.blockTags().isEmpty()) {
      lines.add("");
    }
    for (BlockTag tag : comment.blockTags()) {
      switch (tag) {
        case ConvertedBlockTag t -> {
          ImmutableList<Block> description = printer.resolveBlocks(t.description());
          printer.addRegions(description, regions);
          ImmutableList<String> descriptionLines =
              printer.printBlocks(description, /* placeholders= */ false);
          if (descriptionLines.isEmpty()) {
            lines.add(t.head());
          } else if (description.get(0) instanceof Paragraph
              || (t.head().startsWith("@see") && descriptionLines.size() == 1)) {
            lines.add(t.head() + " " + descriptionLines.get(0));
            lines.addAll(descriptionLines.subList(1, descriptionLines.size()));
          } else {
            lines.add(t.head());
            lines.addAll(descriptionLines);
          }
        }
        case VerbatimBlockTag t -> {
          for (String line : NEWLINE_SPLITTER.split(t.text())) {
            lines.add(line.stripTrailing());
          }
        }
      }
    }
    return new PrintedComment(ImmutableList.copyOf(lines), regions.build());
  }

  /// Returns true if a line that starts with the given text would start an HTML block, and that
  /// HTML block could interrupt a paragraph (start conditions 1 to 6).
  static boolean startsHtmlBlock(String text) {
    return HTML_BLOCK_START.matcher(text).lookingAt();
  }

  /// Returns true if javadoc might interpret the given text, as the label of a Markdown link, as a
  /// reference to a program element (e.g. `Foo#bar(int)`).
  ///
  /// This over-approximates the references that javadoc accepts, in linear time.
  static boolean isReferenceLike(String text) {
    String s = text.strip();
    if (s.isEmpty()) {
      return false;
    }
    int slash = s.indexOf('/');
    int hash = s.indexOf('#', slash + 1);
    int lparen = s.indexOf('(', Math.max(slash, hash) + 1);
    if (lparen >= 0) {
      if (s.indexOf(')', lparen) != s.length() - 1) {
        return false;
      }
      s = s.substring(0, lparen);
    }
    if (hash >= 0 && hash + 1 < s.length() && s.charAt(hash + 1) == '#') {
      // OpenJDK ReferenceParser accepts arbitrary fragments (including spaces/newlines) after `##`.
      String beforeDoubleHash = s.substring(0, hash).strip();
      if (beforeDoubleHash.isEmpty()) {
        return true;
      }
      s = beforeDoubleHash;
    }
    String withoutTypeArgs = stripAngleBrackets(s);
    if (withoutTypeArgs == null) {
      return false;
    }
    String withoutAnnotations = stripTypeAnnotations(withoutTypeArgs);
    if (withoutAnnotations == null) {
      return false;
    }
    String reference = stripWhitespaceAroundPunctuation(withoutAnnotations);
    return !reference.isEmpty() && REFERENCE.matcher(reference).matches();
  }

  /// Removes balanced `<...>` type argument lists in a single linear pass, returning `null` if
  /// angle brackets are unbalanced.
  private static @Nullable String stripAngleBrackets(String s) {
    if (s.indexOf('<') < 0 && s.indexOf('>') < 0) {
      return s;
    }
    StringBuilder out = new StringBuilder(s.length());
    int depth = 0;
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c == '<') {
        depth++;
      } else if (c == '>') {
        if (--depth < 0) {
          return null;
        }
      } else if (depth == 0) {
        out.append(c);
      }
    }
    return depth == 0 ? out.toString() : null;
  }

  /// Removes Java type annotations (e.g. `@Nullable`, `@pkg.Anno(...)`) accepted by OpenJDK 25's
  /// `ReferenceParser.parseType`, in a single linear pass.
  private static @Nullable String stripTypeAnnotations(String s) {
    if (s.indexOf('@') < 0) {
      return s;
    }
    StringBuilder out = new StringBuilder(s.length());
    int i = 0;
    while (i < s.length()) {
      char c = s.charAt(i);
      if (c != '@') {
        out.append(c);
        i++;
        continue;
      }
      i++;
      while (i < s.length() && Character.isWhitespace(s.charAt(i))) {
        i++;
      }
      if (i >= s.length() || !Character.isJavaIdentifierStart(s.charAt(i))) {
        return null;
      }
      while (i < s.length()) {
        if (Character.isJavaIdentifierPart(s.charAt(i)) || s.charAt(i) == '.') {
          i++;
        } else {
          break;
        }
      }
      int afterName = i;
      while (i < s.length() && Character.isWhitespace(s.charAt(i))) {
        i++;
      }
      if (i < s.length() && s.charAt(i) == '(') {
        int depth = 1;
        i++;
        while (i < s.length() && depth > 0) {
          char p = s.charAt(i++);
          if (p == '(') {
            depth++;
          } else if (p == ')') {
            depth--;
          }
        }
        if (depth != 0) {
          return null;
        }
      } else {
        i = afterName;
      }
      while (i < s.length() && Character.isWhitespace(s.charAt(i))) {
        i++;
      }
    }
    return out.toString();
  }

  private static String stripWhitespaceAroundPunctuation(String s) {
    StringBuilder out = new StringBuilder(s.length());
    int i = 0;
    int len = s.length();
    while (i < len && Character.isWhitespace(s.charAt(i))) {
      i++;
    }
    while (i < len) {
      char c = s.charAt(i);
      if (Character.isWhitespace(c)) {
        int j = i + 1;
        while (j < len && Character.isWhitespace(s.charAt(j))) {
          j++;
        }
        if (j < len
            && !out.isEmpty()
            && !isReferencePunctuation(out.charAt(out.length() - 1))
            && !isReferencePunctuation(s.charAt(j))) {
          out.append(' ');
        }
        i = j;
      } else {
        out.append(c);
        i++;
      }
    }
    return out.toString();
  }

  private static boolean isReferencePunctuation(char c) {
    return ".,<>?&[]#()/".indexOf(c) >= 0;
  }

  private static final Splitter NEWLINE_SPLITTER = Splitter.on('\n');

  private static final Pattern HTML_BLOCK_START =
      Pattern.compile(
          "<(?:(?:script|pre|style|textarea)(?:[ \\t>]|$)|!--|\\?|![A-Za-z]|!\\[CDATA\\[|/?(?:"
              + String.join("|", HTML_BLOCK_ELEMENTS)
              + ")(?:[ \\t>]|/>|$))",
          Pattern.CASE_INSENSITIVE);

  /// A complete HTML start or end tag (CommonMark raw HTML grammar, including quoted attributes).
  private static final Pattern HTML_TAG =
      Pattern.compile(
          "</[A-Za-z][A-Za-z0-9-]*[ \\t\\n\\r\\f]*>"
              + "|<[A-Za-z][A-Za-z0-9-]*"
              + "(?:[ \\t\\n\\r\\f]+[A-Za-z_:][A-Za-z0-9_.:-]*"
              + "(?:[ \\t\\n\\r\\f]*=[ \\t\\n\\r\\f]*"
              + "(?:[^ \\t\\n\\r\\f\"'=<>`]+|'[^']*'|\"[^\"]*\"))?)*"
              + "[ \\t\\n\\r\\f]*/?>");

  private static final Pattern ENTITY =
      Pattern.compile("&(?:#[0-9]{1,7}|#[xX][0-9a-fA-F]{1,6}|[A-Za-z][A-Za-z0-9]{0,31});");

  /// CommonMark email autolink (https://spec.commonmark.org/0.31.2/#email-autolink).
  private static final Pattern EMAIL_AUTOLINK =
      Pattern.compile(
          "<[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@"
              + "[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?"
              + "(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*>");

  private static final Pattern SETEXT_UNDERLINE_OR_THEMATIC_BREAK =
      Pattern.compile(
          "(?:=+|-+)[ \\t]*"
              + "|\\*(?:[ \\t]*\\*){2,}[ \\t]*"
              + "|-(?:[ \\t]*-){2,}[ \\t]*"
              + "|_(?:[ \\t]*_){2,}[ \\t]*");

  private static final Pattern BARE_DESTINATION = Pattern.compile("[^\\s\\p{Cntrl}<>\\\\]+");

  private static final String IDENTIFIER =
      "[\\p{javaJavaIdentifierStart}][\\p{javaJavaIdentifierPart}]*";

  private static final String QUALIFIED_NAME = IDENTIFIER + "(?:\\." + IDENTIFIER + ")*";

  private static final Pattern REFERENCE =
      Pattern.compile(
          "(?:"
              + QUALIFIED_NAME
              + "/)?"
              + "(?:"
              + QUALIFIED_NAME
              + "(?:\\[\\])*)?"
              + "(?:#"
              + IDENTIFIER
              + ")?");

  private static final ImmutableList<Character> DELIMITERS = ImmutableList.of('*', '_');

  private MarkdownCommentPrinter() {}

  private void addRegions(ImmutableList<Block> blocks, ImmutableList.Builder<Region> regions) {
    regions.add(
        new Region(blocks, String.join("\n", printBlocks(blocks, /* placeholders= */ true))));
    addInlineTagRegions(blocks, regions);
  }

  private void addInlineTagRegions(List<Block> blocks, ImmutableList.Builder<Region> regions) {
    for (Block block : blocks) {
      switch (block) {
        case Paragraph p -> addInlineTagRegionsFromInlines(p.inlines(), regions);
        case Heading h -> addInlineTagRegionsFromInlines(h.inlines(), regions);
        case ListBlock l -> {
          for (ImmutableList<Block> item : l.items()) {
            addInlineTagRegions(item, regions);
          }
        }
        case CodeBlock c -> {}
        case HtmlBlock h -> {}
      }
    }
  }

  private void addInlineTagRegionsFromInlines(
      List<Inline> inlines, ImmutableList.Builder<Region> regions) {
    for (Inline inline : inlines) {
      switch (inline) {
        case MarkdownInlineTag t -> addRegions(t.body(), regions);
        case Emphasis e -> addInlineTagRegionsFromInlines(e.inlines(), regions);
        case Link l -> addInlineTagRegionsFromInlines(l.inlines(), regions);
        case ReferenceLink r when r.label() != null ->
            addInlineTagRegionsFromInlines(r.label(), regions);
        default -> {}
      }
    }
  }

  // ---------------------------------------------------------------------------------------------
  // Choosing delimiters for emphasis and resolving adjacent code spans.

  private ImmutableList<Block> resolveBlocks(List<Block> blocks) {
    return blocks.stream().map(this::resolveBlock).collect(toImmutableList());
  }

  private Block resolveBlock(Block block) {
    return switch (block) {
      case Paragraph p -> new Paragraph(resolveInlines(p.inlines(), '\n', '\n'));
      case Heading h -> new Heading(h.level(), resolveInlines(h.inlines(), ' ', '\n'));
      case ListBlock l ->
          new ListBlock(
              l.ordered(),
              l.start(),
              l.items().stream().map(this::resolveBlocks).collect(toImmutableList()));
      case CodeBlock c -> c;
      case HtmlBlock h -> h;
    };
  }

  /// Chooses a delimiter for each [Emphasis] in the given inlines (replacing emphasis that can't be
  /// written with delimiters by HTML), and converts adjacent [Code] spans to `<code>` HTML so their
  /// backtick fences don't merge.
  ///
  /// @param before the Unicode code point before the inlines
  /// @param after the Unicode code point after the inlines
  private ImmutableList<Inline> resolveInlines(List<Inline> inlines, int before, int after) {
    List<Inline> result = new ArrayList<>();
    for (int i = 0; i < inlines.size(); i++) {
      int previous = result.isEmpty() ? before : lastCodePoint(result.get(result.size() - 1));
      int next = i + 1 < inlines.size() ? firstCodePoint(inlines.get(i + 1)) : after;
      switch (inlines.get(i)) {
        case Emphasis e -> result.addAll(resolveEmphasis(e, previous, next));
        case Code c when !result.isEmpty() && result.get(result.size() - 1) instanceof Code -> {
          // Two adjacent backtick code spans (e.g. `` `a``b` ``) merge in CommonMark.
          result.add(new Html("<code>"));
          result.add(new Text(c.literal()));
          result.add(new Html("</code>"));
        }
        case Link l -> result.add(new Link(l.destination(), resolveInlines(l.inlines(), '[', ']')));
        case ReferenceLink r when r.label() != null ->
            result.add(new ReferenceLink(r.reference(), resolveInlines(r.label(), '[', ']')));
        case MarkdownInlineTag t ->
            result.add(new MarkdownInlineTag(t.head(), resolveBlocks(t.body())));
        case Inline inline -> result.add(inline);
      }
    }
    return ImmutableList.copyOf(result);
  }

  private ImmutableList<Inline> resolveEmphasis(Emphasis emphasis, int previous, int next) {
    for (char delimiter : DELIMITERS) {
      ImmutableList<Inline> content = resolveInlines(emphasis.inlines(), delimiter, delimiter);
      if (!content.isEmpty()
          && canUseDelimiter(
              delimiter,
              previous,
              firstCodePoint(content.get(0)),
              lastCodePoint(content.get(content.size() - 1)),
              next)) {
        return ImmutableList.of(new Emphasis(emphasis.tag(), content, delimiter));
      }
    }
    return ImmutableList.<Inline>builder()
        .add(new Html("<" + emphasis.tag() + ">"))
        .addAll(resolveInlines(emphasis.inlines(), '>', '<'))
        .add(new Html("</" + emphasis.tag() + ">"))
        .build();
  }

  /// Returns true if emphasis can be written with the given delimiter, given the code points before
  /// and after the opening and closing delimiter runs. See
  /// https://spec.commonmark.org/0.31.2/#emphasis-and-strong-emphasis.
  private static boolean canUseDelimiter(
      char delimiter, int before, int first, int last, int after) {
    if (before == delimiter || first == delimiter || last == delimiter || after == delimiter) {
      // The delimiter run would be merged with an adjacent one.
      return false;
    }
    boolean opens =
        isLeftFlanking(before, first)
            && (delimiter == '*' || !isRightFlanking(before, first) || isPunctuation(before));
    boolean closes =
        isRightFlanking(last, after)
            && (delimiter == '*' || !isLeftFlanking(last, after) || isPunctuation(after));
    return opens && closes;
  }

  private static boolean isLeftFlanking(int before, int after) {
    return !isWhitespace(after)
        && (!isPunctuation(after) || isWhitespace(before) || isPunctuation(before));
  }

  private static boolean isRightFlanking(int before, int after) {
    return !isWhitespace(before)
        && (!isPunctuation(before) || isWhitespace(after) || isPunctuation(after));
  }

  /// Returns the first code point of the given inline when it is printed, or an approximation of it
  /// that is equivalent for the purposes of escaping and choosing delimiters.
  private static int firstCodePoint(Inline inline) {
    return switch (inline) {
      case Text t -> t.literal().isEmpty() ? ' ' : t.literal().codePointAt(0);
      case Code c -> '`';
      // An emphasis whose delimiter hasn't been chosen yet might use either delimiter.
      case Emphasis e -> e.delimiter() != '\0' ? e.delimiter() : '*';
      case Link l -> '[';
      case ReferenceLink r -> '[';
      case InlineTag t -> PLACEHOLDER;
      case MarkdownInlineTag t -> PLACEHOLDER;
      case Html h -> h.html().isEmpty() ? ' ' : h.html().codePointAt(0);
      case Entity e -> '&';
      case SoftBreak s -> '\n';
    };
  }

  /// Returns the last code point of the given inline when it is printed, like [#firstCodePoint].
  private static int lastCodePoint(Inline inline) {
    return switch (inline) {
      case Text t ->
          t.literal().isEmpty() ? ' ' : t.literal().codePointBefore(t.literal().length());
      case Code c -> '`';
      case Emphasis e -> e.delimiter() != '\0' ? e.delimiter() : '*';
      case Link l -> ')';
      case ReferenceLink r -> ']';
      case InlineTag t -> PLACEHOLDER;
      case MarkdownInlineTag t -> PLACEHOLDER;
      case Html h -> h.html().isEmpty() ? ' ' : h.html().codePointBefore(h.html().length());
      case Entity e -> ';';
      case SoftBreak s -> '\n';
    };
  }

  // ---------------------------------------------------------------------------------------------
  // Printing blocks.

  /// Prints blocks, separated by blank lines.
  ///
  /// @param placeholders whether to print inline tags as [#PLACEHOLDER]
  private ImmutableList<String> printBlocks(List<Block> blocks, boolean placeholders) {
    ImmutableList.Builder<String> lines = ImmutableList.builder();
    Block previous = null;
    boolean alternateListMarker = false;
    for (Block block : blocks) {
      if (previous != null) {
        lines.add("");
      }
      switch (block) {
        case Paragraph p -> lines.addAll(printParagraph(p.inlines(), placeholders));
        case Heading h -> lines.add(printHeading(h, placeholders));
        case ListBlock l -> {
          // Consecutive lists of the same kind are only parsed as separate lists if they use
          // different markers.
          alternateListMarker =
              previous instanceof ListBlock p && p.ordered() == l.ordered() && !alternateListMarker;
          lines.addAll(printList(l, alternateListMarker, placeholders));
        }
        case CodeBlock c -> lines.addAll(printCodeBlock(c));
        case HtmlBlock h -> {
          for (String line : NEWLINE_SPLITTER.split(h.html())) {
            lines.add(line.stripTrailing());
          }
        }
      }
      previous = block;
    }
    return lines.build();
  }

  private ImmutableList<String> printParagraph(List<Inline> inlines, boolean placeholders) {
    InlineWriter writer =
        new InlineWriter(placeholders, /* startsLine= */ true, /* singleLine= */ false);
    writer.inlines(avoidHtmlBlockStart(inlines), '\n', /* inLinkLabel= */ false);
    ImmutableList.Builder<String> lines = ImmutableList.builder();
    for (String line : NEWLINE_SPLITTER.split(writer.out)) {
      lines.add(line.stripTrailing());
    }
    return lines.build();
  }

  /// A paragraph whose first line consists of a single HTML tag would be parsed as an HTML block
  /// (start condition 7), so we join its first two lines.
  private static List<Inline> avoidHtmlBlockStart(List<Inline> inlines) {
    if (inlines.size() > 2
        && inlines.get(0) instanceof Html html
        && inlines.get(1) instanceof SoftBreak
        && HTML_TAG.matcher(html.html()).matches()) {
      List<Inline> result = new ArrayList<>(inlines);
      result.set(1, new Text(" "));
      return result;
    }
    return inlines;
  }

  private String printHeading(Heading heading, boolean placeholders) {
    InlineWriter writer =
        new InlineWriter(placeholders, /* startsLine= */ false, /* singleLine= */ true);
    String prefix = "#".repeat(heading.level());
    writer.out.append(prefix).append(' ');
    writer.inlines(heading.inlines(), '\n', /* inLinkLabel= */ false);
    String line = writer.out.toString().stripTrailing();
    // A sequence of `#`s at the end of the heading preceded by space/tab would be parsed as a
    // closing sequence.
    int start = line.length();
    while (start > prefix.length() && line.charAt(start - 1) == '#') {
      start--;
    }
    if (start < line.length() && start > prefix.length() && isSpaceOrTab(line.charAt(start - 1))) {
      line = line.substring(0, start) + "\\" + line.substring(start);
    }
    return line;
  }

  private ImmutableList<String> printList(
      ListBlock list, boolean alternateMarker, boolean placeholders) {
    String marker =
        list.ordered()
            ? list.start() + (alternateMarker ? ")" : ".")
            : (alternateMarker ? "*" : "-");
    String indent = " ".repeat(marker.length() + 1);
    // Blank lines between the blocks of an item make the list loose, so we separate the items
    // with blank lines too.
    boolean loose = list.items().stream().anyMatch(item -> item.size() > 1);
    ImmutableList.Builder<String> lines = ImmutableList.builder();
    for (int i = 0; i < list.items().size(); i++) {
      if (i > 0 && loose) {
        lines.add("");
      }
      ImmutableList<Block> itemBlocks = list.items().get(i);
      ImmutableList<String> item = printBlocks(itemBlocks, placeholders);
      if (item.isEmpty()) {
        lines.add(marker);
        continue;
      }
      if (itemBlocks.get(0) instanceof ListBlock) {
        // Putting two list markers on the same line (e.g. `- - foo`) triggers an infinite loop in
        // OpenJDK 25's DocCommentParser.Markdown.update() and `- - -` parses as a thematic break.
        lines.add(marker);
        for (String line : item) {
          lines.add(line.isEmpty() ? "" : indent + line);
        }
        continue;
      }
      lines.add(marker + " " + item.get(0));
      for (String line : item.subList(1, item.size())) {
        lines.add(line.isEmpty() ? "" : indent + line);
      }
    }
    return lines.build();
  }

  private static ImmutableList<String> printCodeBlock(CodeBlock codeBlock) {
    String literal = codeBlock.literal();
    String info = codeBlock.info();
    char fenceChar = info.contains("`") ? '~' : '`';
    String fence =
        String.valueOf(fenceChar).repeat(Math.max(3, longestRun(literal, fenceChar) + 1));
    ImmutableList.Builder<String> lines = ImmutableList.builder();
    // Separate fence and info with a space if info starts with fenceChar (e.g. `~` fence with
    // `~foo` info), so the opening fence is not extended beyond the closing fence's length.
    lines.add(!info.isEmpty() && info.charAt(0) == fenceChar ? fence + " " + info : fence + info);
    if (!literal.isEmpty()) {
      for (String line : NEWLINE_SPLITTER.split(literal)) {
        lines.add(line.stripTrailing());
      }
    }
    lines.add(fence);
    return lines.build();
  }

  private static int longestRun(String s, char c) {
    int longest = 0;
    int current = 0;
    for (int i = 0; i < s.length(); i++) {
      current = s.charAt(i) == c ? current + 1 : 0;
      longest = Math.max(longest, current);
    }
    return longest;
  }

  // ---------------------------------------------------------------------------------------------
  // Printing inlines.

  /// Prints inline content.
  private final class InlineWriter {
    final StringBuilder out = new StringBuilder();

    /// Whether to print inline tags as [#PLACEHOLDER].
    private final boolean placeholders;

    /// Whether the content starts at the start of a line (as opposed to, e.g., after `# `).
    private final boolean startsLine;

    /// Whether line breaks should be printed as spaces.
    private final boolean singleLine;

    InlineWriter(boolean placeholders, boolean startsLine, boolean singleLine) {
      this.placeholders = placeholders;
      this.startsLine = startsLine;
      this.singleLine = singleLine;
    }

    /// Prints the given inlines.
    ///
    /// @param after the code point that will follow the inlines
    /// @param inLinkLabel whether the inlines are (part of) the text of a link
    void inlines(List<Inline> inlines, int after, boolean inLinkLabel) {
      for (int i = 0; i < inlines.size(); i++) {
        Inline next = i + 1 < inlines.size() ? inlines.get(i + 1) : null;
        inline(inlines.get(i), next, next != null ? firstCodePoint(next) : after, inLinkLabel);
      }
    }

    private void inline(
        Inline inline, @Nullable Inline next, int nextCodePoint, boolean inLinkLabel) {
      switch (inline) {
        case Text t -> text(t.literal(), nextCodePoint, inLinkLabel);
        case Code c -> code(c.literal());
        case Emphasis e -> {
          String delimiter = String.valueOf(e.delimiter()).repeat(e.strong() ? 2 : 1);
          out.append(delimiter);
          inlines(e.inlines(), e.delimiter(), inLinkLabel);
          out.append(delimiter);
        }
        case Link l -> {
          out.append('[');
          inlines(l.inlines(), ']', /* inLinkLabel= */ true);
          out.append("](").append(destination(l.destination())).append(')');
        }
        case ReferenceLink r -> {
          boolean atParagraphStart = out.isEmpty() && startsLine;
          out.append('[');
          if (r.label() != null) {
            inlines(r.label(), ']', /* inLinkLabel= */ true);
            out.append("][");
          }
          out.append(r.reference().replace("[", "\\[").replace("]", "\\]")).append(']');
          if (r.label() == null
              && (nextCodePoint == '('
                  || nextCodePoint == '['
                  || (nextCodePoint == ':' && atParagraphStart))) {
            // Use a collapsed reference link, since e.g. `[Foo](bar)` would be an inline link, and
            // `[Foo]: bar` a link reference definition.
            out.append("[]");
          }
        }
        case InlineTag t -> out.append(placeholders ? String.valueOf(PLACEHOLDER) : t.text());
        case MarkdownInlineTag t -> {
          if (placeholders) {
            out.append(PLACEHOLDER);
          } else if (t.body().isEmpty()) {
            out.append("{@").append(t.head()).append('}');
          } else {
            ImmutableList<String> bodyLines = printBlocks(t.body(), /* placeholders= */ false);
            out.append("{@")
                .append(t.head())
                .append(' ')
                .append(String.join(singleLine ? " " : "\n", bodyLines))
                .append('}');
          }
        }
        case Html h -> out.append(h.html());
        case Entity e -> out.append(e.text());
        case SoftBreak s -> {
          // A line that starts an HTML block would interrupt the paragraph.
          boolean join = singleLine || (next instanceof Html h && startsHtmlBlock(h.html()));
          out.append(join ? ' ' : '\n');
        }
      }
    }

    private boolean atLineStart() {
      return out.isEmpty() ? startsLine : out.charAt(out.length() - 1) == '\n';
    }

    private int previousCodePoint() {
      return out.isEmpty() ? (startsLine ? '\n' : ' ') : out.codePointBefore(out.length());
    }

    private void text(String text, int nextCodePoint, boolean inLinkLabel) {
      int @Nullable [] matchingClose = null;
      boolean @Nullable [] hasNestedNonEmptyBracket = null;
      if (!inLinkLabel && text.indexOf('[') >= 0) {
        int len = text.length();
        matchingClose = new int[len];
        Arrays.fill(matchingClose, -1);
        hasNestedNonEmptyBracket = new boolean[len];
        int[] stack = new int[len];
        boolean[] nestedStack = new boolean[len];
        int top = 0;
        for (int i = 0; i < len; i++) {
          char ch = text.charAt(i);
          if (ch == '[') {
            stack[top] = i;
            nestedStack[top] = false;
            top++;
          } else if (ch == ']' && top > 0) {
            top--;
            int openIdx = stack[top];
            matchingClose[openIdx] = i;
            hasNestedNonEmptyBracket[openIdx] = nestedStack[top];
            if (top > 0 && !isAllWhitespace(text, openIdx + 1, i)) {
              nestedStack[top - 1] = true;
            }
          }
        }
      }
      int lineStartEscape = -1;
      for (int i = 0; i < text.length(); ) {
        int cp = text.codePointAt(i);
        int charCount = Character.charCount(cp);
        if (cp == '\n') {
          out.append(singleLine ? ' ' : '\n');
          i += charCount;
          continue;
        }
        if (atLineStart()) {
          int end = text.indexOf('\n', i);
          String line = end < 0 ? text.substring(i) : text.substring(i, end);
          int index = lineStartEscape(line, /* wholeLine= */ end >= 0 || nextCodePoint == '\n');
          lineStartEscape = index < 0 ? -1 : i + index;
        }
        int following =
            i + charCount < text.length() ? text.codePointAt(i + charCount) : nextCodePoint;
        if (i == lineStartEscape
            || needsEscape(
                text,
                i,
                cp,
                following,
                nextCodePoint,
                inLinkLabel,
                matchingClose,
                hasNestedNonEmptyBracket)) {
          out.append('\\');
        }
        out.appendCodePoint(cp);
        i += charCount;
      }
    }

    /// Returns true if the code point at the given index needs to be escaped to be printed as
    /// literal text.
    private boolean needsEscape(
        String text,
        int index,
        int cp,
        int following,
        int nextCodePoint,
        boolean inLinkLabel,
        int @Nullable [] matchingClose,
        boolean @Nullable [] hasNestedNonEmptyBracket) {
      int previous = previousCodePoint();
      return switch (cp) {
        // A backslash before punctuation would be an escape, and before a line break a hard line
        // break. A backslash before an inline tag would stop javadoc from recognizing the tag.
        // Finally, an odd number of backslashes before `u` would be a Unicode escape in Java.
        case '\\' ->
            following == '\n'
                || following == PLACEHOLDER
                || following == 'u'
                || isAsciiPunctuation(following);
        case '`' -> true;
        // Emphasis delimiters, unless they can neither open nor close emphasis.
        case '*' -> !(isWhitespace(previous) && isWhitespace(following));
        case '_' ->
            !(isWhitespace(previous) && isWhitespace(following))
                && !(isAlphanumeric(previous) && isAlphanumeric(following));
        case '[' ->
            inLinkLabel
                || mayStartLink(
                    text, index, nextCodePoint, matchingClose, hasNestedNonEmptyBracket);
        case ']' -> inLinkLabel;
        // Inline HTML or an autolink (including email autolinks starting with digits/symbols).
        case '<' ->
            isAsciiLetter(following)
                || (following < 128 && "/!?".indexOf(following) >= 0)
                || EMAIL_AUTOLINK.matcher(text).region(index, text.length()).lookingAt();
        case '&' -> ENTITY.matcher(text).region(index, text.length()).lookingAt();
        // An image.
        case '!' -> following == '[';
        // An inline tag (`{@`) or Javadoc's `*@/` escape sequence.
        case '@' -> previous == '{' || (previous == '*' && following == '/');
        default -> false;
      };
    }

    /// Returns true if the `[` at the given index might start a link.
    private boolean mayStartLink(
        String text,
        int index,
        int nextCodePoint,
        int @Nullable [] matchingClose,
        boolean @Nullable [] hasNestedNonEmptyBracket) {
      int close = matchingClose == null ? -1 : matchingClose[index];
      if (close < 0) {
        // The closing bracket might be in a later inline.
        return true;
      }
      int afterClose = close + 1 < text.length() ? text.codePointAt(close + 1) : nextCodePoint;
      if (afterClose == '(' || afterClose == '[' || (afterClose == ':' && atLineStart())) {
        return true;
      }
      if (hasNestedNonEmptyBracket != null && hasNestedNonEmptyBracket[index]) {
        return false;
      }
      return isReferenceLike(text.substring(index + 1, close));
    }

    private void code(String literal) {
      String fence = "`".repeat(longestRun(literal, '`') + 1);
      // CommonMark strips one space from each end of a code span, if there is a space at each end,
      // and a code span can't start or end with its fence character.
      boolean pad =
          literal.startsWith("`")
              || literal.endsWith("`")
              || (literal.startsWith(" ") && literal.endsWith(" ") && !literal.isBlank());
      out.append(fence).append(pad ? " " : "").append(literal).append(pad ? " " : "").append(fence);
    }
  }

  private static boolean isAllWhitespace(String s, int start, int end) {
    for (int i = start; i < end; i++) {
      if (!Character.isWhitespace(s.charAt(i))) {
        return false;
      }
    }
    return true;
  }

  /// Returns the index of a character in the given line that needs to be escaped because the line
  /// would otherwise start a block, e.g. the `.` in `1. `, or -1.
  ///
  /// @param wholeLine whether the given text is the entire line
  private static int lineStartEscape(String line, boolean wholeLine) {
    if (line.isEmpty()) {
      return -1;
    }
    switch (line.charAt(0)) {
      case '#' -> {
        int count = countLeading(line, '#');
        if (count <= 6 && (count < line.length() ? isSpaceOrTab(line.charAt(count)) : wholeLine)) {
          return 0;
        }
      }
      // A block quote, or a block tag.
      case '>', '@' -> {
        return 0;
      }
      case '-', '+', '*' -> {
        if (line.length() > 1 ? isSpaceOrTab(line.charAt(1)) : wholeLine) {
          return 0;
        }
      }
      case '~' -> {
        if (line.startsWith("~~~")) {
          return 0;
        }
      }
      default -> {
        int digits = 0;
        while (digits < line.length() && isAsciiDigit(line.charAt(digits))) {
          digits++;
        }
        if (digits > 0
            && digits <= 9
            && digits < line.length()
            && (line.charAt(digits) == '.' || line.charAt(digits) == ')')
            && (digits + 1 < line.length() ? isSpaceOrTab(line.charAt(digits + 1)) : wholeLine)) {
          return digits;
        }
      }
    }
    if (wholeLine
        && (SETEXT_UNDERLINE_OR_THEMATIC_BREAK.matcher(line).matches()
            || isTableDelimiterRow(line))) {
      return 0;
    }
    return -1;
  }

  /// Returns true if the line could be parsed as a GFM table delimiter row (e.g. `--- | ---` or
  /// `:---`), consisting solely of spaces, tabs, `|`, `:`, and `-` with at least one `-` and at
  /// least one `|` or `:`.
  private static boolean isTableDelimiterRow(String line) {
    boolean hasDash = false;
    boolean hasPipeOrColon = false;
    for (int i = 0; i < line.length(); i++) {
      switch (line.charAt(i)) {
        case '-' -> hasDash = true;
        case '|', ':' -> hasPipeOrColon = true;
        case ' ', '\t' -> {}
        default -> {
          return false;
        }
      }
    }
    return hasDash && hasPipeOrColon;
  }

  private static int countLeading(String s, char c) {
    int count = 0;
    while (count < s.length() && s.charAt(count) == c) {
      count++;
    }
    return count;
  }

  private static String destination(String destination) {
    if (BARE_DESTINATION.matcher(destination).matches() && hasBalancedParentheses(destination)) {
      return destination;
    }
    return "<" + destination.replace("\\", "\\\\").replace("<", "\\<").replace(">", "\\>") + ">";
  }

  private static boolean hasBalancedParentheses(String s) {
    int depth = 0;
    for (int i = 0; i < s.length(); i++) {
      switch (s.charAt(i)) {
        case '(' -> depth++;
        case ')' -> {
          if (--depth < 0) {
            return false;
          }
        }
        default -> {}
      }
    }
    return depth == 0;
  }

  // ---------------------------------------------------------------------------------------------
  // Character classes, as defined by CommonMark (operating on Unicode code points).

  private static boolean isWhitespace(int cp) {
    return cp == ' '
        || cp == '\t'
        || cp == '\n'
        || cp == '\r'
        || cp == '\f'
        || Character.getType(cp) == Character.SPACE_SEPARATOR;
  }

  private static boolean isPunctuation(int cp) {
    if (cp < 128) {
      return isAsciiPunctuation(cp);
    }
    return switch (Character.getType(cp)) {
      case Character.DASH_PUNCTUATION,
          Character.START_PUNCTUATION,
          Character.END_PUNCTUATION,
          Character.CONNECTOR_PUNCTUATION,
          Character.OTHER_PUNCTUATION,
          Character.INITIAL_QUOTE_PUNCTUATION,
          Character.FINAL_QUOTE_PUNCTUATION,
          Character.MATH_SYMBOL,
          Character.CURRENCY_SYMBOL,
          Character.MODIFIER_SYMBOL,
          Character.OTHER_SYMBOL ->
          true;
      default -> false;
    };
  }

  private static boolean isAsciiPunctuation(int cp) {
    return cp < 128 && "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~".indexOf(cp) >= 0;
  }

  private static boolean isAlphanumeric(int cp) {
    return !isWhitespace(cp) && !isPunctuation(cp);
  }

  private static boolean isAsciiLetter(int cp) {
    return (cp >= 'a' && cp <= 'z') || (cp >= 'A' && cp <= 'Z');
  }

  private static boolean isAsciiDigit(int cp) {
    return cp >= '0' && cp <= '9';
  }

  private static boolean isSpaceOrTab(int cp) {
    return cp == ' ' || cp == '\t';
  }
}
