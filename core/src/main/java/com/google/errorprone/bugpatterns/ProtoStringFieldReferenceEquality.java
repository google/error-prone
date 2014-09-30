/*
 * Copyright 2014 Google Inc. All Rights Reserved.
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
import static com.google.errorprone.BugPattern.MaturityLevel.EXPERIMENTAL;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.expressionMethodSelect;
import static com.google.errorprone.matchers.Matchers.isSameType;
import static com.google.errorprone.matchers.Matchers.isSubtypeOf;
import static com.google.errorprone.matchers.Matchers.methodReceiver;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.BinaryTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.tree.JCTree;

@BugPattern(category = ONE_OFF, maturity = EXPERIMENTAL,
    name = "ProtoStringFieldReferenceEquality", severity = ERROR,
    summary = "Comparing protobuf fields of type String using reference equality",
    explanation = "Comparing strings with == is almost always an error, but it is an error 100% "
        + "of the time when one of the strings is a protobuf field.  Additionally, protobuf "
        + "fields cannot be null, so Object.equals(Object) is always more correct.")
public class ProtoStringFieldReferenceEquality extends BugChecker implements BinaryTreeMatcher {

  private static final String PROTO_SUPER_CLASS = "com.google.protobuf.GeneratedMessage";

  private static final Matcher<ExpressionTree> PROTO_STRING_METHOD = allOf(
      expressionMethodSelect(methodReceiver(isSubtypeOf(PROTO_SUPER_CLASS))),
      isSameType("java.lang.String"));

  @Override
  public Description matchBinary(BinaryTree tree, VisitorState state) {
    if (tree.getKind() != Kind.EQUAL_TO && tree.getKind() != Kind.NOT_EQUAL_TO) {
      return Description.NO_MATCH;
    }
    String leftOperand = state.getSourceForNode((JCTree) tree.getLeftOperand()).toString();
    String rightOperand = state.getSourceForNode((JCTree) tree.getRightOperand()).toString();
    if ((PROTO_STRING_METHOD.matches(tree.getLeftOperand(), state)
        && tree.getRightOperand().getKind() != Kind.NULL_LITERAL)
        || (PROTO_STRING_METHOD.matches(tree.getRightOperand(), state)
        && tree.getLeftOperand().getKind() != Kind.NULL_LITERAL)) {
      String result = leftOperand + ".equals(" + rightOperand + ")";
      if (tree.getKind() == Kind.NOT_EQUAL_TO) {
        result = "!" + result;
      }
      return describeMatch(tree, SuggestedFix.replace(tree, result));
    } else {
      return Description.NO_MATCH;
    }
  }

}
