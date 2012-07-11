// Copyright 2012 Google Inc. All Rights Reserved.

package com.google.errorprone.matchers;

import com.google.errorprone.VisitorState;

import com.sun.source.tree.ExpressionTree;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;

/**
 * Matches an instance method expression.
 *
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public class InstanceMethod implements Matcher<ExpressionTree> {

  private final Matcher<ExpressionTree> receiverMatcher;
  private final String methodName;

  public InstanceMethod(Matcher<ExpressionTree> receiverMatcher, String methodName) {
    this.receiverMatcher = receiverMatcher;
    this.methodName = methodName;
  }

  @Override
  public boolean matches(ExpressionTree item, VisitorState state) {
    if (!(item instanceof JCFieldAccess)) {
      return false;
    }
    JCFieldAccess memberSelectTree = (JCFieldAccess) item;
    if (memberSelectTree.sym.isStatic()) {
      return false;
    }
    return memberSelectTree.sym.getQualifiedName().equals(state.getName(methodName))
        && receiverMatcher.matches(memberSelectTree.getExpression(), state);
  }
}
