/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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
import static com.google.errorprone.matchers.Description.NO_MATCH;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.BinaryTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.BinaryTree;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
  name = "IdentityBinaryExpression",
  category = JDK,
  summary = "Writing \"a && a\", \"a || a\", \"a & a\", or \"a | a\" is equivalent to \"a\".",
  explanation = "Writing `a && a`, `a || a`, `a & a`, or `a | a` is equivalent to `a`.",
  severity = ERROR
)
public class IdentityBinaryExpression extends BugChecker implements BinaryTreeMatcher {
  @Override
  public Description matchBinary(BinaryTree tree, VisitorState state) {
    String opName;
    switch (tree.getKind()) {
      case CONDITIONAL_AND:
        opName = "&&";
        break;
      case CONDITIONAL_OR:
        opName = "||";
        break;
      case AND:
        opName = "&";
        break;
      case OR:
        opName = "|";
        break;
      default:
        return NO_MATCH;
    }
    if (!tree.getLeftOperand().toString().equals(tree.getRightOperand().toString())) {
      return NO_MATCH;
    }
    return buildDescription(tree)
        .setMessage(String.format("Writing `a %s a` is equivalent to `a`", opName))
        .build();
  }
}
