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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getAnnotations;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.hasExplicitSource;
import static com.google.errorprone.util.ASTHelpers.isGeneratedConstructor;

import com.google.common.base.CharMatcher;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.ModuleTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.PackageTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.bugpatterns.javadoc.MarkdownCommentPrinter.PrintedComment;
import com.google.errorprone.fixes.FixedPosition;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ErrorProneComment.ErrorProneCommentStyle;
import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModuleTree;
import com.sun.source.tree.PackageTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.DocSourcePositions;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.DCTree.DCDocComment;
import java.util.List;
import java.util.Optional;

/// An experimental tool that converts traditional Javadoc comments to Markdown Javadoc comments.
///
/// The conversion happens in three steps:
///
/// 1. [MarkdownCommentBuilder] converts the comment's doc tree into a [MarkdownComment], keeping
///    any constructs that have no faithful Markdown equivalent as HTML or as traditional inline
///    tags (both of which Markdown documentation comments support).
/// 2. [MarkdownCommentPrinter] prints the [MarkdownComment], escaping any text that would otherwise
///    be interpreted as Markdown.
/// 3. [MarkdownCommentValidator] parses the printed Markdown and checks that it has the intended
///    structure.
///
/// A comment is left unchanged if any of these steps fails.
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
    return isGeneratedConstructor(tree) ? NO_MATCH : checkJavadoc(state);
  }

  @Override
  public Description matchVariable(VariableTree tree, VisitorState state) {
    return switch (getSymbol(tree).getKind()) {
      case FIELD, ENUM_CONSTANT -> checkJavadoc(state);
      default -> NO_MATCH;
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
    if (!hasExplicitSource(state.getPath().getLeaf(), state)) {
      return NO_MATCH;
    }
    JavacTrees docTrees = JavacTrees.instance(state.context);
    if (!(docTrees.getDocCommentTree(state.getPath()) instanceof DCDocComment docComment)
        // ErrorProneCommentStyle is compatible with older JDK versions than CommentStyle.
        || ErrorProneCommentStyle.from(docComment.comment.getStyle())
            != ErrorProneCommentStyle.JAVADOC_BLOCK) {
      return NO_MATCH;
    }
    if (isDeprecatedOnlyByJavadoc(docComment, state.getPath().getLeaf())) {
      return NO_MATCH;
    }
    Optional<CommentRange> range = findComment(docComment, docTrees, state);
    if (range.isEmpty()) {
      return NO_MATCH;
    }
    Optional<MarkdownComment> markdown = MarkdownCommentBuilder.build(docComment, docTrees);
    if (markdown.isEmpty()) {
      return NO_MATCH;
    }
    PrintedComment printed = MarkdownCommentPrinter.print(markdown.get());
    if (printed.lines().isEmpty() || !MarkdownCommentValidator.isValid(printed)) {
      return NO_MATCH;
    }
    Replacement replacement =
        replacement(range.get(), printed.lines(), state.getSourceCode().toString());
    if (containsUnicodeEscape(replacement.text(), 0, replacement.text().length())) {
      // The text of the comment came from the doc tree, in which Unicode escapes have already been
      // translated, so writing it back could create a Unicode escape (e.g. in a code span).
      return NO_MATCH;
    }
    return buildDescription(new FixedPosition(state.getPath().getLeaf(), range.get().start()))
        .addFix(SuggestedFix.replace(replacement.start(), replacement.end(), replacement.text()))
        .build();
  }

  /// Returns true if `s[start..end)` contains a Java Unicode escape (an odd number of backslashes
  /// followed by `u`).
  private static boolean containsUnicodeEscape(String s, int start, int end) {
    int backslashes = 0;
    for (int i = start; i < end; i++) {
      char c = s.charAt(i);
      if (c == '\\') {
        backslashes++;
      } else {
        if (c == 'u' && (backslashes & 1) == 1) {
          return true;
        }
        backslashes = 0;
      }
    }
    return false;
  }

  /// The location of a `/** ... */` comment in the source.
  ///
  /// @param start the position of the `/**`
  /// @param end the position after the `*/`
  private record CommentRange(int start, int end) {}

  /// Finds the source of the given comment.
  ///
  /// Doc comments may be synthesized (e.g. by Lombok), so this checks that the source really is a
  /// `/** ... */` comment that contains the doc comment's content.
  private static Optional<CommentRange> findComment(
      DocCommentTree docComment, JavacTrees docTrees, VisitorState state) {
    String source = state.getSourceCode().toString();
    CompilationUnitTree compilationUnit = state.getPath().getCompilationUnit();
    DocSourcePositions positions = docTrees.getSourcePositions();
    int contentStart = (int) positions.getStartPosition(compilationUnit, docComment, docComment);
    int contentEnd = (int) positions.getEndPosition(compilationUnit, docComment, docComment);
    if (contentStart < 3 || contentEnd <= contentStart || contentEnd > source.length()) {
      return Optional.empty();
    }
    int start = source.lastIndexOf("/**", contentStart - 3);
    int end = source.indexOf("*/", contentEnd);
    if (start < 0
        || end < 0
        || source.indexOf("*/", start + 2) != end
        || !DECORATION.matchesAllOf(source.substring(start + 3, contentStart))
        || !DECORATION.matchesAllOf(source.substring(contentEnd, end))
        // Banner comments like `/*****`, which google-java-format also leaves alone.
        || source.startsWith("/***", start)
        // The doc tree has the translated characters, which we would write back unescaped.
        || containsUnicodeEscape(source, start, end)
        || followsLineDocComment(source, start)) {
      return Optional.empty();
    }
    return Optional.of(new CommentRange(start, end + 2));
  }

  /// Returns true if the comment starting at `start` is on its own line, and the previous line
  /// contains what may be a `///` comment.
  ///
  /// javac merges a `///` comment with a `///` comment on the next line, so converting the comment
  /// would change the documentation (e.g. by prepending a `//////` banner). This errs on the side
  /// of not converting, e.g. if the `///` is in a string literal.
  private static boolean followsLineDocComment(String source, int start) {
    int lineStart = source.lastIndexOf('\n', start - 1) + 1;
    if (lineStart == 0 || !source.substring(lineStart, start).isBlank()) {
      // The comment is on the first line, or follows code that stays on the previous line.
      return false;
    }
    int previousLineStart = source.lastIndexOf('\n', lineStart - 2) + 1;
    return source.substring(previousLineStart, lineStart).contains("///");
  }

  /// Returns true if the comment deprecates the declaration via Javadoc `@deprecated`, but the
  /// declaration isn't annotated with `@java.lang.Deprecated`.
  ///
  /// javac only treats a `@deprecated` tag in a `/** ... */` comment (detected by
  /// `JavaTokenizer.isDeprecated()`) as deprecating the declaration, so converting the comment
  /// would silently un-deprecate it.
  private static boolean isDeprecatedOnlyByJavadoc(DCDocComment docComment, Tree declaration) {
    if (!docComment.comment.isDeprecated()
        && docComment.getBlockTags().stream()
            .noneMatch(tag -> tag.getKind() == DocTree.Kind.DEPRECATED)) {
      return false;
    }
    return getAnnotations(declaration).stream()
        .noneMatch(annotation -> {
          Symbol symbol = getSymbol(annotation);
          return symbol != null && symbol.getQualifiedName().contentEquals("java.lang.Deprecated");
        });
  }

  /// Characters that can appear around the content of a `/** ... */` comment.
  private static final CharMatcher DECORATION = CharMatcher.anyOf(" \t\n\r\f*");

  private static final CharMatcher HORIZONTAL_WHITESPACE = CharMatcher.anyOf(" \t");

  /// A replacement of the source between `start` and `end` with `text`.
  private record Replacement(int start, int end, String text) {}

  /// Returns a replacement of the given comment with a `///` comment with the given lines.
  private static Replacement replacement(CommentRange range, List<String> lines, String source) {
    int start = range.start();
    int end = range.end();
    int lineStart = source.lastIndexOf('\n', start - 1) + 1;
    int lineEnd = source.indexOf('\n', end);
    if (lineEnd < 0) {
      lineEnd = source.length();
    }
    String linePrefix = source.substring(lineStart, start);
    String indent =
        linePrefix.substring(
            0, linePrefix.length() - HORIZONTAL_WHITESPACE.trimLeadingFrom(linePrefix).length());
    StringBuilder replacement = new StringBuilder();
    if (!linePrefix.isBlank()) {
      // Code before the comment on the same line, e.g. `int x; /** Doc. */ int y;`.
      start = lineStart + HORIZONTAL_WHITESPACE.trimTrailingFrom(linePrefix).length();
      replacement.append('\n').append(indent);
    }
    for (int i = 0; i < lines.size(); i++) {
      String line = lines.get(i);
      checkArgument(line.indexOf('\n') < 0 && line.indexOf('\r') < 0, "embedded line break");
      if (i > 0) {
        replacement.append('\n').append(indent);
      }
      replacement.append("///");
      if (!line.isEmpty()) {
        replacement.append(' ').append(line);
      }
    }
    String lineSuffix = source.substring(end, lineEnd);
    if (!lineSuffix.isBlank()) {
      // Code after the comment on the same line, e.g. `/** Doc. */ int x;`.
      end += lineSuffix.length() - HORIZONTAL_WHITESPACE.trimLeadingFrom(lineSuffix).length();
      replacement.append('\n').append(indent);
    }
    return new Replacement(start, end, replacement.toString());
  }
}
