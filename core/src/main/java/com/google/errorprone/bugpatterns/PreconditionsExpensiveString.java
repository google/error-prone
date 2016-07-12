/*
 * Copyright 2012 Google Inc. All Rights Reserved.
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

import static com.google.errorprone.BugPattern.Category.GUAVA;
import static com.google.errorprone.BugPattern.MaturityLevel.EXPERIMENTAL;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.argument;
import static com.google.errorprone.matchers.Matchers.staticMethod;

import com.google.common.collect.Iterables;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.tree.JCTree;
import java.util.regex.Pattern;

/**
 * Error checker for calls to the Preconditions class in Guava which use 'expensive' methods of
 * producing the error string. In most cases, users are better off using the equivalent methods
 * which defer the computation of the string until the test actually fails.
 *
 * @author sjnickerson@google.com (Simon Nickerson)
 */
@BugPattern(
  name = "PreconditionsErrorMessageEagerEvaluation",
  summary =
      "Second argument to Preconditions.* is a call to String.format(), which "
          + "can be unwrapped",
  explanation =
      "Preconditions checks take an error message to display if the check fails. "
          + "The error message is rarely needed, so it should either be cheap to construct "
          + "or constructed only when needed. This check ensures that these error messages "
          + "are not constructed using expensive methods that are evaluated eagerly.",
  category = GUAVA,
  severity = WARNING,
  maturity = EXPERIMENTAL
)
public class PreconditionsExpensiveString extends BugChecker
    implements MethodInvocationTreeMatcher {

  @SuppressWarnings({"vararg", "unchecked"})
  private static final Matcher<MethodInvocationTree> matcher =
      allOf(
          anyOf(
              staticMethod().onClass("com.google.common.base.Preconditions").named("checkNotNull"),
              staticMethod().onClass("com.google.common.base.Preconditions").named("checkState"),
              staticMethod()
                  .onClass("com.google.common.base.Preconditions")
                  .named("checkArgument")),
          argument(
              1,
              Matchers.allOf(
                  Matchers.<ExpressionTree>kindIs(Kind.METHOD_INVOCATION),
                  staticMethod().onClass("java.lang.String").named("format"),
                  new StringFormatCallContainsNoSpecialFormattingMatcher(
                      Pattern.compile("%[^%s]")))));

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!matcher.matches(tree, state)) {
      return Description.NO_MATCH;
    }
    MethodInvocationTree stringFormat = (MethodInvocationTree) tree.getArguments().get(1);
    return describeMatch(
        stringFormat,
        SuggestedFix.builder()
            .replace(
                ((JCTree) stringFormat).getStartPosition(),
                ((JCTree) stringFormat.getArguments().get(0)).getStartPosition(),
                "")
            .replace(
                state.getEndPosition((JCTree) Iterables.getLast(stringFormat.getArguments())),
                state.getEndPosition((JCTree) stringFormat),
                "")
            .build());
  }

  private static class StringFormatCallContainsNoSpecialFormattingMatcher
      implements Matcher<ExpressionTree> {

    private final Pattern invalidFormatCharacters;

    StringFormatCallContainsNoSpecialFormattingMatcher(Pattern invalidFormatCharacters) {
      this.invalidFormatCharacters = invalidFormatCharacters;
    }

    @Override
    public boolean matches(ExpressionTree tree, VisitorState state) {
      if (!(tree instanceof MethodInvocationTree)) {
        return false;
      }
      String formatString =
          ASTHelpers.constValue(((MethodInvocationTree) tree).getArguments().get(0), String.class);
      if (formatString == null) {
        return false;
      }
      return !invalidFormatCharacters.matcher(formatString).find();
    }
  }
}
