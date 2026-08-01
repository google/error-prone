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

import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.isGeneratedConstructor;

import com.google.common.base.Ascii;
import com.google.common.collect.ImmutableMap;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.ModuleTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.PackageTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.fixes.FixedPosition;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ErrorProneComment.ErrorProneCommentStyle;
import com.sun.source.doctree.AttributeTree;
import com.sun.source.doctree.AuthorTree;
import com.sun.source.doctree.CommentTree;
import com.sun.source.doctree.DeprecatedTree;
import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocRootTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.EndElementTree;
import com.sun.source.doctree.EntityTree;
import com.sun.source.doctree.ErroneousTree;
import com.sun.source.doctree.EscapeTree;
import com.sun.source.doctree.HiddenTree;
import com.sun.source.doctree.IndexTree;
import com.sun.source.doctree.InheritDocTree;
import com.sun.source.doctree.LinkTree;
import com.sun.source.doctree.LiteralTree;
import com.sun.source.doctree.ParamTree;
import com.sun.source.doctree.ProvidesTree;
import com.sun.source.doctree.RawTextTree;
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
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModuleTree;
import com.sun.source.tree.PackageTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.DocSourcePositions;
import com.sun.source.util.DocTreePath;
import com.sun.source.util.DocTreePathScanner;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.tree.DCTree.DCDocComment;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/// An experimental tool that converts traditional Javadoc comments to Markdown Javadoc comments.
///
/// Note that malformed traditional Javadocs will likely result in malformed Markdown Javadocs
/// ("garbage in, garbage out").
///
/// **WARNING:** this tool is currently a prototype and may have bugs.
@BugPattern(
    summary = "Converts traditional Javadoc comments into Markdown Javadoc comments",
    severity = SUGGESTION)
