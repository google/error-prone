/*
 * Copyright 2022 The Error Prone Authors.
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
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.isSubtype;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;
import javax.lang.model.element.Modifier;

/** Checks for static fields being assigned with {@code Throwable}. */
@BugPattern(
    severity = WARNING,
    summary =
        "Saving instances of Throwable in static fields is discouraged, prefer to create them"
            + " on-demand when an exception is thrown")
public final class StaticAssignmentOfThrowable extends BugChecker
    implements MethodTreeMatcher, VariableTreeMatcher {

  @Override
  public Description matchVariable(VariableTree variableTree, VisitorState state) {

    if (state.errorProneOptions().isTestOnlyTarget()
        || !variableTree.getModifiers().getFlags().contains(Modifier.STATIC)) {
      return NO_MATCH;
    }

    ExpressionTree initializer = variableTree.getInitializer();
    if (initializer == null) {
      return NO_MATCH;
    }

    Type throwableType = state.getSymtab().throwableType;
    Type variableType = getType(variableTree.getType());
    if (!isSubtype(variableType, throwableType, state)) {
      return NO_MATCH;
    }

    return describeMatch(variableTree);
  }

  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {

    if (state.errorProneOptions().isTestOnlyTarget()) {
      return NO_MATCH;
    }

    MethodSymbol methodSymbol = getSymbol(tree);
    if (methodSymbol == null) {
      return NO_MATCH;
    }
    if (methodSymbol.isConstructor()) {
      // To avoid duplicate/conflicting findings, this scenario delegated to
      // StaticAssignmentInConstructor
      return NO_MATCH;
    }

    buildTreeScanner(state).scan(tree.getBody(), null);
    return NO_MATCH;
  }

  /* Builds a {@code TreeScanner} that searches for assignments to static {@code Throwable} fields,
   * and reports matches. */
  private final TreeScanner<Void, Void> buildTreeScanner(VisitorState state) {

    Type throwableType = state.getSymtab().throwableType;

    return new TreeScanner<Void, Void>() {
      @Override
      public Void visitClass(ClassTree classTree, Void unused) {
        return null;
      }

      @Override
      public Void visitMethod(MethodTree methodTree, Void unused) {
        return null;
      }

      @Override
      public Void visitAssignment(AssignmentTree assignmentTree, Void unused) {
        Symbol variableSymbol = getSymbol(assignmentTree.getVariable());
        Type variableType = getType(assignmentTree.getVariable());
        if (variableSymbol != null
            && variableSymbol.isStatic()
            && isSubtype(variableType, throwableType, state)) {
          state.reportMatch(describeMatch(assignmentTree));
        }
        return super.visitAssignment(assignmentTree, null);
      }
    };
  }
}
