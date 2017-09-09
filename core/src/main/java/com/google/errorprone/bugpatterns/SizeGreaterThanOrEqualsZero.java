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

import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Iterables.transform;
import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.matchers.Matchers.staticMethod;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Table.Cell;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.BinaryTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.predicates.TypePredicate;
import com.google.errorprone.predicates.TypePredicates;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.tree.JCTree;
import java.util.List;

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
  explanation =
      "A standard means of checking non-emptiness of an array or collection is to "
          + "test if the size of that collection is greater than 0. However, one may accidentally "
          + "check if the size is greater than or equal to 0, which is always true.",
  category = JDK,
  severity = ERROR,
  providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION
)
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

  private static final Matcher<ExpressionTree> INSTANCE_METHOD_MATCHER =
      buildInstanceMethodMatcher();
  private static final Matcher<ExpressionTree> STATIC_METHOD_MATCHER = buildStaticMethodMatcher();
  private static final Matcher<MemberSelectTree> ARRAY_LENGTH_MATCHER = arrayLengthMatcher();
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
      if (INSTANCE_METHOD_MATCHER.matches(callToSize, state)) {
        return provideReplacementForMethodInvocation(tree, callToSize, state, expressionType);
      } else if (STATIC_METHOD_MATCHER.matches(callToSize, state)) {
        return provideReplacementForStaticMethodInvocation(tree, callToSize, state, expressionType);
      }
    } else if (operand instanceof MemberSelectTree) {
      if (ARRAY_LENGTH_MATCHER.matches((MemberSelectTree) operand, state)) {
        return removeEqualsFromComparison(tree, state, expressionType);
      }
    }

    return Description.NO_MATCH;
  }

  private static Matcher<ExpressionTree> buildInstanceMethodMatcher() {
    TypePredicate lengthMethodClass =
        TypePredicates.isDescendantOfAny(CLASSES.column(MethodName.LENGTH).keySet());
    TypePredicate sizeMethodClass =
        TypePredicates.isDescendantOfAny(CLASSES.column(MethodName.SIZE).keySet());

    return anyOf(
        instanceMethod().onClass(sizeMethodClass).named("size"),
        instanceMethod().onClass(lengthMethodClass).named("length"));
  }

  private static Matcher<ExpressionTree> buildStaticMethodMatcher() {
    Iterable<Matcher<ExpressionTree>> sizeStaticMethods =
        staticMethodMatcher(STATIC_CLASSES.column(MethodName.SIZE).keySet(), "size");
    Iterable<Matcher<ExpressionTree>> lengthStaticMethods =
        staticMethodMatcher(STATIC_CLASSES.column(MethodName.LENGTH).keySet(), "length");

    return anyOfIterable(concat(sizeStaticMethods, lengthStaticMethods));
  }

  private static Iterable<Matcher<ExpressionTree>> staticMethodMatcher(
      Iterable<String> sizeMethodClassNames, final String methodName) {
    return Iterables.transform(
        sizeMethodClassNames,
        new Function<String, Matcher<ExpressionTree>>() {
          @Override
          public Matcher<ExpressionTree> apply(String className) {
            return staticMethod().onClass(className).named(methodName);
          }
        });
  }

  private static Matcher<ExpressionTree> isSubtypeOfAny(Iterable<String> classes) {
    return anyOfIterable(
        transform(
            classes,
            new Function<String, Matcher<ExpressionTree>>() {
              @Override
              public Matcher<ExpressionTree> apply(String clazzName) {
                return Matchers.isSubtypeOf(clazzName);
              }
            }));
  }

  // From the defined classes above, return a matcher that will match an expression if its type
  // contains a well-behaved isEmpty() method.
  private static Matcher<ExpressionTree> classHasIsEmptyFunction() {
    ImmutableList.Builder<String> classNames = ImmutableList.builder();
    for (Cell<String, MethodName, Boolean> methodInformation :
        Iterables.concat(CLASSES.cellSet(), STATIC_CLASSES.cellSet())) {
      if (!methodInformation.getValue()) {
        continue;
      }
      classNames.add(methodInformation.getRowKey());
    }

    return isSubtypeOfAny(classNames.build());
  }

  private static Matcher<MemberSelectTree> arrayLengthMatcher() {
    return new Matcher<MemberSelectTree>() {
      @Override
      public boolean matches(MemberSelectTree tree, VisitorState state) {
        return ASTHelpers.getSymbol(tree) == state.getSymtab().lengthVar;
      }
    };
  }

  private Description provideReplacementForMethodInvocation(
      BinaryTree tree,
      MethodInvocationTree leftOperand,
      VisitorState state,
      ExpressionType expressionType) {
    ExpressionTree collection = ASTHelpers.getReceiver(leftOperand);

    if (HAS_EMPTY_METHOD.matches(collection, state)) {
      return describeMatch(
          tree,
          SuggestedFix.replace(
              tree, "!" + state.getSourceForNode((JCTree) collection) + ".isEmpty()"));
    } else {
      return removeEqualsFromComparison(tree, state, expressionType);
    }
  }

  private Description provideReplacementForStaticMethodInvocation(
      BinaryTree tree,
      MethodInvocationTree callToSize,
      final VisitorState state,
      ExpressionType expressionType) {
    ExpressionTree classToken = ASTHelpers.getReceiver(callToSize);

    if (HAS_EMPTY_METHOD.matches(classToken, state)) {
      List<CharSequence> argumentSourceValues =
          Lists.transform(
              callToSize.getArguments(),
              new Function<ExpressionTree, CharSequence>() {
                @Override
                public CharSequence apply(ExpressionTree expressionTree) {
                  return state.getSourceForNode((JCTree) expressionTree);
                }
              });
      String argumentString = Joiner.on(',').join(argumentSourceValues);

      return describeMatch(
          tree,
          SuggestedFix.replace(
              tree,
              "!"
                  + state.getSourceForNode((JCTree) classToken)
                  + ".isEmpty("
                  + argumentString
                  + ")"));
    } else {
      return removeEqualsFromComparison(tree, state, expressionType);
    }
  }

  private Description removeEqualsFromComparison(
      BinaryTree tree, VisitorState state, ExpressionType expressionType) {
    String replacement =
        expressionType == ExpressionType.GREATER_THAN_EQUAL
            ? state.getSourceForNode((JCTree) tree.getLeftOperand()) + " > 0"
            : "0 < " + state.getSourceForNode((JCTree) tree.getRightOperand());
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

  private static <T extends Tree> Matcher<T> anyOfIterable(Iterable<Matcher<T>> matchers) {
    final ImmutableList<Matcher<T>> copyOfMatchers = ImmutableList.copyOf(matchers);
    return new Matcher<T>() {
      @Override
      public boolean matches(T t, VisitorState state) {
        for (Matcher<? super T> matcher : copyOfMatchers) {
          if (matcher.matches(t, state)) {
            return true;
          }
        }
        return false;
      }
    };
  }
}
