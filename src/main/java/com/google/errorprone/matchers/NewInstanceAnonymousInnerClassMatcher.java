// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.errorprone.matchers;

import com.google.errorprone.VisitorState;

import com.sun.source.tree.ExpressionTree;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree.JCNewClass;

/**
 * Matcher for a new instance of an anonymous inner class.
 * 
 * @author sjnickerson@google.com (Simon Nickerson)
 */
public class NewInstanceAnonymousInnerClassMatcher implements Matcher<ExpressionTree> {
  
  /**
   * Fully-qualified name of the class or interface we are subclassing for our
   * anonymous inner class, e.g. a new Comparator
   */
  private final String superClass;
  
  public NewInstanceAnonymousInnerClassMatcher(String superClass) {
    this.superClass = superClass;
  }
  
  @Override
  public boolean matches(ExpressionTree t, VisitorState state) {
    if (t instanceof JCNewClass) {
      JCNewClass invocation = (JCNewClass)t;
      Type type = invocation.getIdentifier().type;
      if (type.tsym.getQualifiedName().toString().equals(superClass)) {
        return true;
      }
    }
    return false;
  }
}
