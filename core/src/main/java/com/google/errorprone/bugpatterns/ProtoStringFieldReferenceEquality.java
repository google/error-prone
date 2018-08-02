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

import static com.google.errorprone.BugPattern.Category.ONE_OFF;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.matchers.Matchers.isSameType;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.BinaryTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.suppliers.Suppliers;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree.Kind;

@BugPattern(
    category = ONE_OFF,
    name = "ProtoStringFieldReferenceEquality",
    severity = ERROR,
    summary = "Comparing protobuf fields of type String using reference equality",
    providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION)
public class ProtoStringFieldReferenceEquality extends BugChecker implements BinaryTreeMatcher {

  private static final String PROTO_SUPER_CLASS = "com.google.protobuf.GeneratedMessage";

  private static final String LITE_PROTO_SUPER_CLASS = "com.google.protobuf.GeneratedMessageLite";

  private static final Matcher<ExpressionTree> PROTO_STRING_METHOD =
      allOf(
          instanceMethod().onDescendantOfAny(PROTO_SUPER_CLASS, LITE_PROTO_SUPER_CLASS),
          isSameType(Suppliers.STRING_TYPE));

  @Override
  public Description matchBinary(BinaryTree tree, VisitorState state) {
    switch (tree.getKind()) {
      case EQUAL_TO:
      case NOT_EQUAL_TO:
        break;
      default:
        return NO_MATCH;
    }
    ExpressionTree lhs = tree.getLeftOperand();
    ExpressionTree rhs = tree.getRightOperand();
    if (match(lhs, rhs, state) || match(rhs, lhs, state)) {
      String result =
          String.format("%s.equals(%s)", state.getSourceForNode(lhs), state.getSourceForNode(rhs));
      if (tree.getKind() == Kind.NOT_EQUAL_TO) {
        result = "!" + result;
      }
      return describeMatch(tree, SuggestedFix.replace(tree, result));
    }
    return NO_MATCH;
  }

  private boolean match(ExpressionTree a, ExpressionTree b, VisitorState state) {
    return PROTO_STRING_METHOD.matches(a, state) && b.getKind() != Kind.NULL_LITERAL;
  }
}
