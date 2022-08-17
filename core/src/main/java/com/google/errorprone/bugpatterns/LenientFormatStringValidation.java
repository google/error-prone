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

package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.matchers.Matchers.staticMethod;
import static java.lang.String.format;
import static java.util.Collections.nCopies;
import static java.util.stream.Collectors.joining;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodInvocationTree;
import java.util.regex.Pattern;

/** A BugPattern; see the summary. */
@BugPattern(
    severity = ERROR,
    summary =
        "The number of arguments provided to lenient format methods should match the positional"
            + " specifiers.")
public final class LenientFormatStringValidation extends BugChecker
    implements MethodInvocationTreeMatcher {

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    for (LenientFormatMethod method : METHODS) {
      if (!method.matcher().matches(tree, state)) {
        continue;
      }
      var args = tree.getArguments();
      if (args.size() <= method.formatStringPosition()) {
        continue;
      }
      ExpressionTree formatStringArgument = args.get(method.formatStringPosition());
      Object formatString = ASTHelpers.constValue(formatStringArgument);
      if (!(formatString instanceof String)) {
        continue;
      }
      int expected = occurrences((String) formatString, "%s");
      int actual = args.size() - method.formatStringPosition() - 1;
      if (expected == actual) {
        continue;
      }
      var builder =
          buildDescription(tree)
              .setMessage(format("Expected %s positional arguments, but saw %s", expected, actual));
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
    return NO_MATCH;
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

  // TODO(ghm): Consider replacing this with an annotation-based approach (@LenientFormatString?)
  private static final ImmutableList<LenientFormatMethod> METHODS =
      ImmutableList.of(
          LenientFormatMethod.create(
              staticMethod()
                  .onClass("com.google.common.base.Preconditions")
                  .withNameMatching(Pattern.compile("^check.*")),
              1),
          LenientFormatMethod.create(
              staticMethod()
                  .onClass("com.google.common.base.Verify")
                  .withNameMatching(Pattern.compile("^verify.*")),
              1),
          LenientFormatMethod.create(
              staticMethod().onClass("com.google.common.base.Strings").named("lenientFormat"), 0),
          LenientFormatMethod.create(
              staticMethod().onClass("com.google.common.truth.Truth").named("assertWithMessage"),
              0),
          LenientFormatMethod.create(
              instanceMethod().onDescendantOf("com.google.common.truth.Subject").named("check"), 0),
          LenientFormatMethod.create(
              instanceMethod()
                  .onDescendantOf("com.google.common.truth.StandardSubjectBuilder")
                  .named("withMessage"),
              0));

  @AutoValue
  abstract static class LenientFormatMethod {
    abstract Matcher<ExpressionTree> matcher();

    /** Position of the format string; we assume every argument afterwards is a format argument. */
    abstract int formatStringPosition();

    public static LenientFormatMethod create(
        Matcher<ExpressionTree> matcher, int formatStringPosition) {
      return new AutoValue_LenientFormatStringValidation_LenientFormatMethod(
          matcher, formatStringPosition);
    }
  }
}
