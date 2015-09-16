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
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;

import com.google.common.collect.Iterables;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Type;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Checker for calling Object-accepting methods with types that don't match the type arguments of
 * their container types.  Currently this checker detects problems with the following methods on
 * all their subtypes and subinterfaces:
 * <ul>
 * <li>{@link Collection#contains}
 * <li>{@link Collection#remove}
 * <li>{@link List#indexOf}
 * <li>{@link List#lastIndexOf}
 * <li>{@link Map#get}
 * <li>{@link Map#containsKey}
 * <li>{@link Map#remove}
 * <li>{@link Map#containsValue}
 * </ul>
 */
@BugPattern(
  name = "CollectionIncompatibleType",
  summary = "Incompatible type as argument to Object-accepting Java collections method",
  category = JDK,
  maturity = EXPERIMENTAL,
  severity = WARNING
)
public class CollectionIncompatibleType extends BugChecker implements MethodInvocationTreeMatcher {

  /* TODO(eaftan):
   * 1) Run over the Google codebase to see how robust this check is.
   * 2) Can we construct a suggested fix?  Anything reasonable to do?
   * 3) Add new methods.  The list is in Issue 106.  It might be easier to do it incrementally.
   * 4) Consider whether there is a subset of these that can/should be errors rather than warnings.
   * 5) Bump maturity to MATURE.
   */

  private static final Matcher<ExpressionTree> METHOD_ARG_0_SHOULD_MATCH_TYPE_ARG_0 =
      anyOf(
          instanceMethod()
              .onDescendantOf("java.util.Collection")
              .withSignature("contains(java.lang.Object)"),
          instanceMethod()
              .onDescendantOf("java.util.Collection")
              .withSignature("remove(java.lang.Object)"),
          instanceMethod()
              .onDescendantOf("java.util.List")
              .withSignature("indexOf(java.lang.Object)"),
          instanceMethod()
              .onDescendantOf("java.util.List")
              .withSignature("lastIndexOf(java.lang.Object)"),
          instanceMethod().onDescendantOf("java.util.Map").withSignature("get(java.lang.Object)"),
          instanceMethod()
              .onDescendantOf("java.util.Map")
              .withSignature("containsKey(java.lang.Object)"),
          instanceMethod()
              .onDescendantOf("java.util.Map")
              .withSignature("remove(java.lang.Object)"));

  private static final Matcher<ExpressionTree> METHOD_ARG_0_SHOULD_MATCH_TYPE_ARG_1 =
      anyOf(
          instanceMethod()
              .onDescendantOf("java.util.Map")
              .withSignature("containsValue(java.lang.Object)"));

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {

    int methodArgIndex;
    int typeArgIndex;

    if (METHOD_ARG_0_SHOULD_MATCH_TYPE_ARG_0.matches(tree, state)) {
      methodArgIndex = 0;
      typeArgIndex = 0;
    } else if (METHOD_ARG_0_SHOULD_MATCH_TYPE_ARG_1.matches(tree, state)) {
      methodArgIndex = 0;
      typeArgIndex = 1;
    } else {
      return Description.NO_MATCH;
    }

    com.sun.tools.javac.util.List<Type> tyargs =
        ASTHelpers.getReceiverType(tree).getTypeArguments();
    if (tyargs.size() <= typeArgIndex) {
      // Collection is raw, nothing we can do.
      return Description.NO_MATCH;
    }

    Type typeArg = tyargs.get(typeArgIndex);
    ExpressionTree methodArg = Iterables.get(tree.getArguments(), methodArgIndex);
    Type methodArgType = ASTHelpers.getType(methodArg);
    // TODO(eaftan): Allow cases when the lower bound of the type argument is assignable to
    // the method argument type.
    Type typeArgUpperBound = ASTHelpers.getUpperBound(typeArg, state.getTypes());
    if (state.getTypes().isAssignable(methodArgType, typeArgUpperBound)) {
      return Description.NO_MATCH;
    }

    return buildDescription(tree)
        .setMessage(
            String.format(
                "Argument '%s' should not be passed to this method; its type %s is not compatible "
                    + "with its collection's type argument %s",
                methodArg,
                methodArgType,
                typeArg))
        .build();
  }
}
