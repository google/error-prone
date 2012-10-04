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
import static com.google.errorprone.matchers.Matchers.isDescendantOfMethod;
import static com.google.errorprone.matchers.Matchers.kindIs;
import static com.google.errorprone.matchers.Matchers.methodHasAnnotation;
import static com.google.errorprone.matchers.Matchers.methodSelect;
import static com.google.errorprone.matchers.Matchers.parentNode;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.DescribingMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.MethodInvocationMethodSelect;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;

import java.util.Arrays;
import java.util.List;

/**
 * @author alexeagle@google.com (Alex Eagle)
 */
@BugPattern(name = "ReturnValueIgnored",
    altNames = {"ResultOfMethodCallIgnored"},
    summary = "Ignored return value of method which has no side-effect",
    explanation = "Method calls that have no side-effect are pointless if you ignore the value "
        + "returned.",
    category = JDK, severity = ERROR, maturity = ON_BY_DEFAULT)
public class ReturnValueIgnored extends DescribingMatcher<MethodInvocationTree> {

  /**
   * A list of types which this checker should examine method calls on.
   */
  private static final List<String> typesToCheck = Arrays.asList("java.lang.String",
      "java.math.BigInteger", "java.math.BigDecimal");

  /**
   * Matches if the method being called is a statement (rather than an expression), and the method
   *    1) Has been annotated with @CheckReturnValue, or
   *    2) Is being called on an instance of the specified types, and the method returns the same
   *       type (e.g. String.trim()).
   */
  @SuppressWarnings("unchecked")
  @Override
  public boolean matches(MethodInvocationTree methodInvocationTree, VisitorState state) {
    return allOf(
        parentNode(kindIs(Kind.EXPRESSION_STATEMENT, MethodInvocationTree.class)),
        methodSelect(anyOf(ExpressionTree.class,
            methodHasAnnotation("javax.annotation.CheckReturnValue"),
            allOf(isDescendantOfMethod("java.lang.String", "*"),
                isDescendantOfMethod("java.math.BigDecimal", "*"),
                isDescendantOfMethod("java.math.BigInteger", "*"))))
    ).matches(methodInvocationTree, state);
  }

  /**
   * My guess is that the best fix for this would be to take the receiver of the method invocation
   * and assign the result to that.
   */
  @Override
  public Description describe(MethodInvocationTree methodInvocationTree, VisitorState state) {
    String retTypeStr = ((JCMethodInvocation) methodInvocationTree).type.toString();
    SuggestedFix fix = new SuggestedFix().prefixWith(methodInvocationTree, retTypeStr);
    return new Description(methodInvocationTree, diagnosticMessage, fix);
  }

  public static class Scanner extends com.google.errorprone.Scanner {
    private ReturnValueIgnored matcher = new ReturnValueIgnored();

    @Override
    public Void visitMethodInvocation(MethodInvocationTree node, VisitorState visitorState) {
      evaluateMatch(node, visitorState, matcher);
      return super.visitMethodInvocation(node, visitorState);
    }
  }
}
