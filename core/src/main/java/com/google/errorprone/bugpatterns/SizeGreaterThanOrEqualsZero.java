/*
 * Copyright 2015 The Error Prone Authors.
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

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.matchers.Matchers.staticMethod;
import static com.google.errorprone.util.ASTHelpers.getReceiver;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static java.util.stream.Collectors.joining;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Iterables;
import com.google.common.collect.Streams;
import com.google.common.collect.Table.Cell;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.BinaryTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.matchers.method.MethodMatchers.ParameterMatcher;
import com.google.errorprone.predicates.TypePredicates;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.code.Scope;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import java.util.regex.Pattern;

/**
 * Finds instances where one uses {@code Collection#size() >= 0} or {@code T[].length > 0}. Those
 * comparisons are always true for non-null objects, where the user likely meant to compare size
 * {@literal >} 0 instead.
 *
 * @author glorioso@google.com (Nick Glorioso)
 */
@BugPattern(
    name = "SizeGreaterThanOrEqualsZero",
    summary =
        "Comparison of a size >= 0 is always true, did you intend to check for " + "non-emptiness?",
    category = JDK,
    severity = ERROR,
    providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION)
public class SizeGreaterThanOrEqualsZero extends BugChecker implements BinaryTreeMatcher {

  private enum MethodName {
    LENGTH,
    SIZE
  }

  private enum ExpressionType {
    LESS_THAN_EQUAL,
    GREATER_THAN_EQUAL,
    MISMATCH
  }

  // Class name, whether it uses SIZE or LENGTH, whether or not the class has an appropriate
  // isEmpty() method
  private static final ImmutableTable<String, MethodName, Boolean> CLASSES =
      ImmutableTable.<String, MethodName, Boolean>builder()
          .put("android.util.LongSparseArray", MethodName.SIZE, false)
          .put("android.util.LruCache", MethodName.SIZE, false)
          .put("android.util.SparseArray", MethodName.SIZE, false)
          .put("android.util.SparseBooleanArray", MethodName.SIZE, false)
          .put("android.util.SparseIntArray", MethodName.SIZE, false)
          .put("android.util.SparseLongArray", MethodName.SIZE, false)
          .put("android.support.v4.util.CircularArray", MethodName.SIZE, true)
          .put("android.support.v4.util.CircularIntArray", MethodName.SIZE, true)
          .put("android.support.v4.util.LongSparseArray", MethodName.SIZE, false)
          .put("android.support.v4.util.LruCache", MethodName.SIZE, false)
          .put("android.support.v4.util.SimpleArrayMap", MethodName.SIZE, true)
          .put("android.support.v4.util.SparseArrayCompat", MethodName.SIZE, false)
          .put("com.google.common.collect.FluentIterable", MethodName.SIZE, true)
          .put("com.google.common.collect.Multimap", MethodName.SIZE, true)
          .put("java.io.ByteArrayOutputStream", MethodName.SIZE, false)
          .put("java.util.Collection", MethodName.SIZE, true)
          .put("java.util.Dictionary", MethodName.SIZE, true)
          .put("java.util.Map", MethodName.SIZE, true)
          .put("java.util.BitSet", MethodName.LENGTH, true)
          .put("java.lang.CharSequence", MethodName.LENGTH, false)
          .put("java.lang.String", MethodName.LENGTH, true)
          .put("java.lang.StringBuilder", MethodName.LENGTH, false)
          .put("java.lang.StringBuffer", MethodName.LENGTH, false)
          .build();

  private static final ImmutableTable<String, MethodName, Boolean> STATIC_CLASSES =
      ImmutableTable.<String, MethodName, Boolean>builder()
          .put("com.google.common.collect.Iterables", MethodName.SIZE, true)
          .build();

