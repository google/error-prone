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

import com.google.common.base.Ascii;
import com.google.common.base.CharMatcher;
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
import com.sun.source.doctree.AttributeTree;
import com.sun.source.doctree.AuthorTree;
import com.sun.source.doctree.BlockTagTree;
import com.sun.source.doctree.CommentTree;
import com.sun.source.doctree.DeprecatedTree;
import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.EndElementTree;
import com.sun.source.doctree.EntityTree;
import com.sun.source.doctree.ErroneousTree;
import com.sun.source.doctree.EscapeTree;
import com.sun.source.doctree.HiddenTree;
import com.sun.source.doctree.IndexTree;
import com.sun.source.doctree.LinkTree;
import com.sun.source.doctree.LiteralTree;
import com.sun.source.doctree.ParamTree;
import com.sun.source.doctree.ProvidesTree;
import com.sun.source.doctree.ReferenceTree;
import com.sun.source.doctree.ReturnTree;
import com.sun.source.doctree.SeeTree;
import com.sun.source.doctree.SerialDataTree;
import com.sun.source.doctree.SerialFieldTree;
import com.sun.source.doctree.SerialTree;
import com.sun.source.doctree.SinceTree;
import com.sun.source.doctree.SnippetTree;
import com.sun.source.doctree.SpecTree;
import com.sun.source.doctree.StartElementTree;
import com.sun.source.doctree.SummaryTree;
import com.sun.source.doctree.SystemPropertyTree;
import com.sun.source.doctree.TextTree;
import com.sun.source.doctree.ThrowsTree;
import com.sun.source.doctree.UnknownBlockTagTree;
import com.sun.source.doctree.UnknownInlineTagTree;
import com.sun.source.doctree.UsesTree;
import com.sun.source.doctree.ValueTree;
import com.sun.source.doctree.VersionTree;
import com.sun.source.util.DocTrees;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/// Converts a traditional Javadoc comment into a [MarkdownComment].
///
/// This happens in two steps:
///
/// 1. javac represents HTML in a doc comment as a flat sequence of start and end elements. We first
///    build a tree of [Node]s from them, applying HTML's rules for implied end tags (e.g. `<li>`
///    closes a preceding `<li>`, and `<ul>` closes an open `<p>`).
/// 2. We then lower that tree into Markdown blocks and inlines. Each construct is converted to its
///    Markdown equivalent only when that conversion is faithful; otherwise it is kept as HTML
///    (which Markdown doc comments also support) or as a traditional inline tag. If neither is
///    possible, the comment is not converted at all.
final class MarkdownCommentBuilder {

  /// Thrown when a comment contains something that we cannot convert faithfully.
  static final class UnsupportedCommentException extends Exception {
    UnsupportedCommentException(String message) {
      super(message, null, false, false);
    }

    UnsupportedCommentException(String message, Throwable cause) {
      super(message, cause, false, false);
    }
  }

  /// Thrown when a link would be nested inside another link.
  private static final class NestedLinkException extends Exception {
    NestedLinkException() {
      super(null, null, false, false);
    }
  }

  /// Returns the Markdown equivalent of the given comment, or empty if it cannot be converted.
  static Optional<MarkdownComment> build(DocCommentTree docComment, DocTrees docTrees) {
    try {
      return Optional.of(new MarkdownCommentBuilder(docTrees).buildComment(docComment));
    } catch (UnsupportedCommentException e) {
      return Optional.empty();
    }
  }

  private final DocTrees docTrees;

  /// Whether HTML headings are converted to Markdown headings.
  private boolean convertHeadings;

  /// The level of the highest-level heading, which becomes `#`.
  private int minHeadingLevel = 1;

  private MarkdownCommentBuilder(DocTrees docTrees) {
    this.docTrees = docTrees;
  }

  private MarkdownComment buildComment(DocCommentTree docComment)
      throws UnsupportedCommentException {
    ImmutableList<Node> body = buildTree(docComment.getFullBody());
    List<ImmutableList<Node>> descriptions = new ArrayList<>();
    descriptions.add(body);
    for (DocTree tag : docComment.getBlockTags()) {
      if (!(tag instanceof SeeTree)) {
        descriptions.add(buildTree(description(tag)));
      }
    }
    decideHeadingPolicy(descriptions);

    ImmutableList<Block> bodyBlocks = new Lowering(/* blockContext= */ true).lowerBlocks(body);
    ImmutableList.Builder<BlockTag> blockTags = ImmutableList.builder();
    for (DocTree tag : docComment.getBlockTags()) {
      blockTags.add(lowerBlockTag(tag));
    }
    return new MarkdownComment(bodyBlocks, blockTags.build());
  }

  // ---------------------------------------------------------------------------------------------
  // Step 1: building a tree of HTML elements.

  /// A node in the tree of HTML elements built from javac's flat list of doc tree nodes.
  private sealed interface Node {}

  /// An HTML element and its content.
  ///
  /// @param name the lower-case element name
  /// @param end the end tag, or `null` if the element was closed implicitly or is a void element
  private record Element(
      StartElementTree start,
      String name,
      ImmutableList<Node> children,
      @Nullable EndElementTree end)
      implements Node {
    @Nullable String attribute(String attributeName) {
      for (DocTree attribute : start.getAttributes()) {
        if (attribute instanceof AttributeTree a
            && Ascii.equalsIgnoreCase(a.getName().toString(), attributeName)) {
          return a.getValue() == null ? null : serializeAttributeValue(a.getValue());
        }
      }
      return null;
    }

    ImmutableSet<String> attributeNames() {
      return start.getAttributes().stream()
          .filter(AttributeTree.class::isInstance)
          .map(a -> Ascii.toLowerCase(((AttributeTree) a).getName().toString()))
          .collect(ImmutableSet.toImmutableSet());
    }
  }

  /// Any other doc tree node: text, entities, inline tags, comments, and stray end tags.
  private record Leaf(DocTree tree) implements Node {}

  /// Elements that never have content.
  private static final ImmutableSet<String> VOID_ELEMENTS =
      ImmutableSet.of(
          "area", "base", "br", "col", "embed", "hr", "img", "input", "link", "meta", "param",
          "source", "track", "wbr");

  /// Start tags that implicitly close an open `<p>`.
  private static final ImmutableSet<String> CLOSES_PARAGRAPH =
      ImmutableSet.of(
          "address",
          "article",
          "aside",
          "blockquote",
          "center",
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
          "h1",
          "h2",
          "h3",
          "h4",
          "h5",
          "h6",
          "header",
          "hgroup",
          "hr",
          "li",
          "main",
          "menu",
          "nav",
          "ol",
          "p",
          "pre",
          "section",
          "summary",
          "table",
          "ul");

  /// Elements that limit the search for an open element to close in default scope.
  private static final ImmutableSet<String> SCOPE_BOUNDARIES =
      ImmutableSet.of(
          "applet", "button", "caption", "marquee", "object", "table", "td", "template", "th");

  /// Elements that limit the search for an open element to close in HTML5 table scope.
  private static final ImmutableSet<String> TABLE_SCOPE_BOUNDARIES =
      ImmutableSet.of("table", "template");

  /// Elements whose end tags are matched in HTML5 table scope rather than default scope.
  private static final ImmutableSet<String> TABLE_ELEMENTS =
      ImmutableSet.of("caption", "colgroup", "table", "tbody", "td", "tfoot", "th", "thead", "tr");

  private static final ImmutableSet<String> TABLE_ROW_BOUNDARIES =
      ImmutableSet.of("table", "tbody", "template", "tfoot", "thead");

  private static final ImmutableSet<String> TABLE_CELL_BOUNDARIES =
      ImmutableSet.of("table", "tbody", "template", "tfoot", "thead", "tr");

  private static final class OpenElement {
    final @Nullable StartElementTree start;
    final String name;
    final List<Object> children = new ArrayList<>();
    @Nullable EndElementTree end;

    OpenElement(@Nullable StartElementTree start, String name) {
      this.start = start;
      this.name = name;
    }

    ImmutableList<Node> freezeChildren() {
      return children.stream()
          .map(c -> c instanceof OpenElement o ? o.freeze() : (Node) c)
          .collect(toImmutableList());
    }

    Element freeze() {
      return new Element(start, name, freezeChildren(), end);
    }
  }

