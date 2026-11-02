/*
 * Copyright 2018 The Error Prone Authors.
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
package com.google.errorprone.bugpatterns;

import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.annotations.Var;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * @author cushon@google.com (Liam Miller-Cushon)
 */
@RunWith(JUnit4.class)
public class FieldCanBeFinalTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(FieldCanBeFinal.class, getClass());

  @Test
  public void annotationFieldsAreAlreadyFinal() {
    compilationHelper
        .addSourceLines(
            "Anno.java",
            """
            public @interface Anno {
              int x = 42;
              static int y = 42;
            }
            """)
        .doTest();
  }

  @Test
  public void simple() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              // BUG: Diagnostic contains: private final int x
              private int x;

              Test() {
                x = 42;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void keepAnnotatedFields_ignored() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import com.google.errorprone.annotations.Keep;

            class Test {
              @Keep private int x;

              Test() {
                x = 42;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void injectAnnotatedFields_ignored() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import javax.inject.Inject;

            class Test {
              @Inject private int x;

              Test() {
                x = 42;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void initializerBlocks() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              // BUG: Diagnostic contains: private final int x1
              private int x1;
              private int x2;
              private int x3;
              // BUG: Diagnostic contains: private static final int y1
              private static int y1;
              private static int y2;
              private static int y3;

              {
                x1 = 42;
                x2 = 42;
                x3 = 42;
                x3 = 0;
              }

              static {
                y1 = 42;
                y2 = 42;
                y3 = 42;
                y3 = 0;
              }

              void mutate() {
                x2 = 0;
                y2 = 0;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void staticSetFromInstance() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              // BUG: Diagnostic contains: private static final int x1
              private static int x1;
              private static int x2;

              static {
                x1 = 42;
                x2 = 42;
              }

              {
                x2 = 42;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void suppressionOnField() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              @SuppressWarnings("FieldCanBeFinal")
              private int x;

              Test() {
                x = 42;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void suppressionOnClass() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            @SuppressWarnings("FieldCanBeFinal")
            class Test {
              private int x;

              Test() {
                x = 42;
              }
            }
            """)
        .doTest();
  }

  // the nullary constructor doesn't set x directly, but that's OK
  @Test
  public void constructorChaining() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              // BUG: Diagnostic contains: private final int x
              private int x;

              Test(int x) {
                this.x = x;
              }

              Test() {
                this(42);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void controlFlow() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              private int x;

              Test(boolean flag, int x, int y) {
                if (flag) {
                  this.x = x;
                } else {
                  this.x = y;
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void doubleInitialization() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              private int x;

              Test(int x) {
                this.x = x;
                this.x = x;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void compoundAssignment() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              private int x;

              Test() {
                this.x = 42;
              }

              void incr() {
                x += 1;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void unaryAssignment() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              private int x;

              Test() {
                this.x = 42;
              }

              void incr() {
                x++;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void constructorAssignmentToOtherInstance() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              private int x;
              // BUG: Diagnostic contains: private final int y
              private int y;

              Test(Test other) {
                x = 42;
                y = 42;
                other.x = x;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void assignmentFromOutsideCompilationUnit() {
    compilationHelper
        .addSourceLines(
            "A.java",
            """
            class A {
              int x;

              A(B b) {
                x = 42;
                b.x = 42;
              }
            }
            """)
        .addSourceLines(
            "B.java",
            """
            class B {
              int x;

              B(A a) {
                x = 42;
                a.x = 42;
              }
            }
            """)
        .doTest();
  }

  // don't report an error if the field has an initializer and is also written
  // in the constructor
  @Test
  public void guardInitialization() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            String.format("import %s;", Var.class.getCanonicalName()),
            "class Test {",
            "  private boolean initialized = false;",
            "  Test() {",
            "    initialized = true;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void fieldInitialization() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              // BUG: Diagnostic contains: private final boolean flag
              private boolean flag = false;

              Test() {}
            }
            """)
        .doTest();
  }

  @Test
  public void ignoreInject() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import javax.inject.Inject;

            class Test {
              @Inject private Object x;

              Test() {
                this.x = x;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void allowNonFinal_nonFinalForTesting() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            @interface NonFinalForTesting {}

            class Test {
              @NonFinalForTesting private int x;

              Test(int x) {
                this.x = x;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void visibleForTesting() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import com.google.common.annotations.VisibleForTesting;

            class Test {
              @VisibleForTesting public int x;

              Test() {
                x = 42;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void protectedField() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              protected int x;

              Test() {
                x = 42;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void nonPrivateField() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              public int x;
              int y;

              Test() {
                x = 42;
                y = 42;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void allowObjectifyClasses() {
    compilationHelper
        .addSourceLines(
            "com/googlecode/objectify/v4/annotation/Entity.java",
            """
            package com.googlecode.objectify.v4.annotation;

            public @interface Entity {}
            """)
        .addSourceLines(
            "Test.java",
            """
            import com.googlecode.objectify.v4.annotation.Entity;

            @Entity
            class Test {
              private int x;

              Test(int x) {
                this.x = x;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void initInLambda() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              private int x;
              private final Runnable r;

              Test() {
                r = () -> x = 1;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void constructorAssignmentWithLambdaReassignment() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              private Runnable r;

              Test() {
                r = foo(() -> r = null);
              }

              private static Runnable foo(Runnable r) {
                return r;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void conditionalInitialization() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              private int x;
              private int y;

              Test(boolean flag) {
                if (flag) {
                  x = 1;
                }
                for (int i = 0; i < 2; i++) {
                  y = i;
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void conditionalInitialization_earlyReturn() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              private int x;

              Test(boolean flag) {
                if (flag) {
                  return;
                }
                x = 1;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void initializationAfterStatements() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              // BUG: Diagnostic contains: private final int x
              private int x;
              // BUG: Diagnostic contains: private final int z
              private int z;

              Test(int y, AutoCloseable c) throws Exception {
                if (y < 0) {
                  throw new IllegalArgumentException();
                }
                try {
                  System.err.println(y);
                } catch (RuntimeException e) {
                  System.err.println(e);
                }
                switch (y) {
                  case 0 -> System.err.println();
                  default -> {}
                }
                x = y;
                try (c) {
                  z = y;
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void initializationInTryAndCatch() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              private int x;

              Test(String s) {
                try {
                  x = Integer.parseInt(s);
                } catch (NumberFormatException e) {
                  x = 0;
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void reassignmentInDelegatingConstructor() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              private int x;

              Test(int x) {
                this.x = x;
              }

              Test() {
                this(0);
                if (Math.random() > 0.5) {
                  x = 1;
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void multipleFields_someCannotBeFinal() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              // BUG: Diagnostic contains: private final int a
              private int a;
              private int b;
              // BUG: Diagnostic contains: private final int c
              private int c;
              private int d;
              private int e;

              Test(boolean flag) {
                a = 1;
                try {
                  b = Integer.parseInt("1");
                } catch (NumberFormatException e) {
                  b = 0;
                }
                c = 3;
                Runnable r = () -> System.err.println(d);
                d = 4;
                e = 5;
                e = 6;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void multipleFields_sameNameInNestedClasses() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              // BUG: Diagnostic contains: private final int x
              private int x;
              private int y;

              Test(boolean flag) {
                x = 1;
                try {
                  y = Integer.parseInt("1");
                } catch (NumberFormatException e) {
                  y = 0;
                }
              }

              static class Inner {
                private int x;
                // BUG: Diagnostic contains: private final int y
                private int y;

                Inner() {
                  try {
                    x = Integer.parseInt("1");
                  } catch (NumberFormatException e) {
                    x = 0;
                  }
                  y = 2;
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void returnBeforeInitialization() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              private int x;
              // BUG: Diagnostic contains: private final int y
              private int y;

              Test(boolean flag) {
                y = 1;
                if (flag) {
                  System.err.println();
                  return;
                }
                x = 1;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void readInInitializerLambda() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              private int x;
              private final Runnable r = () -> System.err.println(x);

              Test() {
                x = 1;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void readInInitializerAnonymousClass() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              private final Runnable r =
                  new Runnable() {
                    @Override
                    public void run() {
                      System.err.println(x);
                    }
                  };
              // BUG: Diagnostic contains: private final int x
              private int x;

              Test() {
                x = 1;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void fieldWithInitializerReadByAnotherField() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              // BUG: Diagnostic contains: private final int a = 1;
              private int a = 1;
              private final int b = a + 1;
              private final int c = this.d;
              private int d = 1;
              private int e = this.e;

              {
                System.err.println(this.f);
              }

              private int f = 1;
            }
            """)
        .doTest();
  }

  @Test
  public void assignedFromOtherClasses() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              static class N {
                private static int a;

                static {
                  a = 1;
                }
              }

              static {
                N.a = 2;
              }

              private static class Sub extends N {
                Sub() {
                  N.a = 3;
                }
              }

              void m() {
                class A {
                  int c;

                  A() {
                    c = 1;
                  }
                }

                class B extends A {
                  B() {
                    this.c = 2;
                  }
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void qualifiedStaticAssignment() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              private static int f;

              static {
                Test.f = 1;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void returnInFinally() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              private int f;
              private int g;

              Test(boolean c) {
                try {
                  if (c) {
                    throw new RuntimeException();
                  }
                  f = 1;
                } finally {
                  return;
                }
              }

              Test() {
                try {
                  g = 1;
                } finally {
                  if (Math.random() > 0.5) {
                    return;
                  }
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void multipleVariablesInDeclaration() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              private int f, g;

              Test() {
                f = 1;
                g = 1;
              }

              void m() {
                g = 2;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void exitFromTryWithFinallyAfterAssignment() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              private int f;

              Test(boolean c) {
                try {
                  if (c) {
                    f = 1;
                    return;
                  }
                } finally {
                  System.out.println();
                }
                f = 2;
              }

              int get() {
                return f;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void memberSelectOfPrimitiveClassLiteral() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              // BUG: Diagnostic contains: private final int f
              private int f;

              Test() {
                Class<?> c = int.class;
                f = 1;
              }

              int get() {
                return f;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void readsNotSubjectToDefiniteAssignment() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              // BUG: Diagnostic contains: private final int a
              private int a;
              // BUG: Diagnostic contains: private final int b
              private int b;

              Test(Test other) {
                a = other.a;
                Runnable r =
                    new Runnable() {
                      @Override
                      public void run() {
                        System.err.println(b);
                      }
                    };
                class Local {
                  int get() {
                    return b;
                  }
                }
                b = new Local().get();
              }

              Test() {
                a = 1;
                b = 2;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void readInLambdaBeforeAssignment() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              private int x;

              Test() {
                Runnable r = () -> System.err.println(x);
                x = 1;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void parenthesizedThisReadBeforeAssignment() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              private int f;

              Test() {
                int x = (this).f;
                f = 1;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void readInAnonymousClassInitializerBeforeAssignment() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              private int a;
              private int b;
              private int c;
              private static int d;
              private int e;
              // BUG: Diagnostic contains: private final int g
              private int g =
                  new Object() {
                    final int y = e;
                  }.y;

              Test() {
                new Object() {
                  final int y = a;
                };
                a = 1;
                new Object() {
                  {
                    System.err.println(b);
                  }
                };
                b = 1;
                c =
                    new Object() {
                      final int y = c;
                    }.y;
                e = 1;
              }

              static {
                new Object() {
                  final int y = d;
                };
                d = 1;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void volatileField() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              private volatile int f;

              Test() {
                f = 1;
              }
            }
            """)
        .doTest();
  }
}
