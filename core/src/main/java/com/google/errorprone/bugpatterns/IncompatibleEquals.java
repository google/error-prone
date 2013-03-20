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
import static com.google.errorprone.BugPattern.MaturityLevel.MATURE;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.methodSelect;
import static com.google.errorprone.matchers.Matchers.staticMethod;

import java.util.List;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.DescribingMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;

/**
 * @author Bill Pugh (bill.pugh@gmail.com)
 */
@BugPattern(name = "IncompatibleEquals",
    summary = "Call to equals() comparing different typesf",
    explanation =
        "The arguments to this equal method of distinct types, and thus should always evaluate to false.",
    category = JDK, severity = ERROR, maturity = MATURE)
public class IncompatibleEquals extends DescribingMatcher<MethodInvocationTree> {

  /**
   * Matches calls to the Guava method Objects.equal() in which the two arguments are
   * the same reference.
   *
   * Example: Objects.equal(foo, foo)
   */
  @SuppressWarnings({"unchecked"})
  private static final Matcher<MethodInvocationTree> guavaMatcher = 
      methodSelect(staticMethod("com.google.common.base.Objects", "equal"));

  /**
   * Matches calls to any instance method called "equals" with exactly one argument in which the
   * receiver is the same reference as the argument.
   *
   * Example: foo.equals(foo)
   *
   * TODO(eaftan): This may match too many things, if people are calling methods "equals" that
   * don't really mean equals.
   */
  @SuppressWarnings("unchecked")
  private static final Matcher<MethodInvocationTree> equalsMatcher = 
      methodSelect(Matchers.instanceMethod(Matchers.<ExpressionTree>anything(), "equals"));

  /**
   * The state of the matcher.  Caches the result of matches() for use in describe().
   */
  private MatchState matchState = MatchState.NONE;

  private enum MatchState {
    NONE,
    OBJECTS_EQUAL,
    EQUALS
  }

  /**
   * Should this matcher check for Objects.equal(foo, foo)?
   */
  private boolean checkGuava = true;

  /**
   * Should this matcher check for foo.equals(foo)?
   */
  private boolean checkEquals = true;

  public IncompatibleEquals() {
  }

  /**
   * Construct a new SelfEquals matcher.
   *
   * @param checkGuava Check for Guava Objects.equal(foo, foo) pattern?
   * @param checkEquals Check for foo.equals(foo) pattern?
   */
  public IncompatibleEquals(boolean checkGuava, boolean checkEquals) {
    if (!checkGuava && !checkEquals) {
      throw new IllegalArgumentException("SelfEquals should check something");
    }
    this.checkGuava = checkGuava;
    this.checkEquals = checkEquals;
  }

  @Override
  public boolean matches(MethodInvocationTree methodInvocationTree, VisitorState state) {
      List<? extends ExpressionTree> args = methodInvocationTree.getArguments();
      
    if (checkGuava && guavaMatcher.matches(methodInvocationTree, state)) {
      matchState = MatchState.OBJECTS_EQUAL;
      return incompatible(args.get(0), args.get(1));
    } else if (checkEquals && equalsMatcher.matches(methodInvocationTree, state)) {
      matchState = MatchState.EQUALS;
      MemberSelectTree memberSelect = (MemberSelectTree)  methodInvocationTree.getMethodSelect();
    return  incompatible(memberSelect.getExpression(), args.get(0));
    } else {
      return false;
    }
  }

  private boolean incompatible(ExpressionTree left,
        ExpressionTree right) {
      Type leftType = ((JCTree.JCExpression) left).type;
      Type rightType = ((JCTree.JCExpression) right).type;
      if (leftType.equals(rightType))
          return false;
      if (leftType instanceof Type.ArrayType && rightType instanceof Type.ArrayType)
          return false;
      return isCoreType(leftType) && isCoreType(rightType);
}
  
  private boolean isCoreType(Type type) {
      if (type instanceof Type.ArrayType)
          return true;
      if (!(type instanceof Type.ClassType))
          return false;
      Type.ClassType cType = (Type.ClassType) type;
      String name = cType.toString();
      return name.startsWith("java.lang") && cType.isFinal();
  }
  
  

@Override
  public Description describe(MethodInvocationTree methodInvocationTree, VisitorState state) {
    if (matchState == MatchState.NONE) {
      throw new IllegalStateException("describe() called without a match");
    }

    SuggestedFix fix = new SuggestedFix().replace(methodInvocationTree, "false");



    return new Description(methodInvocationTree, diagnosticMessage, fix);
  }

  public static class Scanner extends com.google.errorprone.Scanner {
    private final DescribingMatcher<MethodInvocationTree> matcher;

    public Scanner() {
      matcher = new IncompatibleEquals();
    }

    public Scanner(boolean checkGuava, boolean checkEquals) {
      matcher = new IncompatibleEquals(checkGuava, checkEquals);
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree node, VisitorState visitorState) {
      evaluateMatch(node, visitorState, matcher);
      return super.visitMethodInvocation(node, visitorState);
    }
  }
}
