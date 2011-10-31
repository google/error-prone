// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.errorprone.matchers;

import com.google.errorprone.VisitorState;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree.Kind;

/**
 * @author sjnickerson@google.com (Simon Nickerson)
 *
 */
public class ExpressionMethodSelectMatcher implements Matcher<ExpressionTree> {

  private final Matcher<ExpressionTree> methodSelectMatcher;
  
  public ExpressionMethodSelectMatcher(Matcher<ExpressionTree> methodSelectMatcher) {
    this.methodSelectMatcher = methodSelectMatcher;
  }
  
  @Override
  public boolean matches(ExpressionTree t, VisitorState state) {
    if (t.getKind() != Kind.METHOD_INVOCATION) {
      return false;  
    }
    
    MethodInvocationTree methodInvocation = (MethodInvocationTree) t;
    return methodSelectMatcher.matches(methodInvocation.getMethodSelect(), state);
  }

}
