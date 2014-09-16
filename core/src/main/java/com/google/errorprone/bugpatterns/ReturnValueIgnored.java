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

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.util.ASTHelpers;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Type;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author alexeagle@google.com (Alex Eagle)
 */
@BugPattern(name = "ReturnValueIgnored",
    altNames = {"ResultOfMethodCallIgnored"},
    summary = "Return value of this method must be used",
    explanation = "Certain library methods do nothing useful if their return value is ignored. " +
        "For example, String.trim() has no side effects, and you must store the return value " +
        "of String.intern() to access the interned string.  This check encodes a list of " +
        "methods in the JDK whose return value must be used and issues an error if they " +
        "are not.",
    category = JDK, severity = ERROR, maturity = MATURE)
public class ReturnValueIgnored extends AbstractReturnValueIgnored {

  /**
   * A set of types which this checker should examine method calls on.
   */
  // There are also some high-priority return value ignored checks in FindBugs for various
  // threading constructs which do not return the same type as the receiver.
  // This check does not deal with them, since the fix is less straightforward.
  // See a list of the FindBugs checks here:
  // http://code.google.com/searchframe#Fccnll6ERQ0/trunk/findbugs/src/java/edu/umd/cs/findbugs/ba/CheckReturnAnnotationDatabase.java
  private static final Set<String> typesToCheck = new HashSet<>(Arrays.asList(
      "java.lang.String", "java.math.BigInteger", "java.math.BigDecimal"));

  /**
   * Return a matcher for method invocations in which the method being called is on an instance of
   * a type in the typesToCheck set and returns the same type (e.g. String.trim() returns a
   * String).
   */
  @Override
  public Matcher<MethodInvocationTree> specializedMatcher() {
    return methodSelect(Matchers.<ExpressionTree>allOf(
        methodReceiverHasType(typesToCheck),
        methodReturnsSameTypeAsReceiver()));
  }

  /**
   * Matches method invocations that return the same type as the receiver object.
   */
  private static Matcher<ExpressionTree> methodReturnsSameTypeAsReceiver() {
    return new Matcher<ExpressionTree>() {
      @Override
      public boolean matches(ExpressionTree expressionTree, VisitorState state) {
        return state.getTypes().isSameType(ASTHelpers.getReceiverType(expressionTree),
            ASTHelpers.getReturnType(expressionTree));
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
        Type receiverType = ASTHelpers.getReceiverType(expressionTree);
        return typeSet.contains(receiverType.toString());
      }
    };
  }

}
