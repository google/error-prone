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
import static com.google.errorprone.util.ASTHelpers.getSymbol;

import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.bugpatterns.javadoc.MarkdownCommentPrinter.PrintedComment;
import com.google.errorprone.util.ErrorProneComment.ErrorProneCommentStyle;
import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModuleTree;
import com.sun.source.tree.PackageTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.DocSourcePositions;
import com.sun.source.util.DocTrees;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.DCTree.DCDocComment;
import java.util.List;
import java.util.Optional;

/// Converts traditional `/** ... */` Javadoc comments to Markdown `///` Javadoc comments without
/// requiring symbol attribution or Error Prone's `VisitorState`.
public final class TraditionalJavadocConverter {

  /// A replacement of the source between `start` and `end` with `text`.
  ///
  /// @param commentStart the position of the `/**` in the original source
  /// @param commentEnd the position immediately after the `*/` in the original source
  public record Replacement(int start, int end, int commentStart, int commentEnd, String text) {}

  /// Converts a single traditional Javadoc comment to a Markdown Javadoc replacement, or returns
  /// empty if the comment is not a convertible `/** ... */` comment or fails validation.
  public static Optional<Replacement> convert(
      CompilationUnitTree compilationUnit,
      Tree declaration,
      DocCommentTree docCommentTree,
      DocTrees docTrees,
      String source) {
    if (!(docCommentTree instanceof DCDocComment docComment)
        // ErrorProneCommentStyle is compatible with older JDK versions than CommentStyle.
        || ErrorProneCommentStyle.from(docComment.comment.getStyle())
            != ErrorProneCommentStyle.JAVADOC_BLOCK) {
      return Optional.empty();
    }
    if (isDeprecatedOnlyByJavadoc(docComment, declaration, compilationUnit)) {
      return Optional.empty();
    }
    Optional<CommentRange> range = findComment(docComment, docTrees, compilationUnit, source);
    if (range.isEmpty()) {
      return Optional.empty();
    }
    Optional<MarkdownComment> markdown = MarkdownCommentBuilder.build(docComment, docTrees);
    if (markdown.isEmpty()) {
      return Optional.empty();
    }
    PrintedComment printed = MarkdownCommentPrinter.print(markdown.get());
    if (printed.lines().isEmpty() || !MarkdownCommentValidator.isValid(printed)) {
      return Optional.empty();
    }
    Replacement replacement = replacement(range.get(), printed.lines(), source);
    if (containsUnicodeEscape(replacement.text(), 0, replacement.text().length())) {
      // The text of the comment came from the doc tree, in which Unicode escapes have already been
      // translated, so writing it back could create a Unicode escape (e.g. in a code span).
      return Optional.empty();
    }
    return Optional.of(replacement);
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
      DocCommentTree docComment,
      DocTrees docTrees,
      CompilationUnitTree compilationUnit,
      String source) {
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
  /// contains `///`.
  ///
  /// javac merges adjacent `///` comments into a single Markdown doc comment—including both
  /// `///////` banner comments and trailing `///` comments after code (e.g. `int x = 1; /// note`).
  /// As a cheap fast path that avoids lexing the file, this checks the previous line with a plain
  /// substring match and errs on the side of not converting (for example, if the previous line
  /// contains `///` inside a string literal like `"file:///..."`).
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
  /// would silently un-deprecate it. Uses symbol attribution when available and falls back to
  /// syntactic + import inspection in parse-only mode.
  private static boolean isDeprecatedOnlyByJavadoc(
      DCDocComment docComment, Tree declaration, CompilationUnitTree compilationUnit) {
    if (!docComment.comment.isDeprecated()
        && docComment.getBlockTags().stream()
            .noneMatch(tag -> tag.getKind() == DocTree.Kind.DEPRECATED)) {
      return false;
    }
    List<? extends AnnotationTree> annotations =
        switch (declaration) {
          case ClassTree classTree -> classTree.getModifiers().getAnnotations();
          case MethodTree method -> method.getModifiers().getAnnotations();
          case VariableTree variable -> variable.getModifiers().getAnnotations();
          case PackageTree packageTree -> packageTree.getAnnotations();
          case ModuleTree module -> module.getAnnotations();
          default -> ImmutableList.of();
        };
    return annotations.stream()
        .noneMatch(annotation -> isJavaLangDeprecated(annotation, compilationUnit));
  }

  private static boolean isJavaLangDeprecated(
      AnnotationTree annotation, CompilationUnitTree compilationUnit) {
    Symbol symbol = getSymbol(annotation);
    if (symbol != null) {
      return symbol.getQualifiedName().contentEquals("java.lang.Deprecated");
    }
    return switch (annotation.getAnnotationType()) {
      case IdentifierTree identifier ->
          identifier.getName().contentEquals("Deprecated")
              && !importsNonJavaLangDeprecated(compilationUnit);
      case MemberSelectTree select -> select.toString().equals("java.lang.Deprecated");
      default -> false;
    };
  }

  private static boolean importsNonJavaLangDeprecated(CompilationUnitTree compilationUnit) {
    return compilationUnit.getImports().stream()
        .filter(imp -> !imp.isStatic())
        .map(imp -> imp.getQualifiedIdentifier().toString())
        .anyMatch(name -> name.endsWith(".Deprecated") && !name.equals("java.lang.Deprecated"));
  }

  /// Characters that can appear around the content of a `/** ... */` comment.
  private static final CharMatcher DECORATION = CharMatcher.anyOf(" \t\n\r\f*");

  private static final CharMatcher HORIZONTAL_WHITESPACE = CharMatcher.anyOf(" \t");

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
    return new Replacement(start, end, range.start(), range.end(), replacement.toString());
  }

  private TraditionalJavadocConverter() {}
}
