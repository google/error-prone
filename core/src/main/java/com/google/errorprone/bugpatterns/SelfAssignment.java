/*
 * Copyright 2012 The Error Prone Authors.
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
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.staticMethod;
import static com.sun.source.tree.Tree.Kind.CLASS;
import static javax.lang.model.element.Modifier.STATIC;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.AssignmentTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import org.jspecify.annotations.Nullable;

/**
 * TODO(eaftan): Consider cases where the parent is not a statement or there is no parent?
 *
 * @author eaftan@google.com (Eddie Aftandilian)
 * @author scottjohnson@google.com (Scott Johnson)
 */
@BugPattern(summary = "Variable assigned to itself", severity = ERROR)
public class SelfAssignment extends BugChecker
    implements AssignmentTreeMatcher, VariableTreeMatcher {
  private static final Matcher<MethodInvocationTree> NON_NULL_MATCHER =
      anyOf(
          staticMethod().onClass("java.util.Objects").named("requireNonNull"),
          staticMethod().onClass("com.google.common.base.Preconditions").named("checkNotNull"),
          staticMethod()
              .onClass("com.google.common.time.Durations")
              .namedAnyOf("checkNotNegative", "checkPositive"),
          staticMethod()
              .onClass("com.google.protobuf.util.Durations")
              .namedAnyOf("checkNotNegative", "checkPositive", "checkValid"),
          staticMethod().onClass("com.google.protobuf.util.Timestamps").named("checkValid"));

  @Override
  public Description matchAssignment(AssignmentTree tree, VisitorState state) {
    // TODO(cushon): consider handling assignment expressions too, i.e. `x = y = x`
    ExpressionTree expression = skipCast(stripNullCheck(tree.getExpression(), state));
    if (ASTHelpers.sameVariable(tree.getVariable(), expression)) {
      return describeForAssignment(tree, state);
    }
    return Description.NO_MATCH;
  }

  @Override
  public Description matchVariable(VariableTree tree, VisitorState state) {
    ExpressionTree initializer = stripNullCheck(tree.getInitializer(), state);
    Tree parent = state.getPath().getParentPath().getLeaf();

    // must be a static class variable with member select initializer
    if (initializer == null
        || !(initializer instanceof MemberSelectTree rhs)
        || parent.getKind() != CLASS
        || !tree.getModifiers().getFlags().contains(STATIC)) {
      return Description.NO_MATCH;
    }

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

  private static ExpressionTree skipCast(ExpressionTree expression) {
    return new SimpleTreeVisitor<ExpressionTree, Void>() {
      @Override
      public ExpressionTree visitParenthesized(ParenthesizedTree node, Void unused) {
        return node.getExpression().accept(this, null);
      }

      @Override
      public ExpressionTree visitTypeCast(TypeCastTree node, Void unused) {
        return node.getExpression().accept(this, null);
      }

      @Override
      protected @Nullable ExpressionTree defaultAction(Tree node, Void unused) {
        return node instanceof ExpressionTree expressionTree ? expressionTree : null;
      }
    }.visit(expression, null);
  }

  /**
   * If the given expression is a call to a method checking the nullity of its first parameter, and
   * otherwise returns that parameter.
   */
  private static ExpressionTree stripNullCheck(ExpressionTree expression, VisitorState state) {
    if (expression != null && expression instanceof MethodInvocationTree methodInvocation) {
      if (NON_NULL_MATCHER.matches(methodInvocation, state)) {
        return methodInvocation.getArguments().getFirst();
      }
    }
    return expression;
  }

  public Description describeForVarDecl(VariableTree tree, VisitorState state) {
    String varDeclStr = state.getSourceForNode(tree);
    int equalsIndex = varDeclStr.indexOf('=');
    if (equalsIndex < 0) {
      throw new IllegalStateException(
          "Expected variable declaration to have an initializer: " + state.getSourceForNode(tree));
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
    if (assignmentTree.getExpression() instanceof MethodInvocationTree) {
      // change the default fix to be "checkNotNull(x)" instead of "x = checkNotNull(x)"
      fix = SuggestedFix.replace(assignmentTree, state.getSourceForNode(rhs));
      // new rhs is first argument to checkNotNull()
      rhs = stripNullCheck(rhs, state);
    }
    rhs = skipCast(rhs);

    ImmutableList<Fix> exploratoryFieldFixes = ImmutableList.of();
    if (lhs instanceof MemberSelectTree) {
      // find a method parameter of the same type and similar name and suggest it
      // as the rhs

      // rhs should be either identifier or field access
      Preconditions.checkState(rhs instanceof IdentifierTree || rhs instanceof MemberSelectTree);

      Type rhsType = ASTHelpers.getType(rhs);
      exploratoryFieldFixes =
          ReplacementVariableFinder.fixesByReplacingExpressionWithMethodParameter(
              rhs, varDecl -> ASTHelpers.isSameType(rhsType, varDecl.type, state), state);
    } else if (rhs instanceof IdentifierTree) {
      // find a field of the same type and similar name and suggest it as the lhs
      // lhs should be identifier
      Preconditions.checkState(lhs instanceof IdentifierTree);

      Type lhsType = ASTHelpers.getType(lhs);
      exploratoryFieldFixes =
          ReplacementVariableFinder.fixesByReplacingExpressionWithLocallyDeclaredField(
              lhs,
              var ->
                  !Flags.isStatic(var.sym)
                      && (var.sym.flags() & Flags.FINAL) == 0
                      && ASTHelpers.isSameType(lhsType, var.type, state),
              state);
    }

    if (exploratoryFieldFixes.isEmpty()) {
      return describeMatch(assignmentTree, fix);
    }

    return buildDescription(assignmentTree).addAllFixes(exploratoryFieldFixes).build();
  }
}
