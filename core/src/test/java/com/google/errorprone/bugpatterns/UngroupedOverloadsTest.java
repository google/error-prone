/*
 * Copyright 2017 The Error Prone Authors.
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

import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.BugCheckerRefactoringTestHelper.TestMode;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * @author hanuszczak@google.com (Łukasz Hanuszczak)
 */
@RunWith(JUnit4.class)
public final class UngroupedOverloadsTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(UngroupedOverloads.class, getClass());

  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(UngroupedOverloads.class, getClass());

  @Test
  public void ungroupedOverloadsPositiveCasesSingle() {
    compilationHelper
        .addSourceLines(
            "UngroupedOverloadsPositiveCasesSingle.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            /**
             * @author hanuszczak@google.com (Łukasz Hanuszczak)
             */
            public class UngroupedOverloadsPositiveCasesSingle {

              public void quux() {
                foo();
              }

              // BUG: Diagnostic contains: ungrouped overloads of 'foo'
              public void foo() {
                foo(42);
              }

              // BUG: Diagnostic contains: ungrouped overloads of 'foo'
              public void foo(int x) {
                foo(x, x);
              }

              public void bar() {
                bar(42);
              }

              public void bar(int x) {
                foo(x);
              }

              // BUG: Diagnostic contains: ungrouped overloads of 'foo'
              public void foo(int x, int y) {
                System.out.println(x + y);
              }

              public void norf() {}
            }
            """)
        .doTest();
  }

  @Test
  public void ungroupedOverloadsPositiveCasesMultiple() {
    compilationHelper
        .addSourceLines(
            "UngroupedOverloadsPositiveCasesMultiple.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            /**
             * @author hanuszczak@google.com (Łukasz Hanuszczak)
             */
            public class UngroupedOverloadsPositiveCasesMultiple {

              private int foo;

              // BUG: Diagnostic contains: ungrouped overloads of 'bar'
              public void bar(int x, String z, int y) {
                System.out.println(String.format("z: %s, x: %d, y: %d", z, x, y));
              }

              private UngroupedOverloadsPositiveCasesMultiple(int foo) {
                this.foo = foo;
              }

              // BUG: Diagnostic contains: ungrouped overloads of 'bar'
              public void bar(int x) {
                bar(foo, x);
              }

              public void baz(String x) {
                bar(42, x, 42);
              }

              // BUG: Diagnostic contains: ungrouped overloads of 'bar'
              public void bar(int x, int y) {
                bar(y, FOO, x);
              }

              public static final String FOO = "foo";

              // BUG: Diagnostic contains: ungrouped overloads of 'bar'
              public void bar(int x, int y, int z) {
                bar(x, String.valueOf(y), z);
              }

              // BUG: Diagnostic contains: ungrouped overloads of 'quux'
              public int quux() {
                return quux(quux);
              }

              public int quux = 42;

              // BUG: Diagnostic contains: ungrouped overloads of 'quux'
              public int quux(int x) {
                return x + quux;
              }

              private static class Quux {}

              // BUG: Diagnostic contains: ungrouped overloads of 'quux'
              public int quux(int x, int y) {
                return quux(x + y);
              }

              // BUG: Diagnostic contains: ungrouped overloads of 'norf'
              public int norf(int x) {
                return quux(x, x);
              }

              // BUG: Diagnostic contains: ungrouped overloads of 'norf'
              public int norf(int x, int y) {
                return norf(x + y);
              }

              public void foo() {
                System.out.println("foo");
              }

              // BUG: Diagnostic contains: ungrouped overloads of 'norf'
              public void norf(int x, int y, int w) {
                norf(x + w, y + w);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void ungroupedOverloadsPositiveCasesInterleaved() {
    compilationHelper
        .addSourceLines(
            "UngroupedOverloadsPositiveCasesInterleaved.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            /**
             * @author hanuszczak@google.com (Łukasz Hanuszczak)
             */
            public class UngroupedOverloadsPositiveCasesInterleaved {

              private int foo;

              // BUG: Diagnostic contains: ungrouped overloads of 'bar'
              public void bar(int x, String z, int y) {
                System.out.println(String.format("z: %s, x: %d, y: %d", z, x, y));
              }

              public UngroupedOverloadsPositiveCasesInterleaved(int foo) {
                this.foo = foo;
              }

              // BUG: Diagnostic contains: ungrouped overloads of 'bar'
              public void bar(int x) {
                bar(foo, x);
              }

              // BUG: Diagnostic contains: ungrouped overloads of 'baz'
              public void baz(String x) {
                baz(x, FOO);
              }

              // BUG: Diagnostic contains: ungrouped overloads of 'bar'
              public void bar(int x, int y) {
                bar(y, FOO, x);
              }

              public static final String FOO = "foo";

              // BUG: Diagnostic contains: ungrouped overloads of 'baz'
              public void baz(String x, String y) {
                bar(foo, x + y, foo);
              }

              public void foo(int x) {}

              public void foo() {
                foo(foo);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void ungroupedOverloadsPositiveCasesCovering() {
    compilationHelper
        .addSourceLines(
            "UngroupedOverloadsPositiveCasesCovering.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            /**
             * @author hanuszczak@google.com (Łukasz Hanuszczak)
             */
            public class UngroupedOverloadsPositiveCasesCovering {

              // BUG: Diagnostic contains: ungrouped overloads of 'foo'
              public void foo(int x) {
                System.out.println(x);
              }

              // BUG: Diagnostic contains: ungrouped overloads of 'bar'
              public void bar() {
                foo();
              }

              public void baz() {
                bar();
              }

              // BUG: Diagnostic contains: ungrouped overloads of 'bar'
              public void bar(int x) {
                foo(x);
              }

              // BUG: Diagnostic contains: ungrouped overloads of 'quux'
              private void quux() {
                norf();
              }

              private void norf() {
                quux();
              }

              // BUG: Diagnostic contains: ungrouped overloads of 'quux'
              public void quux(int x) {
                bar(x);
              }

              // BUG: Diagnostic contains: ungrouped overloads of 'foo'
              public void foo() {
                foo(42);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void ungroupedOverloadsPositiveCasesCoveringOnlyFirstOverload() {
    compilationHelper
        .addSourceLines(
            "UngroupedOverloadsPositiveCasesCoveringOnlyOnFirst.java",
"""
package com.google.errorprone.bugpatterns.testdata;

/**
 * @author hanuszczak@google.com (Łukasz Hanuszczak)
 */
public class UngroupedOverloadsPositiveCasesCoveringOnlyOnFirst {

  // BUG: Diagnostic contains: Constructors and methods with the same name should appear
  public void foo(int x) {
    System.out.println(x);
  }

  public void bar() {
    foo();
  }

  public void baz() {
    bar();
  }

  public void bar(int x) {
    foo(x);
  }

  private void quux() {
    norf();
  }

  private void norf() {
    quux();
  }

  public void quux(int x) {
    bar(x);
  }

  public void foo() {
    foo(42);
  }
}
""")
        .setArgs(ImmutableList.of("-XepOpt:UngroupedOverloads:BatchFindings"))
        .doTest();
  }

  @Test
  public void ungroupedOverloadsNegativeCases() {
    compilationHelper
        .addSourceLines(
            "UngroupedOverloadsNegativeCases.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            /**
             * @author hanuszczak@google.com (Łukasz Hanuszczak)
             */
            public class UngroupedOverloadsNegativeCases {

              private int foo;

              public UngroupedOverloadsNegativeCases(int foo) {
                this.foo = foo;
              }

              public void bar(int x) {
                bar(foo, x);
              }

              public void bar(int x, String z, int y) {
                System.out.println(String.format("z: %s, x: %d, y: %d", z, x, y));
              }

              public void bar(int x, int y) {
                bar(y, FOO, x);
              }

              public static class Baz {}

              public static final String FOO = "foo";

              public void baz(String x) {
                baz(x, FOO);
              }

              public void baz(String x, String y) {
                bar(foo, x + y, foo);
              }

              public int foo() {
                return this.foo;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void ungroupedOverloadsRefactoringComments() {
    refactoringHelper
        .addInputLines(
            "UngroupedOverloadsRefactoringComments.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            /**
             * @author hanuszczak@google.com (Łukasz Hanuszczak)
             */
            class UngroupedOverloadsRefactoringComments {

              private void bar() {}

              public static final String FOO = "foo"; // This is super-important comment for `foo`.

              // Something about `bar`.
              /** Does something. */
              public void bar(int x) {}

              // Something about this `bar`.
              public void bar(int x, int y) {}

              // Something about `baz`.
              public static final String BAZ = "baz"; // Stuff about `baz` continues.

              // More stuff about `bar`.
              public void bar(int x, int y, int z) {
                // Some internal comments too.
              }

              public void quux() {}

              public void bar(String s) {}
            }
            """)
        .addOutputLines(
            "UngroupedOverloadsRefactoringComments_expected.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            /**
             * @author hanuszczak@google.com (Łukasz Hanuszczak)
             */
            class UngroupedOverloadsRefactoringComments {

              private void bar() {}

              // Something about `bar`.
              /** Does something. */
              public void bar(int x) {}

              // Something about this `bar`.
              public void bar(int x, int y) {}

              // More stuff about `bar`.
              public void bar(int x, int y, int z) {
                // Some internal comments too.
              }

              public void bar(String s) {}

              public static final String FOO = "foo"; // This is super-important comment for `foo`.

              // Something about `baz`.
              public static final String BAZ = "baz"; // Stuff about `baz` continues.

              public void quux() {}
            }
            """)
        .doTest();
  }

  @Test
  public void ungroupedOverloadsRefactoringMultiple() {
    refactoringHelper
        .addInputLines(
            "UngroupedOverloadsRefactoringMultiple.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            /**
             * @author hanuszczak@google.com (Łukasz Hanuszczak)
             */
            class UngroupedOverloadsRefactoringMultiple {

              public void foo() {}

              public void foo(int x) {}

              private static class foo {}

              public void foo(int x, int y) {}

              public void bar() {}

              public static final String BAZ = "baz";

              public void foo(int x, int y, int z) {}

              public void quux() {}

              public void quux(int x) {}

              public static final int X = 0;
              public static final int Y = 1;

              public void quux(int x, int y) {}

              private int quux;

              public void norf() {}

              public void quux(int x, int y, int z) {}

              public void thud() {}
            }
            """)
        .addOutputLines(
            "UngroupedOverloadsRefactoringMultiple_expected.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            /**
             * @author hanuszczak@google.com (Łukasz Hanuszczak)
             */
            class UngroupedOverloadsRefactoringMultiple {

              public void foo() {}

              public void foo(int x) {}

              public void foo(int x, int y) {}

              public void foo(int x, int y, int z) {}

              private static class foo {}

              public void bar() {}

              public static final String BAZ = "baz";

              public void quux() {}

              public void quux(int x) {}

              public void quux(int x, int y) {}

              public void quux(int x, int y, int z) {}

              public static final int X = 0;
              public static final int Y = 1;

              private int quux;

              public void norf() {}

              public void thud() {}
            }
            """)
        .doTest();
  }

  @Test
  public void ungroupedOverloadsRefactoringInterleaved() {
    refactoringHelper
        .addInputLines(
            "UngroupedOverloadsRefactoringInterleaved.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            /**
             * @author hanuszczak@google.com (Łukasz Hanuszczak)
             */
            class UngroupedOverloadsRefactoringInterleaved {

              public void foo() {}

              public void baz() {}

              public void bar() {}

              public void foo(int x) {}

              public void baz(int x) {}

              public void foo(int x, int y) {}

              public void quux() {}

              public void baz(int x, int y) {}

              public void quux(int x) {}

              public void bar(int x) {}

              public void quux(int x, int y) {}

              public void foo(int x, int y, int z) {}

              public void bar(int x, int y) {}
            }
            """)
        .addOutputLines(
            "UngroupedOverloadsRefactoringInterleaved_expected.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            /**
             * @author hanuszczak@google.com (Łukasz Hanuszczak)
             */
            class UngroupedOverloadsRefactoringInterleaved {

              public void foo() {}

              public void foo(int x) {}

              public void foo(int x, int y) {}

              public void foo(int x, int y, int z) {}

              public void baz() {}

              public void baz(int x) {}

              public void baz(int x, int y) {}

              public void bar() {}

              public void bar(int x) {}

              public void bar(int x, int y) {}

              public void quux() {}

              public void quux(int x) {}

              public void quux(int x, int y) {}
            }
            """)
        .doTest();
  }

  @Test
  public void ungroupedOverloadsRefactoringBelowCutoffLimit() {
    // Here we have 4 methods so refactoring should be applied.
    refactoringHelper
        .addInputLines(
            "in/BelowLimit.java",
            """
            class BelowLimit {
              BelowLimit() {}

              void foo() {}

              void bar() {}

              void foo(int x) {}
            }
            """)
        .addOutputLines(
            "out/BelowLimit.java",
            """
            class BelowLimit {
              BelowLimit() {}

              void foo() {}

              void foo(int x) {}

              void bar() {}
            }
            """)
        .doTest();
  }

  @Test
  public void ungroupedOverloadsRefactoring_fiveMethods() {
    refactoringHelper
        .addInputLines(
            "in/AboveLimit.java",
            """
            class AboveLimit {
              AboveLimit() {}

              void foo() {}

              void bar() {}

              void foo(int x) {}

              void baz() {}
            }
            """)
        .addOutputLines(
            "out/AboveLimit.java",
            """
            class AboveLimit {
              AboveLimit() {}

              void foo() {}

              void foo(int x) {}

              void bar() {}

              void baz() {}
            }
            """)
        .doTest();
  }

  @Test
  public void staticAndNonStaticInterspersed() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              private void foo(int x) {}

              private static void foo(int x, int y, int z) {}

              private void foo(int x, int y) {}
            }
            """)
        .doTest();
  }

  @Test
  public void suppressOnAnyMethod() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              void foo() {}

              void bar() {}

              @SuppressWarnings("UngroupedOverloads")
              void foo(int x) {}
            }
            """)
        .doTest();
  }

  @Test
  public void javadoc() {
    refactoringHelper
        .addInputLines(
            "in/Test.java",
            """
            class Test {
              void foo() {}

              void bar() {}

              /** doc */
              void foo(int x) {}
            }
            """)
        .addOutputLines(
            "out/Test.java",
            """
            class Test {

              void foo() {}

              /** doc */
              void foo(int x) {}

              void bar() {}
            }
            """)
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void diagnostic() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              // BUG: Diagnostic contains: ungrouped overloads of 'foo' on line(s): 11, 14, 17
              private void foo() {}

              // BUG: Diagnostic contains: ungrouped overloads of 'foo' on line(s): 11, 14, 17
              private void foo(int a) {}

              private void bar() {}

              // BUG: Diagnostic contains: ungrouped overloads of 'foo' on line(s): 3, 6
              private void foo(int a, int b) {}

              // BUG: Diagnostic contains: ungrouped overloads of 'foo' on line(s): 3, 6
              private void foo(int a, int b, int c) {}

              // BUG: Diagnostic contains: ungrouped overloads of 'foo' on line(s): 3, 6
              private void foo(int a, int b, int c, int d) {}
            }
            """)
        .doTest();
  }

  @Test
  public void interleavedUngroupedOverloads() {
    refactoringHelper
        .addInputLines(
            "in/Test.java",
            """
            class Test {
              void foo() {
                System.err.println();
              }

              void bar() {
                System.err.println();
              }

              void foo(int x) {
                System.err.println();
              }

              void bar(int x) {
                System.err.println();
              }
            }
            """)
        .addOutputLines(
            "out/Test.java",
            """
            class Test {

              void foo() {
                System.err.println();
              }

              void foo(int x) {
                System.err.println();
              }

              void bar() {
                System.err.println();
              }

              void bar(int x) {
                System.err.println();
              }
            }
            """)
        .setArgs("-XepOpt:UngroupedOverloads:BatchFindings")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void describingConstructors() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              // BUG: Diagnostic contains: constructor overloads
              Test() {}

              private void bar() {}

              // BUG: Diagnostic contains: constructor overloads
              Test(int i) {}
            }
            """)
        .doTest();
  }

  @Test
  public void recordConstructor() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import com.google.common.collect.ImmutableSet;
            import java.util.Set;

            record MyRecord(ImmutableSet<String> strings) {
              MyRecord(Set<String> strings) {
                this(strings == null ? ImmutableSet.of() : ImmutableSet.copyOf(strings));
              }
            }
            """)
        .doTest();
  }
}
