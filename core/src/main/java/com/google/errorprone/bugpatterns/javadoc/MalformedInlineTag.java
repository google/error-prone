/*
 * Copyright 2021 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.bugpatterns.javadoc.Utils.getDiagnosticPosition;
import static java.util.stream.Collectors.joining;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.DocTreePath;
import com.sun.tools.javac.parser.Tokens.Comment;
import com.sun.tools.javac.tree.DCTree.DCDocComment;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Finds malformed inline tags where {@literal @}{tag is used instead of {{@literal @}tag.
 *
 * @author aaronhurst@google.com (Aaron Hurst)
 */
@BugPattern(
    name = "MalformedInlineTag",
    summary = "This Javadoc tag is malformed. The correct syntax is {@tag and not @{tag.",
    severity = WARNING,
    documentSuppression = false)
public final class MalformedInlineTag extends BugChecker
    implements ClassTreeMatcher, MethodTreeMatcher, VariableTreeMatcher {

  private static final Pattern MALFORMED_PATTERN =
      Pattern.compile(
          "@\\{("
              + JavadocTag.ALL_INLINE_TAGS.stream().map(JavadocTag::name).collect(joining("|"))
              + ")");

  @Override
  public Description matchClass(ClassTree classTree, VisitorState state) {
    return handle(state);
  }

  @Override
  public Description matchMethod(MethodTree methodTree, VisitorState state) {
    return handle(state);
  }

  @Override
  public Description matchVariable(VariableTree variableTree, VisitorState state) {
    return handle(state);
  }

  /**
   * Main action on each class/method/variable Javadoc comment.
   *
   * <p>Match all instances of the malformed regex pattern in the full comment text. There isn't any
   * benefit to iterating over the parsed tree, as the syntax errors can appear anywhere and won't
   * be parsed.
   */
  private Description handle(VisitorState state) {
    DocTreePath path = Utils.getDocTreePath(state);
    if (path == null) {
      return Description.NO_MATCH;
    }

    Comment comment = ((DCDocComment) path.getDocComment()).comment;
    Matcher matcher = MALFORMED_PATTERN.matcher(comment.getText());
    while (matcher.find()) {
      String tag = matcher.group(1);
      int startPos = comment.getSourcePos(matcher.start());
      int endPos = comment.getSourcePos(matcher.end());

      state.reportMatch(
          buildDescription(getDiagnosticPosition(startPos, path.getTreePath().getLeaf()))
              .setMessage(String.format("The correct syntax to open this inline tag is {@%s.", tag))
              .addFix(SuggestedFix.replace(startPos, endPos, "{@" + tag))
              .build());
    }

    // Intentionally returning NO_MATCH here, because multiple findings are reported above
    return Description.NO_MATCH;
  }
}