  private static final Matcher<ExpressionTree> SIZE_OR_LENGTH_INSTANCE_METHOD =
      anyOf(
          instanceMethod()
              .onClass(TypePredicates.isDescendantOfAny(CLASSES.column(MethodName.SIZE).keySet()))
              .named("size"),
          instanceMethod()
              .onClass(TypePredicates.isDescendantOfAny(CLASSES.column(MethodName.LENGTH).keySet()))
              .named("length"));
  private static final Pattern PROTO_COUNT_METHOD_PATTERN = Pattern.compile("get(.+)Count");
  private static final ParameterMatcher PROTO_METHOD_NAMED_GET_COUNT =
      instanceMethod()
          .onClass(TypePredicates.isDescendantOf("com.google.protobuf.GeneratedMessage"))
          .withNameMatching(PROTO_COUNT_METHOD_PATTERN)
          .withParameters();
  private static final Matcher<ExpressionTree> PROTO_REPEATED_FIELD_COUNT_METHOD =
      SizeGreaterThanOrEqualsZero::isProtoRepeatedFieldCountMethod;
  private static final Matcher<ExpressionTree> SIZE_OR_LENGTH_STATIC_METHOD =
      anyOf(
          Streams.concat(
                  STATIC_CLASSES.column(MethodName.SIZE).keySet().stream()
                      .map(className -> staticMethod().onClass(className).named("size")),
                  STATIC_CLASSES.column(MethodName.LENGTH).keySet().stream()
                      .map(className -> staticMethod().onClass(className).named("length")))
              .collect(toImmutableList()));
  private static final Matcher<MemberSelectTree> ARRAY_LENGTH_MATCHER =
      (tree, state) -> ASTHelpers.getSymbol(tree) == state.getSymtab().lengthVar;
  private static final Matcher<ExpressionTree> HAS_EMPTY_METHOD = classHasIsEmptyFunction();

  @Override
  public Description matchBinary(BinaryTree tree, VisitorState state) {
    // Easy stuff: needs to be a binary expression of the form foo >= 0 or 0 <= foo
    ExpressionType expressionType = isGreaterThanEqualToZero(tree);
    if (expressionType == ExpressionType.MISMATCH) {
      return Description.NO_MATCH;
    }

    ExpressionTree operand =
        expressionType == ExpressionType.GREATER_THAN_EQUAL
            ? tree.getLeftOperand()
            : tree.getRightOperand();
    if (operand instanceof MethodInvocationTree) {
      MethodInvocationTree callToSize = (MethodInvocationTree) operand;
      if (SIZE_OR_LENGTH_INSTANCE_METHOD.matches(callToSize, state)) {
        return provideReplacementForInstanceMethodInvocation(
            tree, callToSize, state, expressionType);
      } else if (SIZE_OR_LENGTH_STATIC_METHOD.matches(callToSize, state)) {
        return provideReplacementForStaticMethodInvocation(tree, callToSize, state, expressionType);
      } else if (PROTO_REPEATED_FIELD_COUNT_METHOD.matches(callToSize, state)) {
        return provideReplacementForProtoMethodInvocation(tree, callToSize, state);
      }
    } else if (operand instanceof MemberSelectTree) {
      if (ARRAY_LENGTH_MATCHER.matches((MemberSelectTree) operand, state)) {
        return removeEqualsFromComparison(tree, state, expressionType);
      }
    }
    return Description.NO_MATCH;
  }

  private static boolean isProtoRepeatedFieldCountMethod(ExpressionTree tree, VisitorState state) {
    // Instance method, on proto class, named `get<Field>Count`.
    if (!PROTO_METHOD_NAMED_GET_COUNT.matches(tree, state)) {
      return false;
    }
    // Make sure it's the count method for a repeated field, not the get method for a non-repeated
    // field named <something>_count, by checking for other methods on the repeated field.
    MethodSymbol methodCallSym = getSymbol((MethodInvocationTree) tree);
    if (methodCallSym == null) {
      return false;
    }
    Scope protoClassMembers = methodCallSym.owner.members();
    java.util.regex.Matcher getCountRegexMatcher =
        PROTO_COUNT_METHOD_PATTERN.matcher(methodCallSym.getSimpleName().toString());
    if (!getCountRegexMatcher.matches()) {
      return false;
    }
    String fieldName = getCountRegexMatcher.group(1);
    return protoClassMembers.findFirst(state.getName("get" + fieldName + "List")) != null;
  }

