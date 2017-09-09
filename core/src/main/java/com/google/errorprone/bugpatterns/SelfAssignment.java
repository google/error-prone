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
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.staticMethod;
import static com.sun.source.tree.Tree.Kind.CLASS;
import static com.sun.source.tree.Tree.Kind.IDENTIFIER;
import static com.sun.source.tree.Tree.Kind.MEMBER_SELECT;
import static com.sun.source.tree.Tree.Kind.METHOD;
import static com.sun.source.tree.Tree.Kind.METHOD_INVOCATION;
import static com.sun.source.tree.Tree.Kind.VARIABLE;
import static javax.lang.model.element.Modifier.STATIC;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.AssignmentTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.names.LevenshteinEditDistance;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import java.util.Objects;

/**
 * TODO(eaftan): Consider cases where the parent is not a statement or there is no parent?
 *
 * @author eaftan@google.com (Eddie Aftandilian)
 * @author scottjohnson@google.com (Scott Johnson)
 */
@BugPattern(
  name = "SelfAssignment",
  summary = "Variable assigned to itself",
  category = JDK,
  severity = ERROR,
  providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION
)
public class SelfAssignment extends BugChecker
    implements AssignmentTreeMatcher, VariableTreeMatcher {

  @Override
  public Description matchAssignment(AssignmentTree tree, VisitorState state) {
    ExpressionTree expression = stripCheckNotNull(tree.getExpression(), state);
    if (ASTHelpers.sameVariable(tree.getVariable(), expression)) {
      return describeForAssignment(tree, state);
    }
    return Description.NO_MATCH;
  }

  @Override
  public Description matchVariable(VariableTree tree, VisitorState state) {
    ExpressionTree initializer = stripCheckNotNull(tree.getInitializer(), state);
    Tree parent = state.getPath().getParentPath().getLeaf();

    // must be a static class variable with member select initializer
    if (initializer == null
        || initializer.getKind() != MEMBER_SELECT
        || parent.getKind() != CLASS
        || !tree.getModifiers().getFlags().contains(STATIC)) {
      return Description.NO_MATCH;
    }

    MemberSelectTree rhs = (MemberSelectTree) initializer;
    Symbol rhsClass = ASTHelpers.getSymbol(rhs.getExpression());
    Symbol lhsClass = ASTHelpers.getSymbol(parent);
    if (rhsClass != null
        && lhsClass != null
        && rhsClass.equals(lhsClass)
        && rhs.getIdentifier().contentEquals(tree.getName())) {
      return describeForVarDecl(tree, state);
    }
    return Description.NO_MATCH;
  }

  /**
   * If the given expression is a call to checkNotNull(x), returns x. Otherwise, returns the given
   * expression.
   *
   * <p>TODO(eaftan): Also match calls to Java 7's Objects.requireNonNull() method.
   */
  private ExpressionTree stripCheckNotNull(ExpressionTree expression, VisitorState state) {
    if (expression != null
        && expression.getKind() == METHOD_INVOCATION
        && staticMethod()
            .onClass("com.google.common.base.Preconditions")
            .named("checkNotNull")
            .matches(expression, state)) {
      return ((MethodInvocationTree) expression).getArguments().get(0);
    }
    return expression;
  }

  public Description describeForVarDecl(VariableTree tree, VisitorState state) {
    String varDeclStr = tree.toString();
    int equalsIndex = varDeclStr.indexOf('=');
    if (equalsIndex < 0) {
      throw new IllegalStateException(
          "Expected variable declaration to have an initializer: " + tree);
    }
    varDeclStr = varDeclStr.substring(0, equalsIndex - 1) + ";";

    // Delete the initializer but still declare the variable.
    return describeMatch(tree, SuggestedFix.replace(tree, varDeclStr));
  }

  /**
   * We expect that the lhs is a field and the rhs is an identifier, specifically a parameter to the
   * method. We base our suggested fixes on this expectation.
   *
   * <p>Case 1: If lhs is a field and rhs is an identifier, find a method parameter of the same type
   * and similar name and suggest it as the rhs. (Guess that they have misspelled the identifier.)
   *
   * <p>Case 2: If lhs is a field and rhs is not an identifier, find a method parameter of the same
   * type and similar name and suggest it as the rhs.
   *
   * <p>Case 3: If lhs is not a field and rhs is an identifier, find a class field of the same type
   * and similar name and suggest it as the lhs.
   *
   * <p>Case 4: Otherwise suggest deleting the assignment.
   */
  public Description describeForAssignment(AssignmentTree assignmentTree, VisitorState state) {

    // the statement that is the parent of the self-assignment expression
    Tree parent = state.getPath().getParentPath().getLeaf();

    // default fix is to delete assignment
    Fix fix = SuggestedFix.delete(parent);

    ExpressionTree lhs = assignmentTree.getVariable();
    ExpressionTree rhs = assignmentTree.getExpression();

    // if this is a method invocation, they must be calling checkNotNull()
    if (assignmentTree.getExpression().getKind() == METHOD_INVOCATION) {
      // change the default fix to be "checkNotNull(x)" instead of "x = checkNotNull(x)"
      fix = SuggestedFix.replace(assignmentTree, rhs.toString());
      // new rhs is first argument to checkNotNull()
      rhs = stripCheckNotNull(rhs, state);
    }

    if (lhs.getKind() == MEMBER_SELECT) {
      // find a method parameter of the same type and similar name and suggest it
      // as the rhs

      // rhs should be either identifier or field access
      assert (rhs.getKind() == IDENTIFIER || rhs.getKind() == MEMBER_SELECT);

      // get current name of rhs
      String rhsName = null;
      if (rhs.getKind() == IDENTIFIER) {
        rhsName = ((JCIdent) rhs).name.toString();
      } else if (rhs.getKind() == MEMBER_SELECT) {
        rhsName = ((JCFieldAccess) rhs).name.toString();
      }

      // find method parameters of the same type
      Type type = ((JCFieldAccess) lhs).type;
      TreePath path = state.getPath();
      while (path != null && path.getLeaf().getKind() != METHOD) {
        path = path.getParentPath();
      }
      JCMethodDecl method = (JCMethodDecl) path.getLeaf();
      int minEditDistance = Integer.MAX_VALUE;
      String replacement = null;
      for (JCVariableDecl var : method.params) {
        if (Objects.equals(var.type, type)) {
          int editDistance = LevenshteinEditDistance.getEditDistance(rhsName, var.name.toString());
          if (editDistance < minEditDistance) {
            // pick one with minimum edit distance
            minEditDistance = editDistance;
            replacement = var.name.toString();
          }
        }
      }
      if (replacement != null) {
        // suggest replacing rhs with the parameter
        fix = SuggestedFix.replace(rhs, replacement);
      }
    } else if (rhs.getKind() == IDENTIFIER) {
      // find a field of the same type and similar name and suggest it as the lhs

      // lhs should be identifier
      assert (lhs.getKind() == IDENTIFIER);

      // get current name of lhs
      String lhsName = ((JCIdent) rhs).name.toString();

      // find class instance fields of the same type
      Type type = ((JCIdent) lhs).type;
      TreePath path = state.getPath();
      while (path != null && !(path.getLeaf() instanceof JCClassDecl)) {
        path = path.getParentPath();
      }
      if (path == null) {
        throw new IllegalStateException("Expected to find an enclosing class declaration");
      }
      JCClassDecl klass = (JCClassDecl) path.getLeaf();
      int minEditDistance = Integer.MAX_VALUE;
      String replacement = null;
      for (JCTree member : klass.getMembers()) {
        if (member.getKind() == VARIABLE) {
          JCVariableDecl var = (JCVariableDecl) member;
          if (!Flags.isStatic(var.sym) && Objects.equals(var.type, type)) {
            int editDistance =
                LevenshteinEditDistance.getEditDistance(lhsName, var.name.toString());
            if (editDistance < minEditDistance) {
              // pick one with minimum edit distance
              minEditDistance = editDistance;
              replacement = var.name.toString();
            }
          }
        }
      }
      if (replacement != null) {
        // suggest replacing lhs with the field
        fix = SuggestedFix.replace(lhs, "this." + replacement);
      }
    }

    return describeMatch(assignmentTree, fix);
  }
}
