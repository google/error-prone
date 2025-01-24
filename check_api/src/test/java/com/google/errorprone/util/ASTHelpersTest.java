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

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.util.ASTHelpers.getStartPosition;
import static com.google.errorprone.util.ASTHelpers.hasAnnotation;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.LOCAL_VARIABLE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.util.stream.Collectors.joining;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.google.common.base.Joiner;
import com.google.common.base.Verify;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.IdentifierTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MemberReferenceTreeMatcher;
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
import com.sun.source.tree.AnnotatedTypeTree;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.ModuleTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.PackageTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.PackageSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.TypeVar;
import com.sun.tools.javac.main.Main.Result;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.tree.JCTree.JCLiteral;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.lang.model.element.ElementKind;
import org.junit.After;
import org.junit.Test;
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
  public void getStartPositionUnix() {
    String fileContent =
        UNIX_LINE_JOINER.join(
            "public class A { ", "  public void foo() {", "    int i;", "    i = -1;", "  }", "}");
    writeFile("A.java", fileContent);
    assertCompiles(literalExpressionMatches(literalHasStartPosition(59)));
  }

  @Test
  public void getStartPositionWindows() {
    String fileContent =
        WINDOWS_LINE_JOINER.join(
            "public class A { ", "  public void foo() {", "    int i;", "    i = -1;", "  }", "}");
    writeFile("A.java", fileContent);
    assertCompiles(literalExpressionMatches(literalHasStartPosition(62)));
  }

  @Test
  public void getStartPositionWithWhitespaceUnix() {
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
  public void getStartPositionWithWhitespaceWindows() {
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

  private Matcher<LiteralTree> literalHasStartPosition(int startPosition) {
    return (LiteralTree tree, VisitorState state) -> {
      JCLiteral literal = (JCLiteral) tree;
      return literal.getStartPosition() == startPosition;
    };
  }

  private Scanner literalExpressionMatches(Matcher<LiteralTree> matcher) {
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
  public void getReceiver() {
    writeFile(
        "A.java",
        """
        package p;
        public class A {
          public B b;
          public void foo() {}
          public B bar() {
            return null;
          }
        }
        """);
    writeFile(
        "B.java",
        """
        package p;
        public class B {
          public static void bar() {}
          public void foo() {}
        }
        """);
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
      String expectedReceiver, String expectedType) {
    return Matchers.allOf(
        (ExpressionTree t, VisitorState state) -> {
          ExpressionTree receiver = ASTHelpers.getReceiver(t);
          return expectedReceiver != null
              ? receiver.toString().equals(expectedReceiver)
              : receiver == null;
        },
        (ExpressionTree t, VisitorState state) -> {
          Type type = ASTHelpers.getReceiverType(t);
          return ASTHelpers.isSameType(state.getTypeFromString(expectedType), type, state);
        });
  }

  private Scanner expressionStatementMatches(
      String expectedExpression, Matcher<ExpressionTree> matcher) {
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
  public void annotationHelpers() {
    writeFile(
        "com/google/errorprone/util/InheritedAnnotation.java",
        """
        package com.google.errorprone.util;
        import java.lang.annotation.Inherited;
        @Inherited
        public @interface InheritedAnnotation {}
        """);
    writeFile(
        "B.java",
        """
        import com.google.errorprone.util.InheritedAnnotation;
        @InheritedAnnotation
        public class B {}
        """);
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
                      return hasAnnotation(
                          t, "com.google.errorprone.util.InheritedAnnotation", state);
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
  public void annotationHelpersWrongValueCached() {
    writeFile("D.java", "public class D{}");

    TestScanner scanner =
        new TestScanner() {
          @Override
          public Void visitClass(ClassTree tree, VisitorState state) {
            if (tree.getSimpleName().contentEquals("D")) {
              assertThat(hasAnnotation(tree, "TestAnnotation", state.withPath(getCurrentPath())))
                  .isFalse();
              setAssertionsComplete();
            }
            return super.visitClass(tree, state);
          }
        };
    tests.add(scanner);
    assertCompiles(scanner);

    writeFile(
        "TestAnnotation.java",
        """
        import java.lang.annotation.Inherited;
        @Inherited
        public @interface TestAnnotation {}
        """);
    writeFile(
        "B.java",
        """
        @TestAnnotation
        public class B {}
        """);
    writeFile("C.java", "public class C extends B {}");

    TestScanner scannerWithAnnotation =
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
                      return hasAnnotation(t, "TestAnnotation", state);
                    }
                  });
              setAssertionsComplete();
            }
            return super.visitClass(tree, state);
          }
        };
    tests.add(scannerWithAnnotation);
    assertCompiles(scannerWithAnnotation);
  }

  @Test
  public void inheritedMethodAnnotation() {
    writeFile(
        "com/google/errorprone/util/InheritedAnnotation.java",
        """
        package com.google.errorprone.util;
        import java.lang.annotation.Inherited;
        @Inherited
        public @interface InheritedAnnotation {}
        """);
    writeFile(
        "B.java",
        """
        import com.google.errorprone.util.InheritedAnnotation;
        public class B {
          @InheritedAnnotation
          void f() {}
        }
        """);
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
                      hasAnnotation(t, "com.google.errorprone.util.InheritedAnnotation", s));
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
  public void innerAnnotationType() {
    writeFile(
        "test/Lib.java",
        """
        package test;
        public class Lib {
          public @interface MyAnnotation {}
        }
        """);
    writeFile(
        "test/Test.java",
        """
        package test;
        import test.Lib.MyAnnotation;
        @MyAnnotation
        public class Test {}
        """);

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
                      return hasAnnotation(ASTHelpers.getSymbol(t), "test.Lib$MyAnnotation", state);
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
  public void getTypeOnNestedAnnotationType() {
    writeFile(
        "A.java",
        """
        public class A {
          @B.MyAnnotation
          public void bar() {}
        }
        """);
    writeFile(
        "B.java",
        """
        public class B {
          @interface MyAnnotation {}
        }
        """);
    TestScanner scanner =
        new TestScanner() {
          @Override
          public Void visitAnnotation(AnnotationTree tree, VisitorState state) {
            setAssertionsComplete();
            assertThat(ASTHelpers.getType(tree.getAnnotationType()).toString())
                .isEqualTo("B.MyAnnotation");
            return super.visitAnnotation(tree, state);
          }
        };
    tests.add(scanner);
    assertCompiles(scanner);
  }

  @Test
  public void getTypeOnNestedClassType() {
    writeFile(
        "A.java",
        """
        public class A {
          public void bar() {
            B.C foo;
          }
        }
        """);
    writeFile(
        "B.java",
        """
        public class B {
          public static class C {}
        }
        """);
    TestScanner scanner =
        new TestScanner() {
          @Override
          public Void visitVariable(VariableTree tree, VisitorState state) {
            setAssertionsComplete();
            assertThat(ASTHelpers.getType(tree.getType()).toString()).isEqualTo("B.C");
            return super.visitVariable(tree, state);
          }
        };
    tests.add(scanner);
    assertCompiles(scanner);
  }

  @Test
  public void enclosingElements() {
    writeFile(
        "Outer.java",
        """
        package com.google.test;
        public class Outer {
          static class Middle {
            private class Inner {
              int foo() {int x = 1; return x;}
            }
          }
        }
        """);
    TestScanner scanner =
        new TestScanner() {
          @Override
          public Void visitVariable(VariableTree variable, VisitorState state) {
            setAssertionsComplete();
            assertThat(
                    ASTHelpers.enclosingElements(ASTHelpers.getSymbol(variable))
                        .map(sym -> sym.getSimpleName().toString()))
                .containsExactly(
                    "x", "foo", "Inner", "Middle", "Outer",
                    "test", /* the ModuleSymbol for the unnamed module */ "")
                .inOrder();
            return super.visitVariable(variable, state);
          }
        };

    tests.add(scanner);
    assertCompiles(scanner);
  }

  @Test
  public void getTypeOnParameterizedType() {
    writeFile(
        "Pair.java",
        """
        public class Pair<A, B> {
          public A first;
          public B second;
        }
        """);
    writeFile(
        "Test.java",
        """
        public class Test {
          public Integer doSomething(Pair<Integer, String> pair) {
            return pair.first;
          }
        }
        """);
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

  /* Tests for ASTHelpers#isCheckedExceptionType */

  private TestScanner isCheckedExceptionTypeScanner(boolean expectChecked) {
    return new TestScanner() {
      @Override
      public Void visitMethod(MethodTree tree, VisitorState state) {
        setAssertionsComplete();
        List<? extends ExpressionTree> throwz = tree.getThrows();

        for (ExpressionTree expr : throwz) {
          Type exType = ASTHelpers.getType(expr);
          assertThat(ASTHelpers.isCheckedExceptionType(exType, state)).isEqualTo(expectChecked);
        }
        return super.visitMethod(tree, state);
      }
    };
  }

  @Test
  public void isCheckedExceptionType_yes() {
    writeFile(
        "A.java",
        """
        import java.text.ParseException;
        public class A {
          static class MyException extends Exception {}
          void foo() throws Exception, ParseException {}
        }
        """);
    TestScanner scanner = isCheckedExceptionTypeScanner(true);
    tests.add(scanner);
    assertCompiles(scanner);
  }

  @Test
  public void isCheckedExceptionType_no() {
    writeFile(
        "A.java",
        """
        public class A {
          static class MyException extends RuntimeException {}
          void foo() throws RuntimeException, IllegalArgumentException, MyException,
              Error, VerifyError {}
        }
        """);
    TestScanner scanner = isCheckedExceptionTypeScanner(false);
    tests.add(scanner);
    assertCompiles(scanner);
  }

  /* Tests for ASTHelpers#getUpperBound */

  private TestScanner getUpperBoundScanner(String expectedBound) {
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
  public void getUpperBoundConcreteType() {
    writeFile(
        "A.java",
        """
        import java.lang.Number;
        import java.util.List;
        public class A {
          public List<Number> myList;
        }
        """);
    TestScanner scanner = getUpperBoundScanner("java.lang.Number");
    tests.add(scanner);
    assertCompiles(scanner);
  }

  @Test
  public void getUpperBoundUpperBoundedWildcard() {
    writeFile(
        "A.java",
        """
        import java.lang.Number;
        import java.util.List;
        public class A {
          public List<? extends Number> myList;
        }
        """);
    TestScanner scanner = getUpperBoundScanner("java.lang.Number");
    tests.add(scanner);
    assertCompiles(scanner);
  }

  @Test
  public void getUpperBoundUnboundedWildcard() {
    writeFile(
        "A.java",
        """
        import java.util.List;
        public class A {
          public List<?> myList;
        }
        """);
    TestScanner scanner = getUpperBoundScanner("java.lang.Object");
    tests.add(scanner);
    assertCompiles(scanner);
  }

  @Test
  public void getUpperBoundLowerBoundedWildcard() {
    writeFile(
        "A.java",
        """
        import java.lang.Number;
        import java.util.List;
        public class A {
          public List<? super Number> myList;
        }
        """);
    TestScanner scanner = getUpperBoundScanner("java.lang.Object");
    tests.add(scanner);
    assertCompiles(scanner);
  }

  @Test
  public void getUpperBoundTypeVariable() {
    writeFile(
        "A.java",
        """
        import java.util.List;
        public class A<T> {
          public List<T> myList;
        }
        """);
    TestScanner scanner = getUpperBoundScanner("java.lang.Object");
    tests.add(scanner);
    assertCompiles(scanner);
  }

  @Test
  public void getUpperBoundCapturedTypeVariable() {
    writeFile(
        "A.java",
        """
        import java.lang.Number;
        import java.util.List;
        public class A {
          public void doSomething(List<? extends Number> list) {
            list.get(0);
          }
        }
        """);
    TestScanner scanner =
        new TestScanner() {
          @Override
          public Void visitMethodInvocation(MethodInvocationTree tree, VisitorState state) {
            if (!tree.toString().equals("super()")) { // ignore synthetic super call
              setAssertionsComplete();
              Type type = ASTHelpers.getType(tree);
              assertThat(type).isInstanceOf(TypeVar.class);
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
  public void commentTokens() {
    writeFile(
        "A.java",
        """
        public class A {
          Runnable theRunnable = new Runnable() {
            /**
             * foo
             */
            public void run() {
              /* bar1 */
              /* bar2 */
              System.err.println("Hi");
            }
            // baz number 1
            // baz number 2
          };
        }
        """);
    TestScanner scanner =
        new TestScanner() {
          @Override
          public Void visitNewClass(NewClassTree tree, VisitorState state) {
            setAssertionsComplete();
            List<String> comments = new ArrayList<>();
            int startPos = getStartPosition(tree);
            for (ErrorProneToken t : state.getOffsetTokensForNode(tree)) {
              if (!t.comments().isEmpty()) {
                for (ErrorProneComment c : t.comments()) {
                  Verify.verify(c.getSourcePos(0) >= startPos);
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
  public void hasDirectAnnotationWithSimpleName() {
    writeFile(
        "A.java",
        """
        public class A {
          @Deprecated public void doIt() {}
        }
        """);
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
  public void getAnnotationsWithSimpleName() {
    writeFile(
        "com/google/errorprone/util/RepeatableAnnotations.java",
        """
        package com.google.errorprone.util;
        public @interface RepeatableAnnotations {RepeatableAnnotation[] value();}
        """);
    writeFile(
        "com/google/errorprone/util/RepeatableAnnotation.java",
        """
        package com.google.errorprone.util;
        import java.lang.annotation.Repeatable;
        @Repeatable(RepeatableAnnotations.class)
        public @interface RepeatableAnnotation {}
        """);
    writeFile(
        "A.java",
        """
        import com.google.errorprone.util.RepeatableAnnotation;
        public class A {
          @RepeatableAnnotation
          @RepeatableAnnotation
          public void bar() {}
        }
        """);
    TestScanner scanner =
        new TestScanner() {
          @Override
          public Void visitMethod(MethodTree tree, VisitorState state) {
            if (tree.getName().contentEquals("bar")) {
              setAssertionsComplete();
              assertThat(
                      ASTHelpers.getAnnotationsWithSimpleName(
                          tree.getModifiers().getAnnotations(), "RepeatableAnnotation"))
                  .hasSize(2);
            }
            return super.visitMethod(tree, state);
          }
        };
    tests.add(scanner);
    assertCompiles(scanner);
  }

  @Test
  public void hasAnnotationOnMethodInvocation() {
    writeFile(
        "A.java",
        """
        public class A {
          @Deprecated public void doIt() {}
          void caller() { doIt(); }
        }
        """);
    TestScanner scanner =
        new TestScanner() {
          @Override
          public Void visitMethodInvocation(MethodInvocationTree tree, VisitorState state) {
            if (ASTHelpers.getSymbol(tree).toString().equals("doIt()")) {
              setAssertionsComplete();
              assertThat(hasAnnotation(tree, Deprecated.class.getName(), state)).isFalse();
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
  public static final class CustomCRVTest {
    /** A custom @CRV annotation. */
    @Retention(RetentionPolicy.RUNTIME)
    public @interface CheckReturnValue {}

    @CheckReturnValue
    public static String hello() {
      return "Hello!";
    }

    private CustomCRVTest() {}
  }

  @Test
  public void hasDirectAnnotationWithSimpleNameWithoutAnnotationOnClasspath() {
    CompilationTestHelper.newInstance(HasDirectAnnotationWithSimpleNameChecker.class, getClass())
        .addSourceLines(
            "Test.java",
            """
            class Test {
              void m() {
                // BUG: Diagnostic contains:
                com.google.errorprone.util.ASTHelpersTest.CustomCRVTest.hello();
              }
            }
            """)
        .withClasspath(CustomCRVTest.class, ASTHelpersTest.class, CompilerBasedAbstractTest.class)
        .doTest();
  }

  /* Tests inSamePackage. */

  private TestScanner inSamePackageScanner(boolean expectedBoolean) {
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
  public void samePackagePositive() {
    writeFile(
        "A.java",
        """
        package p;
        public class A {
          public static final String BAR = "BAR";
        }
        """);
    writeFile(
        "B.java",
        """
        package p;
        public class B {
          public String bar() {
            return A.BAR;
          }
        }
        """);
    TestScanner scanner = inSamePackageScanner(true);
    tests.add(scanner);
    assertCompiles(scanner);
  }

  @Test
  public void samePackageNegative() {
    writeFile(
        "A.java",
        """
        package p;
        public class A {
          public static final String BAR = "BAR";
        }
        """);
    writeFile(
        "B.java",
        """
        package q;
        import p.A;
        public class B {
          public String bar() {
            return A.BAR;
          }
        }
        """);
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
      assertThat(matcher.matches(node, state)).isTrue();
    }

    public void verifyAssertionsComplete() {
      assertWithMessage("Expected the visitor to call setAssertionsComplete().")
          .that(assertionsComplete)
          .isTrue();
    }
  }

  /** A checker that reports the constant value of fields. */
  @BugPattern(summary = "", severity = ERROR)
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
            """
            class Test {
              // BUG: Diagnostic contains: Integer(42)
              static final int A = 42;
              // BUG: Diagnostic contains: Boolean(false)
              static final boolean B = false;
            }
            """)
        .doTest();
  }

  /** A {@link BugChecker} that prints the result type of the first argument in method calls. */
  @BugPattern(
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
            """
            abstract class Test {
              abstract <T> T get(T obj);
              abstract void target(Object param);
              private void test() {
                // BUG: Diagnostic contains: java.lang.Integer
                target(get(1));
              }
            }
            """)
        .doTest();
  }

  @Test
  public void getResultType_findsIntType_withPrimitiveInt() {
    CompilationTestHelper.newInstance(PrintResultTypeOfFirstArgument.class, getClass())
        .addSourceLines(
            "Test.java",
            """
            abstract class Test {
              abstract void target(int i);
              private void test(int j) {
                // BUG: Diagnostic contains: int
                target(j);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void getResultType_findsConstructedType_withConstructor() {
    CompilationTestHelper.newInstance(PrintResultTypeOfFirstArgument.class, getClass())
        .addSourceLines(
            "Test.java",
            """
            abstract class Test {
              abstract void target(String s);
              private void test() {
                // BUG: Diagnostic contains: java.lang.String
                target(new String());
              }
            }
            """)
        .doTest();
  }

  @Test
  public void getResultType_findsNullType_withNull() {
    CompilationTestHelper.newInstance(PrintResultTypeOfFirstArgument.class, getClass())
        .addSourceLines(
            "Test.java",
            """
            abstract class Test {
              abstract void target(String s);
              private void test() {
                // BUG: Diagnostic contains: <nulltype>
                target(null);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void getResultType_findsConcreteType_withGenericConstructorCall() {
    CompilationTestHelper.newInstance(PrintResultTypeOfFirstArgument.class, getClass())
        .addSourceLines(
            "Test.java",
            """
            class GenericTest<T> {}
            abstract class Test {
              abstract void target(Object param);
              private void test() {
                // BUG: Diagnostic contains: GenericTest<java.lang.String>
                target(new GenericTest<String>());
              }
            }
            """)
        .doTest();
  }

  /** A {@link BugChecker} that prints the target type of matched method invocations. */
  @BugPattern(severity = SeverityLevel.ERROR, summary = "Prints the target type")
  public static class TargetTypeChecker extends BugChecker
      implements MethodInvocationTreeMatcher, IdentifierTreeMatcher {
    private static final Matcher<ExpressionTree> METHOD_MATCHER =
        MethodMatchers.staticMethod().anyClass().withNameMatching(Pattern.compile("^detect.*"));

    private static final Matcher<IdentifierTree> LOCAL_VARIABLE_MATCHER =
        ((identifierTree, state) -> {
          Symbol symbol = ASTHelpers.getSymbol(identifierTree);
          return symbol != null
              && symbol.getKind() == ElementKind.LOCAL_VARIABLE
              && identifierTree.getName().toString().matches("detect.*");
        });

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

    @Override
    public Description matchIdentifier(IdentifierTree tree, VisitorState state) {
      if (!LOCAL_VARIABLE_MATCHER.matches(tree, state)) {
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
        .addSourceFile("testdata/TargetTypeTest.java")
        .setArgs(ImmutableList.of("-Xmaxerrs", "200", "-Xmaxwarns", "200"))
        .doTest();
  }

  /** A {@link BugChecker} that prints the target type of a parameterized type. */
  @BugPattern(
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
              "Target type of "
                  + state.getSourceForNode(tree)
                  + " is "
                  + (targetType != null ? targetType.type() : null))
          .build();
    }
  }

  @Test
  public void targetType_parentTypeNotMatched() {
    // Make sure that the method isn't implemented in the visitor; that would make this test
    // meaningless.
    List<String> methodNames =
        Arrays.stream(TargetTypeVisitor.class.getDeclaredMethods())
            .map(Method::getName)
            .collect(Collectors.toList());
    assertThat(methodNames).doesNotContain("visitParameterizedType");

    CompilationTestHelper.newInstance(TargetTypeCheckerParentTypeNotMatched.class, getClass())
        .addSourceLines(
            "Test.java",
            """
            import java.util.ArrayList;
            class Foo {
              // BUG: Diagnostic contains: Target type of ArrayList<Integer> is null
              Object obj = new ArrayList<Integer>() {
                int foo() { return 0; }
              };
            }
            """)
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
    volatile Map.@A(1) Entry<@A(2) ?, ? extends @A(3) Object> field;

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
      severity = SeverityLevel.ERROR,
      summary = "Prints whether the method can be overridden.")
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
            """
            class Test {
              // BUG: Diagnostic contains: Cannot be overridden
              Test() {}

              // BUG: Diagnostic contains: Can be overridden
              void canBeOverridden() {}

              // BUG: Diagnostic contains: Cannot be overridden
              final void cannotBeOverriddenBecauseFinal() {}

              // BUG: Diagnostic contains: Cannot be overridden
              static void cannotBeOverriddenBecauseStatic() {}

              // BUG: Diagnostic contains: Cannot be overridden
              private void cannotBeOverriddenBecausePrivate() {}
            }
            """)
        .doTest();
  }

  @Test
  public void methodCanBeOverridden_interface() {
    CompilationTestHelper.newInstance(MethodCanBeOverriddenChecker.class, getClass())
        .addSourceLines(
            "Test.java",
            """
            interface Test {
              // BUG: Diagnostic contains: Can be overridden
              void canBeOverridden();

              // BUG: Diagnostic contains: Can be overridden
              default void defaultCanBeOverridden() {}

              // BUG: Diagnostic contains: Cannot be overridden
              static void cannotBeOverriddenBecauseStatic() {}
            }
            """)
        .doTest();
  }

  @Test
  public void methodCanBeOverridden_enum() {
    CompilationTestHelper.newInstance(MethodCanBeOverriddenChecker.class, getClass())
        .addSourceLines(
            "Test.java",
            """
            enum Test {
              VALUE {
                // BUG: Diagnostic contains: Cannot be overridden
                @Override void abstractCanBeOverridden() {}

                // BUG: Diagnostic contains: Cannot be overridden
                void declaredOnlyInValue() {}
              };

              // BUG: Diagnostic contains: Can be overridden
              abstract void abstractCanBeOverridden();

              // BUG: Diagnostic contains: Can be overridden
              void canBeOverridden() {}
            }
            """)
        .doTest();
  }

  @Test
  public void methodCanBeOverridden_anonymous() {
    CompilationTestHelper.newInstance(MethodCanBeOverriddenChecker.class, getClass())
        .addSourceLines(
            "Test.java",
            """
            class Test {
              Object obj = new Object() {
                // BUG: Diagnostic contains: Cannot be overridden
                void inAnonymousClass() {}
              };
            }
            """)
        .doTest();
  }

  @Test
  public void deprecatedClassDeclaration() {
    writeFile(
        "A.java",
        """
        @Deprecated
        public class A {
        }
        """);
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

  @Test
  public void outermostclass_dotClass() {
    writeFile(
        "Foo.java",
        """
        class Foo {
          void f() {
            Object unused = boolean.class;
          }
        }
        """);

    TestScanner scanner =
        new TestScanner() {
          @Override
          public Void visitMemberSelect(MemberSelectTree tree, VisitorState state) {
            Symbol targetSymbol = ASTHelpers.getSymbol(tree);
            // ASTHelpers#outermostClass shouldn't itself NPE
            assertThat(ASTHelpers.outermostClass(targetSymbol)).isNull();
            setAssertionsComplete();
            return super.visitMemberSelect(tree, state);
          }
        };
    tests.add(scanner);

    assertCompiles(scanner);
  }

  /** Replaces all throws clauses with more specific exceptions. */
  @BugPattern(
      summary = "Replaces all throws clauses with more specific exceptions.",
      severity = WARNING)
  public static final class ReplaceWithExceptions extends BugChecker implements MethodTreeMatcher {
    @Override
    public Description matchMethod(MethodTree tree, VisitorState state) {
      return buildDescription(tree)
          .setMessage(
              ASTHelpers.getThrownExceptions(tree.getBody(), state).stream()
                  .map(t -> t.tsym.getSimpleName().toString())
                  .sorted()
                  .collect(joining(" ", "[", "]")))
          .build();
    }
  }

  private final CompilationTestHelper replaceExceptionHelper =
      CompilationTestHelper.newInstance(ReplaceWithExceptions.class, getClass());

  @Test
  public void getThrownExceptions_trivialThrow() {
    replaceExceptionHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              // BUG: Diagnostic contains: [IllegalStateException]
              void test() throws Exception {
                throw new IllegalStateException();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void getThrownExceptions_caught() {
    replaceExceptionHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              // BUG: Diagnostic contains: []
              void test() throws Exception {
                try {
                  throw new IllegalStateException();
                } catch (Exception e) {}
              }
            }
            """)
        .doTest();
  }

  @Test
  public void getThrownExceptions_caughtAndRethrown() {
    replaceExceptionHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              // BUG: Diagnostic contains: [IllegalStateException]
              void test() throws Exception {
                try {
                  throw new IllegalStateException();
                } catch (Exception e) {
                  throw e;
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void getThrownExceptions_genericThrow() {
    replaceExceptionHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              // BUG: Diagnostic contains: [IllegalStateException]
              int test(java.util.Optional<Integer> x) {
                return x.orElseThrow(() -> new IllegalStateException());
              }
            }
            """)
        .doTest();
  }

  @Test
  public void getThrownExceptions_rethrown() {
    replaceExceptionHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              // BUG: Diagnostic contains: [InterruptedException]
              void test() throws Exception {
                try {
                  test();
                } catch (InterruptedException e) {
                  throw e;
                } catch (Exception e) {}
              }
            }
            """)
        .doTest();
  }

  @Test
  public void getThrownExceptions_rethrownUnion() {
    replaceExceptionHelper
        .addSourceLines(
            "Test.java",
            """
import java.util.concurrent.Callable;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
class Test {
  // BUG: Diagnostic contains: [FileNotFoundException UnsupportedEncodingException]
  void test(Callable<Void> c) throws FileNotFoundException, UnsupportedEncodingException {
    try {
      if (hashCode() == 1) {
        throw new FileNotFoundException("File not found");
      }
      c.call();
    } catch (FileNotFoundException | UnsupportedEncodingException e) {
      throw e;
    } catch (Exception e) {
      return;
    }
  }
}
""")
        .doTest();
  }

  @Test
  public void getThrownExceptions_tryWithResources() {
    replaceExceptionHelper
        .addSourceLines(
            "Test.java",
            """
            abstract class Test {
              // BUG: Diagnostic contains: [InterruptedException]
              void test() throws Exception {
                try (var x = c()) {}
              }
              // BUG: Diagnostic contains:
              abstract C c();
              abstract class C implements AutoCloseable {
                // BUG: Diagnostic contains:
                @Override public abstract void close() throws InterruptedException;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void getThrownExceptions_tryWithResourcesVariable() {
    replaceExceptionHelper
        .addSourceLines(
            "Test.java",
            """
            abstract class Test {
              // BUG: Diagnostic contains: [InterruptedException]
              void test() throws Exception {
                var x = c();
                try (x) {}
              }
              // BUG: Diagnostic contains:
              abstract C c();
              abstract class C implements AutoCloseable {
                // BUG: Diagnostic contains:
                @Override public abstract void close() throws InterruptedException;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void isSubtype_onMethods_isFalse() {
    writeFile(
        "A.java",
        """
        public class A {
          public <T> void doIt(T t) {}
          public int doItAgain(int i) {return 42;}
          private static void doSomething() {}
        }
        class B extends A {
          @Override
          public <T> void doIt(T t) {}
          @Override
          public int doItAgain(int i) {return 42;}
        }
        """);
    TestScanner scanner =
        new TestScanner() {
          @Override
          public Void visitMethod(MethodTree tree, VisitorState state) {
            setAssertionsComplete();
            Symbol sym = ASTHelpers.getSymbol(tree);
            assertThat(ASTHelpers.isSubtype(sym.asType(), sym.asType(), state)).isFalse();
            return super.visitMethod(tree, state);
          }
        };
    tests.add(scanner);
    assertCompiles(scanner);
  }

  /** Comments on method invocations with their receiver chain. */
  @BugPattern(
      summary = "Comments on method invocations with their receiver chain.",
      severity = WARNING)
  public static final class ReceiverStream extends BugChecker
      implements MethodInvocationTreeMatcher {
    @Override
    public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
      return buildDescription(tree)
          .setMessage(
              ASTHelpers.streamReceivers(tree)
                  .map(state::getSourceForNode)
                  .collect(joining(", ", "[", "]")))
          .build();
    }
  }

  @Test
  public void receiverStream() {
    CompilationTestHelper.newInstance(ReceiverStream.class, getClass())
        .addSourceLines(
            "Test.java",
            """
            class Test {
              private Test t;
              private void t() {
                // BUG: Diagnostic contains: []
                t();
                // BUG: Diagnostic contains: [t.t, t]
                t.t.t();
              }
            }
            """)
        .doTest();
  }

  /** Helper for testing {@link ASTHelpers#canBeRemoved}. */
  @BugPattern(summary = "", severity = WARNING)
  public static final class VisibleMembers extends BugChecker
      implements MethodTreeMatcher, VariableTreeMatcher {

    @Override
    public Description matchMethod(MethodTree tree, VisitorState state) {
      return ASTHelpers.canBeRemoved(ASTHelpers.getSymbol(tree), state)
          ? describeMatch(tree)
          : Description.NO_MATCH;
    }

    @Override
    public Description matchVariable(VariableTree tree, VisitorState state) {
      return ASTHelpers.canBeRemoved(ASTHelpers.getSymbol(tree))
          ? describeMatch(tree)
          : Description.NO_MATCH;
    }
  }

  @Test
  public void visibleMembers() {
    CompilationTestHelper.newInstance(VisibleMembers.class, getClass())
        .addSourceLines(
            "Test.java",
            """
            class Test {
              // BUG: Diagnostic contains:
              private Test t;
              private class Inner {
                // BUG: Diagnostic contains:
                public Test t;
                // BUG: Diagnostic contains:
                public void test() {}
                @Override public String toString() { return null; }
              }
            }
            """)
        .doTest();
  }

  private static final ImmutableSet<Class<?>> ALL_TREE_TYPES =
      Arrays.stream(TreePathScanner.class.getMethods())
          .filter(m -> m.getName().startsWith("visit"))
          .filter(m -> m.getParameters().length == 2)
          .map(m -> m.getParameters()[0].getType())
          .collect(toImmutableSet());

  @Test
  public void typesWithModifiersTree() {
    for (Class<?> clazz : ALL_TREE_TYPES) {
      switch (clazz.getSimpleName()) {
        case "MethodTree" -> assertGetModifiersInvoked(MethodTree.class, MethodTree::getModifiers);
        case "ClassTree" -> assertGetModifiersInvoked(ClassTree.class, ClassTree::getModifiers);
        case "VariableTree" ->
            assertGetModifiersInvoked(VariableTree.class, VariableTree::getModifiers);
        case "ModifiersTree" -> {
          ModifiersTree modifiersTree = mock(ModifiersTree.class);
          assertThat(ASTHelpers.getModifiers(modifiersTree)).isSameInstanceAs(modifiersTree);
        }
        default -> assertMethodNotFound(clazz, "getModifiers");
      }
    }
  }

  private static <T extends Tree> void assertGetModifiersInvoked(
      Class<T> clazz, Function<T, ModifiersTree> getter) {
    T tree = mock(clazz);
    var unused = ASTHelpers.getModifiers(tree);

    // This effectively means the same as {@code verify(tree).getModifiers()}.
    ModifiersTree ignored = getter.apply(verify(tree));
  }

  @Test
  public void typesWithGetAnnotations() {
    for (Class<?> clazz : ALL_TREE_TYPES) {
      switch (clazz.getSimpleName()) {
        case "TypeParameterTree" ->
            assertGetAnnotationsInvoked(TypeParameterTree.class, TypeParameterTree::getAnnotations);
        case "ModuleTree" ->
            assertGetAnnotationsInvoked(ModuleTree.class, ModuleTree::getAnnotations);
        case "PackageTree" ->
            assertGetAnnotationsInvoked(PackageTree.class, PackageTree::getAnnotations);
        case "NewArrayTree" ->
            assertGetAnnotationsInvoked(NewArrayTree.class, NewArrayTree::getAnnotations);
        case "ModifiersTree" ->
            assertGetAnnotationsInvoked(ModifiersTree.class, ModifiersTree::getAnnotations);
        case "AnnotatedTypeTree" ->
            assertGetAnnotationsInvoked(AnnotatedTypeTree.class, AnnotatedTypeTree::getAnnotations);
        default -> assertMethodNotFound(clazz, "getAnnotations");
      }
    }
  }

  private static <T extends Tree> void assertGetAnnotationsInvoked(
      Class<T> clazz, Function<T, List<? extends AnnotationTree>> getter) {
    T tree = mock(clazz);
    var unused = ASTHelpers.getAnnotations(tree);
    // This effectively means the same as {@code verify(tree).getAnnotations()}.
    List<? extends AnnotationTree> ignored = getter.apply(verify(tree));
  }

  private static void assertMethodNotFound(Class<?> clazz, String method, Class<?>... params) {
    assertThrows(NoSuchMethodException.class, () -> clazz.getMethod(method, params));
  }

  /** Helper for testing {@link ASTHelpers#enclosingPackage}. */
  @BugPattern(summary = "", severity = WARNING)
  public static final class EnclosingPackage extends BugChecker
      implements VariableTreeMatcher, MemberReferenceTreeMatcher {
    @Override
    public Description matchVariable(VariableTree tree, VisitorState state) {
      Symbol sym = ASTHelpers.getSymbol(tree);
      if (sym.getKind().equals(ElementKind.FIELD)) {
        return check(tree.getType());
      }
      return Description.NO_MATCH;
    }

    @Override
    public Description matchMemberReference(MemberReferenceTree tree, VisitorState state) {
      return check(tree);
    }

    Description check(Tree tree) {
      PackageSymbol packageSymbol = ASTHelpers.enclosingPackage(ASTHelpers.getSymbol(tree));
      return buildDescription(tree).setMessage(String.format("[%s]", packageSymbol)).build();
    }
  }

  @Test
  public void enclosingPackage() {
    CompilationTestHelper.newInstance(EnclosingPackage.class, getClass())
        .addSourceLines(
            "Test.java",
            """
            package p;
            import java.util.function.Function;
            import java.util.function.IntFunction;
            import java.util.stream.Stream;
            class Test {
              // BUG: Diagnostic contains: [java.util.function]
              Function<?, ?> f;
              // BUG: Diagnostic contains: [p]
              Test t;
              {
              // BUG: Diagnostic contains: []
                Stream.of().toArray(IntFunction[]::new);
              }
            }
            """)
        .doTest();
  }

  /**
   * Test checker to ensure that ASTHelpers.hasDirectAnnotationWithSimpleName handles type
   * annotations.
   */
  @BugPattern(
      severity = SeverityLevel.ERROR,
      summary =
          "Test checker to ensure that ASTHelpers.hasDirectAnnotationWithSimpleName handles type"
              + " annotations")
  public static class HasNullableAnnotationChecker extends BugChecker
      implements MethodTreeMatcher, VariableTreeMatcher {
    @Override
    public Description matchVariable(VariableTree tree, VisitorState state) {
      return match(tree, state);
    }

    @Override
    public Description matchMethod(MethodTree tree, VisitorState state) {
      return match(tree, state);
    }

    Description match(Tree tree, VisitorState state) {
      if (ASTHelpers.hasDirectAnnotationWithSimpleName(ASTHelpers.getSymbol(tree), "Nullable")) {
        return describeMatch(tree);
      }
      return Description.NO_MATCH;
    }
  }

  @Test
  public void testHasDirectOrTypeAnnotationWithSimpleName() {
    CompilationTestHelper.newInstance(HasNullableAnnotationChecker.class, getClass())
        .addSourceLines(
            "Declaration.java",
            """
            import static java.lang.annotation.ElementType.METHOD;
            import static java.lang.annotation.ElementType.FIELD;
            import java.lang.annotation.Target;
            class Declaration {
              @Target({METHOD, FIELD})
              @interface Nullable {}
            }
            """)
        .addSourceLines(
            "TypeUse.java",
            """
            import static java.lang.annotation.ElementType.TYPE_USE;
            import java.lang.annotation.Target;
            class TypeUse {
              @Target(TYPE_USE)
              @interface Nullable {}
            }
            """)
        .addSourceLines(
            "Test.java",
            """
            abstract class Test {
              // BUG: Diagnostic contains:
              @Declaration.Nullable public abstract Integer f();
              // BUG: Diagnostic contains:
              public abstract @TypeUse.Nullable Integer g();
              public abstract Integer i();
              // BUG: Diagnostic contains:
              @Declaration.Nullable public Integer x;
              // BUG: Diagnostic contains:
              public @TypeUse.Nullable Integer y;
              public Integer z;
            }
            """)
        .doTest();
  }

  /** Helper for testing {@link ASTHelpers#getTypeSubstitution}. */
  @BugPattern(summary = "", severity = WARNING)
  public static final class GetTypeSubstitution extends BugChecker
      implements MethodInvocationTreeMatcher {

    @Override
    public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
      return buildDescription(tree)
          .setMessage(
              ASTHelpers.getTypeSubstitution(
                      ASTHelpers.getType(tree.getMethodSelect()).asMethodType(),
                      ASTHelpers.getSymbol(tree))
                  .toString())
          .build();
    }
  }

  @Test
  public void getTypeSubstitution() {
    CompilationTestHelper.newInstance(GetTypeSubstitution.class, getClass())
        .addSourceLines(
            "Test.java",
            """
            package p;

            import java.util.List;

            class Test {
              <T> void f(T[] t) {}

              <T> void g(List<T> t) {}

              void test(Integer[] i, List<String> s) {
                // BUG: Diagnostic contains: {}
                f(i);
                // BUG: Diagnostic contains: {T=[java.lang.String]}
                g(s);
              }
            }
            """)
        .doTest();
  }
}
