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
public class SelfAssignmentMatcher implements Matcher<AssignmentTree> {
  
  @Override
  public boolean matches(AssignmentTree t, VisitorState state) {
    return sameFieldAccess(t.getVariable(), t.getExpression());
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
    // throw up our hands if we're not comparing identifiers and/or field accesses
    if ((t1.getKind() != Kind.IDENTIFIER && t1.getKind() != Kind.MEMBER_SELECT) ||
        (t2.getKind() != Kind.IDENTIFIER && t2.getKind() != Kind.MEMBER_SELECT)) {
      return false;
    }
    
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
      // TODO(eaftan): really shouldn't be relying on .toString()
      return expr.toString().equals("this") && s1.equals(s2);
    }
  }

}
