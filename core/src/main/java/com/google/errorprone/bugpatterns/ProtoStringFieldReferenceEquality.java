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
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.matchers.Matchers.isSameType;
import static com.google.errorprone.matchers.ProtobufMatchers.GENERATED_MESSAGE_CLASS;
import static com.google.errorprone.matchers.ProtobufMatchers.GENERATED_MESSAGE_LITE_CLASS;
import static com.google.errorprone.matchers.ProtobufMatchers.MESSAGE_LITE_OR_BUILDER_CLASS;

import com.google.errorprone.BugPattern;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.BinaryTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.suppliers.Suppliers;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree.Kind;
import javax.inject.Inject;

@BugPattern(
    severity = ERROR,
    summary = "Comparing protobuf fields of type String using reference equality")
public class ProtoStringFieldReferenceEquality extends BugChecker implements BinaryTreeMatcher {

  private final boolean checkOrBuilder;

  @Inject
  public ProtoStringFieldReferenceEquality(ErrorProneFlags flags) {
    this.checkOrBuilder =
        flags.getBoolean("ProtoStringFieldReferenceEquality:CheckOrBuilder").orElse(true);
  }

  public ProtoStringFieldReferenceEquality() {
    this(ErrorProneFlags.empty());
  }

  private static final Matcher<ExpressionTree> PROTO_STRING_METHOD_LEGACY =
      allOf(
          instanceMethod().onDescendantOfAny(GENERATED_MESSAGE_CLASS, GENERATED_MESSAGE_LITE_CLASS),
          isSameType(Suppliers.STRING_TYPE));

  private static final Matcher<ExpressionTree> PROTO_STRING_METHOD_OR_BUILDER =
      allOf(
          instanceMethod()
              .onDescendantOfAny(
                  MESSAGE_LITE_OR_BUILDER_CLASS,
                  GENERATED_MESSAGE_CLASS,
                  GENERATED_MESSAGE_LITE_CLASS),
          isSameType(Suppliers.STRING_TYPE));

  @Override
  public Description matchBinary(BinaryTree tree, VisitorState state) {
    switch (tree.getKind()) {
      case EQUAL_TO, NOT_EQUAL_TO -> {}
      default -> {
        return NO_MATCH;
      }
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
    Matcher<ExpressionTree> matcher =
        checkOrBuilder ? PROTO_STRING_METHOD_OR_BUILDER : PROTO_STRING_METHOD_LEGACY;
    return matcher.matches(a, state) && b.getKind() != Kind.NULL_LITERAL;
  }
}
