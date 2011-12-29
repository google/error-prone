// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.errorprone.checkers.objects_equal_self_comparison;

import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.methodSelect;
import static com.google.errorprone.matchers.Matchers.sameArgument;
import static com.google.errorprone.matchers.Matchers.staticMethod;

import com.google.errorprone.ErrorCollectingTreeScanner;
import com.google.errorprone.VisitorState;
import com.google.errorprone.checkers.DescribingMatcher;
import com.google.errorprone.fixes.SuggestedFix;

import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;

import java.util.ArrayList;
import java.util.List;

/**
 * @author alexeagle@google.com (Alex Eagle)
 */
public class ObjectsEqualSelfComparisonChecker extends DescribingMatcher<MethodInvocationTree> {

  @SuppressWarnings({"unchecked"})
  @Override
  public boolean matches(MethodInvocationTree methodInvocationTree, VisitorState state) {
    return allOf(
        methodSelect(staticMethod("com.google.common.base.Objects", "equal")),
        sameArgument(0, 1))
        .matches(methodInvocationTree, state);
  }

  @Override
  public MatchDescription describe(MethodInvocationTree methodInvocationTree, VisitorState state) {
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

    return new MatchDescription(methodInvocationTree,
        "Objects.equal arguments must be different", fix);
  }

  public static class Scanner extends ErrorCollectingTreeScanner {
    private final DescribingMatcher<MethodInvocationTree> checker =
        new ObjectsEqualSelfComparisonChecker();

    @Override
    public List<MatchDescription> visitMethodInvocation(MethodInvocationTree node,
        VisitorState visitorState) {

      VisitorState state = visitorState.withPath(getCurrentPath());
      List<MatchDescription> result = new ArrayList<MatchDescription>();
      if (checker.matches(node, state)) {
        result.add(checker.describe(node, state));
      }
      return result;
    }
  }
}
