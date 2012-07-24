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
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.DescribingMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matchers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ClassType;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.MaturityLevel.EXPERIMENTAL;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.*;

/**
 * @author alexeagle@google.com (Alex Eagle)
 */
@BugPattern(name = "CollectionIncompatibleType",
    summary = "Incompatible type as argument to non-generic Java collections method.",
    explanation = "Java Collections API has non-generic methods such as Collection.contains(Object). " +
        "If an argument is given which isn't of a type that may appear in the collection, these " +
        "methods always return false. This commonly happens when the type of a collection is refactored " +
        "and the developer relies on the Java compiler to detect callsites where the collection access " +
        "needs to be updated.",
    category = JDK, maturity = EXPERIMENTAL, severity = ERROR)
public class CollectionIncompatibleType extends DescribingMatcher<MethodInvocationTree> {

  @SuppressWarnings("unchecked")
  @Override
  public boolean matches(MethodInvocationTree methodInvocationTree, VisitorState state) {
    return allOf(
        methodSelect(anyOf(ExpressionTree.class,
            isDescendantOfMethod("java.util.Map", "get(java.lang.Object)"),
            isDescendantOfMethod("java.util.Collection", "contains(java.lang.Object)"),
            isDescendantOfMethod("java.util.Collection", "remove(java.lang.Object)"))),
        argument(0, not(Matchers.<ExpressionTree>isCastableTo(
            getGenericType(methodInvocationTree.getMethodSelect(), 0))))
    ).matches(methodInvocationTree, state);
  }

  // TODO: is ExpressionTree really the thing we're getting a type from?
  private Type getGenericType(ExpressionTree expressionTree, int typeIndex) {
    if (!(expressionTree instanceof JCFieldAccess)) {
      return Type.noType;
    }
    return ((ClassType) ((JCFieldAccess) expressionTree).getExpression().type).typarams_field.get(typeIndex);
  }

  @Override
  public Description describe(MethodInvocationTree methodInvocationTree, VisitorState state) {
    return new Description(methodInvocationTree, diagnosticMessage,
        new SuggestedFix().replace(methodInvocationTree, "false"));
  }

  public static class Scanner extends com.google.errorprone.Scanner {
    private CollectionIncompatibleType matcher = new CollectionIncompatibleType();

    @Override
    public Void visitMethodInvocation(MethodInvocationTree node, VisitorState visitorState) {
      evaluateMatch(node, visitorState, matcher);
      return super.visitMethodInvocation(node, visitorState);
    }
  }
}
