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

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Test for InconsistentCapitalization bug checker */
@RunWith(JUnit4.class)
public class InconsistentCapitalizationTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(InconsistentCapitalization.class, getClass());
  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(InconsistentCapitalization.class, getClass());

  @Test
  public void negativeCases() {
    compilationHelper
        .addSourceLines(
            "InconsistentCapitalizationNegativeCases.java",
"""
package com.google.errorprone.bugpatterns.testdata;

/** Negative cases for {@link com.google.errorprone.bugpatterns.InconsistentCapitalizationTest}. */
public class InconsistentCapitalizationNegativeCases {

  public void doesntConflictWithOtherVariables() {
    int aa;
    int aA;
  }

  public void doesntConflictWithVariableOutOfScope() {
    if (true) {
      int a;
    }
    if (true) {
      int a;
    }
  }

  public void doesntConflictBetweenForVariables() {
    for (int i = 0; i < 1; i++) {}

    for (int i = 0; i < 1; i++) {}
  }

  private class DoesntConflictBetweenMethods {
    int a;

    void a() {}

    void b(int baba) {
      int c = baba;
      if (c == baba) {}
    }

    void c() {
      int c;
    }
  }

  private static class DoesntConflictWithClass {

    static int B;

    static class A {

      static int A;
    }

    class B {}
  }

  private static class DoesAllowUpperCaseStaticVariable {

    static int A;

    void method() {
      int a;
    }
  }

  private enum DoesntConflictWithUpperCaseEnum {
    TEST;

    private Object test;
  }

  public void doesntConflictWithMethodParameter(long aa) {
    int aA;
  }

  private class DoesntConflictWithConstructorParameter {

    DoesntConflictWithConstructorParameter(Object aa) {
      Object aA;
    }
  }

  private class DoesntConflictOutOfScope {

    class A {
      private Object aaa;
      private Object aab;
    }

    class B {
      private Object aaA;

      void method(String aaB) {
        char aAb;
      }
    }
  }

  private static class DoesntReplaceMember {

    class A {
      Object aa;
      Object ab;

      void method() {
        B b = new B();
        aa = b.aA;
        ab = b.aB.aA;
        new B().aA();
        aa.equals(ab);
        aa.equals(b.aB.aA);
        aa.equals(b.aB);
      }
    }

    class B {
      Object aA;
      C aB = new C();

      void aA() {}
    }

    class C {
      Object aA;
    }
  }

  class DoesntConflictWithNested {
    Object aa;
    Object ab;

    class Nested {
      Object aB;

      Nested(Object aa) {
        DoesntConflictWithNested.this.aa = aa;
      }

      class Nested2 {
        Object aB;

        Nested2(Object aa) {
          DoesntConflictWithNested.this.aa = aa;
        }
      }
    }
  }

  static class DoesntFixExternalParentClassFieldMatch {

    static class Parent {
      Object aa;
    }

    static class Child extends Parent {

      Child(Object aA) {
        aa = aA;
      }
    }
  }
  
  static class HandlesFieldsWithInconsistentCapitalization {
    
    private int abc;
    private int ABC;
    
    void foo(int ABC) {
    
    }
  }
}\
""")
        .doTest();
  }

  @Test
  public void correctsInconsistentVariableNameInMethodDefinitionToFieldCase() {
    refactoringHelper
        .addInputLines(
            "in/Test.java",
            """
            class Test {
              Object aa;

              void method(Object aA) {
                this.aa = aA;
              }
            }
            """)
        .addOutputLines(
            "out/Test.java",
            """
            class Test {
              Object aa;

              void method(Object aa) {
                this.aa = aa;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void correctsInconsistentVariableNameInConstructorDefinitionToFieldCase() {
    refactoringHelper
        .addInputLines(
            "in/Test.java",
            """
            class Test {
              Object aa;

              Test(Object aA) {
                this.aa = aA;
              }
            }
            """)
        .addOutputLines(
            "out/Test.java",
            """
            class Test {
              Object aa;

              Test(Object aa) {
                this.aa = aa;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void correctsInconsistentVariableNameInLambdaDefinitionToFieldCase() {
    refactoringHelper
        .addInputLines(
            "in/Test.java",
            """
            import java.util.function.Function;

            class Test {
              Object ea;

              Test() {
                Function<Void, Object> f =
                    (eA) -> {
                      this.ea = eA;
                      return eA;
                    };
              }
            }
            """)
        .addOutputLines(
            "out/Test.java",
            """
            import java.util.function.Function;

            class Test {
              Object ea;

              Test() {
                Function<Void, Object> f =
                    (ea) -> {
                      this.ea = ea;
                      return ea;
                    };
              }
            }
            """)
        .doTest();
  }

  @Test
  public void
      correctsInconsistentVariableNameInConstructorDefinitionWithMultipleOccurrencesToFieldCase() {
    refactoringHelper
        .addInputLines(
            "in/Test.java",
            """
            class Test {
              Object aa;

              Test(Object aA) {
                this.aa = aA;
                if (aA == this.aa) {
                  for (Object i = aA; ; ) {}
                }
              }
            }
            """)
        .addOutputLines(
            "out/Test.java",
            """
            class Test {
              Object aa;

              Test(Object aa) {
                this.aa = aa;
                if (aa == this.aa) {
                  for (Object i = aa; ; ) {}
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void correctsInconsistentVariableNameToFieldCaseAndQualifiesField() {
    refactoringHelper
        .addInputLines(
            "in/Test.java",
            """
            class Test {
              Object aa;

              Test(Object aA) {
                aa = aA;
                if (aA == aa) {}
              }
            }
            """)
        .addOutputLines(
            "out/Test.java",
            """
            class Test {
              Object aa;

              Test(Object aa) {
                this.aa = aa;
                if (aa == this.aa) {}
              }
            }
            """)
        .doTest();
  }

  @Test
  public void correctsInconsistentVariableNameToFieldCaseAndQualifiesNestedClassField() {
    refactoringHelper
        .addInputLines(
            "in/Test.java",
            """
            import java.util.function.Function;

            class Test {
              Object aa;
              Object ab;

              class Nested {
                Object aB;

                Nested(Object aA) {
                  aa = aA;
                  if (aa == aA) {}
                  Test.this.aa = aA;
                }
              }
            }
            """)
        .addOutputLines(
            "out/Test.java",
            """
            import java.util.function.Function;

            class Test {
              Object aa;
              Object ab;

              class Nested {
                Object aB;

                Nested(Object aa) {
                  Test.this.aa = aa;
                  if (Test.this.aa == aa) {}
                  Test.this.aa = aa;
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void correctsInconsistentVariableNameToFieldCaseAndQualifiesNestedChildClassField() {
    refactoringHelper
        .addInputLines(
            "in/Test.java",
            """
            import java.util.function.Function;

            class Test {
              static class A {
                Object aa;

                static class Nested extends A {
                  Nested(Object aA) {
                    aa = aA;
                    if (aa == aA) {}
                    super.aa = aA;
                  }
                }
              }
            }
            """)
        .addOutputLines(
            "out/Test.java",
            """
            import java.util.function.Function;

            class Test {
              static class A {
                Object aa;

                static class Nested extends A {
                  Nested(Object aa) {
                    super.aa = aa;
                    if (super.aa == aa) {}
                    super.aa = aa;
                  }
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void
      correctsInconsistentVariableNameToFieldCaseInAnonymousClassAndQualifiesNestedChildClassField() {
    refactoringHelper
        .addInputLines(
            "in/Test.java",
            """
            import java.util.function.Function;

            class Test {
              Object aa;
              Function<Object, Object> f =
                  new Function() {
                    public Object apply(Object aA) {
                      aa = aA;
                      return aA;
                    }
                  };
            }
            """)
        .addOutputLines(
            "out/Test.java",
            """
            import java.util.function.Function;

            class Test {
              Object aa;
              Function<Object, Object> f =
                  new Function() {
                    public Object apply(Object aa) {
                      Test.this.aa = aa;
                      return aa;
                    }
                  };
            }
            """)
        .doTest();
  }

  // regression test for https://github.com/google/error-prone/issues/999
  @Test
  public void clash() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              Object _DocumentObjectData_QNAME;
              Object _DocumentObjectdata_QNAME;
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              Object _DocumentObjectData_QNAME;
              Object _DocumentObjectdata_QNAME;
            }
            """)
        .doTest();
  }

  @Test
  public void i1008() {
    compilationHelper
        .addSourceLines(
            "Callback.java",
            """
            public class Callback {
              interface WaitHandler {} // ignore

              private final WaitHandler waitHandler;

              // BUG: Diagnostic contains:
              protected Callback(final WaitHandler waithandler) {
                this.waitHandler = waithandler;
              }

              public static Callback doOnSuccess() {
                return new Callback(null) {};
              }
            }
            """)
        .doTest();
  }
}