  /// Builds a tree of HTML elements from javac's flat list of doc tree nodes.
  private static ImmutableList<Node> buildTree(List<? extends DocTree> trees) {
    OpenElement root = new OpenElement(null, "");
    Deque<OpenElement> stack = new ArrayDeque<>();
    stack.push(root);
    for (DocTree tree : trees) {
      switch (tree) {
        case StartElementTree start -> {
          String name = Ascii.toLowerCase(start.getName().toString());
          closeImplied(stack, name);
          OpenElement element = new OpenElement(start, name);
          stack.peek().children.add(element);
          if (!start.isSelfClosing() && !VOID_ELEMENTS.contains(name)) {
            stack.push(element);
          }
        }
        case EndElementTree end -> {
          String name = Ascii.toLowerCase(end.getName().toString());
          ImmutableSet<String> boundaries =
              name.equals("table")
                  ? ImmutableSet.of("template")
                  : TABLE_ELEMENTS.contains(name) ? TABLE_SCOPE_BOUNDARIES : SCOPE_BOUNDARIES;
          OpenElement open = findOpen(stack, ImmutableSet.of(name), boundaries);
          if (open == null) {
            stack.peek().children.add(new Leaf(end));
          } else {
            popThrough(stack, open);
            open.end = end;
          }
        }
        default -> stack.peek().children.add(new Leaf(tree));
      }
    }
    return root.freezeChildren();
  }

  /// Closes any elements that are implicitly closed by a start tag with the given name.
  private static void closeImplied(Deque<OpenElement> stack, String name) {
    switch (name) {
      case "li" ->
          closeOpen(
              stack,
              ImmutableSet.of("li"),
              ImmutableSet.<String>builder().addAll(SCOPE_BOUNDARIES).add("ul", "ol").build());
      case "dt", "dd" ->
          closeOpen(
              stack,
              ImmutableSet.of("dt", "dd"),
              ImmutableSet.<String>builder().addAll(SCOPE_BOUNDARIES).add("dl").build());
      case "tr" -> {
        if (!tryCloseOpen(stack, ImmutableSet.of("tr"), TABLE_ROW_BOUNDARIES)) {
          closeOpen(
              stack, ImmutableSet.of("td", "th", "caption", "colgroup"), TABLE_ROW_BOUNDARIES);
        }
      }
      case "td", "th" -> closeOpen(stack, ImmutableSet.of("td", "th"), TABLE_CELL_BOUNDARIES);
      case "thead", "tbody", "tfoot", "caption", "colgroup" -> {
        if (!tryCloseOpen(
                stack,
                ImmutableSet.of("thead", "tbody", "tfoot", "caption", "colgroup"),
                TABLE_SCOPE_BOUNDARIES)
            && !tryCloseOpen(stack, ImmutableSet.of("tr"), TABLE_SCOPE_BOUNDARIES)) {
          closeOpen(stack, ImmutableSet.of("td", "th"), TABLE_SCOPE_BOUNDARIES);
        }
      }
      case "option" ->
          closeOpen(
              stack,
              ImmutableSet.of("option"),
              ImmutableSet.<String>builder().addAll(SCOPE_BOUNDARIES).add("select").build());
      default -> {}
    }
    if (CLOSES_PARAGRAPH.contains(name)) {
      closeOpen(stack, ImmutableSet.of("p"), SCOPE_BOUNDARIES);
    }
  }

  private static void closeOpen(
      Deque<OpenElement> stack, Set<String> names, Set<String> boundaries) {
    boolean unused = tryCloseOpen(stack, names, boundaries);
  }

  private static boolean tryCloseOpen(
      Deque<OpenElement> stack, Set<String> names, Set<String> boundaries) {
    OpenElement open = findOpen(stack, names, boundaries);
    if (open != null) {
      popThrough(stack, open);
      return true;
    }
    return false;
  }

  private static @Nullable OpenElement findOpen(
      Deque<OpenElement> stack, Set<String> names, Set<String> boundaries) {
    for (OpenElement element : stack) {
      if (element.start == null) {
        return null;
      }
      if (names.contains(element.name)) {
        return element;
      }
      if (boundaries.contains(element.name)) {
        return null;
      }
    }
    return null;
  }

  private static void popThrough(Deque<OpenElement> stack, OpenElement element) {
    while (stack.pop() != element) {}
  }

  // ---------------------------------------------------------------------------------------------
  // Step 2: lowering the tree into Markdown.

  /// Decides whether to convert headings: we only do so if every heading can be converted, so that
  /// the relative levels of the headings are preserved.
  private void decideHeadingPolicy(List<ImmutableList<Node>> descriptions) {
    List<Element> headings = new ArrayList<>();
    for (ImmutableList<Node> nodes : descriptions) {
      collectHeadings(nodes, headings);
    }
    convertHeadings = headings.stream().allMatch(MarkdownCommentBuilder::canConvertHeading);
    minHeadingLevel = headings.stream().mapToInt(h -> h.name().charAt(1) - '0').min().orElse(1);
  }

  private static boolean canConvertHeading(Element heading) {
    return heading.start().getAttributes().isEmpty()
        && !hasMultilineAtomicInline(heading.children());
  }

  /// Returns true if any descendant node would print as a multiline inline tag or HTML comment,
  /// which cannot appear inside a single-line ATX heading.
  private static boolean hasMultilineAtomicInline(List<Node> nodes) {
    for (Node node : nodes) {
      switch (node) {
        case Element e -> {
          if (hasMultilineAtomicInline(e.children())) {
            return true;
          }
        }
        case Leaf l -> {
          DocTree tree = l.tree();
          switch (tree) {
            case CommentTree c -> {
              if (containsLineBreak(c.getBody())) {
                return true;
              }
            }
            case LiteralTree lit when lit.getKind() == DocTree.Kind.LITERAL -> {
              if (containsLineBreak(lit.getBody().getBody())) {
                return true;
              }
            }
            case SnippetTree s -> {
              if (s.getBody() != null) {
                return true;
              }
            }
            case UnknownInlineTagTree u -> {
              if (u.getContent().stream()
                  .anyMatch(t -> t instanceof TextTree text && containsLineBreak(text.getBody()))) {
                return true;
              }
            }
            default -> {}
          }
        }
      }
    }
    return false;
  }

  private static boolean containsLineBreak(String s) {
    return s.indexOf('\n') >= 0 || s.indexOf('\r') >= 0;
  }

  private static void collectHeadings(List<Node> nodes, List<Element> headings) {
    for (Node node : nodes) {
      if (node instanceof Element e) {
        if (isHeading(e.name())) {
          headings.add(e);
        } else if (!e.name().equals("pre") && !isHtmlBlockPassthrough(e.name())) {
          collectHeadings(e.children(), headings);
        }
      }
    }
  }

  private static boolean isHeading(String name) {
    return name.length() == 2
        && name.charAt(0) == 'h'
        && name.charAt(1) >= '1'
        && name.charAt(1) <= '6';
  }

  /// Whether an element with the given name is kept as an HTML block if it starts a line.
  private static boolean isHtmlBlockPassthrough(String name) {
    return MarkdownCommentPrinter.HTML_BLOCK_ELEMENTS.contains(name)
        && !ImmutableSet.of("p", "ul", "ol", "li", "pre").contains(name)
        && !isHeading(name);
  }

  /// The context for lowering inline content.
  private record InlineContext(boolean inLink, boolean inStrong, boolean inEmphasis) {
    static final InlineContext DEFAULT = new InlineContext(false, false, false);
  }

  /// Lowers a sequence of nodes, either as blocks (e.g. a description) or as inline content (e.g.
  /// the content of a `<b>` element).
  private final class Lowering {
    private final boolean blockContext;
    private final InlineContext context;
    private final boolean keepHtml;
    private final boolean preserveLeadingNewline;
    private final ImmutableList.Builder<Block> blocks = ImmutableList.builder();
    private final List<Inline> inlines = new ArrayList<>();

    /// The number of line breaks since the last inline content.
    private int pendingNewlines;

    /// Whether a trailing space was hoisted out of the previous inline element and should be
    /// emitted if the next inline does not already start with whitespace.
    private boolean pendingHoistedSpace;

    Lowering(boolean blockContext) {
      this(blockContext, InlineContext.DEFAULT, /* keepHtml= */ false, false);
    }

    Lowering(boolean blockContext, InlineContext context) {
      this(blockContext, context, /* keepHtml= */ false, false);
    }

    Lowering(
        boolean blockContext,
        InlineContext context,
        boolean keepHtml,
        boolean preserveLeadingNewline) {
      this.blockContext = blockContext;
      this.context = context;
      this.keepHtml = keepHtml;
      this.preserveLeadingNewline = preserveLeadingNewline;
    }

