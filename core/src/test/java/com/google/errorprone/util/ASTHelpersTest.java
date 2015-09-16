/*
 * Copyright 2013 Google Inc. All Rights Reserved.
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

package com.google.errorprone.util;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.common.base.Joiner;
import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.CompilerBasedAbstractTest;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.scanner.Scanner;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.TypeVar;
import com.sun.tools.javac.tree.JCTree.JCLiteral;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.List;

@RunWith(JUnit4.class)
public class ASTHelpersTest extends CompilerBasedAbstractTest {

  // For tests that expect a specific offset in the file, we test with both Windows and UNIX
  // line separators, but we hardcode the line separator in the tests to ensure the tests are
  // hermetic and do not depend on the platform on which they are run.
  private static final Joiner UNIX_LINE_JOINER = Joiner.on("\n");
  private static final Joiner WINDOWS_LINE_JOINER = Joiner.on("\r\n");

  final List<TestScanner> tests = new ArrayList<>();

  @After
  public void tearDown() {
    for (TestScanner test : tests) {
      test.verifyAssertionsComplete();
    }
  }

  @Test
  public void testGetActualStartPositionUnix() {
    String fileContent = UNIX_LINE_JOINER.join(
        "public class A { ",
        "  public void foo() {",
        "    int i;",
        "    i = -1;",
        "  }",
        "}");
    writeFile("A.java", fileContent);
    assertCompiles(literalExpressionMatches(literalHasActualStartPosition(59)));
  }

  @Test
  public void testGetActualStartPositionWindows() {
    String fileContent = WINDOWS_LINE_JOINER.join(
        "public class A { ",
        "  public void foo() {",
        "    int i;",
        "    i = -1;",
        "  }",
        "}");
    writeFile("A.java", fileContent);
    assertCompiles(literalExpressionMatches(literalHasActualStartPosition(62)));
  }

  @Test
  public void testGetActualStartPositionWithWhitespaceUnix() {
    String fileContent = UNIX_LINE_JOINER.join(
        "public class A { ",
        "  public void foo() {",
        "    int i;",
        "    i = -     1;",
        "  }",
        "}");
    writeFile("A.java", fileContent);
    assertCompiles(literalExpressionMatches(literalHasActualStartPosition(59)));
  }

  @Test
  public void testGetActualStartPositionWithWhitespaceWindows() {
    String fileContent = WINDOWS_LINE_JOINER.join(
        "public class A { ",
        "  public void foo() {",
        "    int i;",
        "    i = -     1;",
        "  }",
        "}");
    writeFile("A.java", fileContent);
    assertCompiles(literalExpressionMatches(literalHasActualStartPosition(62)));
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
    TestScanner scanner = new TestScanner() {
      @Override
      public Void visitLiteral(LiteralTree node, VisitorState state) {
        assertMatch(node, state, matcher);
        setAssertionsComplete();
        return super.visitLiteral(node, state);
      }
    };
    tests.add(scanner);
    return scanner;
  }

  @Test
  public void testGetReceiver() {
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
          setAssertionsComplete();
        }
        return super.visitExpressionStatement(node, state);
      }
    };
  }

  @Test
  public void testAnnotationHelpers() {
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

    TestScanner scanner = new TestScanner() {
      @Override
      public Void visitClass(ClassTree tree, VisitorState state) {
        if (tree.getSimpleName().toString().equals("C")) {
          assertMatch(tree, state, new Matcher<ClassTree>() {
            @Override
            public boolean matches(ClassTree t, VisitorState state) {
              return ASTHelpers.hasAnnotation(t, InheritedAnnotation.class);
            }
          });
          setAssertionsComplete();
        }
        return super.visitClass(tree, state);
      }
    };
    tests.add(scanner);
    assertCompiles(scanner);
  }

  @Test
  public void testGetTypeOnNestedAnnotationType() {
    writeFile("A.java",
        "public class A { ",
        "  @B.MyAnnotation",
        "  public void bar() {}",
        "}");
    writeFile("B.java",
        "public class B { ",
        "  @interface MyAnnotation {}",
        "}");
    TestScanner scanner = new TestScanner() {
      @Override
      public Void visitAnnotation(AnnotationTree tree, VisitorState state) {
        setAssertionsComplete();
        assertEquals("B.MyAnnotation", ASTHelpers.getType(tree.getAnnotationType()).toString());
        return super.visitAnnotation(tree, state);
      }
    };
    tests.add(scanner);
    assertCompiles(scanner);
  }

  @Test
  public void testGetTypeOnNestedClassType() {
    writeFile("A.java",
        "public class A { ",
        "  public void bar() {",
        "    B.C foo;",
        "  }",
        "}");
    writeFile("B.java",
        "public class B { ",
        "  public static class C {}",
        "}");
    TestScanner scanner = new TestScanner() {
      @Override
      public Void visitVariable(VariableTree tree, VisitorState state) {
        setAssertionsComplete();
        assertEquals("B.C", ASTHelpers.getType(tree.getType()).toString());
        return super.visitVariable(tree, state);
      }
    };
    tests.add(scanner);
    assertCompiles(scanner);
  }


  /* Tests for ASTHelpers#getUpperBound */

  private TestScanner getUpperBoundScanner(final String expectedBound) {
    return new TestScanner() {
      @Override
      public Void visitVariable(VariableTree tree, VisitorState state) {
        setAssertionsComplete();
        Type varType = ASTHelpers.getType(tree.getType());
        assertThat(
                ASTHelpers.getUpperBound(varType.getTypeArguments().get(0), state.getTypes())
                    .toString())
            .isEqualTo(expectedBound);
        return super.visitVariable(tree, state);
      }
    };
  }

  @Test
  public void testGetUpperBoundConcreteType() {
    writeFile(
        "A.java",
        "import java.lang.Number;",
        "import java.util.List;",
        "public class A {",
        "  public List<Number> myList;",
        "}");
    TestScanner scanner = getUpperBoundScanner("java.lang.Number");
    tests.add(scanner);
    assertCompiles(scanner);
  }

  @Test
  public void testGetUpperBoundUpperBoundedWildcard() {
    writeFile(
        "A.java",
        "import java.lang.Number;",
        "import java.util.List;",
        "public class A {",
        "  public List<? extends Number> myList;",
        "}");
    TestScanner scanner = getUpperBoundScanner("java.lang.Number");
    tests.add(scanner);
    assertCompiles(scanner);
  }

  @Test
  public void testGetUpperBoundUnboundedWildcard() {
    writeFile(
        "A.java", "import java.util.List;", "public class A {", "  public List<?> myList;", "}");
    TestScanner scanner = getUpperBoundScanner("java.lang.Object");
    tests.add(scanner);
    assertCompiles(scanner);
  }

  @Test
  public void testGetUpperBoundLowerBoundedWildcard() {
    writeFile(
        "A.java",
        "import java.lang.Number;",
        "import java.util.List;",
        "public class A {",
        "  public List<? super Number> myList;",
        "}");
    TestScanner scanner = getUpperBoundScanner("java.lang.Object");
    tests.add(scanner);
    assertCompiles(scanner);
  }

  @Test
  public void testGetUpperBoundTypeVariable() {
    writeFile(
        "A.java", "import java.util.List;", "public class A<T> {", "  public List<T> myList;", "}");
    TestScanner scanner = getUpperBoundScanner("java.lang.Object");
    tests.add(scanner);
    assertCompiles(scanner);
  }

  @Test
  public void testGetUpperBoundCapturedTypeVariable() {
    writeFile(
        "A.java",
        "import java.lang.Number;",
        "import java.util.List;",
        "public class A {",
        "  public void doSomething(List<? extends Number> list) {",
        "    list.get(0);",
        "  }",
        "}");
    TestScanner scanner =
        new TestScanner() {
          @Override
          public Void visitMethodInvocation(MethodInvocationTree tree, VisitorState state) {
            if (!"super()".equals(tree.toString())) {  // ignore synthetic super call
              setAssertionsComplete();
              Type type = ASTHelpers.getType(tree);
              assertThat(type instanceof TypeVar).isTrue();
              assertThat(((TypeVar) type).isCaptured()).isTrue();
              assertThat(ASTHelpers.getUpperBound(type, state.getTypes()).toString())
                  .isEqualTo("java.lang.Number");
            }
            return super.visitMethodInvocation(tree, state);
          }
        };
    tests.add(scanner);
    assertCompiles(scanner);
  }

  /* Test infrastructure */

  private static abstract class TestScanner extends Scanner {
    private boolean assertionsComplete = false;

    /**
     * Subclasses of {@link TestScanner} are expected to call this method within their overridden
     * visitXYZ() method in order to verify that the method has run at least once.
     */
    protected void setAssertionsComplete() {
      this.assertionsComplete = true;
    }

    <T extends Tree> void assertMatch(T node, VisitorState visitorState,
        Matcher<T> matcher) {
      VisitorState state = visitorState.withPath(getCurrentPath());
      assertTrue(matcher.matches(node, state));
    }

    public void verifyAssertionsComplete() {
      assertTrue("Expected the visitor to call setAssertionsComplete().", assertionsComplete);
    }
  }
}
