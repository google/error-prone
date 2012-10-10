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

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.MaturityLevel.ON_BY_DEFAULT;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.kindIs;
import static com.google.errorprone.matchers.Matchers.methodHasAnnotation;
import static com.google.errorprone.matchers.Matchers.methodSelect;
import static com.google.errorprone.matchers.Matchers.parentNode;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.DescribingMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.MethodType;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author alexeagle@google.com (Alex Eagle)
 */
@BugPattern(name = "ReturnValueIgnored",
    altNames = {"ResultOfMethodCallIgnored"},
    summary = "Ignored return value of method which has no side-effect",
    explanation = "Method calls that have no side-effect are pointless if you ignore the value "
        + "returned. Also, this error is triggered if the return value of a method that has been "
        + "annotated with checkReturnValue is ignored.",
    category = JDK, severity = ERROR, maturity = ON_BY_DEFAULT)
public class ReturnValueIgnored extends DescribingMatcher<MethodInvocationTree> {

  /**
   * A set of types which this checker should examine method calls on.
   */
  //TODO(eaftan): Flesh out this list. Get immutable types from IntelliJ source, immutable Guava
  // types, FindBugs, list on StackOverflow
  private static final Set<String> typesToCheck = new HashSet<String>(Arrays.asList(
      "java.lang.String", "java.math.BigInteger", "java.math.BigDecimal"));

  /**
   * Matches if the method being called is a statement (rather than an expression), and the method
   *    1) Has been annotated with @CheckReturnValue, or
   *    2) Is being called on an instance of the specified types, and the method returns the same
   *       type (e.g. String.trim()).
   */
  @SuppressWarnings("unchecked")
  @Override
  public boolean matches(MethodInvocationTree methodInvocationTree, VisitorState state) {
    return allOf(
        parentNode(kindIs(Kind.EXPRESSION_STATEMENT, MethodInvocationTree.class)),
        methodSelect(anyOf(ExpressionTree.class,
            methodHasAnnotation("javax.annotation.CheckReturnValue"),
            allOf(methodReceiverHasType(typesToCheck), methodReturnsSameTypeAsReceiver())))
    ).matches(methodInvocationTree, state);
  }

  /**
   * Fixes the error by assigning the result of the call to the receiver reference, or deleting
   * the method call.
   */
  @Override
  public Description describe(MethodInvocationTree methodInvocationTree, VisitorState state) {
    // Find the root of the field access chain, i.e. a.intern().trim() ==> a.
    ExpressionTree identifierExpr = getRootIdentifier(methodInvocationTree);
    String identifierStr = null;
    Type identifierType = null;
    if (identifierExpr != null) {
      identifierStr = identifierExpr.toString();
      if (identifierExpr instanceof JCIdent) {
        identifierType = ((JCIdent) identifierExpr).sym.type;
      } else if (identifierExpr instanceof JCFieldAccess) {
        identifierType = ((JCFieldAccess) identifierExpr).sym.type;
      } else {
        throw new IllegalStateException("Expected a JCIdent or a JCFieldAccess");
      }
    }

    Type returnType = getReturnType(((JCMethodInvocation) methodInvocationTree).getMethodSelect());

    SuggestedFix fix;
    if (identifierStr != null && !"this".equals(identifierStr) && returnType != null &&
        state.getTypes().isSameType(returnType, identifierType)) {
      // Fix by assigning the assigning the result of the call to the root receiver reference.
      fix = new SuggestedFix().prefixWith(methodInvocationTree, identifierStr + " = ");
    } else {
      // Unclear what the programmer intended.  Should be safe to delete without changing behavior
      // since we expect the method not to have side effects .
      Tree parent = state.getPath().getParentPath().getLeaf();
      fix = new SuggestedFix().delete(parent);
    }
    return new Description(methodInvocationTree, diagnosticMessage, fix);
  }

  public static class Scanner extends com.google.errorprone.Scanner {
    private ReturnValueIgnored matcher = new ReturnValueIgnored();

    @Override
    public Void visitMethodInvocation(MethodInvocationTree node, VisitorState visitorState) {
      evaluateMatch(node, visitorState, matcher);
      return super.visitMethodInvocation(node, visitorState);
    }
  }

  /**
   * Matches method invocations that return the same type as the receiver object.
   */
  private static Matcher<ExpressionTree> methodReturnsSameTypeAsReceiver() {
    return new Matcher<ExpressionTree>() {
      @Override
      public boolean matches(ExpressionTree expressionTree, VisitorState state) {
        Type receiverType = getReceiverType(expressionTree);
        Type returnType = getReturnType(expressionTree);
        if (receiverType == null || returnType == null) {
          return false;
        }
        return state.getTypes().isSameType(getReceiverType(expressionTree),
            getReturnType(expressionTree));
      }
    };
  }

  /**
   * Matches method calls whose receiver objects are of a type included in the set.
   */
  private static Matcher<ExpressionTree> methodReceiverHasType(final Set<String> typeSet) {
    return new Matcher<ExpressionTree>() {
      @Override
      public boolean matches(ExpressionTree expressionTree, VisitorState state) {
        Type receiverType = getReceiverType(expressionTree);
        if (receiverType == null) {
          return false;
        }
        return typeSet.contains(receiverType.toString());
      }
    };
  }

  /**
   * Returns the return type of a method call expression.
   * Precondition: the expressionTree corresponds to a method call
   */
  private static Type getReturnType(ExpressionTree expressionTree) {
    if (expressionTree instanceof JCFieldAccess) {
      JCFieldAccess methodCall = (JCFieldAccess) expressionTree;
      return ((MethodType) methodCall.type).getReturnType();
    } else if (expressionTree instanceof JCIdent) {
      JCIdent methodCall = (JCIdent) expressionTree;
      return ((MethodType) methodCall.type).getReturnType();
    }
    return null;
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
  private static Type getReceiverType(ExpressionTree expressionTree) {
    if (expressionTree instanceof JCFieldAccess) {
      JCFieldAccess methodSelectFieldAccess = (JCFieldAccess) expressionTree;
      return ((MethodSymbol) methodSelectFieldAccess.sym).owner.type;
    } else if (expressionTree instanceof JCIdent) {
      JCIdent methodCall = (JCIdent) expressionTree;
      return ((MethodSymbol) methodCall.sym).owner.type;
    }
    return null;
  }

  /**
   * Find the "root" identifier of a chain of field accesses.  If there is no root (i.e, a bare
   * method call), return null.
   *
   * Examples:
   *    a.trim().intern() ==> a
   *    a.b.trim().intern() ==> a.b
   *    this.intValue.foo() ==> this.intValue
   *    this.foo() ==> this
   *    intern() ==> null
   */
  private ExpressionTree getRootIdentifier(MethodInvocationTree methodInvocationTree) {
    if (!(methodInvocationTree instanceof JCMethodInvocation)) {
      throw new IllegalArgumentException("Expected type to be JCMethodInvocation");
    }

    // Check for bare method call, e.g. intern().
    if (((JCMethodInvocation) methodInvocationTree).getMethodSelect() instanceof JCIdent) {
      return null;
    }

    ExpressionTree expr = methodInvocationTree;
    while (expr instanceof JCMethodInvocation) {
      expr = ((JCMethodInvocation) expr).getMethodSelect();
      if (expr instanceof JCFieldAccess) {
        expr = ((JCFieldAccess) expr).getExpression();
      }
    }
    return expr;
  }
}
