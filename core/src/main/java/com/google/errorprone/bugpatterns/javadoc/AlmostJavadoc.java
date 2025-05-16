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
import java.util.regex.Pattern;

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
    ImmutableMap<Integer, TreePath> javadoccableTrees = getJavadoccableTrees(tree);
    for (ErrorProneToken token :
        ErrorProneTokens.getTokens(state.getSourceCode().toString(), state.context)) {
      for (ErrorProneComment comment : token.comments()) {
        var path = javadoccableTrees.get(token.pos());
        if (path == null) {
          continue;
        }
        if (Utils.getDocTreePath(state.withPath(path)) != null) {
          // Avoid double-javadoc.
          continue;
        }
        if (stream(path).anyMatch(node -> isSuppressed(node, state))) {
          continue;
        }
        generateFix(comment)
            .ifPresent(
                fix ->
                    state.reportMatch(
                        describeMatch(
                            getDiagnosticPosition(comment.getSourcePos(0), path.getLeaf()), fix)));
      }
    }
    return NO_MATCH;
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
