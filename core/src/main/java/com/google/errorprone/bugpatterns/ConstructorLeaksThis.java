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

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import javax.lang.model.element.Name;

/**
 * Looks for constructors from which the 'this' reference escapes. This is bad because the code that
 * uses the reference may observe the object in a partly-constructed state.
 */
@BugPattern(
  name = "ConstructorLeaksThis",
  summary =
      "Constructors should not pass the 'this' reference out in method invocations,"
          + " since the object may not be fully constructed.",
  category = JDK,
  severity = WARNING
)
public class ConstructorLeaksThis extends ConstructorLeakChecker {

  @Override
  protected void traverse(Tree tree, VisitorState state) {
    ClassSymbol thisClass = ASTHelpers.getSymbol(state.findEnclosing(ClassTree.class));
    tree.accept(
        new TreeScanner<Void, Void>() {
          @Override
          public Void visitIdentifier(IdentifierTree node, Void unused) {
            checkForThis(node, node.getName(), thisClass, state);
            return super.visitIdentifier(node, null);
          }

          @Override
          public Void visitMemberSelect(MemberSelectTree node, Void unused) {
            checkForThis(node, node.getIdentifier(), thisClass, state);
            // Don't examine this.foo or MyClass.this.foo
            ExpressionTree left = node.getExpression();
            if ((left instanceof IdentifierTree
                    && ((IdentifierTree) left).getName().contentEquals("this"))
                || (left instanceof MemberSelectTree
                    && ((MemberSelectTree) left).getIdentifier().contentEquals("this"))) {
              return null;
            }
            return super.visitMemberSelect(node, unused);
          }

          @Override
          public Void visitAssignment(AssignmentTree node, Void unused) {
            scan(node.getExpression(), null);
            // ignore references to 'this' in the LHS of assignments
            return null;
          }
        },
        null);
  }

  private void checkForThis(
      ExpressionTree node, Name identifier, ClassSymbol thisClass, VisitorState state) {
    if (identifier.contentEquals("this") && thisClass.equals(ASTHelpers.getSymbol(node).owner)) {
      state.reportMatch(describeMatch(node));
    }
  }
}
