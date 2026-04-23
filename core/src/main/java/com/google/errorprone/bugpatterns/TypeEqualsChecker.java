/*
 * Copyright 2018 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.matchers.Matchers.staticMethod;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.isSubtype;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.BinaryTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.code.Type;

/** Flags TypeMirror equality usage. */
@BugPattern(
    name = "TypeEquals",
    summary =
        "TypeMirror should be compared using Types#isSameType, not equality operators or equals().",
    severity = WARNING)
public class TypeEqualsChecker extends BugChecker
    implements BinaryTreeMatcher, MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> EQUALS_MATCHER =
      instanceMethod().onDescendantOf("javax.lang.model.type.TypeMirror").named("equals");

  private static final Matcher<ExpressionTree> OBJECTS_EQUALS_MATCHER =
      staticMethod().onClass("java.util.Objects").named("equals");

  @Override
  public Description matchBinary(BinaryTree tree, VisitorState state) {
    switch (tree.getKind()) {
      case EQUAL_TO, NOT_EQUAL_TO -> {}
      default -> {
        return NO_MATCH;
      }
    }
    return describe(
        tree,
        tree.getLeftOperand(),
        tree.getRightOperand(),
        tree.getKind() == Kind.NOT_EQUAL_TO,
        state);
  }

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (EQUALS_MATCHER.matches(tree, state)) {
      ExpressionTree receiver = ASTHelpers.getReceiver(tree);
      ExpressionTree argument = tree.getArguments().get(0);
      if (receiver != null) {
        return describe(tree, receiver, argument, false, state);
      }
    }
    if (OBJECTS_EQUALS_MATCHER.matches(tree, state)) {
      var args = tree.getArguments();
      return describe(tree, args.get(0), args.get(1), false, state);
    }
    return NO_MATCH;
  }

  private static boolean isTypeMirror(ExpressionTree tree, VisitorState state) {
    Type type = getType(tree);
    return type != null && isSubtype(type, ProcessingEnvUtils.TYPE_MIRROR_TYPE.get(state), state);
  }

  private Description describe(
      Tree tree, ExpressionTree lhs, ExpressionTree rhs, boolean negate, VisitorState state) {
    if (lhs.getKind() == Kind.NULL_LITERAL || rhs.getKind() == Kind.NULL_LITERAL) {
      return NO_MATCH;
    }
    if (!isTypeMirror(lhs, state) && !isTypeMirror(rhs, state)) {
      return NO_MATCH;
    }
    Description.Builder description = buildDescription(tree);
    ProcessingEnvUtils.getTypesExpr(state)
        .ifPresent(
            typesExpr -> {
              String prefix = negate ? "!" : "";
              String lhsSource = state.getSourceForNode(lhs);
              String rhsSource = state.getSourceForNode(rhs);
              description.addFix(
                  SuggestedFix.replace(
                      tree,
                      String.format(
                          "%s%s.isSameType(%s, %s)", prefix, typesExpr, lhsSource, rhsSource)));
            });
    return description.build();
  }
}
