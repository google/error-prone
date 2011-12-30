// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.errorprone.checkers.objects_equal_self_comparison;

import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.methodSelect;
import static com.google.errorprone.matchers.Matchers.sameArgument;
import static com.google.errorprone.matchers.Matchers.staticMethod;

import com.google.errorprone.RefactoringVisitorState;
import com.google.errorprone.VisitorState;
import com.google.errorprone.checkers.RefactoringMatcher;
import com.google.errorprone.fixes.SuggestedFix;

import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;

/**
 * @author alexeagle@google.com (Alex Eagle)
 */
public class ObjectsEqualSelfComparisonChecker extends RefactoringMatcher<MethodInvocationTree> {

  @SuppressWarnings({"unchecked"})
  @Override
  public boolean matches(MethodInvocationTree methodInvocationTree, VisitorState state) {
    return allOf(
        methodSelect(staticMethod("com.google.common.base.Objects", "equal")),
        sameArgument(0, 1))
        .matches(methodInvocationTree, state);
  }

  @Override
  public Refactor refactor(MethodInvocationTree methodInvocationTree, RefactoringVisitorState state) {
    // If we don't find a good field to use, then just replace with "true"
    SuggestedFix fix = new SuggestedFix().replace(methodInvocationTree, "true");

    JCExpression toReplace = (JCExpression) methodInvocationTree.getArguments().get(1);
    // Find containing block
    TreePath path = state.getPath();
    while(path.getLeaf().getKind() != Kind.BLOCK) {
      path = path.getParentPath();
    }
    JCBlock block = (JCBlock)path.getLeaf();
    for (JCStatement jcStatement : block.getStatements()) {
      if (jcStatement.getKind() == Kind.VARIABLE) {
        JCVariableDecl declaration = (JCVariableDecl) jcStatement;
        TypeSymbol variableTypeSymbol = declaration.getType().type.tsym;

        if (((JCIdent)toReplace).sym.isMemberOf(variableTypeSymbol, state.getTypes())) {
          fix = new SuggestedFix().prefixWith(toReplace, declaration.getName().toString() + ".");
        }
      }
    }

    return new Refactor(methodInvocationTree,
        "Objects.equal arguments must be different", fix);
  }

  public static class Scanner extends TreePathScanner<Void, RefactoringVisitorState> {
    private final RefactoringMatcher<MethodInvocationTree> checker =
        new ObjectsEqualSelfComparisonChecker();

    @Override
    public Void visitMethodInvocation(MethodInvocationTree node, RefactoringVisitorState visitorState) {
      RefactoringVisitorState state = visitorState.withPath(getCurrentPath());
      if (checker.matches(node, state)) {
        visitorState.getReporter().report(checker.refactor(node, state));
      }
      return null;
    }
  }
}
