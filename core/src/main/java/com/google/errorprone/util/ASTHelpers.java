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

package com.google.errorprone.util;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.MethodType;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;

/**
 * This class contains utility methods to work with the javac AST.
 */
public class ASTHelpers {

  /**
   * Determines whether two expressions refer to the same variable. Note that returning false
   * doesn't necessarily mean the expressions do *not* refer to the same field. We don't attempt
   * to do any complex analysis here, just catch the obvious cases.
   */
  public static boolean sameVariable(ExpressionTree expr1, ExpressionTree expr2) {
    // Throw up our hands if we're not comparing identifiers and/or field accesses.
    if ((expr1.getKind() != Kind.IDENTIFIER && expr1.getKind() != Kind.MEMBER_SELECT) ||
        (expr2.getKind() != Kind.IDENTIFIER && expr2.getKind() != Kind.MEMBER_SELECT)) {
      return false;
    }

    Symbol sym1 = getSymbol(expr1);
    Symbol sym2 = getSymbol(expr2);
    if (sym1 == null) {
      throw new IllegalStateException("Couldn't get symbol for " + expr1);
    } else if (sym2 == null) {
      throw new IllegalStateException("Couldn't get symbol for " + expr2);
    }

    if (expr1.getKind() == Kind.IDENTIFIER && expr2.getKind() == Kind.IDENTIFIER) {
      // foo == foo?
      return sym1.equals(sym2);
    } else if (expr1.getKind() == Kind.MEMBER_SELECT && expr2.getKind() == Kind.MEMBER_SELECT) {
      // foo.baz.bar == foo.baz.bar?
      return sym1.equals(sym2) &&
          sameVariable(((JCFieldAccess) expr1).selected,((JCFieldAccess) expr2).selected);
    } else {
      // this.foo == foo?
      ExpressionTree selected = null;
      if (expr1.getKind() == Kind.IDENTIFIER) {
        selected = ((JCFieldAccess) expr2).selected;
      } else {
        selected = ((JCFieldAccess) expr1).selected;
      }
      // TODO(eaftan): really shouldn't be relying on .toString()
      return selected.toString().equals("this") && sym1.equals(sym2);
    }
  }

  /**
   * Gets the symbol for a tree. Returns null if this tree does not have a symbol because it is
   * of the wrong type.
   */
  // TODO(eaftan): refactor other code that accesses symbols to use this method
  public static Symbol getSymbol(Tree tree) {
    switch (tree.getKind()) {
      case CLASS:
        return ((JCClassDecl) tree).sym;
      case METHOD:
        return ((JCMethodDecl) tree).sym;
      case VARIABLE:
        return ((JCVariableDecl) tree).sym;
      case MEMBER_SELECT:
        return ((JCFieldAccess) tree).sym;
      case IDENTIFIER:
        return ((JCIdent) tree).sym;
      default:
        return null;
    }
  }

  /**
   * Find the "root" assignable identifier of a chain of field accesses.  If there is no root
   * (i.e, a bare method call or a static method call), return null.
   *
   * Examples:
   *    a.trim().intern() ==> a
   *    a.b.trim().intern() ==> a.b
   *    this.intValue.foo() ==> this.intValue
   *    this.foo() ==> this
   *    intern() ==> null
   *    String.format() ==> null
   *    java.lang.String.format() ==> null
   */
  public static ExpressionTree getRootIdentifier(MethodInvocationTree methodInvocationTree) {
    if (!(methodInvocationTree instanceof JCMethodInvocation)) {
      throw new IllegalArgumentException("Expected type to be JCMethodInvocation");
    }

    // Check for bare method call, e.g. intern().
    if (((JCMethodInvocation) methodInvocationTree).getMethodSelect() instanceof JCIdent) {
      return null;
    }

    // Unwrap the field accesses until you get to an identifier.
    ExpressionTree expr = methodInvocationTree;
    while (expr instanceof JCMethodInvocation) {
      expr = ((JCMethodInvocation) expr).getMethodSelect();
      if (expr instanceof JCFieldAccess) {
        expr = ((JCFieldAccess) expr).getExpression();
      }
    }

    // Find the symbol for the identifier.
    Symbol sym = null;
    if (expr instanceof JCIdent) {
      sym = ((JCIdent) expr).sym;
    } else if (expr instanceof JCFieldAccess) {
      sym = ((JCFieldAccess) expr).sym;
    }

    // We only want assignable identifiers.
    if (sym != null && sym instanceof VarSymbol) {
      return expr;
    }
    return null;
  }

  /**
   * Gives the return type of an ExpressionTree that represents a method select.
   *
   * TODO(eaftan): Are there other places this could be used?
   */
  public static Type getReturnType(ExpressionTree expressionTree) {
    if (expressionTree instanceof JCFieldAccess) {
      JCFieldAccess methodCall = (JCFieldAccess) expressionTree;
      return ((MethodType) methodCall.type).getReturnType();
    } else if (expressionTree instanceof JCIdent) {
      JCIdent methodCall = (JCIdent) expressionTree;
      return ((MethodType) methodCall.type).getReturnType();
    }
    throw new IllegalArgumentException("Expected a JCFieldAccess or JCIdent");
  }

  /**
   * Returns the type of a receiver of a method call expression.
   * Precondition: the expressionTree corresponds to a method call.
   *
   * Examples:
   *    a.b.foo() ==> type of a.b
   *    a.bar().foo() ==> type of a.bar()
   *    this.foo() ==> type of this
   */
  public static Type getReceiverType(ExpressionTree expressionTree) {
    if (expressionTree instanceof JCFieldAccess) {
      JCFieldAccess methodSelectFieldAccess = (JCFieldAccess) expressionTree;
      return ((MethodSymbol) methodSelectFieldAccess.sym).owner.type;
    } else if (expressionTree instanceof JCIdent) {
      JCIdent methodCall = (JCIdent) expressionTree;
      return ((MethodSymbol) methodCall.sym).owner.type;
    }
    return null;
  }
}
