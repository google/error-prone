package com.google.errorprone.util;

import static org.junit.Assert.assertTrue;

import com.google.errorprone.Scanner;
import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.CompilerBasedTest;
import com.google.errorprone.matchers.Matcher;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.tree.JCTree.JCLiteral;

import org.junit.Test;

import java.io.IOException;

public class ASTHelpersTest extends CompilerBasedTest {

  @Test
  public void testGetActualStartPosition() throws IOException {
    writeFile("A.java",
        "public class A { ",
        "  public void foo() {",
        "    int i;",
        "    i = -1;",
        "  }",
        "}"
    );
    assertCompiles(assignmentExpressionMatches(true, literalHasActualStartPosition(59)));
  }

  @Test
  public void testGetActualStartPositionWithWhitespace() throws IOException {
    writeFile("A.java",
        "public class A { ",
        "  public void foo() {",
        "    int i;",
        "    i = -     1;",
        "  }",
        "}"
    );
    assertCompiles(assignmentExpressionMatches(true, literalHasActualStartPosition(59)));
  }


  private Matcher<ExpressionTree> literalHasActualStartPosition(final int startPosition) {
    return new Matcher<ExpressionTree>() {
      @Override
      public boolean matches(ExpressionTree tree, VisitorState state) {
        if (!(tree instanceof JCLiteral)) {
          return false;
        }
        JCLiteral literal = (JCLiteral) tree;
        return ASTHelpers.getActualStartPosition(literal, state.getSourceCode()) == startPosition;
      }
    };
  }

  private Scanner assignmentExpressionMatches(final boolean shouldMatch,
      final Matcher<ExpressionTree> matcher) {
    return new Scanner() {
      @Override
      public Void visitAssignment(AssignmentTree node, VisitorState state) {
        assertMatch(shouldMatch, node.getExpression(), state, matcher);
        return super.visitAssignment(node, state);
      }

      private <T extends Tree> void assertMatch(boolean shouldMatch, T node,
          VisitorState visitorState, Matcher<T> matcher) {
        VisitorState state = visitorState.withPath(getCurrentPath());
        assertTrue(!shouldMatch ^ matcher.matches(node, state));
      }
    };
  }

}
