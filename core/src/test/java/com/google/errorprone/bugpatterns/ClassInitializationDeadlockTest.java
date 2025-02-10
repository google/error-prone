/*
 * Copyright 2024 The Error Prone Authors.
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

import com.google.auto.value.processor.AutoValueProcessor;
import com.google.errorprone.CompilationTestHelper;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ClassInitializationDeadlockTest {

  private final CompilationTestHelper testHelper =
      CompilationTestHelper.newInstance(ClassInitializationDeadlock.class, getClass());

  @Test
  public void positive() {
    testHelper
        .addSourceLines(
            "A.java",
            """
            public class A {
              // BUG: Diagnostic contains:
              private static Object cycle = new B();

              public static class B extends A {}
            }
            """)
        .doTest();
  }

  @Test
  public void negativeMethod() {
    testHelper
        .addSourceLines(
            "A.java",
            """
            public class A {
              private static Object cycle =
                  new Object() {
                    void f() {
                      new B();
                    }
                  };

              public static class B extends A {}
            }
            """)
        .doTest();
  }

  @Test
  public void negativeEnum() {
    testHelper
        .addSourceLines(
            "E.java",
            """
            enum E {
              ONE(0),
              TWO {
                void f() {}
              };

              E(int x) {}

              E() {}
            }
            """)
        .doTest();
  }

  @Test
  public void negativePrivate() {
    testHelper
        .addSourceLines(
            "A.java",
            """
            public class A {
              private static Object benign_cycle = new B.C();

              private static class B {
                public static class C extends A {}
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negativeAutoValue() {
    testHelper
        .setArgs("-processor", AutoValueProcessor.class.getName())
        .addSourceLines(
            "A.java",
            """
            import com.google.auto.value.AutoValue;

            @AutoValue
            abstract class Animal {
              private static final Animal WALLABY = new AutoValue_Animal("Wallaby", 4);

              static Animal create(String name, int numberOfLegs) {
                return new AutoValue_Animal(name, numberOfLegs);
              }

              abstract String name();

              abstract int numberOfLegs();
            }
            """)
        .doTest();
  }

  @Test
  public void negative() {
    testHelper
        .addSourceLines(
            "A.java",
            """
            public class A {
              static Object constant = 1;
              Object nonStatic = new B();
              static Class<?> classLiteral = B.class;

              static class B extends A {}
            }
            """)
        .doTest();
  }

  @Test
  public void negativeInterface() {
    testHelper
        .addSourceLines(
            "A.java",
            """
            public interface A {
              Object cycle = new B();

              public static class B implements A {}
            }
            """)
        .doTest();
  }

  @Test
  public void positiveInterfaceDefaultMethod() {
    testHelper
        .addSourceLines(
            "A.java",
            """
            public interface A {
              // BUG: Diagnostic contains:
              Object cycle = new B();

              default void f() {}

              public static class B implements A {}
            }
            """)
        .doTest();
  }

  @Test
  public void negativePrivateConstructor() {
    testHelper
        .addSourceLines(
            "A.java",
            """
            public class A {
              private static Object cycle = new B();

              public static final class B extends A {
                private B() {}
              }
            }
            """)
        .doTest();
  }

  @Test
  public void positivePrivateConstructorFactoryMethod() {
    testHelper
        .addSourceLines(
            "A.java",
            """
            public class A {
              // BUG: Diagnostic contains:
              private static Object cycle = new B();

              public static final class B extends A {
                private B() {}

                public static B create() {
                  return new B();
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void positivePrivateConstructorFactoryMethodNonStatic() {
    testHelper
        .addSourceLines(
            "A.java",
            """
            public class A {
              private static Object cycle = new B();

              public static final class B extends A {
                private B() {}

                public B create() {
                  return new B();
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negativeNonStaticInner() {
    testHelper
        .addSourceLines(
            "A.java",
            """
            public class A {
              private static Object cycle = new A().new B();

              public class B extends A {}
            }
            """)
        .doTest();
  }

  @Test
  public void negativeSelf() {
    testHelper
        .addSourceLines(
            "A.java",
            """
            public class A {
              public static class B extends A {
                private static B self = new B();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negativePrivateInterface() {
    testHelper
        .addSourceLines(
            "A.java",
            """
            public class A {
              private interface I {}

              static final I i = new I() {};
            }
            """)
        .doTest();
  }

  @Test
  public void intermediateNonPrivate() {
    testHelper
        .addSourceLines(
            "A.java",
            """
public class A {
  // BUG: Diagnostic contains: C is a subclass of the containing class A (via B, which can be
  // initialized from outside the current file)
  public static final C i = new C();

  public static class B extends A {}

  private static class C extends B {}
}
""")
        .doTest();
  }

  @Test
  public void negativeNonPrivateUnrelatedSuper() {
    testHelper
        .addSourceLines(
            "A.java",
            """
            public class A {
              public static final C i = new C();

              public interface B {
                default void f() {}
              }

              private static class C extends A implements B {}
            }
            """)
        .doTest();
  }

  @Test
  public void nestedEnum() {
    testHelper
        .addSourceLines(
            "TestInterface.java",
            """
            public interface TestInterface {
              default Object foo() {
                return null;
              }

              enum TestEnum implements TestInterface {
                INSTANCE;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void nestedInterface() {
    testHelper
        .addSourceLines(
            "Foo.java",
            """
            interface Foo {
              default void foo() {}

              interface Sub extends Foo {
                final Sub INSTANCE = new Sub() {};
              }
            }
            """)
        .doTest();
  }

  @Test
  public void nonNestedSubclass() {
    testHelper
        .addSourceLines(
            "Foo.java",
            """
            class A {
              // BUG: Diagnostic contains:
              private static Object cycle = new B();
            }

            class B extends A {}
            """)
        .doTest();
  }

  @Test
  public void negativeAutoValueExtension() {
    testHelper
        .addSourceLines(
            "$$AutoValue_Foo.java",
            """
            class $$AutoValue_Foo extends Foo {}
            """)
        .addSourceLines(
            "A.java",
            """
            import com.google.auto.value.AutoValue;

            @AutoValue
            abstract class Foo {
              private static final Foo FOO = new $$AutoValue_Foo();
            }
            """)
        .doTest();
  }

  @Test
  public void simpleSubclassMethodReference() {
    testHelper
        .addSourceLines(
            "Foo.java",
            "import java.util.function.Supplier;",
            "class A {",
            "  static Supplier<B> supplier = B::new;",
            "}",
            "class B extends A {}")
        .doTest();
  }

  @Test
  public void compoundSubclassMethodReference() {
    testHelper
        .addSourceLines(
            "Foo.java",
            "import java.util.Comparator;",
            "class A {",
            "  static Comparator<B> comparator = Comparator.comparing(B::value);",
            "}",
            "class B extends A {",
            "  int value;",
            "  int value() {",
            "    return value;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void lambda() {
    testHelper
        .addSourceLines(
            "Foo.java",
            "import java.util.function.Supplier;",
            "class A {",
            "  static Supplier<B> supplier = () -> new B();",
            "}",
            "class B extends A {}")
        .doTest();
  }

  @Test
  public void subclassStaticMethod() {
    testHelper
        .addSourceLines(
            "Foo.java",
            "class A {",
            "  // BUG: Diagnostic contains:",
            "  static int value = B.value(); ",
            "}",
            "class B extends A {",
            "  static int value() { return 0; }",
            "}")
        .doTest();
  }
}
