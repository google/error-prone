/*
 * Copyright 2011 Google Inc. All Rights Reserved.
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

import static com.google.errorprone.BugPattern.Category.GUAVA;
import static com.google.errorprone.BugPattern.MaturityLevel.EXPERIMENTAL;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.argument;
import static com.google.errorprone.matchers.Matchers.methodSelect;
import static com.google.errorprone.matchers.Matchers.staticMethod;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.DescribingMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.util.ASTHelpers;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree.JCExpression;

/**
 * Checks that the 1st argument to Preconditions.checkNotNull() isn't a primitive.
 * The primitive would be autoboxed to a non-null boxed type, and the check would trivially
 * pass.
 *
 * In our experience, most of these errors are from copied-and-pasted code and should
 * simply be removed.
 *
 * @author sjnickerson@google.com (Simon Nickerson)
 * @author eaftan@google.com (Eddie Aftandilian)
 */
@BugPattern(name = "PreconditionsCheckNotNullPrimitive",
    summary = "First argument to Preconditions.checkNotNull() is a primitive rather " +
    		"than an object reference",
    explanation =
        "Preconditions.checkNotNull() takes as an argument a reference that should be " +
        "non-null. Often a primitive is passed as the argument to check. The primitive " +
        "will be autoboxed into a boxed object, which is non-null, causing the check to " +
        "always pass without the condition being evaluated.\n" +
        "If the intent was to ensure that the primitive met some criterion (e.g., a boolean " +
        "that should be non-null), please use Precondtions.checkState() or " +
        "Preconditions.checkArgument() instead.",
    category = GUAVA, severity = ERROR, maturity = EXPERIMENTAL)
