/*
 * Copyright 2020 The Error Prone Authors.
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
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.isStatic;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;

/** Checks for static fields being assigned within constructors. */
@BugPattern(
    severity = WARNING,
    summary =
        "This assignment is to a static field. Mutating static state from a constructor is highly"
            + " error-prone.")
public final class StaticAssignmentInConstructor extends BugChecker implements MethodTreeMatcher {
  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    MethodSymbol methodSymbol = getSymbol(tree);
    if (!methodSymbol.isConstructor()) {
      return NO_MATCH;
    }
    new TreeScanner<Void, Void>() {
      @Override
      public Void visitClass(ClassTree classTree, Void unused) {
        return null;
      }

      @Override
      public Void visitMethod(MethodTree methodTree, Void unused) {
        return null;
      }

      @Override
      public Void visitLambdaExpression(LambdaExpressionTree lambdaExpressionTree, Void unused) {
        return null;
      }

      @Override
      public Void visitAssignment(AssignmentTree assignmentTree, Void unused) {
        Symbol symbol = getSymbol(assignmentTree.getVariable());
        if (symbol != null && isStatic(symbol) && shouldEmitFinding(assignmentTree)) {
          state.reportMatch(describeMatch(assignmentTree));
        }
        return super.visitAssignment(assignmentTree, null);
      }

      private boolean shouldEmitFinding(AssignmentTree assignmentTree) {
        if (!(assignmentTree.getExpression() instanceof IdentifierTree)) {
          return true;
        }
        IdentifierTree identifierTree = ((IdentifierTree) assignmentTree.getExpression());
        return !identifierTree.getName().contentEquals("this");
      }
    }.scan(tree.getBody(), null);
    return NO_MATCH;
  }
}