    ImmutableList<Block> lowerBlocks(List<Node> nodes) throws UnsupportedCommentException {
      try {
        lowerAll(nodes);
      } catch (NestedLinkException e) {
        throw new UnsupportedCommentException("nested link", e);
      }
      flushParagraph();
      return blocks.build();
    }

    ImmutableList<Inline> lowerInlines(List<Node> nodes)
        throws UnsupportedCommentException, NestedLinkException {
      lowerAll(nodes);
      return finishInlines(inlines);
    }

    private void lowerAll(List<Node> nodes)
        throws UnsupportedCommentException, NestedLinkException {
      for (int i = 0; i < nodes.size(); i++) {
        boolean endsLine = i == nodes.size() - 1 || startsWithNewline(nodes.get(i + 1));
        switch (nodes.get(i)) {
          case Element e -> lowerElement(e);
          case Leaf l -> lowerLeaf(l.tree(), endsLine);
        }
      }
    }

    /// Whether the next output would be at the start of a line.
    private boolean atLineStart() {
      return pendingNewlines > 0 || inlines.isEmpty();
    }

    private void addInline(Inline inline) {
      if (pendingNewlines > 0) {
        pendingHoistedSpace = false;
        if (pendingNewlines > 1 && blockContext) {
          flushParagraph();
        } else if (!inlines.isEmpty() || preserveLeadingNewline) {
          inlines.add(SoftBreak.INSTANCE);
        }
        pendingNewlines = 0;
      } else if (pendingHoistedSpace) {
        pendingHoistedSpace = false;
        if (!(inline instanceof SoftBreak)
            && !(inline instanceof Text t
                && !t.literal().isEmpty()
                && WHITESPACE.matches(t.literal().charAt(0)))) {
          inlines.add(new Text(" "));
        }
      }
      inlines.add(inline);
    }

    private void addInlines(List<Inline> toAdd) {
      for (Inline inline : toAdd) {
        addInline(inline);
      }
    }

    private void addHoisted(boolean leadingSpace, List<Inline> content, boolean trailingSpace) {
      if (leadingSpace && needsLeadingSpace()) {
        addInline(new Text(" "));
      }
      addInlines(content);
      if (trailingSpace) {
        pendingHoistedSpace = true;
      }
    }

    private boolean needsLeadingSpace() {
      if (pendingHoistedSpace || pendingNewlines > 0 || inlines.isEmpty()) {
        return false;
      }
      Inline last = getLast(inlines);
      if (last instanceof SoftBreak) {
        return false;
      }
      return !(last instanceof Text t
          && !t.literal().isEmpty()
          && WHITESPACE.matches(t.literal().charAt(t.literal().length() - 1)));
    }

    private void addBlock(Block block) throws UnsupportedCommentException {
      if (!blockContext) {
        throw new UnsupportedCommentException("block in inline context");
      }
      flushParagraph();
      blocks.add(block);
    }

    private void flushParagraph() {
      pendingNewlines = 0;
      pendingHoistedSpace = false;
      ImmutableList<Inline> paragraph = finishInlines(inlines);
      inlines.clear();
      if (!paragraph.isEmpty()) {
        blocks.add(new Paragraph(paragraph));
      }
    }

    private void lowerText(String body) {
      List<String> lines = NEWLINE_SPLITTER.splitToList(body);
      for (int i = 0; i < lines.size(); i++) {
        if (i > 0) {
          pendingNewlines++;
        }
        String line = lines.get(i);
        if (pendingNewlines > 0) {
          line = WHITESPACE.trimLeadingFrom(line);
        }
        if (!line.isEmpty()) {
          addInlines(splitBacktickPairs(line));
        }
      }
    }

    private void lowerLeaf(DocTree tree, boolean endsLine)
        throws UnsupportedCommentException, NestedLinkException {
      switch (tree) {
        // EscapeTree and ErroneousTree are subtypes of TextTree.
        case EscapeTree escape -> addInline(new Text(escape.getBody()));
        case ErroneousTree erroneous -> addInline(new Text(erroneous.getBody()));
        case TextTree text -> lowerText(text.getBody());
        case EntityTree entity -> addInline(new Entity("&" + entity.getName() + ";"));
        case CommentTree comment -> {
          boolean isBlockComment = inlines.isEmpty() || pendingNewlines > 1;
          if (blockContext && endsLine && isBlockComment) {
            addBlock(new HtmlBlock(comment.getBody()));
          } else {
            addInline(new Html(comment.getBody()));
          }
        }
        case EndElementTree end -> {
          // A stray end tag.
          String name = Ascii.toLowerCase(end.getName().toString());
          if (name.equals("p") && !keepHtml) {
            if (blockContext) {
              flushParagraph();
            }
          } else if (blockContext
              && atLineStart()
              && MarkdownCommentPrinter.HTML_BLOCK_ELEMENTS.contains(name)) {
            addBlock(new HtmlBlock(serializeTraditional(end)));
          } else {
            addInline(new Html(serializeTraditional(end)));
          }
        }
        case LiteralTree literal when literal.getKind() == DocTree.Kind.CODE ->
            addCodeSpan(literal.getBody().getBody(), /* fromCodeTag= */ true);
        case LinkTree link -> addInlines(lowerLink(link));
        case SnippetTree snippet -> {
          CodeBlock codeBlock = keepHtml ? null : snippetCodeBlock(snippet);
          if (codeBlock != null && blockContext) {
            addBlock(codeBlock);
          } else {
            addInline(new InlineTag(serializeTraditional(snippet)));
          }
        }
        // There is no case for RawTextTree: it only appears in `///` comments, and it was added in
        // JDK 23, so referencing it would make this switch fail to link on older JDKs.
        case ReferenceTree reference -> addInline(new Text(reference.getSignature()));
        default -> addInline(lowerInlineTag(tree));
      }
    }

    private void addCodeSpan(String code, boolean fromCodeTag) {
      if (fromCodeTag && code.startsWith("\n")) {
        // google-java-format wraps lines right after `{@code`, leaving `\n` as the tag separator.
        code = HORIZONTAL_WHITESPACE.trimLeadingFrom(code.substring(1));
      }
      if (code.isEmpty()) {
        return;
      }
      String collapsed = collapseWhitespace(code);
      if (WHITESPACE.matchesAllOf(collapsed)) {
        addInline(new Code(" "));
        return;
      }
      boolean leadingSpace = WHITESPACE.matches(collapsed.charAt(0));
      boolean trailingSpace = WHITESPACE.matches(collapsed.charAt(collapsed.length() - 1));
      String trimmed = WHITESPACE.trimFrom(collapsed);
      addHoisted(leadingSpace, ImmutableList.of(new Code(trimmed)), trailingSpace);
    }

    private void lowerElement(Element element)
        throws UnsupportedCommentException, NestedLinkException {
      String name = element.name();
      if (keepHtml) {
        if (blockContext
            && (inlines.isEmpty() || pendingNewlines > 1)
            && MarkdownCommentPrinter.HTML_BLOCK_ELEMENTS.contains(name)) {
          addBlock(htmlBlock(element));
        } else {
          addInlines(inlinePassthrough(element));
        }
        return;
      }
      switch (name) {
        case "p" -> {
          if (!blockContext) {
            throw new UnsupportedCommentException("<p> in inline context");
          }
          if (!ImmutableSet.of("id").containsAll(element.attributeNames())
              || (element.attributeNames().contains("id") && element.attribute("id") == null)) {
            addBlock(htmlBlock(element));
            return;
          }
          flushParagraph();
          String id = element.attribute("id");
          if (id != null) {
            addInline(new Html("<a id=\"" + escapeHtmlAttribute(id) + "\"></a>"));
          }
          lowerAll(element.children());
          flushParagraph();
        }
        case "ul", "ol" -> {
          for (Block block : lowerList(element)) {
            addBlock(block);
          }
        }
        case "pre" -> addBlock(lowerPre(element));
        case "b", "strong" -> lowerEmphasis(element, /* strong= */ true);
        case "i", "em" -> lowerEmphasis(element, /* strong= */ false);
        case "code" -> lowerCode(element);
        case "a" -> lowerAnchor(element);
        default -> {
          if (isHeading(name)) {
            addBlock(lowerHeading(element));
          } else if (blockContext
              && atLineStart()
              && MarkdownCommentPrinter.HTML_BLOCK_ELEMENTS.contains(name)) {
            addBlock(htmlBlock(element));
          } else {
            addInlines(inlinePassthrough(element));
          }
        }
      }
    }

