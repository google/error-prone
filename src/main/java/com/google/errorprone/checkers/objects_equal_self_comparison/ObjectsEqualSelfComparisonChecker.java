// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.errorprone.checkers.objects_equal_self_comparison;

import static com.google.errorprone.fixes.SuggestedFix.prefixWith;
import static com.google.errorprone.fixes.SuggestedFix.replace;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.methodSelect;
import static com.google.errorprone.matchers.Matchers.sameArgument;
import static com.google.errorprone.matchers.Matchers.staticMethod;

import com.google.errorprone.ErrorCollectingTreeScanner;
import com.google.errorprone.VisitorState;
import com.google.errorprone.checkers.ErrorChecker;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Matcher;

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
public class ObjectsEqualSelfComparisonChecker extends ErrorChecker<MethodInvocationTree> {

  @SuppressWarnings({"unchecked"})
  @Override
  public Matcher<MethodInvocationTree> matcher() {
    return allOf(
        methodSelect(staticMethod("com.google.common.base.Objects", "equal")),
        sameArgument(0, 1));
  }

  @Override
  public AstError produceError(MethodInvocationTree methodInvocationTree, VisitorState state) {
    // If we don't find a good field to use, then just replace with "true"
    SuggestedFix fix = replace(getPosition(methodInvocationTree), "true");

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
          fix = prefixWith(getPosition(toReplace), declaration.getName().toString() + ".");
        }
      }
    }

    return new AstError(methodInvocationTree, "Objects.equal arguments must be different", fix);
  }

  public static class Scanner extends ErrorCollectingTreeScanner {
    private final ErrorChecker<MethodInvocationTree> checker =
        new ObjectsEqualSelfComparisonChecker();

    @Override
    public List<AstError> visitMethodInvocation(MethodInvocationTree node,
        VisitorState visitorState) {

      AstError error = checker.check(node, visitorState.withPath(getCurrentPath()));
      List<AstError> result = new ArrayList<AstError>();
      if (error != null) {
        result.add(error);
      }
      return result;
    }
  }
}
