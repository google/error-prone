/*
 * Copyright 2022 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.formatstring;

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.bugpatterns.formatstring.LenientFormatStringUtils.getLenientFormatStringPosition;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static java.lang.String.format;
import static java.util.Collections.nCopies;
import static java.util.stream.Collectors.joining;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.NewClassTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.google.errorprone.util.SourceCodeEscapers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import java.util.List;

/** A BugPattern; see the summary. */
@BugPattern(
    severity = ERROR,
    summary =
        "The number of arguments provided to lenient format methods should match the positional"
            + " specifiers.")
public final class LenientFormatStringValidation extends BugChecker
    implements MethodInvocationTreeMatcher, NewClassTreeMatcher {

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    return match(tree, tree.getArguments(), state);
  }

  @Override
  public Description matchNewClass(NewClassTree tree, VisitorState state) {
    return match(tree, tree.getArguments(), state);
  }

  private Description match(
      ExpressionTree tree, List<? extends ExpressionTree> args, VisitorState state) {
    int formatStringPosition = getLenientFormatStringPosition(tree, state);
    if (formatStringPosition < 0) {
      return NO_MATCH;
    }
    if (args.size() <= formatStringPosition) {
      return NO_MATCH;
    }
    ExpressionTree formatStringArgument = args.get(formatStringPosition);
    Object formatString = ASTHelpers.constValue(formatStringArgument);
    if (!(formatString instanceof String string)) {
      return NO_MATCH;
    }
    int expected = occurrences(string, "%s");
    int actual = args.size() - formatStringPosition - 1;
    if (expected == actual) {
      return NO_MATCH;
    }
    String replacedNumericPlaceholders = string.replace("%d", "%s");
    var builder =
        buildDescription(tree)
            .setMessage(
                format(
                    "Expected %s positional arguments, but saw %s. Note that lenient format strings"
                        + " only support %%s placeholders.",
                    expected, actual));
    if (occurrences(replacedNumericPlaceholders, "%s") == actual
        && formatStringArgument instanceof LiteralTree) {
      builder.addFix(
          SuggestedFix.replace(
              formatStringArgument,
              "\""
                  + SourceCodeEscapers.javaCharEscaper().escape(replacedNumericPlaceholders)
                  + "\""));
      return builder.build();
    }
    if (expected < actual) {
      String extraArgs =
          nCopies(actual - expected, "%s").stream().collect(joining(", ", " (", ")"));
      int endPos = state.getEndPosition(formatStringArgument);
      builder.addFix(
          formatStringArgument instanceof LiteralTree
              ? SuggestedFix.replace(endPos - 1, endPos, extraArgs + "\"")
              : SuggestedFix.postfixWith(formatStringArgument, format("+ \"%s\"", extraArgs)));
    }
    return builder.build();
  }

  private static int occurrences(String haystack, String needle) {
    int count = 0;
    int start = 0;
    while (true) {
      start = haystack.indexOf(needle, start);
      if (start == -1) {
        return count;
      }
      count++;
      start += needle.length();
    }
  }
}
