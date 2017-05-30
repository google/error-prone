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
import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.common.base.Joiner;
import com.google.common.base.Verify;
import com.google.common.collect.Iterables;
import com.google.common.io.ByteStreams;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.Category;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.matchers.CompilerBasedAbstractTest;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.scanner.Scanner;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.TypeVar;
import com.sun.tools.javac.parser.Tokens.Comment;
import com.sun.tools.javac.tree.JCTree.JCLiteral;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link ASTHelpers}. */
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
  public void testGetStartPositionUnix() {
    String fileContent =
        UNIX_LINE_JOINER.join(
            "public class A { ", "  public void foo() {", "    int i;", "    i = -1;", "  }", "}");
    writeFile("A.java", fileContent);
    assertCompiles(literalExpressionMatches(literalHasStartPosition(59)));
  }

  @Test
  public void testGetStartPositionWindows() {
    String fileContent =
        WINDOWS_LINE_JOINER.join(
            "public class A { ", "  public void foo() {", "    int i;", "    i = -1;", "  }", "}");
    writeFile("A.java", fileContent);
    assertCompiles(literalExpressionMatches(literalHasStartPosition(62)));
  }

  @Test
  public void testGetStartPositionWithWhitespaceUnix() {
    String fileContent =
        UNIX_LINE_JOINER.join(
            "public class A { ",
            "  public void foo() {",
            "    int i;",
            "    i = -     1;",
            "  }",
            "}");
    writeFile("A.java", fileContent);
    assertCompiles(literalExpressionMatches(literalHasStartPosition(59)));
  }

  @Test
  public void testGetStartPositionWithWhitespaceWindows() {
    String fileContent =
        WINDOWS_LINE_JOINER.join(
            "public class A { ",
            "  public void foo() {",
            "    int i;",
            "    i = -     1;",
            "  }",
            "}");
    writeFile("A.java", fileContent);
    assertCompiles(literalExpressionMatches(literalHasStartPosition(62)));
  }

  private Matcher<LiteralTree> literalHasStartPosition(final int startPosition) {
    return new Matcher<LiteralTree>() {
      @Override
      public boolean matches(LiteralTree tree, VisitorState state) {
        JCLiteral literal = (JCLiteral) tree;
        return literal.getStartPosition() == startPosition;
      }
    };
  }

  private Scanner literalExpressionMatches(final Matcher<LiteralTree> matcher) {
    TestScanner scanner =
        new TestScanner() {
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
    writeFile(
        "A.java",
        "package p;",
        "public class A { ",
        "  public B b;",
        "  public void foo() {}",
        "  public B bar() {",
        "    return null;",
        "  }",
        "}");
    writeFile(
        "B.java",
        "package p;",
        "public class B { ",
        "  public static void bar() {}",
        "  public void foo() {}",
        "}");
    writeFile(
        "C.java",
        "package p;",
        "import static p.B.bar;",
        "public class C { ",
        "  public static void foo() {}",
        "  public void test() {",
        "     A a = new A();",
        "     a.foo();", // a
        "     a.b.foo();", // a.b
        "     a.bar().foo();", // a.bar()
        "     this.test();", // this
        "     test();", // null
        "     C.foo();", // C
        "     foo();", // null
        "     C c = new C();",
        "     c.foo();", // c
        "     bar();", // null
        "  }",
        "}");
    assertCompiles(expressionStatementMatches("a.foo()", expressionHasReceiverAndType("a", "p.A")));
    assertCompiles(
        expressionStatementMatches("a.b.foo()", expressionHasReceiverAndType("a.b", "p.B")));
    assertCompiles(
        expressionStatementMatches(
            "a.bar().foo()", expressionHasReceiverAndType("a.bar()", "p.B")));
    assertCompiles(
        expressionStatementMatches("this.test()", expressionHasReceiverAndType("this", "p.C")));
    assertCompiles(expressionStatementMatches("test()", expressionHasReceiverAndType(null, "p.C")));
    assertCompiles(expressionStatementMatches("C.foo()", expressionHasReceiverAndType("C", "p.C")));
    assertCompiles(expressionStatementMatches("foo()", expressionHasReceiverAndType(null, "p.C")));
    assertCompiles(expressionStatementMatches("c.foo()", expressionHasReceiverAndType("c", "p.C")));
    assertCompiles(expressionStatementMatches("bar()", expressionHasReceiverAndType(null, "p.B")));
  }

  private Matcher<ExpressionTree> expressionHasReceiverAndType(
      final String expectedReceiver, final String expectedType) {
    return Matchers.allOf(
        new Matcher<ExpressionTree>() {
          @Override
          public boolean matches(ExpressionTree t, VisitorState state) {
            ExpressionTree receiver = ASTHelpers.getReceiver(t);
            return expectedReceiver != null
                ? receiver.toString().equals(expectedReceiver)
                : receiver == null;
          }
        },
        new Matcher<ExpressionTree>() {
          @Override
          public boolean matches(ExpressionTree t, VisitorState state) {
            Type type = ASTHelpers.getReceiverType(t);
            return state.getTypeFromString(expectedType).equals(type);
          }
        });
  }

  private Scanner expressionStatementMatches(
      final String expectedExpression, final Matcher<ExpressionTree> matcher) {
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
    writeFile(
        "com/google/errorprone/util/InheritedAnnotation.java",
        "package com.google.errorprone.util;",
        "import java.lang.annotation.Inherited;",
        "@Inherited",
        "public @interface InheritedAnnotation {}");
    writeFile(
        "B.java",
        "import com.google.errorprone.util.InheritedAnnotation;",
        "@InheritedAnnotation",
        "public class B {}");
    writeFile("C.java", "public class C extends B {}");

    TestScanner scanner =
        new TestScanner() {
          @Override
          public Void visitClass(ClassTree tree, VisitorState state) {
            if (tree.getSimpleName().contentEquals("C")) {
              assertMatch(
                  tree,
                  state,
                  new Matcher<ClassTree>() {
                    @Override
                    public boolean matches(ClassTree t, VisitorState state) {
                      return ASTHelpers.hasAnnotation(t, InheritedAnnotation.class, state);
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

  // verify that hasAnnotation(Symbol, String, VisitorState) uses binary names for inner classes
  @Test
  public void testInnerAnnotationType() {
    writeFile(
        "test/Lib.java",
        "package test;",
        "public class Lib {",
        "  public @interface MyAnnotation {}",
        "}");
    writeFile(
        "test/Test.java",
        "package test;",
        "import test.Lib.MyAnnotation;",
        "@MyAnnotation",
        "public class Test {}");

    TestScanner scanner =
        new TestScanner() {
          @Override
          public Void visitClass(ClassTree tree, VisitorState state) {
            if (tree.getSimpleName().contentEquals("Test")) {
              assertMatch(
                  tree,
                  state,
                  new Matcher<ClassTree>() {
                    @Override
                    public boolean matches(ClassTree t, VisitorState state) {
                      return ASTHelpers.hasAnnotation(
                          ASTHelpers.getSymbol(t), "test.Lib$MyAnnotation", state);
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

  /* Tests for ASTHelpers#getType */

  @Test
  public void testGetTypeOnNestedAnnotationType() {
    writeFile("A.java", "public class A { ", "  @B.MyAnnotation", "  public void bar() {}", "}");
    writeFile("B.java", "public class B { ", "  @interface MyAnnotation {}", "}");
    TestScanner scanner =
        new TestScanner() {
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
    writeFile("A.java", "public class A { ", "  public void bar() {", "    B.C foo;", "  }", "}");
    writeFile("B.java", "public class B { ", "  public static class C {}", "}");
    TestScanner scanner =
        new TestScanner() {
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

  @Test
  public void testGetTypeOnParameterizedType() {
    writeFile(
        "Pair.java", "public class Pair<A, B> { ", "  public A first;", "  public B second;", "}");
    writeFile(
        "Test.java",
        "public class Test {",
        "  public Integer doSomething(Pair<Integer, String> pair) {",
        "    return pair.first;",
        "  }",
        "}");
    TestScanner scanner =
        new TestScanner() {
          @Override
          public Void visitReturn(ReturnTree tree, VisitorState state) {
            setAssertionsComplete();
            assertThat(ASTHelpers.getType(tree.getExpression()).toString())
                .isEqualTo("java.lang.Integer");
            return super.visitReturn(tree, state);
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
            if (!"super()".equals(tree.toString())) { // ignore synthetic super call
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

  @Test
  public void testCommentTokens() {
    writeFile(
        "A.java",
        "public class A {",
        "  Runnable theRunnable = new Runnable() {",
        "    /**",
        "     * foo",
        "     */",
        "    public void run() {",
        "      /* bar1 */",
        "      /* bar2 */",
        "      System.err.println(\"Hi\");",
        "    }",
        "    // baz number 1",
        "    // baz number 2",
        "  };",
        "}");
    TestScanner scanner =
        new TestScanner() {
          @Override
          public Void visitNewClass(NewClassTree tree, VisitorState state) {
            setAssertionsComplete();
            List<String> comments = new ArrayList<>();
            for (ErrorProneToken t : state.getTokensForNode(tree)) {
              if (!t.comments().isEmpty()) {
                for (Comment c : t.comments()) {
                  Verify.verify(c.getSourcePos(0) >= 0);
                  comments.add(c.getText());
                }
              }
            }
            assertThat(comments)
                .containsExactly(
                    "/**\n     * foo\n     */",
                    "/* bar1 */",
                    "/* bar2 */",
                    "// baz number 1",
                    "// baz number 2")
                .inOrder();
            return super.visitNewClass(tree, state);
          }
        };
    tests.add(scanner);
    assertCompiles(scanner);
  }

  @Test
  public void testHasDirectAnnotationWithSimpleName() {
    writeFile(
        "A.java", //
        "public class A {",
        "  @Deprecated public void doIt() {}",
        "}");
    TestScanner scanner =
        new TestScanner() {
          @Override
          public Void visitMethod(MethodTree tree, VisitorState state) {
            if (tree.getName().contentEquals("doIt")) {
              setAssertionsComplete();
              Symbol sym = ASTHelpers.getSymbol(tree);
              assertThat(ASTHelpers.hasDirectAnnotationWithSimpleName(sym, "Deprecated")).isTrue();
              assertThat(ASTHelpers.hasDirectAnnotationWithSimpleName(sym, "Nullable")).isFalse();
            }
            return super.visitMethod(tree, state);
          }
        };
    tests.add(scanner);
    assertCompiles(scanner);
  }

  @Test
  public void testHasAnnotationOnMethodInvocation() {
    writeFile(
        "A.java", //
        "public class A {",
        "  @Deprecated public void doIt() {}",
        "  void caller() { doIt(); }",
        "}");
    TestScanner scanner =
        new TestScanner() {
          @Override
          public Void visitMethodInvocation(MethodInvocationTree tree, VisitorState state) {
            if (ASTHelpers.getSymbol(tree).toString().equals("doIt()")) {
              setAssertionsComplete();
              assertThat(ASTHelpers.hasAnnotation(tree, Deprecated.class, state)).isFalse();
            }
            return super.visitMethodInvocation(tree, state);
          }
        };
    tests.add(scanner);
    assertCompiles(scanner);
  }

  @BugPattern(
    name = "HasDirectAnnotationWithSimpleNameChecker",
    category = Category.ONE_OFF,
    severity = SeverityLevel.ERROR,
    summary =
        "Test checker to ensure that ASTHelpers.hasDirectAnnotationWithSimpleName() "
            + "does require the annotation symbol to be on the classpath"
  )
  public static class HasDirectAnnotationWithSimpleNameChecker extends BugChecker
      implements MethodInvocationTreeMatcher {
    @Override
    public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
      if (ASTHelpers.hasDirectAnnotationWithSimpleName(
          ASTHelpers.getSymbol(tree), "CheckReturnValue")) {
        return describeMatch(tree);
      }
      return Description.NO_MATCH;
    }
  }

  /** Test class containing a method annotated with a custom @CheckReturnValue. */
  public static class CustomCRVTest {
    /** A custom @CRV annotation. */
    @Retention(RetentionPolicy.RUNTIME)
    public @interface CheckReturnValue {}

    @CheckReturnValue
    public static String hello() {
      return "Hello!";
    }
  }

  @Rule public final TemporaryFolder tempFolder = new TemporaryFolder();

  static void addClassToJar(JarOutputStream jos, Class<?> clazz) throws IOException {
    String entryPath = clazz.getName().replace('.', '/') + ".class";
    try (InputStream is = clazz.getClassLoader().getResourceAsStream(entryPath)) {
      jos.putNextEntry(new JarEntry(entryPath));
      ByteStreams.copy(is, jos);
    }
  }

  @Test
  public void testHasDirectAnnotationWithSimpleNameWithoutAnnotationOnClasspath()
      throws IOException {
    File libJar = tempFolder.newFile("lib.jar");
    try (FileOutputStream fis = new FileOutputStream(libJar);
        JarOutputStream jos = new JarOutputStream(fis)) {
      addClassToJar(jos, CustomCRVTest.class);
      addClassToJar(jos, ASTHelpersTest.class);
      addClassToJar(jos, CompilerBasedAbstractTest.class);
    }

    CompilationTestHelper.newInstance(HasDirectAnnotationWithSimpleNameChecker.class, getClass())
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void m() {",
            "    // BUG: Diagnostic contains:",
            "    com.google.errorprone.util.ASTHelpersTest.CustomCRVTest.hello();",
            "  }",
            "}")
        .setArgs(Arrays.asList("-cp", libJar.toString()))
        .doTest();
  }

  /* Test infrastructure */

  private abstract static class TestScanner extends Scanner {
    private boolean assertionsComplete = false;

    /**
     * Subclasses of {@link TestScanner} are expected to call this method within their overridden
     * visitXYZ() method in order to verify that the method has run at least once.
     */
    protected void setAssertionsComplete() {
      this.assertionsComplete = true;
    }

    <T extends Tree> void assertMatch(T node, VisitorState visitorState, Matcher<T> matcher) {
      VisitorState state = visitorState.withPath(getCurrentPath());
      assertTrue(matcher.matches(node, state));
    }

    public void verifyAssertionsComplete() {
      assertTrue("Expected the visitor to call setAssertionsComplete().", assertionsComplete);
    }
  }

  /** A checker that reports the constant value of fields. */
  @BugPattern(name = "ConstChecker", category = JDK, summary = "", severity = ERROR)
  public static class ConstChecker extends BugChecker implements VariableTreeMatcher {
    @Override
    public Description matchVariable(VariableTree tree, VisitorState state) {
      Object value = ASTHelpers.constValue(tree.getInitializer());
      return buildDescription(tree)
          .setMessage(String.format("%s(%s)", value.getClass().getSimpleName(), value))
          .build();
    }
  }

  @Test
  public void constValue() {
    CompilationTestHelper.newInstance(ConstChecker.class, getClass())
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  // BUG: Diagnostic contains: Integer(42)",
            "  static final int A = 42;",
            "  // BUG: Diagnostic contains: Boolean(false)",
            "  static final boolean B = false;",
            "}")
        .doTest();
  }

  /** A {@link BugChecker} that prints the result type of the first argument in method calls. */
  @BugPattern(
    name = "PrintResultTypeOfFirstArgument",
    category = Category.ONE_OFF,
    severity = SeverityLevel.ERROR,
    summary = "Prints the type of the first argument in method calls"
  )
  public static class PrintResultTypeOfFirstArgument extends BugChecker
      implements MethodInvocationTreeMatcher {
    @Override
    public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
      List<? extends ExpressionTree> arguments = tree.getArguments();
      if (arguments.isEmpty()) {
        return Description.NO_MATCH;
      }
      return buildDescription(tree)
          .setMessage(ASTHelpers.getResultType(Iterables.getFirst(arguments, null)).toString())
          .build();
    }
  }

  @Test
  public void getResultType_findsConcreteType_withGenericMethodCall() {
    CompilationTestHelper.newInstance(PrintResultTypeOfFirstArgument.class, getClass())
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  abstract <T> T get(T obj);",
            "  abstract void target(Object param);",
            "  private void test() {",
            "    // BUG: Diagnostic contains: java.lang.Integer",
            "    target(get(1));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void getResultType_findsIntType_withPrimitiveInt() {
    CompilationTestHelper.newInstance(PrintResultTypeOfFirstArgument.class, getClass())
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  abstract void target(int i);",
            "  private void test(int j) {",
            "    // BUG: Diagnostic contains: int",
            "    target(j);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void getResultType_findsConstructedType_withConstructor() {
    CompilationTestHelper.newInstance(PrintResultTypeOfFirstArgument.class, getClass())
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  abstract void target(String s);",
            "  private void test() {",
            "    // BUG: Diagnostic contains: java.lang.String",
            "    target(new String());",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void getResultType_findsNullType_withNull() {
    CompilationTestHelper.newInstance(PrintResultTypeOfFirstArgument.class, getClass())
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  abstract void target(String s);",
            "  private void test() {",
            "    // BUG: Diagnostic contains: <nulltype>",
            "    target(null);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void getResultType_findsConcreteType_withGenericConstructorCall() {
    CompilationTestHelper.newInstance(PrintResultTypeOfFirstArgument.class, getClass())
        .addSourceLines(
            "Test.java",
            "class GenericTest<T> {}",
            "abstract class Test {",
            "  abstract void target(Object param);",
            "  private void test() {",
            "    // BUG: Diagnostic contains: GenericTest<java.lang.String>",
            "    target(new GenericTest<String>());",
            "  }",
            "}")
        .doTest();
  }
}
