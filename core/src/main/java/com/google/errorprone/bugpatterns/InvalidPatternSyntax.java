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

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.MaturityLevel.ON_BY_DEFAULT;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.argument;
import static com.google.errorprone.matchers.Matchers.isDescendantOfMethod;
import static com.google.errorprone.matchers.Matchers.methodSelect;
import static com.google.errorprone.matchers.Matchers.staticMethod;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.DescribingMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.tree.JCTree.JCExpression;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * @author mdempsky@google.com (Matthew Dempsky)
 */
@BugPattern(name = "InvalidPatternSyntax",
    summary = "Invalid syntax used for a regular expression",
    explanation = "This error is triggered by calls to Pattern.compile() and String.split() "
        + "that are called with invalid syntax.",
    category = JDK, severity = ERROR, maturity = ON_BY_DEFAULT)
public class InvalidPatternSyntax extends DescribingMatcher<MethodInvocationTree> {

  /* Match string literals that are not valid syntax for regular expressions. */
  private static final Matcher<ExpressionTree> BAD_REGEX_LITERAL = new Matcher<ExpressionTree>() {
    @Override
    public boolean matches(ExpressionTree tree, VisitorState state) {
      Object value = ((JCExpression) tree).type.constValue();
      return value instanceof String && !isValidSyntax((String) value);
    }

    private boolean isValidSyntax(String regex) {
      // Actually valid, but useless.
      if (".".equals(regex)) {
        return false;
      }
      try {
        Pattern.compile(regex);
        return true;
      } catch (PatternSyntaxException e) {
        return false;
      }
    }
  };

  /* Match invocations to String.split() and Pattern.compile() with bad string literals. */
  @SuppressWarnings("unchecked")
  private static final Matcher<MethodInvocationTree> BAD_REGEX_USAGE =
      allOf(
          anyOf(
              methodSelect(isDescendantOfMethod("java.lang.String", "matches(java.lang.String)")),
              methodSelect(isDescendantOfMethod("java.lang.String", "replaceAll(java.lang.String,java.lang.String)")),
              methodSelect(isDescendantOfMethod("java.lang.String", "replaceFirst(java.lang.String,java.lang.String)")),
              methodSelect(isDescendantOfMethod("java.lang.String", "split(java.lang.String)")),
              methodSelect(isDescendantOfMethod("java.lang.String", "split(java.lang.String,int)")),
              methodSelect(staticMethod("java.util.regex.Pattern", "compile")),
              methodSelect(staticMethod("java.util.regex.Pattern", "matches"))),
          argument(0, BAD_REGEX_LITERAL));

  @Override
  public boolean matches(MethodInvocationTree methodInvocationTree, VisitorState state) {
    return BAD_REGEX_USAGE.matches(methodInvocationTree, state);
  }

  @Override
  public Description describe(MethodInvocationTree methodInvocationTree, VisitorState state) {
    SuggestedFix fix = null;
    ExpressionTree arg = methodInvocationTree.getArguments().get(0);
    if ((arg instanceof LiteralTree) && ".".equals(((LiteralTree)arg).getValue())) {
      fix = new SuggestedFix().replace(arg, "\"\\\\.\"");
    }
    return new Description(methodInvocationTree, diagnosticMessage, fix);
  }

  public static class Scanner extends com.google.errorprone.Scanner {
    private InvalidPatternSyntax matcher = new InvalidPatternSyntax();

    @Override
    public Void visitMethodInvocation(MethodInvocationTree node, VisitorState visitorState) {
      evaluateMatch(node, visitorState, matcher);
      return super.visitMethodInvocation(node, visitorState);
    }
  }
}