    private ImmutableList<Block> lowerList(Element list) throws UnsupportedCommentException {
      boolean ordered = list.name().equals("ol");
      if (!(ordered ? ImmutableSet.of("start") : ImmutableSet.of())
              .containsAll(list.attributeNames())
          || list.children().stream()
              .anyMatch(c -> c instanceof Element e && !e.start().getAttributes().isEmpty())) {
        // e.g. <ol type="a"> or <li value="3">, which Markdown cannot represent.
        return ImmutableList.of(htmlBlock(list));
      }
      int start = 1;
      if (ordered && list.attributeNames().contains("start")) {
        String startAttribute = list.attribute("start");
        if (startAttribute == null) {
          return ImmutableList.of(htmlBlock(list));
        }
        try {
          start = Integer.parseInt(WHITESPACE.trimFrom(startAttribute));
        } catch (NumberFormatException e) {
          return ImmutableList.of(htmlBlock(list));
        }
        if (start < 0 || start > 999_999_999) {
          return ImmutableList.of(htmlBlock(list));
        }
      }
      List<ImmutableList<Block>> items = new ArrayList<>();
      ImmutableList.Builder<Block> result = ImmutableList.builder();
      for (Node child : list.children()) {
        if (child instanceof Element e && e.name().equals("li")) {
          items.add(new Lowering(/* blockContext= */ true).lowerBlocks(e.children()));
        } else if (!isBlankText(child)) {
          // Content between list items, e.g. a comment: attach it to the previous item.
          ImmutableList<Block> content =
              new Lowering(/* blockContext= */ true).lowerBlocks(ImmutableList.of(child));
          if (items.isEmpty()) {
            result.addAll(content);
          } else {
            items.set(
                items.size() - 1,
                ImmutableList.<Block>builder()
                    .addAll(items.get(items.size() - 1))
                    .addAll(content)
                    .build());
          }
        }
      }
      if (!items.isEmpty()) {
        result.add(new ListBlock(ordered, start, ImmutableList.copyOf(items)));
      }
      return result.build();
    }

    private Block lowerPre(Element pre) throws UnsupportedCommentException {
      // Allow styling `class` attributes (e.g. `<pre class="code">`), but preserve `id` anchors and
      // other attributes as HTML.
      if (!ImmutableSet.of("class").containsAll(pre.attributeNames())) {
        return htmlBlock(pre);
      }
      StringBuilder literal = new StringBuilder();
      if (!appendPreformattedText(pre.children(), literal)) {
        return htmlBlock(pre);
      }
      String text = literal.toString();
      if (text.startsWith("\n")) {
        text = text.substring(1);
      }
      return new CodeBlock("", stripTrailingWhitespaceLines(text));
    }

    private Block lowerHeading(Element heading)
        throws UnsupportedCommentException, NestedLinkException {
      if (!convertHeadings) {
        return htmlBlock(heading);
      }
      ImmutableList<Inline> content =
          new Lowering(/* blockContext= */ false, context).lowerInlines(heading.children());
      List<Inline> singleLine = new ArrayList<>();
      for (Inline inline : content) {
        if (containsLineBreak(inline)) {
          throw new UnsupportedCommentException("multiline inline in heading");
        }
        singleLine.add(inline instanceof SoftBreak ? new Text(" ") : inline);
      }
      int level = heading.name().charAt(1) - '0';
      return new Heading(Math.max(1, level - minHeadingLevel + 1), finishInlines(singleLine));
    }

    private void lowerEmphasis(Element element, boolean strong)
        throws UnsupportedCommentException, NestedLinkException {
      if (element.end() == null || !element.start().getAttributes().isEmpty()) {
        // An unclosed <b> or <i> cannot be represented with Markdown delimiters, and Markdown
        // emphasis has no attributes.
        addInlines(inlinePassthrough(element));
        return;
      }
      if (strong ? context.inStrong() : context.inEmphasis()) {
        // e.g. <b>outer <b>inner</b> outer</b>
        addInlines(
            new Lowering(
                    /* blockContext= */ false,
                    context,
                    /* keepHtml= */ false,
                    /* preserveLeadingNewline= */ true)
                .lowerInlinesUntrimmed(element.children()));
        return;
      }
      InlineContext innerContext =
          strong
              ? new InlineContext(context.inLink(), true, context.inEmphasis())
              : new InlineContext(context.inLink(), context.inStrong(), true);
      List<Inline> content =
          new ArrayList<>(
              new Lowering(
                      /* blockContext= */ false,
                      innerContext,
                      /* keepHtml= */ false,
                      /* preserveLeadingNewline= */ true)
                  .lowerInlinesUntrimmed(element.children()));
      // Move whitespace at the edges outside of the delimiters, e.g. <b>Note: </b>x -> **Note:** x.
      boolean leadingSpace = stripLeadingEdgeWhitespace(content);
      boolean trailingSpace = stripTrailingEdgeWhitespace(content);
      ImmutableList<Inline> wrapped =
          content.isEmpty()
              ? ImmutableList.of()
              : ImmutableList.of(new Emphasis(element.name(), finishInlines(content)));
      addHoisted(leadingSpace, wrapped, trailingSpace);
    }

    private ImmutableList<Inline> lowerInlinesUntrimmed(List<Node> nodes)
        throws UnsupportedCommentException, NestedLinkException {
      lowerAll(nodes);
      if (pendingNewlines > 0 && !inlines.isEmpty()) {
        inlines.add(SoftBreak.INSTANCE);
      } else if (pendingHoistedSpace) {
        inlines.add(new Text(" "));
      }
      return ImmutableList.copyOf(mergeText(inlines));
    }

    private void lowerCode(Element code) throws UnsupportedCommentException, NestedLinkException {
      StringBuilder literal = new StringBuilder();
      if (code.end() != null
          && code.start().getAttributes().isEmpty()
          && appendPlainText(code.children(), literal)) {
        addCodeSpan(literal.toString(), /* fromCodeTag= */ false);
        return;
      }
      addInlines(inlinePassthrough(code));
    }

    private void lowerAnchor(Element anchor)
        throws UnsupportedCommentException, NestedLinkException {
      String href = anchor.attribute("href");
      if (href != null
          && anchor.end() != null
          && anchor.attributeNames().equals(ImmutableSet.of("href"))
          && !href.contains("{@")
          && !containsLineBreak(href)) {
        if (context.inLink()) {
          throw new NestedLinkException();
        }
        try {
          List<Inline> content =
              new ArrayList<>(
                  new Lowering(
                          /* blockContext= */ false,
                          new InlineContext(true, context.inStrong(), context.inEmphasis()),
                          /* keepHtml= */ false,
                          /* preserveLeadingNewline= */ true)
                      .lowerInlinesUntrimmed(anchor.children()));
          boolean leadingSpace = stripLeadingEdgeWhitespace(content);
          boolean trailingSpace = stripTrailingEdgeWhitespace(content);
          ImmutableList<Inline> finished = finishInlines(content);
          if (!finished.isEmpty()) {
            addHoisted(leadingSpace, ImmutableList.of(new Link(href, finished)), trailingSpace);
            return;
          }
        } catch (NestedLinkException e) {
          // Fall through, and keep the anchor as HTML.
        }
      }
      addInlines(inlinePassthrough(anchor));
    }

