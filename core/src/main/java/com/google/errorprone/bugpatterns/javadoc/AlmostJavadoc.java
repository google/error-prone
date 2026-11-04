/*
 * Copyright 2019 The Error Prone Authors.
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

import static com.google.common.collect.Streams.stream;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.BugPattern.StandardTags.STYLE;
import static com.google.errorprone.bugpatterns.javadoc.Utils.getDiagnosticPosition;
import static com.google.errorprone.bugpatterns.javadoc.Utils.getJavadoccableTrees;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static java.util.stream.Collectors.joining;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Streams;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.CompilationUnitTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ErrorProneComment;
import com.google.errorprone.util.ErrorProneToken;
import com.google.errorprone.util.ErrorProneTokens;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.TreePath;
import java.util.Optional;
import org.safere.Pattern;

/**
 * Flags comments which appear to be intended to be Javadoc, but are not started with an extra
 * {@code *}.
 */
@BugPattern(
    summary =
        "This comment contains Javadoc or HTML tags, but isn't started with a double asterisk"
            + " (/**); is it meant to be Javadoc?",
    severity = WARNING,
    tags = STYLE,
    documentSuppression = false)
public final class AlmostJavadoc extends BugChecker implements CompilationUnitTreeMatcher {
  private static final Pattern HAS_TAG =
      Pattern.compile(
          String.format(
              "</(em|b|a|strong|i|pre|code)>|@(%s)",
              Streams.concat(
                      JavadocTag.VALID_CLASS_TAGS.stream(),
                      JavadocTag.VALID_METHOD_TAGS.stream(),
                      JavadocTag.VALID_VARIABLE_TAGS.stream())
                  .map(JavadocTag::name)
                  .map(Pattern::quote)
                  .distinct()
                  .collect(joining("|"))));

  @Override
  public Description matchCompilationUnit(CompilationUnitTree tree, VisitorState state) {
    String source = state.getSourceCode().toString();
    if (!mayContainCandidateComment(source)) {
      return NO_MATCH;
    }
    ImmutableMap<Integer, TreePath> javadoccableTrees = getJavadoccableTrees(tree);
    for (ErrorProneToken token : ErrorProneTokens.getTokens(source, state.context)) {
      for (ErrorProneComment comment : token.comments()) {
        var path = javadoccableTrees.get(token.pos());
        if (path == null) {
          continue;
        }
        Optional<SuggestedFix> fix = generateFix(comment);
        if (fix.isEmpty()) {
          continue;
        }
        if (Utils.getDocTreePath(state.withPath(path)) != null) {
          // Avoid double-javadoc.
          continue;
        }
        if (stream(path).anyMatch(node -> isSuppressed(node, state))) {
          continue;
        }
        state.reportMatch(
            describeMatch(
                getDiagnosticPosition(comment.getSourcePos(0), path.getLeaf()), fix.get()));
      }
    }
    return NO_MATCH;
  }

  /**
   * Returns false if {@code source} definitely doesn't contain any comment for which {@link
   * #generateFix} would return a fix. This allows skipping tokenizing the file, which is relatively
   * expensive.
   *
   * <p>This errs on the side of returning true: e.g. it doesn't distinguish comments from string
   * literals. Comments written using unicode escapes aren't handled, so they make this return true.
   */
  private static boolean mayContainCandidateComment(String source) {
    if (source.contains("\\u")) {
      return true;
    }
    int blockEnd = -1;
    int currentLineEnd = -1;
    int checkedUntil = -1;
    for (int start = source.indexOf('/'); start >= 0; start = source.indexOf('/', start + 1)) {
      if (start + 2 >= source.length()) {
        break;
      }
      char next = source.charAt(start + 1);
      int end;
      if (next == '*') {
        // A block comment that isn't Javadoc.
        if (source.charAt(start + 2) == '*') {
          continue;
        }
        if (blockEnd < start + 2) {
          blockEnd = source.indexOf("*/", start + 2);
          if (blockEnd < 0) {
            return false;
          }
        }
        end = blockEnd + 2;
      } else if (next == '/') {
        // A line comment that ends with "*/".
        if (currentLineEnd < start) {
          currentLineEnd = lineEnd(source, start + 2);
        }
        end = currentLineEnd;
        if (end - start < 4 || !source.startsWith("*/", end - 2)) {
          continue;
        }
      } else {
        continue;
      }
      if (end <= checkedUntil) {
        continue;
      }
      if (HAS_TAG.matcher(source.substring(start, end)).find()) {
        return true;
      }
      checkedUntil = end;
    }
    return false;
  }

  private static int lineEnd(String source, int start) {
    for (int i = start; i < source.length(); i++) {
      char c = source.charAt(i);
      if (c == '\n' || c == '\r') {
        return i;
      }
    }
    return source.length();
  }

  private static Optional<SuggestedFix> generateFix(ErrorProneComment comment) {
    String text = comment.getText();
    if (text.startsWith("/*") && !text.startsWith("/**") && HAS_TAG.matcher(text).find()) {
      int pos = comment.getSourcePos(1);
      return Optional.of(SuggestedFix.replace(pos, pos, "*"));
    }
    if (text.startsWith("//") && text.endsWith("*/") && HAS_TAG.matcher(text).find()) {
      if (text.startsWith("// /**")) {
        return Optional.of(
            SuggestedFix.replace(comment.getSourcePos(0), comment.getSourcePos(2), ""));
      }
      int endReplacement = 2;
      while (endReplacement < text.length()) {
        char c = text.charAt(endReplacement);
        if (c == '/') {
          return Optional.empty();
        }
        if (c != '*' && c != ' ') {
          break;
        }
        ++endReplacement;
      }
      return Optional.of(
          SuggestedFix.replace(
              comment.getSourcePos(1), comment.getSourcePos(endReplacement), "**"));
    }
    return Optional.empty();
  }
}
