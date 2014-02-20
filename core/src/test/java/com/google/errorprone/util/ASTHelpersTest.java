package com.google.errorprone.util;

import static org.junit.Assert.assertTrue;

import com.google.errorprone.Scanner;
import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.CompilerBasedTest;
import com.google.errorprone.matchers.Matcher;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
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
        "}");
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
        "}");
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
    return new TestScanner() {
      @Override
      public Void visitLiteral(LiteralTree node, VisitorState state) {
        assertMatch(node, state, matcher);
        return super.visitLiteral(node, state);
      }
    };
  }

  @Test
  public void testGetReceiver() throws IOException {
    writeFile("A.java",
        "public class A { ",
        "  public B b;",
        "  public void foo() {}",
        "  public B bar() {",
        "    return null;",
        "  }",
        "}");
    writeFile("B.java",
        "public class B { ",
        "  public void foo() {}",
        "}");
    writeFile("C.java",
        "public class C { ",
        "  public void test() {",
        "     A a = new A();",
        "     a.foo();", // a
        "     a.b.foo();", // a.b
        "     a.bar().foo();", // a.bar()
        "  }",
        "}");
    assertCompiles(expressionStatementMatches("a.foo()", expressionHasReceiver("a")));
    assertCompiles(expressionStatementMatches("a.b.foo()", expressionHasReceiver("a.b")));
    assertCompiles(expressionStatementMatches("a.bar().foo()", expressionHasReceiver("a.bar()")));
  }

  private Matcher<ExpressionTree> expressionHasReceiver(final String expectedReceiver) {
    return new Matcher<ExpressionTree>() {
      @Override
      public boolean matches(ExpressionTree t, VisitorState state) {
        return ASTHelpers.getReceiver(t).toString().equals(expectedReceiver);
      }
    };
  }

  private Scanner expressionStatementMatches(final String expectedExpression,
      final Matcher<ExpressionTree> matcher) {
    return new TestScanner() {
      @Override
      public Void visitExpressionStatement(ExpressionStatementTree node, VisitorState state) {
        ExpressionTree expression = node.getExpression();
        if (expression.toString().equals(expectedExpression)) {
          assertMatch(node.getExpression(), state, matcher);
        }
        return super.visitExpressionStatement(node, state);
      }
    };
  }

  private static abstract class TestScanner extends Scanner {
    <T extends Tree> void assertMatch(T node, VisitorState visitorState,
        Matcher<T> matcher) {
      VisitorState state = visitorState.withPath(getCurrentPath());
      assertTrue(matcher.matches(node, state));
    }
  }

  @Test
  public void testAnnotationHelpers() throws IOException {
    writeFile("com/google/errorprone/util/InheritedAnnotation.java",
        "package com.google.errorprone.util;",
        "import java.lang.annotation.Inherited;",
        "@Inherited",
        "public @interface InheritedAnnotation {}");
    writeFile("B.java",
        "import com.google.errorprone.util.InheritedAnnotation;",
        "@InheritedAnnotation",
        "public class B {}");
    writeFile("C.java",
        "public class C extends B {}");

    assertCompiles(new TestScanner() {
      @Override
      public Void visitClass(ClassTree tree, VisitorState state) {
        if (tree.getSimpleName().toString().equals("C")) {
          assertMatch(tree, state, new Matcher<ClassTree>() {
            @Override
            public boolean matches(ClassTree t, VisitorState state) {
              return ASTHelpers.hasAnnotation(t, InheritedAnnotation.class);
            }
          });
        }
        return super.visitClass(tree, state);
      }
    });
  }
}