    private ImmutableList<Inline> lowerLink(LinkTree link)
        throws UnsupportedCommentException, NestedLinkException {
      if (link.getReference() == null) {
        return ImmutableList.of(new InlineTag(serializeTraditional(link)));
      }
      String reference =
          EMPTY_BRACKETS
              .matcher(WHITESPACE.trimFrom(collapseWhitespace(link.getReference().getSignature())))
              .replaceAll("[]");
      if (reference.isEmpty()) {
        return ImmutableList.of(new InlineTag(serializeTraditional(link)));
      }
      if (context.inLink()) {
        throw new NestedLinkException();
      }
      boolean isPlain = link.getKind() == DocTree.Kind.LINK_PLAIN;
      if (link.getLabel().isEmpty()) {
        if (isPlain || !canUseShortcutReferenceLink(reference)) {
          return ImmutableList.of(
              new InlineTag("{@" + (isPlain ? "linkplain" : "link") + " " + reference + "}"));
        }
        return ImmutableList.of(new ReferenceLink(reference, null));
      }
      ImmutableList<Node> label = buildTree(link.getLabel());
      if (!isPlain) {
        // The label of a {@link} is rendered in code font, which we can only express in Markdown if
        // the label is plain text.
        StringBuilder text = new StringBuilder();
        if (!appendPlainText(label, text)) {
          return ImmutableList.of(keptLinkTag(link, reference));
        }
        String code = WHITESPACE.trimFrom(collapseWhitespace(text.toString()));
        if (code.isEmpty()) {
          if (!canUseShortcutReferenceLink(reference)) {
            return ImmutableList.of(new InlineTag("{@link " + reference + "}"));
          }
          return ImmutableList.of(new ReferenceLink(reference, null));
        }
        return ImmutableList.of(new ReferenceLink(reference, ImmutableList.of(new Code(code))));
      }
      ImmutableList<Inline> content =
          new Lowering(
                  /* blockContext= */ false,
                  new InlineContext(true, context.inStrong(), context.inEmphasis()))
              .lowerInlines(label);
      if (content.isEmpty()) {
        return ImmutableList.of(new InlineTag("{@linkplain " + reference + "}"));
      }
      // In Markdown comments, `[Foo][Foo]` (label text equal to reference) lowers to `{@link}`,
      // rendering in code font and shortening qualified names rather than staying `{@linkplain}`.
      if (content.size() == 1
          && content.get(0) instanceof Text t
          && t.literal().equals(reference)) {
        return ImmutableList.of(keptLinkTag(link, reference));
      }
      return ImmutableList.of(new ReferenceLink(reference, content));
    }

    private Inline keptLinkTag(LinkTree link, String reference) throws UnsupportedCommentException {
      String head = (link.getKind() == DocTree.Kind.LINK ? "link " : "linkplain ") + reference;
      ImmutableList<Block> labelBody =
          lowerInlineTagBody(
              link.getLabel(),
              /* keepHtml= */ true,
              new InlineContext(/* inLink= */ true, context.inStrong(), context.inEmphasis()));
      return labelBody.isEmpty()
          ? new InlineTag("{@" + head + "}")
          : new MarkdownInlineTag(head, labelBody);
    }

    /// Keeps an element as inline HTML, converting its content.
    private ImmutableList<Inline> inlinePassthrough(Element element)
        throws UnsupportedCommentException, NestedLinkException {
      ImmutableList.Builder<Inline> result = ImmutableList.builder();
      result.add(new Html(collapseWhitespace(serializeTraditional(element.start()))));
      result.addAll(
          new Lowering(
                  /* blockContext= */ false, context, keepHtml, /* preserveLeadingNewline= */ true)
              .lowerInlinesUntrimmed(element.children()));
      if (element.end() != null) {
        result.add(new Html(serializeTraditional(element.end())));
      }
      return result.build();
    }
  }

  private static final Splitter NEWLINE_SPLITTER = Splitter.on('\n');

  /// ASCII HTML whitespace (excluding non-breaking spaces like `\u00A0`).
  private static final CharMatcher WHITESPACE = CharMatcher.anyOf(" \t\n\r\f");

  private static final CharMatcher HORIZONTAL_WHITESPACE = CharMatcher.anyOf(" \t");

  private static final Pattern EMPTY_BRACKETS = Pattern.compile("\\[\\s+]");

  /// CommonMark type-1 HTML block elements, whose content continues across blank lines until the
  /// closing tag.
  private static final ImmutableSet<String> RAW_HTML_BLOCK_ELEMENTS =
      ImmutableSet.of("pre", "script", "style", "textarea");

  /// JEP 413 snippet markup directives in Java-style and properties-style comments.
  private static final Pattern SNIPPET_MARKUP_JAVA =
      Pattern.compile("//[ \\t]*@[ \\t]*(?:highlight|replace|link|start|end)\\b");

  private static final Pattern SNIPPET_MARKUP_PROPERTIES =
      Pattern.compile("[#!][ \\t]*@[ \\t]*(?:highlight|replace|link|start|end)\\b");

  private static boolean startsWithNewline(Node node) {
    return node instanceof Leaf l
        && l.tree() instanceof TextTree t
        && HORIZONTAL_WHITESPACE.trimLeadingFrom(t.getBody()).startsWith("\n");
  }

  private static boolean isBlankText(Node node) {
    return node instanceof Leaf l
        && l.tree() instanceof TextTree t
        && WHITESPACE.matchesAllOf(t.getBody());
  }

  private static boolean isBlank(Inline inline) {
    return inline instanceof SoftBreak
        || (inline instanceof Text t && WHITESPACE.matchesAllOf(t.literal()));
  }

  private static boolean stripLeadingEdgeWhitespace(List<Inline> content) {
    boolean stripped = false;
    while (!content.isEmpty() && isBlank(content.get(0))) {
      content.remove(0);
      stripped = true;
    }
    if (!content.isEmpty()
        && content.get(0) instanceof Text t
        && !t.literal().isEmpty()
        && WHITESPACE.matches(t.literal().charAt(0))) {
      String trimmed = WHITESPACE.trimLeadingFrom(t.literal());
      if (trimmed.isEmpty()) {
        content.remove(0);
      } else {
        content.set(0, new Text(trimmed));
      }
      stripped = true;
    }
    return stripped;
  }

  private static boolean stripTrailingEdgeWhitespace(List<Inline> content) {
    boolean stripped = false;
    while (!content.isEmpty() && isBlank(content.get(content.size() - 1))) {
      content.remove(content.size() - 1);
      stripped = true;
    }
    if (!content.isEmpty()
        && content.get(content.size() - 1) instanceof Text t
        && !t.literal().isEmpty()
        && WHITESPACE.matches(t.literal().charAt(t.literal().length() - 1))) {
      String trimmed = WHITESPACE.trimTrailingFrom(t.literal());
      if (trimmed.isEmpty()) {
        content.remove(content.size() - 1);
      } else {
        content.set(content.size() - 1, new Text(trimmed));
      }
      stripped = true;
    }
    return stripped;
  }

  private static boolean containsLineBreak(Inline inline) {
    return switch (inline) {
      case InlineTag t -> containsLineBreak(t.text());
      case Html h -> containsLineBreak(h.html());
      case MarkdownInlineTag t ->
          containsLineBreak(t.head())
              || t.body().size() > 1
              || (t.body().size() == 1
                  && (!(t.body().get(0) instanceof Paragraph p)
                      || p.inlines().stream().anyMatch(MarkdownCommentBuilder::containsLineBreak)));
      case Emphasis e -> e.inlines().stream().anyMatch(MarkdownCommentBuilder::containsLineBreak);
      case Link l ->
          containsLineBreak(l.destination())
              || l.inlines().stream().anyMatch(MarkdownCommentBuilder::containsLineBreak);
      case ReferenceLink r ->
          containsLineBreak(r.reference())
              || (r.label() != null
                  && r.label().stream().anyMatch(MarkdownCommentBuilder::containsLineBreak));
      case Code c -> containsLineBreak(c.literal());
      case Text t -> false;
      case Entity e -> containsLineBreak(e.text());
      case SoftBreak s -> false;
    };
  }

  /// Returns true if `reference` can be written as a shortcut Markdown reference link `[reference]`
  /// without CommonMark parsing inline markup inside it.
  private static boolean canUseShortcutReferenceLink(String reference) {
    int underscores = 0;
    boolean hasFlankingUnderscore = false;
    for (int i = 0; i < reference.length(); i++) {
      char c = reference.charAt(i);
      switch (c) {
        case '<', '>', '`', '\\', '*', '&', '!' -> {
          return false;
        }
        case '_' -> {
          underscores++;
          boolean prevAlnum = i > 0 && Character.isLetterOrDigit(reference.charAt(i - 1));
          boolean nextAlnum =
              i + 1 < reference.length() && Character.isLetterOrDigit(reference.charAt(i + 1));
          if (!prevAlnum || !nextAlnum) {
            hasFlankingUnderscore = true;
          }
        }
        default -> {}
      }
    }
    return underscores < 2 || !hasFlankingUnderscore;
  }

  private static String escapeHtmlAttribute(String value) {
    return value
        .replace("&", "&amp;")
        .replace("\"", "&quot;")
        .replace("<", "&lt;")
        .replace(">", "&gt;");
  }

