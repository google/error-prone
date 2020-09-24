/*
 * Copyright 2014 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.argument;
import static com.google.errorprone.matchers.Matchers.hasArguments;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.matchers.Matchers.staticMethod;
import static com.google.errorprone.predicates.TypePredicates.isArray;

import com.google.common.base.Preconditions;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.ChildMultiMatcher.MatchType;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;

/** @author eaftan@google.com (Eddie Aftandilian) */
@BugPattern(
    name = "ArrayHashCode",
    summary = "hashcode method on array does not hash array contents",
    severity = ERROR)
public class ArrayHashCode extends BugChecker implements MethodInvocationTreeMatcher {

  /**
   * Matches calls to varargs hashcode methods {@link com.google.common.base.Objects#hashCode} and
   * {@link java.util.Objects#hash} in which at least one argument is of array type.
   */
  private static final Matcher<MethodInvocationTree> varargsHashCodeMethodMatcher =
      allOf(
          Matchers.anyOf(
              staticMethod().onClass("com.google.common.base.Objects").named("hashCode"),
              staticMethod().onClass("java.util.Objects").named("hash")),
          hasArguments(MatchType.AT_LEAST_ONE, Matchers.<ExpressionTree>isArrayType()));

  /**
   * Matches calls to the JDK7 method {@link java.util.Objects#hashCode} with an argument of array
   * type. This method is not varargs, so we don't need to check the number of arguments.
   */
  private static final Matcher<MethodInvocationTree> jdk7HashCodeMethodMatcher =
      allOf(
          staticMethod().onClass("java.util.Objects").named("hashCode"),
          argument(0, Matchers.<ExpressionTree>isArrayType()));

  /** Matches calls to the hashCode instance method on an array. */
  private static final Matcher<ExpressionTree> instanceHashCodeMethodMatcher =
      instanceMethod().onClass(isArray()).named("hashCode");

  /**
   * Wraps identity hashcode computations in calls to {@link java.util.Arrays#hashCode} if the array
   * is single dimensional or {@link java.util.Arrays#deepHashCode} if the array is
   * multidimensional.
   *
   * <p>If there is only one argument to the hashcode method or the instance hashcode method is
   * used, replaces the whole method invocation. If there are multiple arguments, wraps any that are
   * of array type with the appropriate {@link java.util.Arrays} hashcode method.
   */
  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    SuggestedFix.Builder fix = null;
    Types types = state.getTypes();
    if (jdk7HashCodeMethodMatcher.matches(tree, state)) {
      // java.util.Objects#hashCode takes a single argument, so rewrite the whole method call
      // to use Arrays.hashCode/deepHashCode instead.
      fix =
          SuggestedFix.builder()
              .replace(tree, rewriteArrayArgument(tree.getArguments().get(0), state));
    } else if (instanceHashCodeMethodMatcher.matches(tree, state)) {
      // Rewrite call to instance hashCode method to use Arrays.hashCode/deepHashCode instead.
      fix =
          SuggestedFix.builder()
              .replace(
                  tree,
                  rewriteArrayArgument(
                      ((JCFieldAccess) tree.getMethodSelect()).getExpression(), state));
    } else if (varargsHashCodeMethodMatcher.matches(tree, state)) {
      // Varargs hash code methods, java.util.Objects#hash and
      // com.google.common.base.Objects#hashCode
      if (tree.getArguments().size() == 1) {
        // If only one argument, type must be either primitive array or multidimensional array.
        // Types like Object[], String[], etc. are not an error because they don't get boxed
        // in this single-argument varargs call.
        ExpressionTree arg = tree.getArguments().get(0);
        Type elemType = types.elemtype(ASTHelpers.getType(arg));
        if (elemType.isPrimitive() || types.isArray(elemType)) {
          fix = SuggestedFix.builder().replace(tree, rewriteArrayArgument(arg, state));
        }
      } else {
        // If more than one argument, wrap each argument in a call to Arrays#hashCode/deepHashCode.
        fix = SuggestedFix.builder();
        for (ExpressionTree arg : tree.getArguments()) {
          if (types.isArray(ASTHelpers.getType(arg))) {
            fix.replace(arg, rewriteArrayArgument(arg, state));
          }
        }
      }
    }

    if (fix != null) {
      fix.addImport("java.util.Arrays");
      return describeMatch(tree, fix.build());
    }

    return Description.NO_MATCH;
  }

  /**
   * Given an {@link ExpressionTree} that represents an argument of array type, rewrites it to wrap
   * it in a call to either {@link java.util.Arrays#hashCode} if it is single dimensional, or {@link
   * java.util.Arrays#deepHashCode} if it is multidimensional.
   */
  private static String rewriteArrayArgument(ExpressionTree arg, VisitorState state) {
    Types types = state.getTypes();
    Type argType = ASTHelpers.getType(arg);
    Preconditions.checkState(types.isArray(argType), "arg must be of array type");
    if (types.isArray(types.elemtype(argType))) {
      return "Arrays.deepHashCode(" + state.getSourceForNode(arg) + ")";
    } else {
      return "Arrays.hashCode(" + state.getSourceForNode(arg) + ")";
    }
  }
}