public final class TraditionalJavadocToMarkdown extends BugChecker
    implements ClassTreeMatcher,
        MethodTreeMatcher,
        VariableTreeMatcher,
        PackageTreeMatcher,
        ModuleTreeMatcher {

  @Override
  public Description matchClass(ClassTree tree, VisitorState state) {
    return checkJavadoc(state);
  }

  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    return isGeneratedConstructor(tree) ? Description.NO_MATCH : checkJavadoc(state);
  }

  @Override
  public Description matchVariable(VariableTree tree, VisitorState state) {
    return switch (getSymbol(tree).getKind()) {
      case FIELD, ENUM_CONSTANT -> checkJavadoc(state);
      default -> Description.NO_MATCH;
    };
  }

  @Override
  public Description matchPackage(PackageTree tree, VisitorState state) {
    return checkJavadoc(state);
  }

  @Override
  public Description matchModule(ModuleTree tree, VisitorState state) {
    return checkJavadoc(state);
  }

  private Description checkJavadoc(VisitorState state) {
    DocCommentTree docCommentTree =
        JavacTrees.instance(state.context).getDocCommentTree(state.getPath());
    if (docCommentTree == null) {
      return Description.NO_MATCH;
    }

    DCDocComment docComment = (DCDocComment) docCommentTree;

    // Instead of checking against CommentStyle directly, we use ErrorProneCommentStyle for
    // compatibility with older JDK versions.
    ErrorProneCommentStyle style = ErrorProneCommentStyle.from(docComment.comment.getStyle());

    // Only check javadoc block (/** ... */) style comments
    if (style != ErrorProneCommentStyle.JAVADOC_BLOCK) {
      return Description.NO_MATCH;
    }

    DocTreePath docTreePath = new DocTreePath(state.getPath(), docCommentTree);
    String source = state.getSourceCode().toString();
    DocSourcePositions positions = JavacTrees.instance(state.context).getSourcePositions();
    CompilationUnitTree compilationUnitTree = state.getPath().getCompilationUnit();

    int startPos = (int) positions.getStartPosition(compilationUnitTree, docComment, docComment);
    int endPos = (int) positions.getEndPosition(compilationUnitTree, docComment, docComment);

    if (startPos == -1 || endPos == -1) {
      return Description.NO_MATCH;
    }

    // Search backwards for /** and forwards for */
    int realStart = source.lastIndexOf("/**", startPos);
    int realEnd = source.indexOf("*/", endPos);
    if (realStart == -1 || realEnd == -1) {
      // Fallback to comment object positions
      realStart = docComment.comment.getSourcePos(0);
      realEnd = realStart + docComment.comment.getText().length();
    } else {
      realEnd += 2; // include */
    }

    String markdown = convertToMarkdown(docTreePath, state, realStart);
    return buildDescription(new FixedPosition(docTreePath.getTreePath().getLeaf(), realStart))
        .addFix(SuggestedFix.replace(realStart, realEnd, markdown))
        .build();
  }

  private String convertToMarkdown(DocTreePath docTreePath, VisitorState state, int startPos) {
    int minHeadingLevel = findMinHeadingLevel(docTreePath);
    StringBuilder sb = new StringBuilder();
    new MarkdownConverterScanner(minHeadingLevel).scan(docTreePath, sb);
    String content = sb.toString();

    // Collapse multiple blank lines into a single blank line
    content = content.replaceAll("(?m)^\\s+$", ""); // treat whitespace lines as empty
    content = content.replaceAll("\n{3,}", "\n\n");
    content = content.trim();

    if (content.isEmpty()) {
      return "///";
    }

    // Get original indentation (ensure it only contains spaces)
    String source = state.getSourceCode().toString();
    int lineStart = source.lastIndexOf('\n', startPos) + 1;
    String indent = source.substring(lineStart, startPos).replaceAll("[^ ]", "");

    String[] lines = content.split("\n", -1);
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < lines.length; i++) {
      if (i > 0) {
        result.append('\n').append(indent);
      }
      String line = lines[i].stripTrailing();
      result.append("///").append(line.isEmpty() ? "" : " " + line);
    }
    return result.toString();
  }

  private static int findMinHeadingLevel(DocTreePath docTreePath) {
    var scanner =
        new DocTreePathScanner<Void, Void>() {
          int minLevel = 6;

          @Override
          public Void visitStartElement(StartElementTree node, Void unused) {
            switch (Ascii.toLowerCase(node.getName().toString())) {
              case "h1", "h2", "h3", "h4", "h5", "h6" -> {
                int level = node.getName().charAt(1) - '0';
                minLevel = Math.min(minLevel, level);
              }
              default -> {}
            }
            return null;
          }
        };
    scanner.scan(docTreePath, null);
    return scanner.minLevel;
  }

  private static final class MarkdownConverterScanner
      extends DocTreePathScanner<Void, StringBuilder> {
    private final int minHeadingLevel;
    private final Deque<TagFrame> scopeStack = new ArrayDeque<>();

    /// Represents an active HTML element or inline tag frame on the scope stack.
    private record TagFrame(String name, ImmutableMap<String, String> attributes) {
      TagFrame {
        name = Ascii.toLowerCase(name);
      }

      TagFrame(String name) {
        this(name, ImmutableMap.of());
      }

      Optional<String> getAttribute(String key) {
        return Optional.ofNullable(attributes.get(Ascii.toLowerCase(key)));
      }
    }

    MarkdownConverterScanner(int minHeadingLevel) {
      this.minHeadingLevel = minHeadingLevel;
    }

    /// Returns the name of the innermost matching tag from the scope stack, if any.
    private Optional<String> firstOf(String... names) {
      List<String> nameList = Arrays.asList(names);
      return scopeStack.stream().map(TagFrame::name).filter(nameList::contains).findFirst();
    }

    private boolean inSee() {
      // An inline {@link} takes precedence over a surrounding @see block because inline links
      // should not be treated as reference signatures (which would escape brackets).
      //
      // Note: It is actually possible (though rare and likely unintended) to have a {@link}
      // nested inside an HTML anchor tag, which in turn might be inside an @see block, e.g.:
      //   @see <a href="http://google.com">{@link Integer#signum}</a>
      // This generates nested <a> tags in traditional Javadoc HTML output.
      return firstOf("link", "see").filter("see"::equals).isPresent();
    }

    private boolean innermostListIsOrdered() {
      return firstOf("ol", "ul").filter("ol"::equals).isPresent();
    }

    private int countOf(String... names) {
      List<String> nameList = Arrays.asList(names);
      return (int) scopeStack.stream().filter(frame -> nameList.contains(frame.name())).count();
    }

    private boolean notIn(String... names) {
      return countOf(names) == 0;
    }

    private boolean notInPre() {
      return notIn("pre");
    }

    /// Pops the top-most matching frame with the given lowercased name from the scope stack.
    @CanIgnoreReturnValue
    private @Nullable TagFrame popMatchingFrame(String name) {
      var match = scopeStack.stream().filter(frame -> frame.name().equals(name)).findFirst();
      match.ifPresent(scopeStack::remove);
      return match.orElse(null);
    }

    @Override
    public Void visitDocComment(DocCommentTree node, StringBuilder sb) {
      scan(node.getFullBody(), sb);
      if (!node.getBlockTags().isEmpty()) {
        ensureBlankLine(sb);
        scan(node.getBlockTags(), sb);
      }
      return null;
    }

    private static void ensureNewline(StringBuilder sb) {
      // Strip trailing horizontal whitespace (but not newlines)
      while (!sb.isEmpty()
          && sb.charAt(sb.length() - 1) != '\n'
          && Character.isWhitespace(sb.charAt(sb.length() - 1))) {
        sb.setLength(sb.length() - 1);
      }
      if (sb.length() > 0 && sb.charAt(sb.length() - 1) != '\n') {
        sb.append('\n');
      }
    }

    private static void ensureBlankLine(StringBuilder sb) {
      if (!sb.isEmpty()) {
        ensureNewline(sb);
        if (sb.length() == 1 || (sb.length() >= 2 && sb.charAt(sb.length() - 2) != '\n')) {
          sb.append('\n');
        }
      }
    }

    @Override
    public Void visitText(TextTree node, StringBuilder sb) {
      String text = node.getBody();
      boolean inPre = countOf("pre") > 0;
      // Remove the newline after leading ``` if present.
      int offset = (inPre && isAtFence(sb) && text.startsWith("\n")) ? 1 : 0;
      sb.append(text, offset, text.length());
      return null;
    }

    /// Checks if the StringBuilder ends with {@code ```\n}. This is more efficient than {@code
    /// sb.toString().endsWith("```\n")}.
    private static boolean isAtFence(StringBuilder sb) {
      int len = sb.length();
      return len >= 4 && sb.substring(len - 4, len).equals("```\n");
    }

    @Override
    public Void visitReference(ReferenceTree node, StringBuilder sb) {
      String sig = node.getSignature();
      if (sig != null) {
        if (inSee()) {
          sb.append(sig);
        } else {
          sb.append(sig.replace("[", "\\[").replace("]", "\\]"));
        }
      }
      return null;
    }

    @Override
    public Void visitLiteral(LiteralTree node, StringBuilder sb) {
      if (node.getKind() == DocTree.Kind.CODE) {
        if (notInPre()) {
          sb.append("`").append(node.getBody().getBody()).append("`");
        } else {
          // handle the common <pre>{@code...}</pre> idiom
          sb.append(node.getBody().getBody());
        }
      } else {
        // Preserve {@literal}, which is usually used to quote characters that would otherwise be
        // interpreted as HTML markup (e.g. '<' or '>').
        sb.append("{@literal ").append(node.getBody().getBody()).append("}");
      }
      return null;
    }

    @Override
    public Void visitLink(LinkTree node, StringBuilder sb) {
      boolean plain = node.getKind() == DocTree.Kind.LINK_PLAIN;
      scopeStack.push(new TagFrame("link"));
      StringBuilder refBuilder = new StringBuilder();
      String reference;
      String label;
      try {
        scan(node.getReference(), refBuilder);
        reference = refBuilder.toString().trim();
        if (!node.getLabel().isEmpty()) {
          StringBuilder labelBuilder = new StringBuilder();
          scan(node.getLabel(), labelBuilder);
          label = labelBuilder.toString();
        } else {
          label = "";
        }
      } finally {
        popMatchingFrame("link");
      }
      if (label.isEmpty()) {
        sb.append("[").append(reference).append("]");
      } else {
        if (plain) {
          sb.append("[").append(label).append("][").append(reference).append("]");
        } else {
          sb.append("[`").append(label).append("`][").append(reference).append("]");
        }
      }
      return null;
    }

    @Override
    public Void visitInheritDoc(InheritDocTree node, StringBuilder sb) {
      sb.append("{@inheritDoc}");
      return null;
    }

    @Override
    public Void visitStartElement(StartElementTree node, StringBuilder sb) {
      if (inSee()) {
        reconstructTag(node, sb);
        return null;
      }
      String name = Ascii.toLowerCase(node.getName().toString());
      ImmutableMap<String, String> attributes = ImmutableMap.of();

      switch (name) {
        case "b", "strong" -> {
          if (notIn("b", "strong")) {
            sb.append("**");
          }
        }
        case "i", "em" -> {
          if (notIn("i", "em")) {
            sb.append("*");
          }
        }
        case "code" -> {
          if (notIn("code") && notInPre()) {
            sb.append("`");
          }
        }
        case "p", "ul", "ol" -> ensureBlankLine(sb);
        case "li" -> {
          ensureNewline(sb);
          int listDepth = countOf("ul", "ol");
          int indent = Math.max(0, (listDepth - 1) * 2);
          String bullet = innermostListIsOrdered() ? "1. " : "- ";
          sb.repeat(" ", indent).append(bullet);
        }
        case "h1", "h2", "h3", "h4", "h5", "h6" -> {
          ensureBlankLine(sb);
          int level = name.charAt(1) - '0';
          int hashCount = level - minHeadingLevel + 1;
          sb.repeat("#", Math.max(1, hashCount)).append(" ");
        }
        case "pre" -> {
          if (notInPre()) {
            ensureBlankLine(sb);
            sb.append("```\n");
          }
        }
        case "a" -> {
          if (node.isSelfClosing()) {
            reconstructTag(node, sb);
            return null;
          }
          Optional<String> href = findAttributeValue(node.getAttributes(), "href");
          if (href.isPresent()) {
            sb.append("[");
            attributes = ImmutableMap.of("href", href.get());
          } else {
            reconstructTag(node, sb);
          }
        }
        default -> reconstructTag(node, sb);
      }

      if (!node.isSelfClosing()) {
        scopeStack.push(new TagFrame(name, attributes));
      }
      return null;
    }

    private void reconstructTag(StartElementTree node, StringBuilder sb) {
      sb.append('<').append(node.getName());
      scan(node.getAttributes(), sb);
      if (node.isSelfClosing()) {
        sb.append('/');
      }
      sb.append('>');
    }

    @Override
    public Void visitEndElement(EndElementTree node, StringBuilder sb) {
      String name = Ascii.toLowerCase(node.getName().toString());
      // Inside a @see block, HTML tags are reconstructed as raw text and were not pushed to
      // scopeStack.
      if (inSee()) {
        sb.append("</").append(node.getName()).append(">");
        return null;
      }

      TagFrame poppedFrame = popMatchingFrame(name);

      switch (name) {
        case "b", "strong" -> {
          if (notIn("b", "strong")) {
            sb.append("**");
          }
        }
        case "i", "em" -> {
          if (notIn("i", "em")) {
            sb.append('*');
          }
        }
        case "code" -> {
          if (notIn("code") && notInPre()) {
            sb.append('`');
          }
        }
        case "pre" -> {
          if (notInPre()) {
            ensureNewline(sb);
            sb.append("```\n");
          }
        }
        case "a" -> {
          if (poppedFrame != null && poppedFrame.getAttribute("href").isPresent()) {
            sb.append("](").append(poppedFrame.getAttribute("href").get()).append(')');
          } else {
            sb.append("</a>");
          }
        }
        case "p", "ul", "ol", "li", "h1", "h2", "h3", "h4", "h5", "h6" -> {}
        default -> sb.append("</").append(node.getName()).append(">");
      }
      return null;
    }

    @Override
    public Void visitParam(ParamTree node, StringBuilder sb) {
      ensureNewline(sb);
      sb.append("@param ")
          .append(node.isTypeParameter() ? "<" : "")
          .append(node.getName())
          .append(node.isTypeParameter() ? ">" : "")
          .append(' ');
      scan(node.getDescription(), sb);
      return null;
    }

    @Override
    public Void visitAuthor(AuthorTree node, StringBuilder sb) {
      return visitBlockTag("author", node.getName(), sb);
    }

    @Override
    public Void visitSince(SinceTree node, StringBuilder sb) {
      return visitBlockTag("since", node.getBody(), sb);
    }

    @Override
    public Void visitVersion(VersionTree node, StringBuilder sb) {
      return visitBlockTag("version", node.getBody(), sb);
    }

    @Override
    public Void visitSee(SeeTree node, StringBuilder sb) {
      scopeStack.push(new TagFrame("see"));
      try {
        List<? extends DocTree> ref = node.getReference();
        if (!ref.isEmpty() && ref.get(0).getKind() == DocTree.Kind.REFERENCE && ref.size() > 1) {
          ensureNewline(sb);
          sb.append("@see ");
          scan(ref.get(0), sb);
          sb.append(' ');
          scan(ref.subList(1, ref.size()), sb);
          return null;
        }
        return visitBlockTag("see", ref, sb);
      } finally {
        popMatchingFrame("see");
      }
    }

    @Override
    public Void visitErroneous(ErroneousTree node, StringBuilder sb) {
      if (node.getBody().startsWith("@") && !node.getBody().startsWith("{@")) {
        ensureNewline(sb);
      }
      sb.append(node.getBody());
      return null;
    }

    @Override
    public Void visitDeprecated(DeprecatedTree node, StringBuilder sb) {
      return visitBlockTag("deprecated", node.getBody(), sb);
    }

    @Override
    public Void visitReturn(ReturnTree node, StringBuilder sb) {
      return visitBlockTag("return", node.getDescription(), sb);
    }

    @Override
    public Void visitUnknownBlockTag(UnknownBlockTagTree node, StringBuilder sb) {
      return visitBlockTag(node.getTagName(), node.getContent(), sb);
    }

    @Override
    public Void visitUnknownInlineTag(UnknownInlineTagTree node, StringBuilder sb) {
      sb.append("{@").append(node.getTagName()).append(' ');
      scan(node.getContent(), sb);
      sb.append("}");
      return null;
    }

    @Override
    public Void visitAttribute(AttributeTree node, StringBuilder sb) {
      sb.append(' ').append(node.getName());
      if (node.getValueKind() == AttributeTree.ValueKind.EMPTY) {
        return null;
      }

      String quote =
          switch (node.getValueKind()) {
            case SINGLE -> "'";
            case DOUBLE -> "\"";
            default -> "";
          };

      sb.append('=');
      sb.append(quote);
      scan(node.getValue(), sb);
      sb.append(quote);
      return null;
    }

    @Override
    public Void visitSnippet(SnippetTree node, StringBuilder sb) {
      CodeBlock codeBlock = asCodeBlock(node);
      if (codeBlock != null) {
        sb.append("```").append(codeBlock.lang).append('\n');
        sb.append(codeBlock.body);
        sb.append("```");
        return null;
      }

      sb.append("{@snippet");
      scan(node.getAttributes(), sb);
      if (node.getBody() != null) {
        sb.append(" :\n");
        sb.append(node.getBody().getBody());
      }
      sb.append('}');
      return null;
    }

    private @Nullable CodeBlock asCodeBlock(SnippetTree node) {
      TextTree body = node.getBody();
      if (body == null) {
        // If the body is null, that means it's an external snippet so it can't use backticks
        return null;
      }

      for (DocTree docTree : node.getAttributes()) {
        if (!(docTree instanceof AttributeTree attribute
            && Ascii.equalsIgnoreCase(attribute.getName(), "lang"))) {
          return null;
        }
      }
      String lang = findAttributeValue(node.getAttributes(), "lang").orElse("");
      return new CodeBlock(lang, body.getBody());
    }

    @Override
    public Void visitThrows(ThrowsTree node, StringBuilder sb) {
      ensureNewline(sb);
      sb.append("@throws ");
      scan(node.getExceptionName(), sb);
      sb.append(' ');
      scan(node.getDescription(), sb);
      return null;
    }

    @Override
    public Void visitComment(CommentTree node, StringBuilder sb) {
      sb.append(node.getBody());
      return null;
    }

    @Override
    public Void visitDocRoot(DocRootTree node, StringBuilder sb) {
      sb.append("{@docRoot}");
      return null;
    }

    @Override
    public Void visitEntity(EntityTree node, StringBuilder sb) {
      sb.append('&').append(node.getName()).append(';');
      return null;
    }

    @Override
    public Void visitEscape(EscapeTree node, StringBuilder sb) {
      switch (node.getBody()) {
        case "@" -> sb.append("\\@");
        case "*" -> sb.append("\\*");
        default -> sb.append(node.getBody());
      }
      return null;
    }

    @Override
    public Void visitHidden(HiddenTree node, StringBuilder sb) {
      return visitBlockTag("hidden", node.getBody(), sb);
    }

    @Override
    public Void visitIndex(IndexTree node, StringBuilder sb) {
      sb.append("{@index ");
      scan(node.getSearchTerm(), sb);
      if (!node.getDescription().isEmpty()) {
        sb.append(' ');
        scan(node.getDescription(), sb);
      }
      sb.append('}');
      return null;
    }

    @Override
    public Void visitProvides(ProvidesTree node, StringBuilder sb) {
      ensureNewline(sb);
      sb.append("@provides ");
      scan(node.getServiceType(), sb);
      if (!node.getDescription().isEmpty()) {
        sb.append(' ');
        scan(node.getDescription(), sb);
      }
      return null;
    }

    @Override
    public Void visitRawText(RawTextTree node, StringBuilder sb) {
      sb.append(node.getContent());
      return null;
    }

    @Override
    public Void visitSerial(SerialTree node, StringBuilder sb) {
      return visitBlockTag("serial", node.getDescription(), sb);
    }

    @Override
    public Void visitSerialData(SerialDataTree node, StringBuilder sb) {
      return visitBlockTag("serialData", node.getDescription(), sb);
    }

    @Override
    public Void visitSerialField(SerialFieldTree node, StringBuilder sb) {
      ensureNewline(sb);
      sb.append("@serialField ").append(node.getName()).append(' ');
      scan(node.getType(), sb);
      if (!node.getDescription().isEmpty()) {
        sb.append(' ');
        scan(node.getDescription(), sb);
      }
      return null;
    }

    @Override
    public Void visitSpec(SpecTree node, StringBuilder sb) {
      ensureNewline(sb);
      sb.append("@spec ");
      scan(node.getURL(), sb);
      if (!node.getTitle().isEmpty()) {
        sb.append(' ');
        scan(node.getTitle(), sb);
      }
      return null;
    }

    @Override
    public Void visitSummary(SummaryTree node, StringBuilder sb) {
      sb.append("{@summary");
      if (!node.getSummary().isEmpty()) {
        sb.append(' ');
        scan(node.getSummary(), sb);
      }
      sb.append('}');
      return null;
    }

    @Override
    public Void visitSystemProperty(SystemPropertyTree node, StringBuilder sb) {
      sb.append("{@systemProperty ").append(node.getPropertyName()).append('}');
      return null;
    }

    @Override
    public Void visitUses(UsesTree node, StringBuilder sb) {
      ensureNewline(sb);
      sb.append("@uses ");
      scan(node.getServiceType(), sb);
      if (!node.getDescription().isEmpty()) {
        sb.append(' ');
        scan(node.getDescription(), sb);
      }
      return null;
    }

    @Override
    public Void visitValue(ValueTree node, StringBuilder sb) {
      sb.append("{@value");
      if (node.getFormat() != null) {
        sb.append(' ');
        scan(node.getFormat(), sb);
      }
      if (node.getReference() != null) {
        sb.append(' ');
        scan(node.getReference(), sb);
      }
      sb.append('}');
      return null;
    }

    /// Handles the common pattern for block tags by ensuring a newline, appending the tag name, and
    /// scanning the content. This simplifies individual block tag visitor methods.
    private Void visitBlockTag(
        String tagName, Iterable<? extends DocTree> content, StringBuilder sb) {
      ensureNewline(sb);
      sb.append('@').append(tagName).append(' ');
      scan(content, sb);
      return null;
    }

    private Optional<String> findAttributeValue(
        Iterable<? extends DocTree> attributes, String name) {
      for (DocTree docTree : attributes) {
        if (docTree instanceof AttributeTree attribute
            && Ascii.equalsIgnoreCase(attribute.getName(), name)) {
          StringBuilder value = new StringBuilder();
          scan(attribute.getValue(), value);
          return Optional.of(value.toString());
        }
      }
      return Optional.empty();
    }
  }

  private record CodeBlock(String lang, String body) {}
}