  /// Keeps an element (and its content) as an HTML block, in traditional Javadoc syntax.
  private HtmlBlock htmlBlock(Element element) throws UnsupportedCommentException {
    String html = serializeTraditional(element);
    if (RAW_HTML_BLOCK_ELEMENTS.contains(element.name())) {
      // Type-1 HTML blocks (<pre>, <script>, <style>, <textarea>) continue across blank lines until
      // their closing tag.
      return new HtmlBlock(stripTrailingWhitespaceLines(html));
    }
    // Other HTML blocks end at a blank line.
    List<String> lines = new ArrayList<>();
    for (String line : NEWLINE_SPLITTER.split(html)) {
      if (!WHITESPACE.matchesAllOf(line)) {
        lines.add(line.stripTrailing());
      }
    }
    if (lines.size() < NEWLINE_SPLITTER.splitToList(WHITESPACE.trimFrom(html)).size()
        && containsBlankLineSensitiveContent(element)) {
      throw new UnsupportedCommentException("blank line in preformatted content inside HTML block");
    }
    return new HtmlBlock(String.join("\n", lines));
  }

  private static boolean containsBlankLineSensitiveContent(Element element) {
    for (Node child : element.children()) {
      switch (child) {
        case Element e -> {
          if (RAW_HTML_BLOCK_ELEMENTS.contains(e.name()) || containsBlankLineSensitiveContent(e)) {
            return true;
          }
        }
        case Leaf l -> {
          if (l.tree() instanceof SnippetTree) {
            return true;
          }
        }
      }
    }
    return false;
  }

  /// Splits text into [Text] and [Code] nodes, treating pairs of backticks as code spans.
  ///
  /// Backticks have no meaning in traditional Javadoc, but they are commonly used as if they were
  /// Markdown code spans, which is what they become in a Markdown comment. Unpaired backticks are
  /// escaped when printed.
  private static List<Inline> splitBacktickPairs(String line) {
    List<Inline> result = new ArrayList<>();
    int start = 0;
    int i = 0;
    while (i < line.length()) {
      if (line.charAt(i) != '`') {
        i++;
        continue;
      }
      int runEnd = i;
      while (runEnd < line.length() && line.charAt(runEnd) == '`') {
        runEnd++;
      }
      String run = line.substring(i, runEnd);
      int close = findClosingBacktickRun(line, runEnd, run.length());
      if (close < 0) {
        i = runEnd;
        continue;
      }
      String content = line.substring(runEnd, close);
      if (content.isBlank()) {
        i = close + run.length();
        continue;
      }
      if (i > start) {
        result.add(new Text(line.substring(start, i)));
      }
      result.add(new Code(normalizeCodeSpanContent(content)));
      i = close + run.length();
      start = i;
    }
    if (start < line.length()) {
      result.add(new Text(line.substring(start)));
    }
    return result;
  }

  private static int findClosingBacktickRun(String line, int from, int length) {
    int i = from;
    while (i < line.length()) {
      if (line.charAt(i) != '`') {
        i++;
        continue;
      }
      int runEnd = i;
      while (runEnd < line.length() && line.charAt(runEnd) == '`') {
        runEnd++;
      }
      if (runEnd - i == length) {
        return i;
      }
      i = runEnd;
    }
    return -1;
  }

  /// Strips a single leading and trailing space from code span content, as CommonMark does.
  private static String normalizeCodeSpanContent(String content) {
    if (content.length() >= 2
        && content.startsWith(" ")
        && content.endsWith(" ")
        && !content.isBlank()) {
      return content.substring(1, content.length() - 1);
    }
    return content;
  }

  /// Returns a code span for inline code, e.g. from `{@code ...}`. Whitespace is collapsed, as it
  /// would be when the traditional comment is rendered as HTML.
  private static ImmutableList<Inline> codeSpan(String code) {
    String collapsed = WHITESPACE.trimFrom(collapseWhitespace(code));
    return collapsed.isEmpty() ? ImmutableList.of() : ImmutableList.of(new Code(collapsed));
  }

  private static String collapseWhitespace(String s) {
    return WHITESPACE.collapseFrom(s, ' ');
  }

  /// Appends the text of the given nodes, if they consist only of text (including entities and
  /// `{@code}`/`{@literal}` tags). Returns false otherwise.
  private boolean appendPlainText(List<Node> nodes, StringBuilder sb) {
    for (Node node : nodes) {
      switch (node) {
        case Element e
            when e.name().equals("code")
                && e.end() != null
                && e.start().getAttributes().isEmpty() -> {
          if (!appendPlainText(e.children(), sb)) {
            return false;
          }
        }
        case Leaf l when l.tree() instanceof TextTree t -> sb.append(t.getBody());
        case Leaf l when l.tree() instanceof EntityTree entity -> sb.append(decode(entity));
        case Leaf l when l.tree() instanceof LiteralTree literal ->
            sb.append(literal.getBody().getBody());
        default -> {
          return false;
        }
      }
    }
    return true;
  }

  /// Like [#appendPlainText], but for the content of a `<pre>` element, in which line breaks are
  /// significant.
  private boolean appendPreformattedText(List<Node> nodes, StringBuilder sb) {
    for (Node node : nodes) {
      switch (node) {
        case Element e
            when ImmutableSet.of("code", "pre").contains(e.name())
                && ImmutableSet.of("class").containsAll(e.attributeNames()) -> {
          if (!appendPreformattedText(e.children(), sb)) {
            return false;
          }
        }
        case Element e when e.name().equals("br") && e.children().isEmpty() -> sb.append('\n');
        case Leaf l when l.tree() instanceof TextTree t -> sb.append(t.getBody());
        case Leaf l when l.tree() instanceof EntityTree entity -> sb.append(decode(entity));
        case Leaf l when l.tree() instanceof LiteralTree literal ->
            sb.append(literal.getBody().getBody());
        default -> {
          return false;
        }
      }
    }
    return true;
  }

  private String decode(EntityTree entity) {
    String decoded = docTrees.getCharacters(entity);
    return decoded != null ? decoded : "&" + entity.getName() + ";";
  }

  private static String stripTrailingWhitespaceLines(String text) {
    List<String> lines = new ArrayList<>();
    for (String line : NEWLINE_SPLITTER.split(text)) {
      lines.add(line.stripTrailing());
    }
    while (!lines.isEmpty() && lines.get(lines.size() - 1).isEmpty()) {
      lines.remove(lines.size() - 1);
    }
    return String.join("\n", lines);
  }

  private static @Nullable CodeBlock snippetCodeBlock(SnippetTree snippet) {
    TextTree body = snippet.getBody();
    if (body == null) {
      // An external snippet.
      return null;
    }
    String lang = "";
    for (DocTree attribute : snippet.getAttributes()) {
      if (!(attribute instanceof AttributeTree a)
          || !Ascii.equalsIgnoreCase(a.getName().toString(), "lang")
          || a.getValue() == null) {
        return null;
      }
      lang = serializeAttributeValue(a.getValue());
    }
    if (containsLineBreak(lang)) {
      return null;
    }
    String text = body.getBody();
    Pattern markupPattern =
        Ascii.equalsIgnoreCase(lang, "properties")
            ? SNIPPET_MARKUP_PROPERTIES
            : SNIPPET_MARKUP_JAVA;
    if (markupPattern.matcher(text).find()) {
      // Keep snippets that use JEP 413 markup directives (@highlight, @replace, @link, etc.).
      return null;
    }
    text = text.stripIndent();
    if (text.startsWith("\n")) {
      text = text.substring(1);
    }
    return new CodeBlock(lang, stripTrailingWhitespaceLines(text));
  }

  // ---------------------------------------------------------------------------------------------
  // Normalizing inline content.

  /// Normalizes whitespace in a sequence of inlines.
  private static ImmutableList<Inline> finishInlines(List<Inline> inlines) {
    List<Inline> result = mergeText(inlines);
    // Remove whitespace around line breaks, and at the start and end.
    List<Inline> trimmed = new ArrayList<>();
    for (int i = 0; i < result.size(); i++) {
      Inline inline = result.get(i);
      boolean afterBreak = trimmed.isEmpty() || getLast(trimmed) instanceof SoftBreak;
      boolean beforeBreak = i == result.size() - 1 || result.get(i + 1) instanceof SoftBreak;
      if (inline instanceof SoftBreak) {
        if (!afterBreak) {
          trimmed.add(inline);
        }
        continue;
      }
      if (inline instanceof Text t) {
        String literal = t.literal();
        if (afterBreak) {
          literal = WHITESPACE.trimLeadingFrom(literal);
        }
        if (beforeBreak) {
          literal = WHITESPACE.trimTrailingFrom(literal);
        }
        if (!literal.isEmpty()) {
          trimmed.add(new Text(literal));
        }
        continue;
      }
      trimmed.add(inline);
    }
    while (!trimmed.isEmpty() && getLast(trimmed) instanceof SoftBreak) {
      trimmed.remove(trimmed.size() - 1);
    }
    return ImmutableList.copyOf(mergeText(trimmed));
  }

