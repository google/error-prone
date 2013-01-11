/*
 * Copyright 2012 Google Inc. All Rights Reserved.
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
import static com.google.errorprone.BugPattern.MaturityLevel.EXPERIMENTAL;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.DescribingMatcher;
import com.google.errorprone.matchers.Description;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;

/**
 * @author ptoomey@google.com (Patrick Toomey)
 */
@BugPattern(name = "InvalidStringEquality",
    summary = "Invalid syntax used for a comparing strings",
    explanation = "This error is triggered by comparing string equality/inequality using == or !="
        + "instead of .equals()",
    category = JDK, severity = WARNING, maturity = EXPERIMENTAL)
public class InvalidStringEquality extends DescribingMatcher<BinaryTree> {

  /* Match string that are compared with == and != */
  @Override  
  public boolean matches(BinaryTree tree, VisitorState state) {
    if (tree.getKind() == Tree.Kind.EQUAL_TO || tree.getKind() == Tree.Kind.NOT_EQUAL_TO) {
      Type stringType = state.getTypeFromString("java.lang.String"); 
      ExpressionTree leftOperand = tree.getLeftOperand();
      Type leftHandType = ((JCTree.JCExpression) leftOperand).type;
      ExpressionTree rightOperand = tree.getRightOperand();
      Type rightHandType = ((JCTree.JCExpression) rightOperand).type;
      if (leftHandType.equals(stringType) && rightHandType.equals(stringType)) {
        return true;
      }
    }
    return false;
  }
  
  @Override
  public Description describe(BinaryTree binaryTree, VisitorState state) {
    // TODO:  This should be warning, not necessarily a fix..?? 
    return new Description(binaryTree, diagnosticMessage, null);
  }

  public static class Scanner extends com.google.errorprone.Scanner {
    private InvalidStringEquality matcher = new InvalidStringEquality();

    @Override
    public Void visitBinary(BinaryTree node, VisitorState visitorState) {
      evaluateMatch(node, visitorState, matcher);
      return super.visitBinary(node, visitorState);
    }
  }
}
