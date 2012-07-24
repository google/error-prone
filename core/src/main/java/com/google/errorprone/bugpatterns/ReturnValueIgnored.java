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

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.DescribingMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.MaturityLevel.EXPERIMENTAL;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.*;

/**
 * @author alexeagle@google.com (Alex Eagle)
 */
@BugPattern(name = "ReturnValueIgnored",
    altNames = {"ResultOfMethodCallIgnored"},
    summary = "Ignored return value of method which has no side-effect",
    explanation = "Method calls that have no side-effect are pointless if you ignore the value returned.",
    category = JDK, severity = ERROR, maturity = EXPERIMENTAL)
public class ReturnValueIgnored extends DescribingMatcher<MethodInvocationTree> {
  @SuppressWarnings("unchecked")
  @Override
  public boolean matches(MethodInvocationTree methodInvocationTree, VisitorState state) {
    //TODO: look for JSR305's javax.annotation.CheckReturnValue Annotation
    return allOf(
        parentNode(kindIs(Kind.EXPRESSION_STATEMENT, MethodInvocationTree.class)),
        methodSelect(anyOf(
            isDescendantOfMethod("java.lang.String", "*"),
            isDescendantOfMethod("java.math.BigDecimal", "*"),
            isDescendantOfMethod("java.math.BigInteger", "*")))
    ).matches(methodInvocationTree, state);
  }

  private Matcher<MethodInvocationTree> parentIsStatement() {
    return new Matcher<MethodInvocationTree>(){
      @Override
      public boolean matches(MethodInvocationTree methodInvocationTree, VisitorState state) {
        Tree parent = state.getPath().getParentPath().getLeaf();
        return parent instanceof StatementTree;
      }
    };
  }


  @Override
  public Description describe(MethodInvocationTree methodInvocationTree, VisitorState state) {
    return new Description(methodInvocationTree, diagnosticMessage, null);
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