  private static List<Inline> mergeText(List<Inline> inlines) {
    List<Inline> result = new ArrayList<>();
    for (Inline inline : inlines) {
      if (inline instanceof Text t
          && !result.isEmpty()
          && getLast(result) instanceof Text previous) {
        result.set(result.size() - 1, new Text(previous.literal() + t.literal()));
      } else if (!(inline instanceof Text t && t.literal().isEmpty())) {
        result.add(inline);
      }
    }
    return result;
  }

  private static <T> T getLast(List<T> list) {
    return list.get(list.size() - 1);
  }

  // ---------------------------------------------------------------------------------------------
  // Block tags.

  private BlockTag lowerBlockTag(DocTree tag) throws UnsupportedCommentException {
    if (tag instanceof SeeTree see) {
      return lowerSee(see);
    }
    String head =
        switch (tag) {
          case ParamTree param ->
              "@param "
                  + (param.isTypeParameter()
                      ? "<" + param.getName().getName() + ">"
                      : param.getName().getName().toString());
          case ThrowsTree t -> {
            if (t.getExceptionName() == null) {
              throw new UnsupportedCommentException("missing exception reference");
            }
            yield "@" + t.getTagName() + " " + singleLineReference(t.getExceptionName());
          }
          case SerialFieldTree field -> {
            if (field.getName() == null || field.getType() == null) {
              throw new UnsupportedCommentException("missing @serialField name or type");
            }
            yield "@serialField "
                + field.getName().getName()
                + " "
                + singleLineReference(field.getType());
          }
          case SpecTree spec -> {
            if (spec.getURL() == null) {
              throw new UnsupportedCommentException("missing @spec URL");
            }
            String url = serializeTraditional(ImmutableList.of(spec.getURL()));
            if (containsLineBreak(url)) {
              throw new UnsupportedCommentException("multiline @spec URL");
            }
            yield "@spec " + url;
          }
          case ProvidesTree provides -> {
            if (provides.getServiceType() == null) {
              throw new UnsupportedCommentException("missing @provides service type");
            }
            yield "@provides " + singleLineReference(provides.getServiceType());
          }
          case UsesTree uses -> {
            if (uses.getServiceType() == null) {
              throw new UnsupportedCommentException("missing @uses service type");
            }
            yield "@uses " + singleLineReference(uses.getServiceType());
          }
          case BlockTagTree blockTag -> "@" + blockTag.getTagName();
          default -> null;
        };
    if (head == null) {
      return new VerbatimBlockTag(serializeTraditional(ImmutableList.of(tag)));
    }
    ImmutableList<Block> description =
        new Lowering(/* blockContext= */ true).lowerBlocks(buildTree(description(tag)));
    return new ConvertedBlockTag(head, description);
  }

  private static String singleLineReference(ReferenceTree reference) {
    return WHITESPACE.trimFrom(collapseWhitespace(reference.getSignature()));
  }

  /// Returns the description of a block tag.
  private static List<? extends DocTree> description(DocTree tag) {
    return switch (tag) {
      case ParamTree t -> t.getDescription();
      case ThrowsTree t -> t.getDescription();
      case ReturnTree t -> t.getDescription();
      case DeprecatedTree t -> t.getBody();
      case SinceTree t -> t.getBody();
      case VersionTree t -> t.getBody();
      case AuthorTree t -> t.getName();
      case HiddenTree t -> t.getBody();
      case SerialTree t -> t.getDescription();
      case SerialDataTree t -> t.getDescription();
      case SerialFieldTree t ->
          t.getDescription() != null ? t.getDescription() : ImmutableList.of();
      case SpecTree t -> t.getTitle();
      case ProvidesTree t -> t.getDescription();
      case UsesTree t -> t.getDescription();
      case UnknownBlockTagTree t -> t.getContent();
      case SeeTree t -> t.getReference();
      default -> ImmutableList.of();
    };
  }

  /// Lowers a `@see` tag. Quoted `@see "..."` tags are not parsed as Markdown by javadoc, while
  /// `@see reference label` and `@see <...>` labels are parsed as Markdown and validated as
  /// regions. HTML tags inside `@see` are kept as HTML because the JDK does not support Markdown
  /// links directly after `@see`.
  private BlockTag lowerSee(SeeTree see) throws UnsupportedCommentException {
    List<? extends DocTree> reference = see.getReference();
    if (reference.isEmpty()) {
      return new VerbatimBlockTag("@see");
    }
    if (reference.size() == 1
        && reference.get(0) instanceof TextTree t
        && t.getBody().startsWith("\"")) {
      // `@see "..."` is parsed as Kind.TEXT, not MARKDOWN.
      return new VerbatimBlockTag("@see " + t.getBody());
    }
    if (reference.get(0) instanceof ReferenceTree ref) {
      String head = "@see " + singleLineReference(ref);
      List<? extends DocTree> label = reference.subList(1, reference.size());
      if (label.isEmpty()) {
        return new ConvertedBlockTag(head, ImmutableList.of());
      }
      if (isCodeLabel(ref.getSignature(), label)) {
        // javadoc renders this label in code font in a traditional comment, but in plain font in a
        // Markdown comment unless it is a code span.
        String labelText = ((TextTree) label.get(0)).getBody();
        ImmutableList<Inline> span = codeSpan(labelText);
        return new ConvertedBlockTag(
            head, span.isEmpty() ? ImmutableList.of() : ImmutableList.of(new Paragraph(span)));
      }
      return new ConvertedBlockTag(head, lowerSeeBlocks(label));
    }
    return new ConvertedBlockTag("@see", lowerSeeBlocks(reference));
  }

  private ImmutableList<Block> lowerSeeBlocks(List<? extends DocTree> trees)
      throws UnsupportedCommentException {
    ImmutableList<Node> nodes = buildTree(trees);
    return new Lowering(
            /* blockContext= */ true,
            InlineContext.DEFAULT,
            /* keepHtml= */ true,
            /* preserveLeadingNewline= */ false)
        .lowerBlocks(nodes);
  }

