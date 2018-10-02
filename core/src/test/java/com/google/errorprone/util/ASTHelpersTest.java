/*
 * Copyright 2013 The Error Prone Authors.
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
import static com.google.common.truth.Truth8.assertThat;
import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.LOCAL_VARIABLE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.common.base.Joiner;
import com.google.common.base.Verify;
import com.google.common.collect.Iterables;
import com.google.common.io.ByteStreams;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.Category;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.ParameterizedTypeTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.CompilerBasedAbstractTest;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.matchers.method.MethodMatchers;
import com.google.errorprone.scanner.Scanner;
import com.google.errorprone.util.ASTHelpers.TargetType;
import com.google.errorprone.util.ASTHelpers.TargetTypeVisitor;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.TypeVar;
import com.sun.tools.javac.main.Main.Result;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.parser.Tokens.Comment;
import com.sun.tools.javac.tree.JCTree.JCLiteral;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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

  @Test
  public void testInheritedMethodAnnotation() {
    writeFile(
        "com/google/errorprone/util/InheritedAnnotation.java",
        "package com.google.errorprone.util;",
        "import java.lang.annotation.Inherited;",
        "@Inherited",
        "public @interface InheritedAnnotation {}");
    writeFile(
        "B.java",
        "import com.google.errorprone.util.InheritedAnnotation;",
        "public class B {",
        "  @InheritedAnnotation",
        "  void f() {}",
        "}");
    writeFile("C.java", "public class C extends B {}");

    TestScanner scanner =
        new TestScanner() {
          @Override
          public Void visitMethod(MethodTree tree, VisitorState state) {
            if (tree.getName().contentEquals("f")) {
              assertMatch(
                  tree,
                  state,
                  (MethodTree t, VisitorState s) ->
                      ASTHelpers.hasAnnotation(t, InheritedAnnotation.class, s));
              setAssertionsComplete();
            }
            return super.visitMethod(tree, state);
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

  /**
   * Test checker to ensure that ASTHelpers.hasDirectAnnotationWithSimpleName() does require the
   * annotation symbol to be on the classpath.
   */
  @BugPattern(
      name = "HasDirectAnnotationWithSimpleNameChecker",
      category = Category.ONE_OFF,
      severity = SeverityLevel.ERROR,
      summary =
          "Test checker to ensure that ASTHelpers.hasDirectAnnotationWithSimpleName() "
              + "does require the annotation symbol to be on the classpath")
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

  /* Tests inSamePackage. */

  private TestScanner inSamePackageScanner(final boolean expectedBoolean) {
    return new TestScanner() {
      @Override
      public Void visitMemberSelect(MemberSelectTree tree, VisitorState state) {
        setAssertionsComplete();
        Symbol targetSymbol = ASTHelpers.getSymbol(tree);
        assertThat(ASTHelpers.inSamePackage(targetSymbol, state)).isEqualTo(expectedBoolean);
        return super.visitMemberSelect(tree, state);
      }
    };
  }

  @Test
  public void testSamePackagePositive() {
    writeFile(
        "A.java",
        "package p;",
        "public class A { ",
        "  public static final String BAR = \"BAR\";",
        "}");
    writeFile(
        "B.java",
        "package p;",
        "public class B { ",
        "  public String bar() {",
        "    return A.BAR;",
        "  }",
        "}");
    TestScanner scanner = inSamePackageScanner(true);
    tests.add(scanner);
    assertCompiles(scanner);
  }

  @Test
  public void testSamePackageNegative() {
    writeFile(
        "A.java",
        "package p;",
        "public class A { ",
        "  public static final String BAR = \"BAR\";",
        "}");
    writeFile(
        "B.java",
        "package q;",
        "import p.A;",
        "public class B { ",
        "  public String bar() {",
        "    return A.BAR;",
        "  }",
        "}");
    TestScanner scanner = inSamePackageScanner(false);
    tests.add(scanner);
    assertCompiles(scanner);
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
      summary = "Prints the type of the first argument in method calls")
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

  /** A {@link BugChecker} that prints the target type of matched method invocations. */
  @BugPattern(
      name = "TargetTypeChecker",
      category = Category.ONE_OFF,
      severity = SeverityLevel.ERROR,
      summary = "Prints the target type")
  public static class TargetTypeChecker extends BugChecker implements MethodInvocationTreeMatcher {
    private static final Matcher<ExpressionTree> METHOD_MATCHER =
        MethodMatchers.staticMethod().anyClass().withNameMatching(Pattern.compile("^detect.*"));

    @Override
    public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
      if (!METHOD_MATCHER.matches(tree, state)) {
        return Description.NO_MATCH;
      }
      TargetType targetType = ASTHelpers.targetType(state);
      return buildDescription(tree)
          .setMessage(String.valueOf(targetType != null ? targetType.type() : null))
          .build();
    }
  }

  @Test
  public void targetType() {
    CompilationTestHelper.newInstance(TargetTypeChecker.class, getClass())
        .addSourceFile("TargetTypeTest.java")
        .doTest();
  }

  @Test
  public void targetType_methodHandle() {
    if (!isJdk8OrEarlier()) {
      // JDK >= 9 complains about splitting java.lang.invoke
      return;
    }
    CompilationTestHelper.newInstance(TargetTypeChecker.class, getClass())
        .addSourceLines(
            "Test.java",
            "package java.lang.invoke;",
            "class Test {",
            "  void f(MethodHandle mh) throws Throwable {",
            "    // BUG: Diagnostic contains:",
            "    mh.invokeBasic(detectString(), detectString(), detectString());",
            "  }",
            "  static String detectString() {",
            "    return \"\";",
            "  }",
            "}")
        .doTest();
  }

  /** A {@link BugChecker} that prints the target type of a parameterized type. */
  @BugPattern(
      name = "TargetTypeCheckerParentTypeNotMatched",
      category = Category.ONE_OFF,
      severity = SeverityLevel.ERROR,
      summary =
          "Prints the target type for ParameterizedTypeTree, which is not handled explicitly.")
  public static class TargetTypeCheckerParentTypeNotMatched extends BugChecker
      implements ParameterizedTypeTreeMatcher {

    @Override
    public Description matchParameterizedType(ParameterizedTypeTree tree, VisitorState state) {
      TargetType targetType = ASTHelpers.targetType(state);
      return buildDescription(tree)
          .setMessage(
              "Target type of " + tree + " is " + (targetType != null ? targetType.type() : null))
          .build();
    }
  }

  @Test
  public void targetType_parentTypeNotMatched() {
    // Make sure that the method isn't implemented in the visitor; that would make this test
    // meaningless.
    List<String> methodNames =
        Stream.of(TargetTypeVisitor.class.getDeclaredMethods())
            .map(Method::getName)
            .collect(Collectors.toList());
    assertThat(methodNames).doesNotContain("visitParameterizedType");

    CompilationTestHelper.newInstance(TargetTypeCheckerParentTypeNotMatched.class, getClass())
        .addSourceLines(
            "Test.java",
            "import java.util.ArrayList;",
            "class Foo {",
            "  // BUG: Diagnostic contains: Target type of ArrayList<Integer> is null",
            "  Object obj = new ArrayList<Integer>() {",
            "    int foo() { return 0; }",
            "  };",
            "}")
        .expectResult(Result.ERROR)
        .doTest();
  }

  @Target(TYPE_USE)
  @interface A {
    int value();
  }

  @Target({METHOD, FIELD, LOCAL_VARIABLE, PARAMETER})
  @interface B {
    String value();
  }

  @interface ExpectedAnnotation {
    String expected();

    Target target();
  }

  /** A {@link Lib}rary for testing. */
  public static class Lib {
    @B("one")
    private volatile Map.@A(1) Entry<@A(2) ?, ? extends @A(3) Object> field;

    @B("two")
    Map.@A(4) Entry<@A(5) ?, ? extends @A(6) Object> method(
        @B("three") Map.@A(7) Entry<@A(8) ?, ? extends @A(9) Object> param1,
        @B("four") @A(11) Object @A(10) [] param2) {
      return null;
    }
  }

  @Test
  public void getDeclarationAndTypeAttributesTest() {
    BasicJavacTask tool =
        (BasicJavacTask) JavacTool.create().getTask(null, null, null, null, null, null);
    ClassSymbol element =
        JavacElements.instance(tool.getContext()).getTypeElement(Lib.class.getCanonicalName());
    VarSymbol field =
        (VarSymbol)
            element.getEnclosedElements().stream()
                .filter(e -> e.getSimpleName().contentEquals("field"))
                .findAny()
                .get();
    assertThat(ASTHelpers.getDeclarationAndTypeAttributes(field).map(String::valueOf))
        .containsExactly(
            "@com.google.errorprone.util.ASTHelpersTest.B(\"one\")",
            "@com.google.errorprone.util.ASTHelpersTest.A(1)");

    MethodSymbol method =
        (MethodSymbol)
            element.getEnclosedElements().stream()
                .filter(e -> e.getSimpleName().contentEquals("method"))
                .findAny()
                .get();
    assertThat(ASTHelpers.getDeclarationAndTypeAttributes(method).map(String::valueOf))
        .containsExactly(
            "@com.google.errorprone.util.ASTHelpersTest.B(\"two\")",
            "@com.google.errorprone.util.ASTHelpersTest.A(4)");
    assertThat(
            ASTHelpers.getDeclarationAndTypeAttributes(method.getParameters().get(0))
                .map(String::valueOf))
        .containsExactly(
            "@com.google.errorprone.util.ASTHelpersTest.B(\"three\")",
            "@com.google.errorprone.util.ASTHelpersTest.A(7)");
    assertThat(
            ASTHelpers.getDeclarationAndTypeAttributes(method.getParameters().get(1))
                .map(String::valueOf))
        .containsExactly(
            "@com.google.errorprone.util.ASTHelpersTest.B(\"four\")",
            "@com.google.errorprone.util.ASTHelpersTest.A(10)");
  }

  /** A {@link BugChecker} that prints if the method can be overridden. */
  @BugPattern(
      name = "MethodCanBeOverriddenChecker",
      category = Category.ONE_OFF,
      severity = SeverityLevel.ERROR,
      summary = "Prints whether the method can be overridden.",
      providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION)
  public static class MethodCanBeOverriddenChecker extends BugChecker implements MethodTreeMatcher {
    @Override
    public Description matchMethod(MethodTree tree, VisitorState state) {
      boolean methodCanBeOverridden = ASTHelpers.methodCanBeOverridden(ASTHelpers.getSymbol(tree));
      String description = methodCanBeOverridden ? "Can be overridden" : "Cannot be overridden";
      return describeMatch(tree, SuggestedFix.prefixWith(tree, "/* " + description + " */\n"));
    }
  }

  @Test
  public void methodCanBeOverridden_class() {
    CompilationTestHelper.newInstance(MethodCanBeOverriddenChecker.class, getClass())
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  // BUG: Diagnostic contains: Cannot be overridden",
            "  Test() {}",
            "",
            "  // BUG: Diagnostic contains: Can be overridden",
            "  void canBeOverridden() {}",
            "",
            "  // BUG: Diagnostic contains: Cannot be overridden",
            "  final void cannotBeOverriddenBecauseFinal() {}",
            "",
            "  // BUG: Diagnostic contains: Cannot be overridden",
            "  static void cannotBeOverriddenBecauseStatic() {}",
            "",
            "  // BUG: Diagnostic contains: Cannot be overridden",
            "  private void cannotBeOverriddenBecausePrivate() {}",
            "}")
        .doTest();
  }

  @Test
  public void methodCanBeOverridden_interface() {
    CompilationTestHelper.newInstance(MethodCanBeOverriddenChecker.class, getClass())
        .addSourceLines(
            "Test.java",
            "interface Test {",
            "  // BUG: Diagnostic contains: Can be overridden",
            "  void canBeOverridden();",
            "",
            "  // BUG: Diagnostic contains: Can be overridden",
            "  default void defaultCanBeOverridden() {}",
            "",
            "  // BUG: Diagnostic contains: Cannot be overridden",
            "  static void cannotBeOverriddenBecauseStatic() {}",
            "}")
        .doTest();
  }

  @Test
  public void methodCanBeOverridden_enum() {
    CompilationTestHelper.newInstance(MethodCanBeOverriddenChecker.class, getClass())
        .addSourceLines(
            "Test.java",
            "enum Test {",
            "  VALUE {",
            "    // BUG: Diagnostic contains: Cannot be overridden",
            "    @Override void abstractCanBeOverridden() {}",
            "",
            "    // BUG: Diagnostic contains: Cannot be overridden",
            "    void declaredOnlyInValue() {}",
            "  };",
            "",
            "  // BUG: Diagnostic contains: Can be overridden",
            "  abstract void abstractCanBeOverridden();",
            "",
            "  // BUG: Diagnostic contains: Can be overridden",
            "  void canBeOverridden() {}",
            "}")
        .doTest();
  }

  @Test
  public void methodCanBeOverridden_anonymous() {
    CompilationTestHelper.newInstance(MethodCanBeOverriddenChecker.class, getClass())
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  Object obj = new Object() {",
            "    // BUG: Diagnostic contains: Cannot be overridden",
            "    void inAnonymousClass() {}",
            "  };",
            "}")
        .doTest();
  }

  @Test
  public void testDeprecatedClassDeclaration() {
    writeFile(
        "A.java", //
        "@Deprecated",
        "public class A {",
        "}");
    TestScanner scanner =
        new TestScanner() {
          @Override
          public Void visitClass(ClassTree node, VisitorState visitorState) {
            // we specifically want to test getSymbol(Tree), not getSymbol(ClassTree)
            Tree tree = node;
            assertThat(ASTHelpers.getSymbol(tree).isDeprecated()).isTrue();
            setAssertionsComplete();
            return super.visitClass(node, visitorState);
          }
        };
    tests.add(scanner);
    assertCompiles(scanner);
  }

  private static boolean isJdk8OrEarlier() {
    try {
      Method versionMethod = Runtime.class.getMethod("version");
      Object version = versionMethod.invoke(null);
      int majorVersion = (int) version.getClass().getMethod("major").invoke(version);
      return majorVersion <= 8;
    } catch (ReflectiveOperationException e) {
      return true;
    }
  }
}
