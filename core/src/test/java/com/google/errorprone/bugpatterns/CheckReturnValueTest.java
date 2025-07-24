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

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.base.Joiner;
import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * @author eaftan@google.com (Eddie Aftandilian)
 */
@RunWith(JUnit4.class)
public class CheckReturnValueTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(CheckReturnValue.class, getClass());

  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(CheckReturnValue.class, getClass());

  @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void positiveCases() {
    compilationHelper
        .addSourceLines(
            "CheckReturnValuePositiveCases.java",
"""
package com.google.errorprone.bugpatterns.testdata;

import com.google.errorprone.annotations.CheckReturnValue;
import org.junit.rules.ExpectedException;

/**
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public class CheckReturnValuePositiveCases {

  IntValue intValue = new IntValue(0);

  @CheckReturnValue
  private int increment(int bar) {
    return bar + 1;
  }

  public void foo() {
    int i = 1;
    // BUG: Diagnostic contains: The result of `increment(...)` must be used
    //
    // If you really don't want to use the result, then assign it to a variable: `var unused = ...`.
    //
    // If callers of `increment(...)` shouldn't be required to use its result, then annotate it with
    // `@CanIgnoreReturnValue`.
    increment(i);
    System.out.println(i);
  }

  public void bar() {
    // BUG: Diagnostic contains: this.intValue = this.intValue.increment()
    this.intValue.increment();
  }

  public void testIntValue() {
    IntValue value = new IntValue(10);
    // BUG: Diagnostic contains: value = value.increment()
    value.increment();
  }

  private void callRunnable(Runnable runnable) {
    runnable.run();
  }

  public void testResolvedToVoidLambda() {
    // BUG: Diagnostic contains:
    callRunnable(() -> this.intValue.increment());
  }

  public void testResolvedToVoidMethodReference(boolean predicate) {
    // BUG: Diagnostic contains: The result of `increment()` must be used
    //
    // `this.intValue::increment` acts as an implementation of `Runnable.run`
    // -- which is a `void` method, so it doesn't use the result of `increment()`.
    //
    // To use the result, you may need to restructure your code.
    //
    // If you really don't want to use the result, then switch to a lambda that assigns it to a
    // variable: `() -> { var unused = ...; }`.
    //
    // If callers of `increment()` shouldn't be required to use its result, then annotate it with
    // `@CanIgnoreReturnValue`.
    callRunnable(this.intValue::increment);
    // BUG: Diagnostic contains: The result of `increment()` must be used
    callRunnable(predicate ? this.intValue::increment : this.intValue::increment2);
  }

  public void testConstructorResolvedToVoidMethodReference() {
    // BUG: Diagnostic contains: The result of `new MyObject()` must be used
    //
    // `MyObject::new` acts as an implementation of `Runnable.run`
    // -- which is a `void` method, so it doesn't use the result of `new MyObject()`.
    //
    // To use the result, you may need to restructure your code.
    //
    // If you really don't want to use the result, then switch to a lambda that assigns it to a
    // variable: `() -> { var unused = ...; }`.
    //
    // If callers of `MyObject()` shouldn't be required to use its result, then annotate it with
    // `@CanIgnoreReturnValue`.
    callRunnable(MyObject::new);
  }

  public void testRegularLambda() {
    callRunnable(
        () -> {
          // BUG: Diagnostic contains:
          this.intValue.increment();
        });
  }

  public void testBeforeAndAfterRule() {
    // BUG: Diagnostic contains:
    new IntValue(1).increment();
    ExpectedException.none().expect(IllegalStateException.class);
    new IntValue(1).increment(); // No error here, last statement in block
  }

  public void constructor() {
    // BUG: Diagnostic contains: The result of `new MyObject()` must be used
    new MyObject() {};

    class MySubObject1 extends MyObject {}

    class MySubObject2 extends MyObject {
      MySubObject2() {}
    }

    class MySubObject3 extends MyObject {
      MySubObject3() {
        super();
      }
    }

    // BUG: Diagnostic contains: The result of `new MyObject()` must be used
    //
    // If you really don't want to use the result, then assign it to a variable: `var unused = ...`.
    //
    // If callers of `MyObject()` shouldn't be required to use its result, then annotate it with
    // `@CanIgnoreReturnValue`.
    new MyObject();
  }

  private class IntValue {
    final int i;

    public IntValue(int i) {
      this.i = i;
    }

    @javax.annotation.CheckReturnValue
    public IntValue increment() {
      return new IntValue(i + 1);
    }

    public void increment2() {
      // BUG: Diagnostic contains:
      this.increment();
    }

    public void increment3() {
      // BUG: Diagnostic contains:
      increment();
    }
  }

  private static class MyObject {
    @CheckReturnValue
    MyObject() {}
  }

  private abstract static class LB1<A> {}

  private static class LB2<A> extends LB1<A> {

    @CheckReturnValue
    public static <T> LB2<T> lb1() {
      return new LB2<T>();
    }

    public static <T> LB2<T> lb2() {
      // BUG: Diagnostic contains:
      lb1();
      return lb1();
    }
  }

  private static class JavaxAnnotation {
    @javax.annotation.CheckReturnValue
    public static int check() {
      return 1;
    }

    public static void ignoresCheck() {
      // BUG: Diagnostic contains:
      check();
    }
  }
}\
""")
        .doTest();
  }

  @Test
  public void customCheckReturnValueAnnotation() {
    compilationHelper
        .addSourceLines(
            "foo/bar/CheckReturnValue.java",
            """
            package foo.bar;

            public @interface CheckReturnValue {}
            """)
        .addSourceLines(
            "test/TestCustomCheckReturnValueAnnotation.java",
            """
            package test;

            import foo.bar.CheckReturnValue;

            public class TestCustomCheckReturnValueAnnotation {
              @CheckReturnValue
              public String getString() {
                return "string";
              }

              public void doIt() {
                // BUG: Diagnostic contains: CheckReturnValue
                getString();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void customCanIgnoreReturnValueAnnotation() {
    compilationHelper
        .addSourceLines(
            "foo/bar/CanIgnoreReturnValue.java",
            """
            package foo.bar;

            public @interface CanIgnoreReturnValue {}
            """)
        .addSourceLines(
            "test/TestCustomCanIgnoreReturnValueAnnotation.java",
            """
            package test;

            import foo.bar.CanIgnoreReturnValue;

            @com.google.errorprone.annotations.CheckReturnValue
            public class TestCustomCanIgnoreReturnValueAnnotation {
              @CanIgnoreReturnValue
              public String ignored() {
                return null;
              }

              public void doIt() {
                ignored();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negativeCase() {
    compilationHelper
        .addSourceLines(
            "CheckReturnValueNegativeCases.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            import com.google.errorprone.annotations.CheckReturnValue;
            import java.util.function.Supplier;

            /**
             * @author eaftan@google.com (Eddie Aftandilian)
             */
            public class CheckReturnValueNegativeCases {

              public void test1() {
                test2();
                Object obj = new String();
                obj.toString();
              }

              @SuppressWarnings("foo") // wrong annotation
              public void test2() {}

              @CheckReturnValue
              private int mustCheck() {
                return 5;
              }

              private int nothingToCheck() {
                return 42;
              }

              private void callRunnable(Runnable runnable) {
                runnable.run();
              }

              private void testNonCheckedCallsWithMethodReferences() {
                Object obj = new String();
                callRunnable(String::new);
                callRunnable(this::test2);
                callRunnable(obj::toString);
              }

              private void callSupplier(Supplier<Integer> supplier) {
                supplier.get();
              }

              public void testResolvedToIntLambda(boolean predicate) {
                callSupplier(() -> mustCheck());
                callSupplier(predicate ? () -> mustCheck() : () -> nothingToCheck());
              }

              public void testMethodReference(boolean predicate) {
                callSupplier(this::mustCheck);
                callSupplier(predicate ? this::mustCheck : this::nothingToCheck);
              }
            }\
            """)
        .doTest();
  }

  @Test
  public void packageAnnotation() {
    compilationHelper
        .addSourceLines(
            "package-info.java",
            """
            @com.google.errorprone.annotations.CheckReturnValue
            package lib;
            """)
        .addSourceLines(
            "lib/Lib.java",
            """
            package lib;

            public class Lib {
              public static int f() {
                return 42;
              }
            }
            """)
        .addSourceLines(
            "Test.java",
            """
            class Test {
              void m() {
                // BUG: Diagnostic contains: CheckReturnValue
                lib.Lib.f();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void classAnnotation() {
    compilationHelper
        .addSourceLines(
            "lib/Lib.java",
            """
            package lib;

            @com.google.errorprone.annotations.CheckReturnValue
            public class Lib {
              public static int f() {
                return 42;
              }
            }
            """)
        .addSourceLines(
            "Test.java",
            """
            class Test {
              void m() {
                // BUG: Diagnostic contains: CheckReturnValue
                lib.Lib.f();
              }
            }
            """)
        .doTest();
  }

  // Don't match void-returning methods in packages with @CRV
  @Test
  public void voidReturningMethodInAnnotatedPackage() {
    compilationHelper
        .addSourceLines(
            "package-info.java",
            """
            @com.google.errorprone.annotations.CheckReturnValue
            package lib;
            """)
        .addSourceLines(
            "lib/Lib.java",
            """
            package lib;

            public class Lib {
              public static void f() {}
            }
            """)
        .addSourceLines(
            "Test.java",
            """
            class Test {
              void m() {
                lib.Lib.f();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void badCRVOnProcedure() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            package lib;

            @com.google.errorprone.annotations.CheckReturnValue
            public class Test {
              @com.google.errorprone.annotations.CheckReturnValue
              // BUG: Diagnostic contains: may not be applied to void-returning methods
              public static void f() {}
            }
            """)
        .doTest();
  }

  @Test
  public void badCRVOnPseudoProcedure() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            package lib;

            @com.google.errorprone.annotations.CheckReturnValue
            public class Test {
              @com.google.errorprone.annotations.CheckReturnValue
              // BUG: Diagnostic contains: may not be applied to void-returning methods
              public static Void f() {
                return null;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void packageAnnotationButCanIgnoreReturnValue() {
    compilationHelper
        .addSourceLines(
            "package-info.java",
            """
            @com.google.errorprone.annotations.CheckReturnValue
            package lib;
            """)
        .addSourceLines(
            "lib/Lib.java",
            """
            package lib;

            public class Lib {
              @com.google.errorprone.annotations.CanIgnoreReturnValue
              public static int f() {
                return 42;
              }
            }
            """)
        .addSourceLines(
            "Test.java",
            """
            class Test {
              void m() {
                lib.Lib.f();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void classAnnotationButCanIgnoreReturnValue() {
    compilationHelper
        .addSourceLines(
            "lib/Lib.java",
            """
            package lib;

            @com.google.errorprone.annotations.CheckReturnValue
            public class Lib {
              @com.google.errorprone.annotations.CanIgnoreReturnValue
              public static int f() {
                return 42;
              }
            }
            """)
        .addSourceLines(
            "Test.java",
            """
            class Test {
              void m() {
                lib.Lib.f();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void badCanIgnoreReturnValueOnProcedure() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            package lib;

            @com.google.errorprone.annotations.CheckReturnValue
            public class Test {
              @com.google.errorprone.annotations.CanIgnoreReturnValue
              // BUG: Diagnostic contains: may not be applied to void-returning methods
              public static void f() {}
            }
            """)
        .doTest();
  }

  @Test
  public void nestedClassAnnotation() {
    compilationHelper
        .addSourceLines(
            "lib/Lib.java",
            """
            package lib;

            @com.google.errorprone.annotations.CheckReturnValue
            public class Lib {
              public static class Inner {
                public static class InnerMost {
                  public static int f() {
                    return 42;
                  }
                }
              }
            }
            """)
        .addSourceLines(
            "Test.java",
            """
            class Test {
              void m() {
                // BUG: Diagnostic contains: CheckReturnValue
                lib.Lib.Inner.InnerMost.f();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void nestedClassWithCanIgnoreAnnotation() {
    compilationHelper
        .addSourceLines(
            "lib/Lib.java",
            """
            package lib;

            @com.google.errorprone.annotations.CheckReturnValue
            public class Lib {
              @com.google.errorprone.annotations.CanIgnoreReturnValue
              public static class Inner {
                public static class InnerMost {
                  public static int f() {
                    return 42;
                  }
                }
              }
            }
            """)
        .addSourceLines(
            "Test.java",
            """
            class Test {
              void m() {
                lib.Lib.Inner.InnerMost.f();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void packageWithCanIgnoreAnnotation() {
    compilationHelper
        .addSourceLines(
            "package-info.java",
            """
            @com.google.errorprone.annotations.CheckReturnValue
            package lib;
            """)
        .addSourceLines(
            "lib/Lib.java",
            """
            package lib;

            @com.google.errorprone.annotations.CanIgnoreReturnValue
            public class Lib {
              public static int f() {
                return 42;
              }
            }
            """)
        .addSourceLines(
            "Test.java",
            """
            class Test {
              void m() {
                lib.Lib.f();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void errorBothClass() {
    compilationHelper
        .addSourceLines(
            "Test.java",
"""
@com.google.errorprone.annotations.CanIgnoreReturnValue
@com.google.errorprone.annotations.CheckReturnValue
// BUG: Diagnostic contains: @CheckReturnValue and @CanIgnoreReturnValue cannot be applied to the
// same class
class Test {}
""")
        .doTest();
  }

  @Test
  public void errorBothMethod() {
    compilationHelper
        .addSourceLines(
            "Test.java",
"""
class Test {
  @com.google.errorprone.annotations.CanIgnoreReturnValue
  @com.google.errorprone.annotations.CheckReturnValue
  // BUG: Diagnostic contains: @CheckReturnValue and @CanIgnoreReturnValue cannot be applied to the
  // same method
  void m() {}
}
""")
        .doTest();
  }

  // Don't match Void-returning methods in packages with @CRV
  @Test
  public void javaLangVoidReturningMethodInAnnotatedPackage() {
    compilationHelper
        .addSourceLines(
            "package-info.java",
            """
            @com.google.errorprone.annotations.CheckReturnValue
            package lib;
            """)
        .addSourceLines(
            "lib/Lib.java",
            """
            package lib;

            public class Lib {
              public static Void f() {
                return null;
              }
            }
            """)
        .addSourceLines(
            "Test.java",
            """
            class Test {
              void m() {
                lib.Lib.f();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void ignoreVoidReturningMethodReferences() {
    compilationHelper
        .addSourceLines(
            "Lib.java",
            """
            @com.google.errorprone.annotations.CheckReturnValue
            public class Lib {
              public static void consume(Object o) {}
            }
            """)
        .addSourceLines(
            "Test.java",
            """
            class Test {
              void m(java.util.List<Object> xs) {
                xs.forEach(Lib::consume);
              }
            }
            """)
        .doTest();
  }

  /** Test class containing a method annotated with @CRV. */
  public static final class CRVTest {
    @com.google.errorprone.annotations.CheckReturnValue
    public static int f() {
      return 42;
    }

    private CRVTest() {}
  }

  @Test
  public void noCRVonClasspath() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              void m() {
                // BUG: Diagnostic contains: CheckReturnValue
                com.google.errorprone.bugpatterns.CheckReturnValueTest.CRVTest.f();
              }
            }
            """)
        .withClasspath(CRVTest.class, CheckReturnValueTest.class)
        .doTest();
  }

  @Test
  public void constructor() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              @com.google.errorprone.annotations.CheckReturnValue
              public Test() {}

              public static void foo() {
                // BUG: Diagnostic contains: CheckReturnValue
                new Test();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void constructor_telescoping() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              @com.google.errorprone.annotations.CheckReturnValue
              public Test() {}

              public Test(int foo) {
                this();
              }

              public static void foo() {
                Test foo = new Test(42);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void constructor_superCall() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              @com.google.errorprone.annotations.CheckReturnValue
              public Test() {}

              static class SubTest extends Test {
                SubTest() {
                  super();
                }
              }

              public static void foo() {
                Test derived = new SubTest();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void constructor_anonymousClassInheritsCIRV() {
    compilationHelperLookingAtAllConstructors()
        .addSourceLines(
            "Test.java",
            """
            class Test {
              @com.google.errorprone.annotations.CanIgnoreReturnValue
              public Test() {}

              public static void foo() {
                new Test() {};
                new Test() {
                  {
                    System.out.println("Lookie, instance initializer");
                  }
                };
              }
            }
            """)
        .doTest();
  }

  @Test
  public void constructor_anonymousClassInheritsCRV() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              @com.google.errorprone.annotations.CheckReturnValue
              public Test() {}

              public static void foo() {
                // BUG: Diagnostic contains: CheckReturnValue
                new Test() {};
              }
            }
            """)
        .doTest();
  }

  @Test
  public void constructor_hasOuterInstance() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              class Inner {
                @com.google.errorprone.annotations.CheckReturnValue
                public Inner() {}
              }

              public static void foo() {
                // BUG: Diagnostic contains: CheckReturnValue
                new Test().new Inner() {};
              }
            }
            """)
        .doTest();
  }

  @Test
  public void constructor_anonymousClassInheritsCRV_syntheticConstructor() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  @com.google.errorprone.annotations.CheckReturnValue",
            "  static class Nested {}",
            "  public static void foo() {",
            "    // BUG: Diagnostic contains: CheckReturnValue",
            "    new Nested() {};", // The "called" constructor is synthetic, but within @CRV Nested
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void constructor_inheritsFromCrvInterface() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  @com.google.errorprone.annotations.CheckReturnValue",
            "  static interface IFace {}",
            "  public static void foo() {",
            //  TODO(b/226203690): It's arguable that this might need to be @CRV?
            //   The superclass of the anonymous class is Object, not IFace, but /shrug
            "    new IFace() {};",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void constructor_throwingContexts() {
    compilationHelper
        .addSourceLines(
            "Foo.java",
            """
            @com.google.errorprone.annotations.CheckReturnValue
            public class Foo {}
            """)
        .addSourceLines(
            "Test.java",
            """
            class Test {
              void f() {
                try {
                  new Foo();
                  org.junit.Assert.fail();
                } catch (Exception expected) {
                }
                org.junit.Assert.assertThrows(IllegalArgumentException.class, () -> new Foo());
              }
            }
            """)
        .doTest();
  }

  @Test
  public void constructor_reference() {
    compilationHelper
        .addSourceLines(
            "Foo.java",
            """
            @com.google.errorprone.annotations.CheckReturnValue
            public class Foo {}
            """)
        .addSourceLines(
            "Test.java",
            """
            class Test {
              void f() {
                // BUG: Diagnostic contains: CheckReturnValue
                Runnable ignoresResult = Foo::new;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void constructor_withoutCrvAnnotation() {
    compilationHelperLookingAtAllConstructors()
        .addSourceLines(
            "Test.java",
            """
            class Test {
              public Test() {}

              public static void foo() {
                // BUG: Diagnostic contains: CheckReturnValue
                new Test();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void allMethods_withoutCIRVAnnotation() {
    compilationHelperLookingAtAllMethods()
        .addSourceLines(
            "Test.java",
            """
            class Test {
              public int bar() {
                return 42;
              }

              public static void foo() {
                // BUG: Diagnostic contains: CheckReturnValue
                new Test().bar();
              }
            }
            """)
        .doTest();
  }

  // In the following test methods, we define parallel skeletons of classes like java.util.List,
  // because the real java.util.List may have had @CanIgnoreReturnValue annotations inserted.

  @Test
  public void allMethods_withExternallyConfiguredIgnoreList() {
    compileWithExternalApis("my.java.util.List#add(java.lang.Object)")
        .addSourceLines(
            "Test.java",
            """
            import my.java.util.List;

            class Test {
              public static void foo(List<Integer> x) {
                x.add(42);
                // BUG: Diagnostic contains: CheckReturnValue
                x.get(0);
              }
            }
            """)
        .addSourceLines(
            "my/java/util/List.java",
            """
            package my.java.util;

            public interface List<E> {
              boolean add(E e);

              E get(int index);
            }
            """)
        .doTest();
  }

  @Test
  public void packagesRule() {
    compilationHelperWithPackagePatterns("my.java.util")
        .addSourceLines(
            "Test.java",
            """
            import my.java.util.List;
            import my.java.util.regex.Pattern;

            class Test {
              public static void foo(List<Integer> list, Pattern pattern) {
                // BUG: Diagnostic contains: CheckReturnValue
                list.get(0);
                // BUG: Diagnostic contains: CheckReturnValue
                pattern.matcher("blah");
              }
            }
            """)
        .addSourceLines(
            "my/java/util/List.java",
            """
            package my.java.util;

            public interface List<E> {
              E get(int index);
            }
            """)
        .addSourceLines(
            "my/java/util/regex/Pattern.java",
            """
            package my.java.util.regex;

            public interface Pattern {
              String matcher(CharSequence input);
            }
            """)
        .doTest();
  }

  @Test
  public void packagesRule_negativePattern() {
    compilationHelperWithPackagePatterns("my.java.util", "-my.java.util.regex")
        .addSourceLines(
            "Test.java",
            """
            import my.java.util.List;
            import my.java.util.regex.Pattern;

            class Test {
              public static void foo(List<Integer> list, Pattern pattern) {
                // BUG: Diagnostic contains: CheckReturnValue
                list.get(0);
                pattern.matcher("blah");
              }
            }
            """)
        .addSourceLines(
            "my/java/util/List.java",
            """
            package my.java.util;

            public interface List<E> {
              E get(int index);
            }
            """)
        .addSourceLines(
            "my/java/util/regex/Pattern.java",
            """
            package my.java.util.regex;

            public interface Pattern {
              String matcher(CharSequence input);
            }
            """)
        .doTest();
  }

  @Test
  public void packagesRule_negativePattern_doesNotMakeOptional() {
    // A negative pattern just makes the packages rule itself not apply to that package and its
    // subpackages if it otherwise would because of a positive pattern on a superpackage. It doesn't
    // make APIs in that package CIRV.
    compilationHelperWithPackagePatterns("my.java.util", "-my.java.util.regex")
        .addSourceLines(
            "Test.java",
            """
            import my.java.util.List;
            import my.java.util.regex.Pattern;
            import my.java.util.regex.PatternSyntaxException;

            class Test {
              public static void foo(List<Integer> list, Pattern pattern) {
                // BUG: Diagnostic contains: CheckReturnValue
                list.get(0);
                pattern.matcher("blah");
                // BUG: Diagnostic contains: CheckReturnValue
                new PatternSyntaxException("", "", 0);
              }
            }
            """)
        .addSourceLines(
            "my/java/util/List.java",
            """
            package my.java.util;

            public interface List<E> {
              E get(int index);
            }
            """)
        .addSourceLines(
            "my/java/util/regex/Pattern.java",
            """
            package my.java.util.regex;

            public interface Pattern {
              String matcher(CharSequence input);
            }
            """)
        .addSourceLines(
            "my/java/util/regex/PatternSyntaxException.java",
            """
            package my.java.util.regex;

            public class PatternSyntaxException extends IllegalArgumentException {
              public PatternSyntaxException(String desc, String regex, int index) {}
            }
            """)
        .doTest();
  }

  @Test
  public void refactoringAssignsToOriginalBasedOnSubstitutedTypes() {
    refactoringHelper
        .addInputLines(
            "Builder.java",
            """
            @com.google.errorprone.annotations.CheckReturnValue
            interface Builder<B extends Builder<B>> {
              B setFoo(String s);
            }
            """)
        .expectUnchanged()
        .addInputLines(
            "SomeBuilder.java", //
            "interface SomeBuilder extends Builder<SomeBuilder> {}")
        .expectUnchanged()
        .addInputLines(
            "Test.java",
            """
            class Test {
              void f(SomeBuilder builder, String s) {
                builder.setFoo(s);
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              void f(SomeBuilder builder, String s) {
                builder = builder.setFoo(s);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void suggestCanIgnoreReturnValueForMethodInvocation() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import com.google.errorprone.annotations.CheckReturnValue;

            @CheckReturnValue
            class Test {
              void foo() {
                makeBarOrThrow();
              }

              String makeBarOrThrow() {
                throw new UnsupportedOperationException();
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import com.google.errorprone.annotations.CanIgnoreReturnValue;
            import com.google.errorprone.annotations.CheckReturnValue;

            @CheckReturnValue
            class Test {
              void foo() {
                makeBarOrThrow();
              }

              @CanIgnoreReturnValue
              String makeBarOrThrow() {
                throw new UnsupportedOperationException();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void suggestCanIgnoreReturnValueForMethodReference() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import com.google.errorprone.annotations.CheckReturnValue;

            @CheckReturnValue
            class Test {
              Runnable r = this::makeBarOrThrow;

              String makeBarOrThrow() {
                throw new UnsupportedOperationException();
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import com.google.errorprone.annotations.CanIgnoreReturnValue;
            import com.google.errorprone.annotations.CheckReturnValue;

            @CheckReturnValue
            class Test {
              Runnable r = this::makeBarOrThrow;

              @CanIgnoreReturnValue
              String makeBarOrThrow() {
                throw new UnsupportedOperationException();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void suggestCanIgnoreReturnValueForConstructor() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import com.google.errorprone.annotations.CheckReturnValue;

            @CheckReturnValue
            class Test {
              Test() {}

              void run() {
                new Test();
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import com.google.errorprone.annotations.CanIgnoreReturnValue;
            import com.google.errorprone.annotations.CheckReturnValue;

            @CheckReturnValue
            class Test {
              @CanIgnoreReturnValue
              Test() {}

              void run() {
                new Test();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void suggestCanIgnoreReturnValueAndRemoveCheckReturnValue() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import com.google.errorprone.annotations.CheckReturnValue;

            class Test {
              void foo() {
                makeBarOrThrow();
              }

              @CheckReturnValue
              String makeBarOrThrow() {
                throw new UnsupportedOperationException();
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import com.google.errorprone.annotations.CanIgnoreReturnValue;
            import com.google.errorprone.annotations.CheckReturnValue;

            class Test {
              void foo() {
                makeBarOrThrow();
              }

              @CanIgnoreReturnValue
              String makeBarOrThrow() {
                throw new UnsupportedOperationException();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void doesNotSuggestCanIgnoreReturnValueForOtherFile() {
    refactoringHelper
        .addInputLines(
            "Lib.java",
            """
            import com.google.errorprone.annotations.CheckReturnValue;

            @CheckReturnValue
            class Lib {
              String makeBarOrThrow() {
                throw new UnsupportedOperationException();
              }
            }
            """)
        .expectUnchanged()
        .addInputLines(
            "Test.java",
            """
            import com.google.errorprone.annotations.CheckReturnValue;

            @CheckReturnValue
            class Test {
              void foo(Lib l) {
                l.makeBarOrThrow();
              }
            }
            """)
        // The checker doesn't suggest CIRV, so it applies a different fix instead.
        .addOutputLines(
            "Test.java",
            """
            import com.google.errorprone.annotations.CheckReturnValue;

            @CheckReturnValue
            class Test {
              void foo(Lib l) {
                var unused = l.makeBarOrThrow();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void suggestsVarUnusedForConstructor() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            @com.google.errorprone.annotations.CheckReturnValue
            class Test {
              void go() {
                new Test();
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            @com.google.errorprone.annotations.CheckReturnValue
            class Test {
              void go() {
                var unused = new Test();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void suggestsVarUnused2() {
    refactoringHelper
        .addInputLines(
            "Lib.java",
            """
            import com.google.errorprone.annotations.CheckReturnValue;

            @CheckReturnValue
            interface Lib {
              int a();

              int b();
            }
            """)
        .expectUnchanged()
        .addInputLines(
            "Test.java",
            """
            class Test {
              void foo(Lib lib) {
                var unused = lib.a();
                lib.b();
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              void foo(Lib lib) {
                var unused = lib.a();
                var unused2 = lib.b();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void inheritsCanIgnoreReturnValue() {
    refactoringHelper
        .addInputLines(
            "Super.java",
            """
            import com.google.errorprone.annotations.CheckReturnValue;
            import com.google.errorprone.annotations.CanIgnoreReturnValue;

            @CheckReturnValue
            interface Super {
              int a();

              @CanIgnoreReturnValue
              int b();
            }
            """)
        .expectUnchanged()
        .addInputLines(
            "Lib.java",
            """
            import com.google.errorprone.annotations.CheckReturnValue;

            @CheckReturnValue
            interface Lib extends Super {
              @Override
              int a();

              @Override
              int b();
            }
            """)
        .expectUnchanged()
        .addInputLines(
            "Test.java",
            """
            class Test {
              void foo(Lib lib) {
                lib.a();
                lib.b();
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              void foo(Lib lib) {
                var unused = lib.a();
                lib.b();
              }
            }
            """)
        .doTest();
  }

  private CompilationTestHelper compilationHelperLookingAtAllConstructors() {
    return compilationHelper.setArgs(
        "-XepOpt:" + CheckReturnValue.CHECK_ALL_CONSTRUCTORS + "=true");
  }

  private CompilationTestHelper compilationHelperLookingAtAllMethods() {
    return compilationHelper.setArgs("-XepOpt:" + CheckReturnValue.CHECK_ALL_METHODS + "=true");
  }

  private CompilationTestHelper compileWithExternalApis(String... apis) {
    try {
      Path file = temporaryFolder.newFile().toPath();
      Files.writeString(file, Joiner.on('\n').join(apis), UTF_8);

      return compilationHelper.setArgs(
          "-XepOpt:" + CheckReturnValue.CHECK_ALL_METHODS + "=true",
          "-XepOpt:CheckReturnValue:ApiExclusionList=" + file);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private CompilationTestHelper compilationHelperWithPackagePatterns(String... patterns) {
    return compilationHelper.setArgs(
        "-XepOpt:" + CheckReturnValue.CRV_PACKAGES + "=" + Joiner.on(',').join(patterns),
        "-XepOpt:" + CheckReturnValue.CHECK_ALL_CONSTRUCTORS + "=true");
  }
}
