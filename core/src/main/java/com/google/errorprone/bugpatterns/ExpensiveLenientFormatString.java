/*
 * Copyright 2025 The Error Prone Authors.
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

import static com.google.common.collect.Iterables.getLast;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.bugpatterns.formatstring.LenientFormatStringUtils.getLenientFormatStringPosition;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.staticMethod;
import static com.google.errorprone.util.ASTHelpers.getStartPosition;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import java.util.regex.Pattern;

/**
 * Error checker for calls to the Preconditions class in Guava which use 'expensive' methods of
 * producing the error string. In most cases, users are better off using the equivalent methods
 * which defer the computation of the string until the test actually fails.
 *
 * @author sjnickerson@google.com (Simon Nickerson)
 */
@BugPattern(
    altNames = "PreconditionsExpensiveString",
    summary =
        "String.format is passed to a lenient formatting method, which can be unwrapped to improve"
            + " efficiency.",
    severity = WARNING)
public class ExpensiveLenientFormatString extends BugChecker
    implements MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> STRING_FORMAT_MATCHER =
      staticMethod().onClass("java.lang.String").named("format");

  private static final Pattern INVALID_FORMAT_CHARACTERS = Pattern.compile("%[^%s]");

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    var lenientFormatStringPosition = getLenientFormatStringPosition(tree, state);
    if (lenientFormatStringPosition == -1) {
      return NO_MATCH;
    }
    if (tree.getArguments().size() < lenientFormatStringPosition + 1) {
      return NO_MATCH;
    }
    ExpressionTree argument = tree.getArguments().get(lenientFormatStringPosition);
    if (!STRING_FORMAT_MATCHER.matches(argument, state)) {
      return NO_MATCH;
    }
    MethodInvocationTree stringFormat = (MethodInvocationTree) argument;
    var stringFormatArguments = stringFormat.getArguments();
    String formatString = ASTHelpers.constValue(stringFormatArguments.getFirst(), String.class);
    if (formatString == null) {
      return NO_MATCH;
    }
    if (INVALID_FORMAT_CHARACTERS.matcher(formatString).find()) {
      return NO_MATCH;
    }
    return describeMatch(
        stringFormat,
        SuggestedFix.builder()
            .replace(
                getStartPosition(stringFormat),
                getStartPosition(stringFormatArguments.getFirst()),
                "")
            .replace(
                state.getEndPosition(getLast(stringFormatArguments)),
                state.getEndPosition(stringFormat),
                "")
            .build());
  }
}
