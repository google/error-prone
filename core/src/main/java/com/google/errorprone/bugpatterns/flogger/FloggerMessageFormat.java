/*
 * Copyright 2020 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.flogger;

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.instanceMethod;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import java.util.List;
import java.util.regex.Pattern;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
    name = "FloggerMessageFormat",
    summary = "Invalid message format-style format specifier ({0}), expected printf-style (%s)",
    explanation =
        "Flogger uses printf-style format specifiers, such as %s and %d. Message format-style"
            + " specifiers like {0} don't work.",
    severity = WARNING)
public class FloggerMessageFormat extends BugChecker implements MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> LOG_MATCHER =
      instanceMethod().onDescendantOf("com.google.common.flogger.LoggingApi").named("log");

  private static final Pattern MESSAGE_FORMAT_SPECIFIER = Pattern.compile("\\{[0-9]\\}");

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!LOG_MATCHER.matches(tree, state)) {
      return NO_MATCH;
    }

    List<? extends ExpressionTree> arguments = tree.getArguments();
    // There's a 0-arg 'log' method for terminating complex chains, with no format string
    if (arguments.isEmpty()) {
      return NO_MATCH;
    }
    ExpressionTree formatArg = arguments.get(0);
    String formatString = ASTHelpers.constValue(formatArg, String.class);
    if (formatString == null) {
      return NO_MATCH;
    }
    if (!MESSAGE_FORMAT_SPECIFIER.matcher(formatString).find()) {
      return NO_MATCH;
    }
    Description.Builder description = buildDescription(tree);
    String source = state.getSourceForNode(formatArg);
    String fixed = MESSAGE_FORMAT_SPECIFIER.matcher(source).replaceAll("%s");
    if (!source.equals(fixed)) {
      description.addFix(SuggestedFix.replace(formatArg, fixed));
    }
    return description.build();
  }
}
