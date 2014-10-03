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
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.bugpatterns.BugChecker.BinaryTreeMatcher;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.kindIs;
import static com.sun.source.tree.Tree.Kind.EQUAL_TO;
import static com.sun.source.tree.Tree.Kind.NOT_EQUAL_TO;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;

import java.util.List;

/**
 * @author scottjohnson@google.com (Scott Johnson)
 */
@BugPattern(name = "SelfEquality",
    summary = "Variable compared to itself",
    explanation = "There is no good reason to test a primitive value or reference for equality " +
          "with itself.",
    category = JDK, severity = ERROR, maturity = EXPERIMENTAL)
public class SelfEquality extends BugChecker implements BinaryTreeMatcher {

  @Override
  public Description matchBinary(BinaryTree tree, VisitorState state) {
    if (!(anyOf(kindIs(EQUAL_TO), kindIs(NOT_EQUAL_TO)).matches(tree, state)
        && ASTHelpers.sameVariable(tree.getLeftOperand(), tree.getRightOperand()))) {
      return Description.NO_MATCH;
    }

    StringBuilder fixedExpression = new StringBuilder();
    Fix fix = null;

    ExpressionTree leftOperand = tree.getLeftOperand();
    ExpressionTree rightOperand = tree.getRightOperand();
    Type leftType = ((JCTree) leftOperand).type;
    Types types = state.getTypes();
    Symtab symtab = state.getSymtab();

    /**
     * Try to figure out what they were trying to do.
     * Cases:
     * 1) (foo == foo) ==> (foo == other.foo)
     * 2) (foo == this.foo) ==> (other.foo == this.foo)
     * 3) (this.foo == foo) ==> (this.foo == other.foo)
     * 4) (this.foo == this.foo) ==> (this.foo == other.foo)
     */

    // Choose argument to replace.
    ExpressionTree toReplace;
    if (rightOperand.getKind() == Kind.IDENTIFIER) {
      toReplace = rightOperand;
    } else if (leftOperand.getKind() == Kind.IDENTIFIER) {
      toReplace = leftOperand;
    } else {
      // If we don't have a good reason to replace one or the other, replace the second.
      toReplace = rightOperand;
    }

    // Find containing block
    TreePath path = state.getPath();
    while (path.getLeaf() != null && path.getLeaf().getKind() != Kind.CLASS
        && path.getLeaf().getKind() != Kind.BLOCK) {
      path = path.getParentPath();
    }
    if (path.getLeaf() != null) {
      List<? extends JCTree> members;
      // Must be block or class
      if (path.getLeaf().getKind() == Kind.CLASS) {
        members = ((JCClassDecl) path.getLeaf()).getMembers();
      } else {
        members = ((JCBlock) path.getLeaf()).getStatements();
      }
      for (JCTree jcTree : members) {
        if (jcTree.getKind() == Kind.VARIABLE) {
          JCVariableDecl declaration = (JCVariableDecl) jcTree;
          TypeSymbol variableTypeSymbol = declaration.getType().type.tsym;

          if (ASTHelpers.getSymbol(toReplace).isMemberOf(variableTypeSymbol, state.getTypes())) {
            if (toReplace.getKind() == Kind.IDENTIFIER) {
              fix = SuggestedFix.prefixWith(toReplace, declaration.getName() + ".");
            } else {
              fix = SuggestedFix.replace(
                  ((JCFieldAccess) toReplace).getExpression(), declaration.getName().toString());
            }
          }
        }
      }
    }

    if (fix == null) {
      // No good replacement, let's try something else!

      // For floats or doubles, y!=y -> isNaN(y)
      if (tree.getKind() == Tree.Kind.EQUAL_TO) {
        fixedExpression.append("!");
      }
      if (types.isSameType(leftType, symtab.floatType)) {
        fixedExpression.append("Float.isNaN(" + leftOperand + ")");
        fix = SuggestedFix.replace(tree, fixedExpression.toString());
      } else if (types.isSameType(leftType, symtab.doubleType)) {
        fixedExpression.append("Double.isNaN(" + leftOperand + ")");
        fix = SuggestedFix.replace(tree, fixedExpression.toString());
      } else {

        // last resort, just replace with true or false
        if (tree.getKind() == Tree.Kind.EQUAL_TO) {
          fix = SuggestedFix.replace(tree, "true");
        } else {
          fix = SuggestedFix.replace(tree, "false");
        }
      }
    }

    return (fix == null) ? describeMatch(tree) : describeMatch(tree, fix);
  }
}