  /// Returns true if javadoc renders the given label of a `@see` tag with the given reference in
  /// code font, in a traditional comment. This is the case if the label looks like a signature with
  /// the same simple name as the reference, e.g. `@see Foo#bar(int) Foo.bar()`.
  ///
  /// This is a port of `SeeTaglet.isPlain` from the JDK.
  private static boolean isCodeLabel(String reference, List<? extends DocTree> label) {
    if (label.size() != 1 || !(label.get(0) instanceof TextTree text)) {
      return false;
    }
    String s = text.getBody().trim();
    boolean inParentheses = false;
    String lastIdentifier = null;
    StringBuilder identifier = new StringBuilder();
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (!identifier.isEmpty() && !Character.isJavaIdentifierPart(c)) {
        lastIdentifier = identifier.toString();
        identifier.setLength(0);
      }
      switch (c) {
        case '(' -> {
          if (inParentheses) {
            return false;
          }
          inParentheses = true;
        }
        case ')' -> {
          if (inParentheses && i < s.length() - 1) {
            return false;
          }
          inParentheses = false;
        }
        default -> {
          if (!inParentheses) {
            if (Character.isJavaIdentifierStart(c)
                || (!identifier.isEmpty() && Character.isJavaIdentifierPart(c))) {
              identifier.append(c);
            } else if (Character.isWhitespace(c)) {
              return false;
            }
          }
        }
      }
    }
    if (!identifier.isEmpty()) {
      lastIdentifier = identifier.toString();
    }
    String signature = WHITESPACE.removeFrom(reference);
    int hash = signature.indexOf('#');
    String simpleName;
    if (hash < 0) {
      simpleName = signature.substring(signature.lastIndexOf('.') + 1);
    } else {
      int parenthesis = signature.indexOf('(', hash);
      simpleName =
          signature.substring(hash + 1, parenthesis < 0 ? signature.length() : parenthesis);
    }
    return lastIdentifier != null && lastIdentifier.equals(simpleName);
  }

  // ---------------------------------------------------------------------------------------------
  // Serializing doc trees in traditional Javadoc syntax.

  private String serializeTraditional(Node node) {
    return switch (node) {
      case Element e -> {
        StringBuilder sb = new StringBuilder(serializeTraditional(e.start()));
        for (Node child : e.children()) {
          sb.append(serializeTraditional(child));
        }
        if (e.end() != null) {
          sb.append(serializeTraditional(e.end()));
        }
        yield sb.toString();
      }
      case Leaf l -> serializeTraditional(ImmutableList.of(l.tree()));
    };
  }

  private String serializeTraditional(List<? extends DocTree> trees) {
    StringBuilder sb = new StringBuilder();
    for (DocTree tree : trees) {
      serializeTraditional(tree, sb);
    }
    return sb.toString();
  }

  private String serializeTraditional(DocTree tree) {
    StringBuilder sb = new StringBuilder();
    serializeTraditional(tree, sb);
    return sb.toString();
  }

  private void serializeTraditional(DocTree tree, StringBuilder sb) {
    switch (tree) {
      case EscapeTree t -> sb.append('@').append(t.getBody());
      case TextTree t -> sb.append(t.getBody());
      case EntityTree t -> sb.append('&').append(t.getName()).append(';');
      case CommentTree t -> sb.append(t.getBody());
      case StartElementTree t -> {
        sb.append('<').append(t.getName());
        for (DocTree attribute : t.getAttributes()) {
          serializeTraditional(attribute, sb);
        }
        sb.append(t.isSelfClosing() ? "/>" : ">");
      }
      case EndElementTree t -> sb.append("</").append(t.getName()).append('>');
      case AttributeTree t -> {
        sb.append(' ').append(t.getName());
        String quote =
            switch (t.getValueKind()) {
              case EMPTY -> null;
              case UNQUOTED -> "";
              case SINGLE -> "'";
              case DOUBLE -> "\"";
            };
        if (quote != null) {
          sb.append('=').append(quote).append(serializeTraditional(t.getValue())).append(quote);
        }
      }
      case LiteralTree t ->
          inlineTag(
              sb, t.getKind() == DocTree.Kind.CODE ? "code" : "literal", t.getBody().getBody());
      case LinkTree t -> {
        sb.append("{@").append(t.getKind() == DocTree.Kind.LINK ? "link" : "linkplain");
        if (t.getReference() != null) {
          sb.append(' ').append(t.getReference().getSignature());
        }
        if (!t.getLabel().isEmpty()) {
          sb.append(' ').append(serializeTraditional(t.getLabel()));
        }
        sb.append('}');
      }
      case ReferenceTree t -> sb.append(t.getSignature());
      case SnippetTree t -> {
        sb.append("{@snippet");
        for (DocTree attribute : t.getAttributes()) {
          serializeTraditional(attribute, sb);
        }
        if (t.getBody() != null) {
          sb.append(" :\n").append(t.getBody().getBody());
        }
        sb.append('}');
      }
      case SummaryTree t -> inlineTag(sb, "summary", serializeTraditional(t.getSummary()));
      case IndexTree t -> {
        String description = serializeTraditional(t.getDescription());
        inlineTag(
            sb,
            "index",
            serializeTraditional(ImmutableList.of(t.getSearchTerm()))
                + (description.isEmpty() ? "" : " " + description));
      }
      case SystemPropertyTree t -> inlineTag(sb, "systemProperty", t.getPropertyName().toString());
      case ValueTree t -> serializeValueTag(t, sb);
      case ReturnTree t when t.isInline() ->
          inlineTag(sb, "return", serializeTraditional(t.getDescription()));
      case UnknownInlineTagTree t ->
          inlineTag(sb, t.getTagName(), serializeTraditional(t.getContent()));
      default ->
          sb.append(tree.getKind() == DocTree.Kind.DOC_ROOT ? "{@docRoot}" : tree.toString());
    }
  }

  private static void serializeValueTag(ValueTree t, StringBuilder sb) {
    sb.append("{@value");
    if (t.getFormat() != null) {
      sb.append(' ').append(t.getFormat().getBody());
    }
    if (t.getReference() != null) {
      sb.append(' ').append(t.getReference().getSignature());
    }
    sb.append('}');
  }

  private static void inlineTag(StringBuilder sb, String name, String content) {
    sb.append("{@").append(name);
    if (!content.isEmpty()
        && (content.charAt(0) == ' ' || !WHITESPACE.matches(content.charAt(0)))) {
      sb.append(' ');
    }
    sb.append(content).append('}');
  }

  /// Lowers inline tags that are kept as Javadoc inline tags in a Markdown comment. Tags whose
  /// bodies javadoc parses as Markdown (`{@summary}`, `{@return}`, `{@index}`) are represented as
  /// [MarkdownInlineTag] so that their bodies are escaped and validated as regions.
  private Inline lowerInlineTag(DocTree tree) throws UnsupportedCommentException {
    return switch (tree) {
      case LiteralTree t -> {
        StringBuilder sb = new StringBuilder();
        inlineTag(sb, "literal", t.getBody().getBody());
        yield new InlineTag(sb.toString());
      }
      case SummaryTree t -> {
        ImmutableList<Block> body =
            lowerInlineTagBody(t.getSummary(), /* keepHtml= */ false, InlineContext.DEFAULT);
        yield body.isEmpty() ? new InlineTag("{@summary}") : new MarkdownInlineTag("summary", body);
      }
      case IndexTree t -> {
        String term = serializeTraditional(ImmutableList.of(t.getSearchTerm()));
        ImmutableList<Block> description =
            lowerInlineTagBody(t.getDescription(), /* keepHtml= */ false, InlineContext.DEFAULT);
        yield description.isEmpty()
            ? new InlineTag("{@index " + term + "}")
            : new MarkdownInlineTag("index " + term, description);
      }
      case SystemPropertyTree t -> {
        StringBuilder sb = new StringBuilder();
        inlineTag(sb, "systemProperty", t.getPropertyName().toString());
        yield new InlineTag(sb.toString());
      }
      case ValueTree t -> {
        StringBuilder sb = new StringBuilder();
        serializeValueTag(t, sb);
        yield new InlineTag(sb.toString());
      }
      case ReturnTree t when t.isInline() -> {
        ImmutableList<Block> body =
            lowerInlineTagBody(t.getDescription(), /* keepHtml= */ false, InlineContext.DEFAULT);
        yield body.isEmpty() ? new InlineTag("{@return}") : new MarkdownInlineTag("return", body);
      }
      // DocCommentParser parses unknown inline tags as Kind.TEXT (not MARKDOWN) even in `///`
      // comments, so their content must not be Markdown-escaped.
      case UnknownInlineTagTree t -> {
        String content = serializeTraditional(t.getContent());
        yield new InlineTag(
            content.isEmpty()
                ? "{@" + t.getTagName() + "}"
                : "{@" + t.getTagName() + " " + content + "}");
      }
      default ->
          new InlineTag(tree.getKind() == DocTree.Kind.DOC_ROOT ? "{@docRoot}" : tree.toString());
    };
  }

  /// Lowers the Markdown body of an inline tag (e.g. `{@summary ...}` or a kept `{@link ...}`).
  private ImmutableList<Block> lowerInlineTagBody(
      List<? extends DocTree> content, boolean keepHtml, InlineContext context)
      throws UnsupportedCommentException {
    ImmutableList<Node> nodes = buildTree(content);
    if (keepHtml) {
      return new Lowering(
              /* blockContext= */ true,
              context,
              /* keepHtml= */ true,
              /* preserveLeadingNewline= */ false)
          .lowerBlocks(nodes);
    }
    try {
      ImmutableList<Inline> inlines =
          new Lowering(/* blockContext= */ false, context).lowerInlines(nodes);
      return inlines.isEmpty() ? ImmutableList.of() : ImmutableList.of(new Paragraph(inlines));
    } catch (NestedLinkException e) {
      throw new UnsupportedCommentException("nested link in inline tag", e);
    }
  }

  /// Serializes the value of an HTML attribute.
  private static String serializeAttributeValue(@Nullable List<? extends DocTree> value) {
    if (value == null) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    for (DocTree tree : value) {
      switch (tree) {
        case TextTree t -> sb.append(t.getBody());
        case EntityTree t -> sb.append('&').append(t.getName()).append(';');
        default -> sb.append(tree);
      }
    }
    return sb.toString();
  }
}
