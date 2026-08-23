/*
 * Copyright 2020 The Error Prone Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.argument;
import static com.google.errorprone.matchers.Matchers.isPrimitiveType;
import static com.google.errorprone.matchers.Matchers.staticEqualsInvocation;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.google.errorprone.util.OperatorPrecedence;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Type;
import java.util.Objects;
import javax.lang.model.type.TypeKind;

/**
 * Check for usage of {@code Objects.equal} on primitive types.
 *
 * @author vlk@google.com (Volodymyr Kachurovskyi)
 */
@BugPattern(
    summary = "Avoid unnecessary boxing by using plain == for primitive types.",
    tags = StandardTags.PERFORMANCE,
    severity = WARNING)
public class ObjectEqualsForPrimitives extends BugChecker implements MethodInvocationTreeMatcher {

  /** Matches when {@link java.util.Objects#equals}-like methods compare two primitive types. */
  private static final Matcher<MethodInvocationTree> MATCHER =
      allOf(
          staticEqualsInvocation(), argument(0, isPrimitiveType()), argument(1, isPrimitiveType()));

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!MATCHER.matches(tree, state)) {
      return NO_MATCH;
    }

    ExpressionTree expression1 = tree.getArguments().get(0);
    ExpressionTree expression2 = tree.getArguments().get(1);
    if (isFloatingPoint(expression1) || isFloatingPoint(expression2)) {
      // Objects.equal(a_double, another_double) compares NaN as equal, but a_double ==
      // another_double does not.
      // We could replace this with Double.doubleToLongBits(a_double) ==
      // Double.doubleToLongBits(another_double)
      // to avoid boxing, but that can be considered as less readable.
      return NO_MATCH;
    }

    String arg1 = state.getSourceForNode(expression1);
    String arg2 = state.getSourceForNode(expression2);

    TreePath parentPath = state.getPath().getParentPath();
    while (parentPath != null && parentPath.getLeaf() instanceof ParenthesizedTree) {
      parentPath = parentPath.getParentPath();
    }
    if (parentPath != null && parentPath.getLeaf().getKind() == Kind.LOGICAL_COMPLEMENT) {
      Tree target = parentPath.getLeaf();
      String replacement =
          String.format(requiresParentheses(parentPath) ? "(%s != %s)" : "%s != %s", arg1, arg2);
      Fix fix = SuggestedFix.builder().replace(target, replacement).build();
      return describeMatch(target, fix);
    }

    String replacement =
        String.format(requiresParentheses(state.getPath()) ? "(%s == %s)" : "%s == %s", arg1, arg2);
    Fix fix = SuggestedFix.builder().replace(tree, replacement).build();
    return describeMatch(tree, fix);
  }

  private static boolean requiresParentheses(TreePath path) {
    TreePath parentPath = path.getParentPath();
    if (parentPath == null) {
      return false;
    }
    Tree parent = parentPath.getLeaf();
    switch (parent.getKind()) {
      case PARENTHESIZED,
          RETURN,
          EXPRESSION_STATEMENT,
          ARRAY_ACCESS,
          ASSERT,
          LAMBDA_EXPRESSION,
          CONDITIONAL_EXPRESSION -> {
        return false;
      }
      case VARIABLE -> {
        if (Objects.equals(((VariableTree) parent).getInitializer(), path.getLeaf())) {
          return false;
        }
      }
      case ASSIGNMENT -> {
        if (Objects.equals(((AssignmentTree) parent).getExpression(), path.getLeaf())) {
          return false;
        }
      }
      case METHOD_INVOCATION -> {
        if (((MethodInvocationTree) parent).getArguments().contains(path.getLeaf())) {
          return false;
        }
      }
      case NEW_CLASS -> {
        if (((NewClassTree) parent).getArguments().contains(path.getLeaf())) {
          return false;
        }
      }
      case AND, XOR, OR, EQUAL_TO, NOT_EQUAL_TO -> {
        return true;
      }
      default -> {
        return OperatorPrecedence.optionallyFrom(parent.getKind())
            .map(p -> p.isHigher(OperatorPrecedence.EQUALITY))
            .orElse(false);
      }
    }
    return true;
  }

  private static boolean isFloatingPoint(ExpressionTree expression) {
    Type type = ASTHelpers.getType(expression);
    if (type == null) {
      return false;
    }
    return type.getKind() == TypeKind.DOUBLE || type.getKind() == TypeKind.FLOAT;
  }
}
