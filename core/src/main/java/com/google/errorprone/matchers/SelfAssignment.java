// Copyright 2012 Google Inc. All Rights Reserved.

package com.google.errorprone.matchers;

import com.google.errorprone.VisitorState;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCIdent;

/**
 * Matches if the left-hand side and right-hand sides of an assignment
 * refer to the same variable or same field of the same object.
 * 
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public class SelfAssignment implements Matcher<AssignmentTree> {
  
  @Override
  public boolean matches(AssignmentTree t, VisitorState state) {
    ExpressionTree lhs = t.getVariable();
    ExpressionTree rhs = t.getExpression();
    
    /* We only consider cases where the lhs and rhs are identifiers or field accesses.
     * 
     * Samples:
     * a = a
     * foo.a = a
     * foo.a = bar.a
     * foo.bar.a = bar.a
     * foo.bar.a = foo.bar.a
     * super.a = a  // a here could also be from the superclass
     */
    assert(lhs.getKind() == Kind.IDENTIFIER || lhs.getKind() == Kind.MEMBER_SELECT);
    assert(rhs.getKind() == Kind.IDENTIFIER || rhs.getKind() == Kind.MEMBER_SELECT);
    
    return sameFieldAccess(lhs, rhs);
  }
  
  /**
   * Returns true if the two member select trees refer to the same field access.
   * Note that returning false doesn't necessarily mean the member select trees do 
   * *not* refer to the same field.  For example, consider this code fragment:
   * 
   * a.foo = b.foo
   * 
   * If a and b point to the same variable, then this is a self assignment. But
   * to determine statically whether a and b point to the same variable is known
   * to be undecidable (Ramalingam 94).  
   */
  private boolean sameFieldAccess(ExpressionTree t1, ExpressionTree t2) {
    if (t1.getKind() == Kind.IDENTIFIER && t2.getKind() == Kind.IDENTIFIER) {
      return ((JCIdent)t1).sym.equals(((JCIdent)t2).sym);
    } else if (t1.getKind() == Kind.MEMBER_SELECT && t2.getKind() == Kind.MEMBER_SELECT) {
      if (((JCFieldAccess)t1).sym.equals(((JCFieldAccess)t2).sym)) {
        return sameFieldAccess(((JCFieldAccess)t1).selected, 
            ((JCFieldAccess)t2).selected);
      } 
      return false;
    } else {
      Symbol s1 = null;
      Symbol s2 = null;
      ExpressionTree expr = null;
      if (t1.getKind() == Kind.IDENTIFIER) {
        s1 = ((JCIdent)t1).sym;
        s2 = ((JCFieldAccess)t2).sym;
        expr = ((JCFieldAccess)t2).selected;
      } else {
        s1 = ((JCIdent)t2).sym;
        s2 = ((JCFieldAccess)t1).sym;
        expr = ((JCFieldAccess)t1).selected;
      }
      // TODO(eaftan): what about foo.a = a? where foo is of same type as a.
      return expr.toString().equals("this") && s1.equals(s2);
    }
  }

}
