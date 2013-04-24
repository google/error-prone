package com.google.errorprone.util;

import static org.junit.Assert.assertTrue;

import com.google.errorprone.Scanner;
import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.CompilerBasedTest;
import com.google.errorprone.matchers.Matcher;

import com.sun.source.tree.LiteralTree;
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
    assertCompiles(literalExpressionMatches(literalHasActualStartPosition(59)));
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
    assertCompiles(literalExpressionMatches(literalHasActualStartPosition(59)));
  }

  private Matcher<LiteralTree> literalHasActualStartPosition(final int startPosition) {
    return new Matcher<LiteralTree>() {
      @Override
      public boolean matches(LiteralTree tree, VisitorState state) {
        JCLiteral literal = (JCLiteral) tree;
        return ASTHelpers.getActualStartPosition(literal, state.getSourceCode()) == startPosition;
      }
    };
  }

  private Scanner literalExpressionMatches(final Matcher<LiteralTree> matcher) {
    return new Scanner() {
      @Override
      public Void visitLiteral(LiteralTree node, VisitorState state) {
        assertMatch(node, state, matcher);
        return super.visitLiteral(node, state);
      }

      private <T extends Tree> void assertMatch(T node, VisitorState visitorState,
          Matcher<T> matcher) {
        VisitorState state = visitorState.withPath(getCurrentPath());
        assertTrue(matcher.matches(node, state));
      }
    };
  }

}
