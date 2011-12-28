// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.errorprone.checkers.objects_equal_self_comparison;

import static com.google.errorprone.fixes.SuggestedFix.replace;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.methodSelect;
import static com.google.errorprone.matchers.Matchers.staticMethod;

import com.google.errorprone.ErrorCollectingTreeScanner;
import com.google.errorprone.VisitorState;
import com.google.errorprone.checkers.ErrorChecker;
import com.google.errorprone.matchers.Matcher;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.tree.JCTree.JCIdent;

import java.util.ArrayList;
import java.util.List;

/**
 * @author alexeagle@google.com (Alex Eagle)
 */
public class ObjectsEqualSelfComparisonChecker extends ErrorChecker<MethodInvocationTree> {

  @Override
  public Matcher<MethodInvocationTree> matcher() {
    return allOf(
        methodSelect(staticMethod("com.google.common.base.Objects", "equal")),
        new Matcher<MethodInvocationTree>() {
          @Override
          public boolean matches(MethodInvocationTree methodInvocationTree, VisitorState state) {
            List<? extends ExpressionTree> arguments = methodInvocationTree.getArguments();
            if (arguments.get(0).getKind() == Kind.IDENTIFIER &&
                arguments.get(1).getKind() == Kind.IDENTIFIER) {
              return ((JCIdent) arguments.get(0)).sym == ((JCIdent) arguments.get(1)).sym;
            }

            return false;
          }
        });
  }

  @Override
  public AstError produceError(MethodInvocationTree methodInvocationTree, VisitorState state) {
    return new AstError(methodInvocationTree, "Objects.equal arguments must be different",
        replace(getPosition(methodInvocationTree), "true"));
  }

  public static class Scanner extends ErrorCollectingTreeScanner {
    private final ErrorChecker<MethodInvocationTree> checker =
        new ObjectsEqualSelfComparisonChecker();

    @Override
    public List<AstError> visitMethodInvocation(MethodInvocationTree node,
        VisitorState visitorState) {

      AstError error = checker.check(node, visitorState);
      List<AstError> result = new ArrayList<AstError>();
      if (error != null) {
        result.add(error);
      }
      return result;
    }
  }
}