  // From the defined classes above, return a matcher that will match an expression if its type
  // contains a well-behaved isEmpty() method.
  private static Matcher<ExpressionTree> classHasIsEmptyFunction() {
    ImmutableList.Builder<String> classNames = ImmutableList.builder();
    for (Cell<String, MethodName, Boolean> methodInformation :
        Iterables.concat(CLASSES.cellSet(), STATIC_CLASSES.cellSet())) {
      if (methodInformation.getValue()) {
        classNames.add(methodInformation.getRowKey());
      }
    }
    return anyOf(classNames.build().stream().map(Matchers::isSubtypeOf).collect(toImmutableList()));
  }

  private Description provideReplacementForInstanceMethodInvocation(
      BinaryTree tree,
      MethodInvocationTree leftOperand,
      VisitorState state,
      ExpressionType expressionType) {
    ExpressionTree collection = getReceiver(leftOperand);

    if (HAS_EMPTY_METHOD.matches(collection, state)) {
      return describeMatch(
          tree,
          SuggestedFix.replace(tree, "!" + state.getSourceForNode(collection) + ".isEmpty()"));
    } else {
      return removeEqualsFromComparison(tree, state, expressionType);
    }
  }

  private Description provideReplacementForStaticMethodInvocation(
      BinaryTree tree,
      MethodInvocationTree callToSize,
      final VisitorState state,
      ExpressionType expressionType) {
    ExpressionTree classToken = getReceiver(callToSize);

    if (HAS_EMPTY_METHOD.matches(classToken, state)) {
      String argumentString =
          callToSize.getArguments().stream().map(state::getSourceForNode).collect(joining(","));

      return describeMatch(
          tree,
          SuggestedFix.replace(
              tree, "!" + state.getSourceForNode(classToken) + ".isEmpty(" + argumentString + ")"));
    } else {
      return removeEqualsFromComparison(tree, state, expressionType);
    }
  }

  private Description provideReplacementForProtoMethodInvocation(
      BinaryTree tree, MethodInvocationTree protoGetSize, VisitorState state) {
    String expSrc = state.getSourceForNode(protoGetSize);
    java.util.regex.Matcher protoGetCountMatcher = PROTO_COUNT_METHOD_PATTERN.matcher(expSrc);
    if (!protoGetCountMatcher.find()) {
      throw new AssertionError(protoGetSize + " does not contain a get<RepeatedField>Count method");
    }
    return describeMatch(
        tree,
        SuggestedFix.replace(
            tree,
            "!"
                + protoGetCountMatcher.replaceFirst("get" + protoGetCountMatcher.group(1) + "List")
                + ".isEmpty()"));
  }

  private Description removeEqualsFromComparison(
      BinaryTree tree, VisitorState state, ExpressionType expressionType) {
    String replacement =
        expressionType == ExpressionType.GREATER_THAN_EQUAL
            ? state.getSourceForNode(tree.getLeftOperand()) + " > 0"
            : "0 < " + state.getSourceForNode(tree.getRightOperand());
    return describeMatch(tree, SuggestedFix.replace(tree, replacement));
  }

  private ExpressionType isGreaterThanEqualToZero(BinaryTree tree) {
    ExpressionTree literalOperand;
    ExpressionType returnType;

    switch (tree.getKind()) {
      case GREATER_THAN_EQUAL:
        literalOperand = tree.getRightOperand();
        returnType = ExpressionType.GREATER_THAN_EQUAL;
        break;
      case LESS_THAN_EQUAL:
        literalOperand = tree.getLeftOperand();
        returnType = ExpressionType.LESS_THAN_EQUAL;
        break;
      default:
        return ExpressionType.MISMATCH;
    }

    if (literalOperand.getKind() != Kind.INT_LITERAL) {
      return ExpressionType.MISMATCH;
    }
    if (!((LiteralTree) literalOperand).getValue().equals(0)) {
      return ExpressionType.MISMATCH;
    }

    return returnType;
  }
}
