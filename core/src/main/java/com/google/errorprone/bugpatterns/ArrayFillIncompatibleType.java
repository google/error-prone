/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.staticMethod;

import com.google.common.collect.Iterables;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.google.errorprone.util.Signatures;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.code.Type;

/**
 * Checks when Arrays.fill(Object[], Object) is called with object types that are guaranteed to
 * result in an ArrayStoreException.
 */
@BugPattern(
  name = "ArrayFillIncompatibleType",
  summary = "Arrays.fill(Object[], Object) called with incompatible types.",
  category = JDK,
  severity = ERROR
)
public class ArrayFillIncompatibleType extends BugChecker implements MethodInvocationTreeMatcher {
  private static final Matcher<ExpressionTree> ARRAY_FILL_MATCHER =
      anyOf(
          staticMethod()
              .onClass("java.util.Arrays")
              .withSignature("fill(java.lang.Object[],java.lang.Object)"),
          staticMethod()
              .onClass("java.util.Arrays")
              .withSignature("fill(java.lang.Object[],int,int,java.lang.Object)"));

  @Override
  public Description matchMethodInvocation(
      MethodInvocationTree invocationTree, final VisitorState state) {
    if (!ARRAY_FILL_MATCHER.matches(invocationTree, state)) {
      return Description.NO_MATCH;
    }

    Type arrayComponentType =
        state.getTypes().elemtype(ASTHelpers.getType(invocationTree.getArguments().get(0)));
    Tree fillingArgument = Iterables.getLast(invocationTree.getArguments());
    Type fillingObjectType = ASTHelpers.getType(fillingArgument);

    // You can put an Integer or an int into a Number[], but you can't put a Number into an
    // Integer[].
    // (Note that you can assign Integer[] to Number[] and then try to put the Number into it, but
    // that's a hole provided by array covariance we can't plug here)
    if (isValidArrayFill(state, arrayComponentType, fillingObjectType)) {
      return Description.NO_MATCH;
    }

    // Due to some funky behavior, javac doesn't appear to fully expand the type of a ternary
    // when passed into an "Object" context. Let's explore both sides
    if (fillingArgument.getKind() == Kind.CONDITIONAL_EXPRESSION) {
      ConditionalExpressionTree cet = (ConditionalExpressionTree) fillingArgument;
      Type trueExpressionType = ASTHelpers.getType(cet.getTrueExpression());
      if (!isValidArrayFill(state, arrayComponentType, trueExpressionType)) {
        return reportMismatch(invocationTree, arrayComponentType, trueExpressionType);
      }

      Type falseExpressionType = ASTHelpers.getType(cet.getFalseExpression());
      if (!isValidArrayFill(state, arrayComponentType, falseExpressionType)) {
        return reportMismatch(invocationTree, arrayComponentType, falseExpressionType);
      }

      // Looks like we were able to find a ternary that would actually work
      return Description.NO_MATCH;
    }

    return reportMismatch(invocationTree, arrayComponentType, fillingObjectType);
  }

  private Description reportMismatch(
      MethodInvocationTree invocationTree, Type arrayComponentType, Type fillingObjectType) {
    return buildDescription(invocationTree)
        .setMessage(getMessage(fillingObjectType, arrayComponentType))
        .build();
  }

  private boolean isValidArrayFill(
      VisitorState state, Type arrayComponentType, Type fillingObjectType) {
    if (arrayComponentType == null || fillingObjectType == null) {
      return true; // shrug
    }
    return ASTHelpers.isSubtype(
        state.getTypes().boxedTypeOrType(fillingObjectType), arrayComponentType, state);
  }

  private static String getMessage(Type fillingObjectType, Type arrayComponentType) {
    String fillingTypeString = Signatures.prettyType(fillingObjectType);
    String arrayComponentTypeString = Signatures.prettyType(arrayComponentType);
    if (arrayComponentTypeString.equals(fillingTypeString)) {
      fillingTypeString = fillingObjectType.toString();
      arrayComponentTypeString = arrayComponentType.toString();
    }
    return "Calling Arrays.fill trying to put a "
        + fillingTypeString
        + " into an array of type "
        + arrayComponentTypeString;
  }
}
