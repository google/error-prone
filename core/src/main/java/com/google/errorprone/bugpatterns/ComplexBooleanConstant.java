/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.Category;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.BinaryTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.BinaryTree;
import com.sun.tools.javac.tree.JCTree.JCLiteral;

/** @author Sumit Bhagwani (bhagwani@google.com) */
@BugPattern(
  name = "ComplexBooleanConstant",
  summary = "Non-trivial compile time constant boolean expressions shouldn't be used.",
  category = Category.JDK,
  severity = SeverityLevel.ERROR
)
public class ComplexBooleanConstant extends BugChecker implements BinaryTreeMatcher {

  @Override
  public Description matchBinary(BinaryTree tree, VisitorState state) {
    if (!(tree.getLeftOperand() instanceof JCLiteral)) {
      return Description.NO_MATCH;
    }
    if (!(tree.getRightOperand() instanceof JCLiteral)) {
      return Description.NO_MATCH;
    }
    Boolean constValue = ASTHelpers.constValue(tree, Boolean.class);
    if (constValue == null) {
      return Description.NO_MATCH;
    }
    return buildDescription(tree)
        .addFix(SuggestedFix.replace(tree, constValue.toString()))
        .setMessage(
            String.format(
                "This expression always evalutes to `%s`, prefer a boolean literal for clarity.",
                constValue))
        .build();
  }
}
