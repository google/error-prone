/*
 * Copyright 2014 Google Inc. All Rights Reserved.
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
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.argument;
import static com.google.errorprone.matchers.Matchers.argumentCount;
import static com.google.errorprone.matchers.Matchers.methodSelect;
import static com.google.errorprone.matchers.Matchers.staticMethod;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;

/**
 * @author eaftan@google.com (Eddie Aftandilian)
 */
@BugPattern(name = "ArrayHashCode",
    summary = "hashcode method on array does not hash array contents",
    explanation = "Computing a hashcode for an array is tricky.  Typically you "
        + "want a hashcode that depends on the value of each element in the array, but many of "
        + "the common ways to do this actually return a hashcode based on the _identity_ of the "
        + "array rather than its contents.\n\n"
        + "This check flags attempts to compute a hashcode from an array that do not take the "
        + "contents of the array into account. There are several ways to mess this up:\n"
        + "  * Call the instance .hashCode() method on an array.\n"
        + "  * Call the JDK method java.util.Objects#hashCode() with an argument of array "
        + "type.\n"
        + "  * Call either the JDK method java.util.Objects#hash() or the Guava method "
        + "com.google.common.base.Objects#hashCode() with a single argument of _primitive_ array "
        + "type. Because these are varags methods that take Object..., the primitive array is "
        + "autoboxed into a single-element Object array, and these methods use the identity "
        + "hashcode of the primitive array rather than examining its contents. Note that calling "
        + "these methods on an argument of _Object_ array type actually does the right thing "
        + "because no boxing is needed.\n\n"
        + "Please use java.util.Arrays#hashCode() instead to compute a hash value that depends "
        + "on the contents of the array. If you really intended to compute the identity hash code, "
        + "consider using java.lang.System#identityHashCode() instead for clarity.",
    category = JDK, severity = ERROR, maturity = MATURE)
public class ArrayHashCode extends BugChecker implements MethodInvocationTreeMatcher {

  /**
   * Matches calls to varargs hashcode methods com.google.common.base.Objects#hashCode() and
   * java.util.Objects#hash() with a single argument of type primitive array.  The primitive array
   * will be autoboxed into an Object array with a single element, and the hashcode method will
   * return the identity hashcode from the newly allocated Object array.
   */
  @SuppressWarnings({"unchecked"})
  private static final Matcher<MethodInvocationTree> varargsHashCodeMethodMatcher = allOf(
      Matchers.anyOf(
        methodSelect(staticMethod("com.google.common.base.Objects", "hashCode")),
        methodSelect(staticMethod("java.util.Objects", "hash"))),
      argumentCount(1),
      argument(0, Matchers.<ExpressionTree>isPrimitiveArrayType()));

  /**
   * Matches calls to the JDK7 method java.util.Objects#hashCode() with an argument of array
   * type. This method is not varargs, so we don't need to check the number of arguments.
   */
  @SuppressWarnings({"unchecked"})
  private static final Matcher<MethodInvocationTree> jdk7HashCodeMethodMatcher = allOf(
      methodSelect(staticMethod("java.util.Objects", "hashCode")),
      argument(0, Matchers.<ExpressionTree>isArrayType()));

  /**
   * Matches calls to the hashCode instance method on an array.
   */
  private static final Matcher<MethodInvocationTree> instanceHashCodeMethodMatcher = allOf(
      methodSelect(Matchers.instanceMethod(Matchers.<ExpressionTree>isArrayType(), "hashCode")));

  /**
   * Substitutes Arrays.hashCode() for any of the incorrect hashcode invocation patterns above.
   */
  @Override
  public Description matchMethodInvocation(MethodInvocationTree t, VisitorState state) {
    String arrayArg = null;
    if (varargsHashCodeMethodMatcher.matches(t, state)
        || jdk7HashCodeMethodMatcher.matches(t, state)) {
      arrayArg = t.getArguments().get(0).toString();
    } else if (instanceHashCodeMethodMatcher.matches(t, state)) {
      arrayArg = ((JCFieldAccess) t.getMethodSelect()).getExpression().toString();
    }

    if (arrayArg == null) {
      return NO_MATCH;
    }

    Fix fix = SuggestedFix.builder()
        .replace(t, "Arrays.hashCode(" + arrayArg + ")")
        .addImport("java.util.Arrays")
        .build();
    return describeMatch(t, fix);
  }
}