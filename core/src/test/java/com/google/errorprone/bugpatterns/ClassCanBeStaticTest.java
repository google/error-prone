/*
 * Copyright 2012 The Error Prone Authors.
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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link ClassCanBeStatic}Test */
@RunWith(JUnit4.class)
public class ClassCanBeStaticTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(ClassCanBeStatic.class, getClass());

  @Test
  public void negativeCase() {
    compilationHelper
        .addSourceLines(
            "ClassCanBeStaticNegativeCases.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            /**
             * @author alexloh@google.com (Alex Loh)
             */
            public class ClassCanBeStaticNegativeCases {
              int outerVar;

              public int outerMethod() {
                return 0;
              }

              public static class Inner1 { // inner class already static
                int innerVar;
              }

              public class Inner2 { // inner class references an outer variable
                int innerVar = outerVar;
              }

              public class Inner3 { // inner class references an outer variable in a method
                int localMethod() {
                  return outerVar;
                }
              }

              public class Inner4 { // inner class references an outer method in a method
                int localMethod() {
                  return outerMethod();
                }
              }

              // outer class is a nested but non-static, and thus cannot have a static class
              class NonStaticOuter {
                int nonStaticVar = outerVar;

                class Inner5 {}
              }

              // inner class is local and thus cannot be static
              void foo() {
                class Inner6 {}
              }

              // inner class is anonymous and thus cannot be static
              Object bar() {
                return new Object() {};
              }

              // enums are already static
              enum Inner7 {
                RED,
                BLUE,
                VIOLET,
              }

              // outer class is a nested but non-static, and thus cannot have a static class
              void baz() {
                class NonStaticOuter2 {
                  int nonStaticVar = outerVar;

                  class Inner8 {}
                }
              }

              // inner class references a method from inheritance
              public interface OuterInter {
                int outerInterMethod();
              }

              abstract static class AbstractOuter implements OuterInter {
                class Inner8 {
                  int localMethod() {
                    return outerInterMethod();
                  }
                }
              }
            }\
            """)
        .setArgs("--release", "11")
        .doTest();
  }

  @Test
  public void positiveCase1() {
    compilationHelper
        .addSourceLines(
            "ClassCanBeStaticPositiveCase1.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            /**
             * @author alexloh@google.com (Alex Loh)
             */
            public class ClassCanBeStaticPositiveCase1 {

              int outerVar;

              // Non-static inner class that does not use outer scope
              // BUG: Diagnostic contains: static class Inner1
              class Inner1 {
                int innerVar;
              }
            }\
            """)
        .doTest();
  }

  @Test
  public void positiveCase2() {
    compilationHelper
        .addSourceLines(
            "ClassCanBeStaticPositiveCase2.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            /**
             * @author alexloh@google.com (Alex Loh)
             */
            public class ClassCanBeStaticPositiveCase2 {

              int outerVar1;
              int outerVar2;

              // Outer variable overridden
              // BUG: Diagnostic contains: private /* COMMENT */ static final class Inner2
              private /* COMMENT */ final class Inner2 {
                int outerVar1;
                int innerVar = outerVar1;

                int localMethod(int outerVar2) {
                  return outerVar2;
                }
              }
            }\
            """)
        .doTest();
  }

  @Test
  public void positiveCase3() {
    compilationHelper
        .addSourceLines(
            "ClassCanBeStaticPositiveCase3.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            /**
             * @author alexloh@google.com (Alex Loh)
             */
            public class ClassCanBeStaticPositiveCase3 {

              static int outerVar;

              // Nested non-static inner class inside a static inner class
              static class NonStaticOuter {
                int nonStaticVar = outerVar;

                // BUG: Diagnostic contains: public static class Inner3
                public class Inner3 {}
              }
            }
            """)
        .doTest();
  }

  @Test
  public void positiveReference() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              // BUG: Diagnostic contains:
              private class One {
                int field;
              }

              // BUG: Diagnostic contains:
              private class Two {
                String field;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void nonMemberField() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              int x;

              private class One {
                {
                  System.err.println(x);
                }
              }

              // BUG: Diagnostic contains:
              private class Two {
                void f(Test t) {
                  System.err.println(t.x);
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void qualifiedThis() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              private class One {
                {
                  System.err.println(Test.this);
                }
              }

              // BUG: Diagnostic contains:
              private class Two {
                void f(Test t) {
                  System.err.println(Test.class);
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void referencesSibling() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              private class One {
                {
                  new Two();
                }
              }

              private class Two {
                void f(Test t) {
                  System.err.println(Test.this);
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void referenceInAnonymousClass() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              private class Two {
                {
                  new Runnable() {
                    @Override
                    public void run() {
                      System.err.println(Test.this);
                    }
                  }.run();
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void extendsInnerClass() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              private class One {
                {
                  System.err.println(Test.this);
                }
              }

              private class Two extends One {}
            }
            """)
        .doTest();
  }

  @Test
  public void ctorParametricInnerClass() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              private class One<T> {
                {
                  System.err.println(Test.this);
                }
              }

              private abstract class Two {
                {
                  new One<String>();
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void extendsParametricInnerClass() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              private class One<T> {
                {
                  System.err.println(Test.this);
                }
              }

              private abstract class Two<T> extends One<T> {}
            }
            """)
        .doTest();
  }

  @Test
  public void referencesTypeParameter() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import java.util.List;

            class Test<T> {
              private class One {
                List<T> xs;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void referencesTypeParameterImplicit() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import java.util.List;

            class Test<T> {
              class One {
                {
                  System.err.println(Test.this);
                }
              }

              class Two {
                One one; // implicit reference of Test<T>.One
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negative_referencesTypeParameterImplicit() {
    compilationHelper
        .addSourceLines(
            "One.java",
            """
            package test;

            public class One<T> {
              public class Inner {
                {
                  System.err.println(One.this);
                }
              }
            }
            """)
        .addSourceLines(
            "Test.java",
            """
            import test.One.Inner;

            class Test {
              // BUG: Diagnostic contains:
              class Two {
                Inner inner; // ok: implicit reference of One.Inner
              }
            }
            """)
        .doTest();
  }

  @Test
  public void qualifiedSuperReference() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              class One {
                {
                  Test.super.getClass();
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void annotationMethod() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              // BUG: Diagnostic contains:
              class One {
                @SuppressWarnings(value = "")
                void f() {}
              }
            }
            """)
        .doTest();
  }

  @Test
  public void extendsHiddenInnerClass() {
    compilationHelper
        .addSourceLines(
            "A.java",
            """
            public class A {
              public class Inner {
                {
                  System.err.println(A.this);
                }
              }
            }
            """)
        .addSourceLines(
            "B.java",
            """
            public class B extends A {
              public class Inner extends A.Inner {}
            }
            """)
        .doTest();
  }

  @Test
  public void nestedInAnonymous() {
    compilationHelper
        .addSourceLines(
            "A.java",
            """
            public class A {
              static Runnable r =
                  new Runnable() {
                    class Inner {}

                    public void run() {}
                  };
            }
            """)
        .doTest();
  }

  @Test
  public void nestedInLocal() {
    compilationHelper
        .addSourceLines(
            "A.java",
            """
            public class A {
              static void f() {
                class Outer {
                  class Inner {}
                }
              }
            }
            """)
        .setArgs("--release", "11")
        .doTest();
  }

  @Test
  public void nestedInLocal_static() {
    compilationHelper
        .addSourceLines(
            "A.java",
            """
            public class A {
              static void f() {
                class Outer {
                  // BUG: Diagnostic contains:
                  class Inner {}
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void innerClassMethodReference() {
    compilationHelper
        .addSourceLines(
            "T.java",
            """
            import java.util.function.Supplier;

            public class T {
              class A {
                {
                  System.err.println(T.this);
                }
              }

              class B {
                {
                  Supplier<A> s = A::new; // capture enclosing instance
                  System.err.println(s.get());
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void labelledBreak() {
    compilationHelper
        .addSourceLines(
            "A.java",
            """
            public class A {
              // BUG: Diagnostic contains:
              class Inner {
                void f() {
                  OUTER:
                  while (true) {
                    break OUTER;
                  }
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void refaster() {
    compilationHelper
        .addSourceLines(
            "BeforeTemplate.java",
            """
            package com.google.errorprone.refaster.annotation;

            import java.lang.annotation.ElementType;
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;
            import java.lang.annotation.Target;

            @Target(ElementType.METHOD)
            @Retention(RetentionPolicy.SOURCE)
            public @interface BeforeTemplate {}
            """)
        .addSourceLines(
            "A.java",
            """
            import com.google.errorprone.refaster.annotation.BeforeTemplate;

            public class A {
              class Inner {
                @BeforeTemplate
                void f() {}
              }
            }
            """)
        .doTest();
  }

  @Test
  public void junitNestedClass() {
    compilationHelper
        .addSourceLines(
            "Nested.java",
            """
            package org.junit.jupiter.api;

            import java.lang.annotation.ElementType;
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;
            import java.lang.annotation.Target;

            @Target(ElementType.TYPE)
            @Retention(RetentionPolicy.RUNTIME)
            public @interface Nested {}
            """)
        .addSourceLines(
            "A.java",
            """
            import org.junit.jupiter.api.Nested;

            public class A {
              @Nested
              class Inner {
                void f() {}
              }
            }
            """)
        .doTest();
  }
}
