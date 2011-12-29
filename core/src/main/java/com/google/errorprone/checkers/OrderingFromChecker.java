// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.errorprone.checkers;

import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.argument;
import static com.google.errorprone.matchers.Matchers.methodSelect;
import static com.google.errorprone.matchers.Matchers.staticMethod;

import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.NewInstanceAnonymousInnerClassMatcher;

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
public class OrderingFromChecker extends DescribingMatcher<MethodInvocationTree> {
  @Override
  @SuppressWarnings({"unchecked", "varargs"})
  public boolean matches(MethodInvocationTree methodInvocationTree, VisitorState state) {
    return allOf(
        methodSelect(staticMethod(
            "com.google.common.collect.Ordering", "from")),
        argument(0, new NewInstanceAnonymousInnerClassMatcher("java.util.Comparator")))
        .matches(methodInvocationTree, state);
  }
  
  @Override
  public MatchDescription describe(MethodInvocationTree t,
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
    return new MatchDescription(t, "Call to Guava's Ordering.from() taking an anonymous inner "
        + "subclass of Comparator<T>; suggest using new Ordering instead.", fix);
  }
}