public class PreconditionsCheckNotNullPrimitive
    extends DescribingMatcher<MethodInvocationTree> {

  @SuppressWarnings("unchecked")
  @Override
  public boolean matches(MethodInvocationTree methodInvocationTree, VisitorState state) {
    return allOf(
        methodSelect(staticMethod(
            "com.google.common.base.Preconditions", "checkNotNull")),
        argument(0, Matchers.<ExpressionTree>isPrimitiveType()))
        .matches(methodInvocationTree, state);
  }

  /**
   * If the call to Preconditions.checkNotNull is part of an expression (assignment, return, etc.),
   * we substitute the argument for the method call. E.g.:
   *   bar = Preconditions.checkNotNull(foo); ==> bar = foo;
   * If the argument to Preconditions.checkNotNull is a comparison using == or != and one of the
   * operands is null, we call checkNotNull on the non-null operand. E.g.:
   *   checkNotNull(a == null); ==> checkNotNull(a);
   * If the argument is a method call or binary tree and its return type is boolean, change it to a
   * checkArgument/checkState. E.g.:
   *   Preconditions.checkNotNull(foo.hasFoo()) ==> Preconditions.checkArgument(foo.hasFoo())
   * Otherwise, delete the checkNotNull call. E.g.:
   *   Preconditions.checkNotNull(foo); ==> [delete the line]
   */
  @Override
  public Description describe(MethodInvocationTree methodInvocationTree, VisitorState state) {
    ExpressionTree arg1 = methodInvocationTree.getArguments().get(0);
    Tree parent = state.getPath().getParentPath().getLeaf();

    // Assignment, return, etc.
    if (parent.getKind() != Kind.EXPRESSION_STATEMENT) {
      return new Description(arg1, diagnosticMessage,
          new SuggestedFix().replace(methodInvocationTree, arg1.toString()));
    }

    // Comparison to null
    if (arg1.getKind() == Kind.EQUAL_TO || arg1.getKind() == Kind.NOT_EQUAL_TO) {
      BinaryTree binaryExpr = (BinaryTree) arg1;
      if (binaryExpr.getLeftOperand().getKind() == Kind.NULL_LITERAL) {
        return new Description(arg1, diagnosticMessage,
            new SuggestedFix().replace(arg1, binaryExpr.getRightOperand().toString()));
      }
      if (binaryExpr.getRightOperand().getKind() == Kind.NULL_LITERAL) {
        return new Description(arg1, diagnosticMessage,
            new SuggestedFix().replace(arg1, binaryExpr.getLeftOperand().toString()));
      }
    }

    if ((arg1 instanceof BinaryTree || arg1.getKind() == Kind.METHOD_INVOCATION) &&
        ((JCExpression) arg1).type == state.getSymtab().booleanType) {
      return new Description(arg1, diagnosticMessage,
          createCheckArgumentOrStateCall(methodInvocationTree, state, arg1));
    }

    return new Description(arg1, diagnosticMessage, new SuggestedFix().delete(parent));
  }

  /**
   * Creates a SuggestedFix that replaces the checkNotNull call with a checkArgument or checkState
   * call.
   */
  private SuggestedFix createCheckArgumentOrStateCall(MethodInvocationTree methodInvocationTree,
      VisitorState state, ExpressionTree arg1) {
    SuggestedFix fix = new SuggestedFix();
    String replacementMethod = "checkState";
    if (arg1.getKind() == Kind.METHOD_INVOCATION && isMethodParameter(
        state.getPath(), (MethodInvocationTree) arg1)) {
      replacementMethod = "checkArgument";
    }

    StringBuilder replacement = new StringBuilder();

    // Was the original call to Preconditions.checkNotNull a static import or not?
    if (methodInvocationTree.getMethodSelect().getKind() == Kind.IDENTIFIER) {
      replacement.append(replacementMethod + "(");
      fix.addStaticImport("com.google.common.base.Preconditions." + replacementMethod);
    } else {
      replacement.append("Preconditions." + replacementMethod + "(");
    }

    // Create argument list.
    for (ExpressionTree arg : methodInvocationTree.getArguments()) {
      replacement.append(arg.toString());
      replacement.append(", ");
    }
    replacement.delete(replacement.length() - 2, replacement.length());
    replacement.append(")");
    fix.replace(methodInvocationTree, replacement.toString());
    return fix;
  }

  /**
   * Determines whether the root identifier of the tree node is a parameter to the enclosing
   * method.
   *
   * TODO(eaftan): Extract this to ASTHelpers.
   *
   * @param path the path to the current tree node
   * @param tree the node to compare against the parameters
   * @return whether the argument is a parameter to the enclosing method
   */
  private static boolean isMethodParameter(TreePath path, MethodInvocationTree tree) {

    // Extract the identifier from the method select.
    ExpressionTree expr = tree.getMethodSelect();
    while (expr.getKind() == Kind.MEMBER_SELECT) {
      expr = ((MemberSelectTree) expr).getExpression();
    }
    if (expr.getKind() != Kind.IDENTIFIER) {
      throw new IllegalStateException("Expected an identifier");
    }
    Symbol sym = ASTHelpers.getSymbol(expr);

    if (sym.isLocal()) {
      // Check against parameters of enclosing method declaration.
      while (path != null && !(path.getLeaf() instanceof MethodTree)) {
        path = path.getParentPath();
      }
      if (path == null) {
        throw new IllegalStateException("Should have an enclosing method declaration");
      }
      MethodTree methodDecl = (MethodTree) path.getLeaf();
      for (VariableTree param : methodDecl.getParameters()) {
        if (ASTHelpers.getSymbol(param) == sym) {
          return true;
        }
      }
    }
    return false;
  }

  public static class Scanner extends com.google.errorprone.Scanner {
    public DescribingMatcher<MethodInvocationTree> matcher =
        new PreconditionsCheckNotNullPrimitive();

    @Override
    public Void visitMethodInvocation(MethodInvocationTree node, VisitorState visitorState) {
      evaluateMatch(node, visitorState, matcher);
      return super.visitMethodInvocation(node, visitorState);
    }
  }

}
