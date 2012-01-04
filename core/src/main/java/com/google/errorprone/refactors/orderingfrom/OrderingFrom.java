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

package com.google.errorprone.refactors.orderingfrom;

import static com.google.errorprone.BugPattern.Category.GUAVA;
import static com.google.errorprone.BugPattern.MaturityLevel.ON_BY_DEFAULT;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.argument;
import static com.google.errorprone.matchers.Matchers.methodSelect;
import static com.google.errorprone.matchers.Matchers.staticMethod;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.NewInstanceAnonymousInnerClass;
import com.google.errorprone.refactors.RefactoringMatcher;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.tree.JCTree.JCNewClass;

/**
 * Checker for a call of the form:
 * <pre>
 * Ordering.from(new Comparator<T>() { ... })
 * </pre>
 * 
 * <p>This can be unwrapped to a new anonymous subclass of Ordering:
 * <pre>
 * new Ordering<T>() { ... }
 * </pre>
 * which is shorter and cleaner (and potentially more efficient).
 * 
 * @author sjnickerson@google.com (Simon Nickerson)
 *
 */
@BugPattern(
    name = "Ordering from",
    category = GUAVA,
    severity = WARNING,
    maturity = ON_BY_DEFAULT,
    summary = "Refactor Ordering.from() to cleaner form",
    explanation =
        "Calls of the form\n" +
        "{{{Ordering.from(new Comparator<T>() { ... })}}}\n" +
        "can be unwrapped to a new anonymous subclass of Ordering\n" +
        "{{{new Ordering<T>() { ... }}}}\n" +
        "which is shorter and cleaner (and potentially more efficient).")
public class OrderingFrom extends RefactoringMatcher<MethodInvocationTree> {
  @Override
  @SuppressWarnings({"unchecked", "varargs"})
  public boolean matches(MethodInvocationTree methodInvocationTree, VisitorState state) {
    return allOf(
        methodSelect(staticMethod(
            "com.google.common.collect.Ordering", "from")),
        argument(0, new NewInstanceAnonymousInnerClass("java.util.Comparator")))
        .matches(methodInvocationTree, state);
  }
  
  @Override
  public Refactor refactor(MethodInvocationTree t,
      VisitorState state) {
    ExpressionTree arg = t.getArguments().get(0);
    JCNewClass invocation = (JCNewClass) arg;
    
//    Position lastBracket = new Position(getPosition(t).end - 1, getPosition(t).end, t);
//    Position pos = new Position(getPosition(t).start, getPosition(invocation).start, t);
//    JCTypeApply identifier = (JCTypeApply)invocation.getIdentifier();
//
    SuggestedFix fix = new SuggestedFix();
//        .replace(getPosition(identifier), "Ordering<" + identifier.getTypeArguments() + ">")
//        .replace(getPosition(t).start, getPosition(invocation).start, "")
//        .replace(getPosition(t).end - 1, getPosition(t).end, "");
//
    return new Refactor(t, "Call to Guava's Ordering.from() taking an anonymous inner "
        + "subclass of Comparator<T>; suggest using new Ordering instead.", fix);
  }
}
