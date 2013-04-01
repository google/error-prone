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
import static com.google.errorprone.BugPattern.MaturityLevel.EXPERIMENTAL;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.methodSelect;
import static com.google.errorprone.matchers.Matchers.staticMethod;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.DescribingMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.util.ASTHelpers;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;

import java.util.List;

/**
 * @author Bill Pugh (bill.pugh@gmail.com)
 */
@BugPattern(name = "IncompatibleEquals",
summary = "Comparing %s and %s for equality, which are incompatible and should never be equal",
explanation = "The arguments to this equal method of distinct types, and thus should always " +
    "evaluate to false.",
    category = JDK, severity = ERROR, maturity = EXPERIMENTAL)
public class IncompatibleEquals extends DescribingMatcher<MethodInvocationTree> {


  @SuppressWarnings({ "unchecked" })
  private static final Matcher<MethodInvocationTree> guavaMatcher = methodSelect(staticMethod(
      "com.google.common.base.Objects", "equal"));

  @SuppressWarnings({ "unchecked" })
  private static final Matcher<MethodInvocationTree> objectsMatcher = methodSelect(staticMethod(
      "java.util.Objects", "equals"));

  @SuppressWarnings("unchecked")
  private static final Matcher<MethodInvocationTree> equalsMatcher = methodSelect(Matchers
      .instanceMethod(Matchers.<ExpressionTree> anything(), "equals"));

  /**
   * The state of the matcher. Caches the result of matches() for use in
   * describe().
   */
  private MatchState matchState = MatchState.NONE;

  private enum MatchState {
    NONE, GUAVA_EQUAL, OBJECTS_EQUALS, EQUALS
  }

  Type leftType, rightType;

  public IncompatibleEquals() {
  }


  @Override
  public boolean matches(MethodInvocationTree methodInvocationTree,
      VisitorState state) {
    List<? extends ExpressionTree> args = methodInvocationTree
        .getArguments();

    if ( guavaMatcher.matches(methodInvocationTree, state)) {
      matchState = MatchState.GUAVA_EQUAL;
      return incompatible(args.get(0), args.get(1), state);
    } else if ( objectsMatcher.matches(methodInvocationTree, state)) {
      matchState = MatchState.OBJECTS_EQUALS;
      return incompatible(args.get(0), args.get(1), state);
    } else if (equalsMatcher.matches(methodInvocationTree, state)) {
      matchState = MatchState.EQUALS;

      ExpressionTree methodSelect = methodInvocationTree
          .getMethodSelect();

      Type t;
      if (methodSelect instanceof  MemberSelectTree) {
        ExpressionTree invokedOn =  ((MemberSelectTree)methodSelect).getExpression();
        t =  ((JCTree.JCExpression) invokedOn).type;
      }
      else t  = ASTHelpers.getReceiverType( methodSelect);
      return incompatible(t, ((JCTree.JCExpression)  args.get(0)).type,
          state);
    } else {
      return false;
    }
  }

  Type boxedTypeOrType(Type t,  VisitorState state) {
    if (!t.isPrimitive()) return t;
    ClassSymbol boxedClass = state.getTypes().boxedClass(t);
    return boxedClass.type;
  }


  private boolean incompatible(Type left, Type right, VisitorState state) {
    leftType = boxedTypeOrType(left, state);
    rightType = boxedTypeOrType(right, state);
    if (leftType.equals(rightType))
      return false;
    if (leftType instanceof Type.ArrayType
        && rightType instanceof Type.ArrayType)
      return false;
    if (leftType instanceof Type.ArrayType
        && !rightType.isInterface())
      return true;
    if (rightType instanceof Type.ArrayType
        && !leftType.isInterface())
      return true;
    //        if (leftType.isInterface() && !rightType.isFinal())
      //            return false;
    //
    //        if (rightType.isInterface() && !leftType.isFinal())
      //            return false;
    if (!state.getTypes().disjointType(leftType, rightType))
      return false;
    leftType = state.getTypes().erasure(leftType);
    rightType = state.getTypes().erasure(rightType);


    if (state.getTypes().isCastable(leftType, rightType))
      return false;
    if (state.getTypes().isCastable(rightType, leftType))
      return false;
    Tree parent = state.getPath().getParentPath().getLeaf();
    if (parent instanceof MethodInvocationTree) {
      MethodInvocationTree p = (MethodInvocationTree) parent;
      Symbol sym = ASTHelpers.getSymbol(p.getMethodSelect());
      if (sym.name.contentEquals("assertFalse"))
        return false;

    }
    if (isCoreType(leftType) || isCoreType(rightType))
      return true;
    String lCollection = collectionType(leftType);
    String rCollection = collectionType(rightType);
    if (lCollection != null && rCollection != null && !lCollection.equals(rCollection))
      return true;

    return false;
  }

  private boolean incompatible(ExpressionTree left, ExpressionTree right,
      VisitorState state) {
    return incompatible(((JCTree.JCExpression) left).type,
        ((JCTree.JCExpression) right).type, state);

  }

  private String collectionType(Type t) {
    String name = t.toString();
    if (name.endsWith("Map"))
      return "Map";
    if (name.endsWith("List"))
      return "List";
    if (name.endsWith("Set"))
      return "Set";
    return null;
  }


  private boolean isCoreType(Type type) {
    if (type instanceof Type.ArrayType)
      return true;
    if (!(type instanceof Type.ClassType))
      return false;
    if (type.isInterface())
      return false;
    Type.ClassType cType = (Type.ClassType) type;
    String name = cType.toString();
    return name.startsWith("java.lang");
  }

  @Override
  public Description describe(MethodInvocationTree methodInvocationTree,
      VisitorState state) {
    if (matchState == MatchState.NONE) {
      throw new IllegalStateException("describe() called without a match");
    }

    SuggestedFix fix = new SuggestedFix().replace(methodInvocationTree,
        String.format("false /* equals comparison of %s and %s */", leftType, rightType));

    return new Description(methodInvocationTree,
        getCustomDiagnosticMessage(leftType, rightType), fix);
  }

  public static class Scanner extends com.google.errorprone.Scanner {
    private final DescribingMatcher<MethodInvocationTree> matcher;

    public Scanner() {
      matcher = new IncompatibleEquals();
    }


    @Override
    public Void visitMethodInvocation(MethodInvocationTree node,
        VisitorState visitorState) {
      evaluateMatch(node, visitorState, matcher);
      return super.visitMethodInvocation(node, visitorState);
    }
  }
}
