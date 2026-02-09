/*
 * Copyright 2025 The Error Prone Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.fixes.SuggestedFix.replace;
import static com.google.errorprone.fixes.SuggestedFixes.qualifyType;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;
import static com.google.errorprone.suppliers.Suppliers.OBJECT_TYPE;
import static com.google.errorprone.suppliers.Suppliers.OBJECT_TYPE_ARRAY;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.TargetType.targetType;
import static com.sun.source.tree.Tree.Kind.NULL_LITERAL;
import static javax.lang.model.type.TypeKind.ARRAY;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.TypeCastTree;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ArrayType;
import java.util.List;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    summary =
        "This call passes a null *array*, so it always produces NullPointerException. To pass a"
            + " null *element*, cast to the element type.",
    severity = ERROR)
public final class NullNeedsCastForVarargs extends BugChecker
    implements MethodInvocationTreeMatcher {
  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    var arguments = tree.getArguments();

    if (METHOD_WITH_SOLE_PARAMETER_OBJECT_VARARGS.matches(tree, state)) {
      return matchObjectVarargs(arguments, /* varargsStart= */ 0);
    } else if (METHOD_WITH_SECOND_PARAMETER_OBJECT_VARARGS.matches(tree, state)) {
      return matchObjectVarargs(arguments, /* varargsStart= */ 1);
    } else if (METHOD_WITH_THIRD_PARAMETER_OBJECT_VARARGS.matches(tree, state)) {
      return matchObjectVarargs(arguments, /* varargsStart= */ 2);
    } else if (METHOD_WITH_SOLE_PARAMETER_GENERIC_VARARGS.matches(tree, state)) {
      return matchGenericVarargs(state, arguments);
    }

    return NO_MATCH;
  }

  private Description matchObjectVarargs(
      List<? extends ExpressionTree> arguments, int varargsStart) {
    if (arguments.size() != varargsStart + 1) {
      return NO_MATCH;
    }
    var arg = arguments.getLast();
    if (arg.getKind() == NULL_LITERAL || isCastOfNullToArray(arg)) {
      return describeMatch(arg, replace(arg, "(Object) null"));
    }
    return NO_MATCH;
  }

  private Description matchGenericVarargs(
      VisitorState state, List<? extends ExpressionTree> arguments) {
    if (arguments.size() != 1) {
      return NO_MATCH;
    }
    var arg = arguments.getLast();
    if (arg.getKind() == NULL_LITERAL || isCastOfNullToArrayOfTargetTypeElementType(arg, state)) {
      var elementType = targetTypElementType(state);
      var fix = SuggestedFix.builder();
      var prettyName = qualifyType(state, fix, elementType);
      fix.replace(arg, "(%s) null".formatted(prettyName));
      return describeMatch(arg, fix.build());
    }
    return NO_MATCH;
  }

  private static boolean isCastOfNullToArray(ExpressionTree arg) {
    if (!(arg instanceof TypeCastTree castTree)) {
      return false;
    }
    if (castTree.getExpression().getKind() != NULL_LITERAL) {
      return false;
    }
    var castType = getType(castTree.getType());
    return castType.getKind() == ARRAY;
  }

  /**
   * Returns whether {@code arg} is being cast to {@code Foo[]} and the target type of the method
   * call at the path of {@code state} is {@code SomeGenericType<Foo>}.
   */
  private static boolean isCastOfNullToArrayOfTargetTypeElementType(
      ExpressionTree arg, VisitorState state) {
    if (!isCastOfNullToArray(arg)) {
      return false;
    }

    var castTree = (TypeCastTree) arg;
    var castType = (ArrayType) getType(castTree.getType());
    var elementType = targetTypElementType(state);
    return state.getTypes().isSubtype(castType.getComponentType(), elementType);
  }

  private static Type targetTypElementType(VisitorState state) {
    var targetType = targetType(state);
    return (targetType != null && !targetType.type().getTypeArguments().isEmpty())
        // The target is likely List<T>/Stream<T>/..., so the signature has `T...`. Return `T`.
        ? targetType.type().getTypeArguments().getFirst()
        : state.getSymtab().objectType;
  }

  private static final Matcher<ExpressionTree> METHOD_WITH_SOLE_PARAMETER_OBJECT_VARARGS =
      instanceMethod()
          .onDescendantOf("com.google.common.truth.Subject")
          .named("containsExactly")
          .withParametersOfType(OBJECT_TYPE_ARRAY);

  private static final Matcher<ExpressionTree> METHOD_WITH_SECOND_PARAMETER_OBJECT_VARARGS =
      instanceMethod()
          .onDescendantOf("com.google.common.reflect.Invokable")
          .named("invoke")
          .withParametersOfType(OBJECT_TYPE, OBJECT_TYPE_ARRAY);

  // TODO: b/429160687 - Also cover the methods in UsingCorrespondence.

  private static final Matcher<ExpressionTree> METHOD_WITH_THIRD_PARAMETER_OBJECT_VARARGS =
      instanceMethod()
          .onDescendantOf("com.google.common.truth.Subject")
          .namedAnyOf("containsAnyOf", "containsAtLeast", "containsNoneOf", "isAnyOf", "isNoneOf")
          .withParametersOfType(ImmutableList.of(OBJECT_TYPE, OBJECT_TYPE, OBJECT_TYPE_ARRAY));

  private static final Matcher<ExpressionTree> METHOD_WITH_SOLE_PARAMETER_GENERIC_VARARGS =
      anyOf(
          staticMethod()
              .onClass("com.google.common.collect.AndroidAccessToCompactDataStructures")
              .namedAnyOf("newCompactHashSet", "newCompactLinkedHashSet")
              .withParametersOfType(OBJECT_TYPE_ARRAY),
          staticMethod()
              .onClassAny(
                  "com.google.common.collect.CompactHashSet",
                  "com.google.common.collect.CompactLinkedHashSet")
              .named("create")
              .withParametersOfType(OBJECT_TYPE_ARRAY),
          staticMethod()
              .onClassAny(
                  "com.google.common.collect.Iterables", "com.google.common.collect.Iterators")
              .namedAnyOf("cycle", "forArray")
              .withParametersOfType(OBJECT_TYPE_ARRAY),
          staticMethod()
              .onClass("com.google.common.collect.Lists")
              .namedAnyOf("newArrayList", "newLinkedList")
              .withParametersOfType(OBJECT_TYPE_ARRAY),
          staticMethod()
              .onClass("com.google.common.collect.Sets")
              .namedAnyOf("newHashSet", "newLinkedHashSet")
              .withParametersOfType(OBJECT_TYPE_ARRAY),
          staticMethod().onClass("java.util.Arrays").named("asList"),
          staticMethod()
              .onClass("java.util.stream.Stream")
              .named("of")
              /*
               * The check probably handles the `of(T)` overload fine even if we don't exclude it
               * explicitly here, but why chance it?
               *
               * (Similarly, we may or may not need to call `withParametersOfType` for the other
               * cases above in which overloads exist, but we do anyway.)
               */
              .withParametersOfType(OBJECT_TYPE_ARRAY));
}
