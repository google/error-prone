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

package com.google.errorprone.bugpatterns.overloading;

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Test cases for {@link InconsistentOverloads}.
 *
 * @author hanuszczak@google.com (Łukasz Hanuszczak)
 */
@RunWith(JUnit4.class)
public final class InconsistentOverloadsTest {
  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(InconsistentOverloads.class, getClass());

  @Test
  public void inconsistentOverloadsNegativeCases() {
    compilationHelper
        .addSourceLines(
            "InconsistentOverloadsNegativeCases.java",
            """
            package com.google.errorprone.bugpatterns.overloading.testdata;

            public final class InconsistentOverloadsNegativeCases {

              public void foo(Object object) {}

              public void foo(Object object, int x, int y) {}

              public void foo(Object object, int x, int y, String string) {}

              public void bar(int x, int y, int z) {}

              public void bar(int x) {}

              public void bar(int x, int y) {}

              public void baz(String string) {}

              public void baz(int x, int y, String otherString) {}

              public void baz(int x, int y, String otherString, Object object) {}

              public void quux(int x, int y, int z) {}

              public void quux(int x, int y, String string) {}

              public void norf(int x, int y) {}

              public void norf(Object object, String string) {}
            }""")
        .doTest();
  }

  @Test
  public void inconsistentOverloadsPositiveCasesAnnotations() {
    compilationHelper
        .addSourceLines(
            "InconsistentOverloadsPositiveCasesAnnotations.java",
            """
            package com.google.errorprone.bugpatterns.overloading.testdata;

            import org.jspecify.annotations.Nullable;

            public abstract class InconsistentOverloadsPositiveCasesAnnotations {

              @interface Bar {}

              @interface Baz {}

              // BUG: Diagnostic contains: foo(String x, String y, Object z)
              abstract void foo(@Nullable Object z, String y, @Nullable String x);

              abstract void foo(@Nullable String x);

              // BUG: Diagnostic contains: foo(String x, String y)
              abstract void foo(String y, @Nullable String x);

              // BUG: Diagnostic contains: quux(Object object, String string)
              int quux(String string, @Bar @Baz Object object) {
                return string.hashCode() + quux(object);
              }

              int quux(@Bar @Baz Object object) {
                return object.hashCode();
              }

              // BUG: Diagnostic contains: quux(Object object, String string, int x, int y)
              abstract int quux(String string, int x, int y, @Bar @Baz Object object);

              abstract int norf(@Bar @Baz String string);

              // BUG: Diagnostic contains: norf(String string, Object object)
              abstract int norf(Object object, @Baz @Bar String string);
            }""")
        .doTest();
  }

  @Test
  public void inconsistentOverloadsPositiveCasesGeneral() {
    compilationHelper
        .addSourceLines(
            "InconsistentOverloadsPositiveCasesGeneral.java",
            """
            package com.google.errorprone.bugpatterns.overloading.testdata;

            public final class InconsistentOverloadsPositiveCasesGeneral {

              public void foo(Object object) {}

              // BUG: Diagnostic contains: foo(Object object, int i)
              public void foo(int i, Object object) {}

              // BUG: Diagnostic contains: foo(Object object, int i, String string)
              public void foo(String string, Object object, int i) {}

              // BUG: Diagnostic contains: bar(int i, int j, String x, String y, Object object)
              public void bar(Object object, String x, String y, int i, int j) {}

              public void bar(int i, int j) {}

              // BUG: Diagnostic contains: bar(int i, int j, String x, String y)
              public void bar(String x, String y, int i, int j) {}

              public void baz(int i, int j) {}

              public void baz(Object object) {}

              // BUG: Diagnostic contains: baz(int i, int j, String x, Object object)
              public void baz(String x, int i, int j, Object object) {}

              public void quux(int x, int y, String string) {}

              // BUG: Diagnostic contains: quux(int x, int y, Object object)
              public void quux(Object object, int y, int x) {}
            }""")
        .doTest();
  }

  @Test
  public void inconsistentOverloadsPositiveCasesGenerics() {
    compilationHelper
        .addSourceLines(
            "InconsistentOverloadsPositiveCasesGenerics.java",
            """
package com.google.errorprone.bugpatterns.overloading.testdata;

import java.util.List;

public final class InconsistentOverloadsPositiveCasesGenerics {

  // BUG: Diagnostic contains: foo(List<Integer> numbers, List<List<Integer>> nestedNumbers)
  public void foo(List<List<Integer>> nestedNumbers, List<Integer> numbers) {}

  public void foo(List<Integer> numbers) {}

  // BUG: Diagnostic contains: foo(Iterable<Integer> numbers, String description)
  public void foo(String description, Iterable<Integer> numbers) {}

  public void bar(int x) {}

  // BUG: Diagnostic contains: bar(int x, List<? extends java.util.ArrayList<String>> strings)
  public void bar(List<? extends java.util.ArrayList<String>> strings, int x) {}
}""")
        .doTest();
  }

  @Test
  public void inconsistentOverloadsPositiveCasesInterleaved() {
    compilationHelper
        .addSourceLines(
            "InconsistentOverloadsPositiveCasesInterleaved.java",
            """
            package com.google.errorprone.bugpatterns.overloading.testdata;

            public final class InconsistentOverloadsPositiveCasesInterleaved {

              // BUG: Diagnostic contains: baz(int x, String string, int y)
              public void baz(int y, int x, String string) {}

              // BUG: Diagnostic contains: foo(int x, int y, int z, String string)
              public void foo(int x, int z, int y, String string) {}

              public void foo(int x, int y) {}

              public void bar(String string, Object object) {}

              // BUG: Diagnostic contains: baz(int x, String string)
              public void baz(String string, int x) {}

              // BUG: Diagnostic contains: foo(int x, int y, int z)
              public void foo(int z, int x, int y) {}

              // BUG: Diagnostic contains: bar(String string, Object object, int x, int y)
              public void bar(int x, int y, String string, Object object) {}

              public void baz(int x) {}
            }""")
        .doTest();
  }

  @Test
  public void inconsistentOverloadsPositiveCasesSimple() {
    compilationHelper
        .addSourceLines(
            "InconsistentOverloadsPositiveCasesSimple.java",
            """
            package com.google.errorprone.bugpatterns.overloading.testdata;

            public final class InconsistentOverloadsPositiveCasesSimple {

              public void foo(Object object) {}

              // BUG: Diagnostic contains: foo(Object object, int x, int y)
              public void foo(int x, int y, Object object) {}

              // BUG: Diagnostic contains: foo(Object object, int x, int y, String string)
              public void foo(String string, int y, Object object, int x) {}
            }""")
        .doTest();
  }

  @Test
  public void inconsistentOverloadsPositiveCasesVarargs() {
    compilationHelper
        .addSourceLines(
            "InconsistentOverloadsPositiveCasesVarargs.java",
            """
            package com.google.errorprone.bugpatterns.overloading.testdata;

            public abstract class InconsistentOverloadsPositiveCasesVarargs {

              public void foo(String... rest) {}

              public void foo(int x, String... rest) {}

              // BUG: Diagnostic contains: foo(int x, int y, String... rest)
              public void foo(int y, int x, String... rest) {}

              abstract void bar(float x, float y);

              // BUG: Diagnostic contains: bar(float x, float y, float z, Object... rest)
              abstract void bar(float z, float y, float x, Object... rest);

              // BUG: Diagnostic contains: bar(float x, float y, float z, String string)
              abstract void bar(float y, String string, float x, float z);

              abstract void bar(Object... rest);
            }""")
        .doTest();
  }

  @Test
  public void inconsistentOverloadsOverrides() {
    compilationHelper
        .addSourceLines(
            "InconsistentOverloadsPositiveCasesOverrides.java",
            """
package com.google.errorprone.bugpatterns.overloading.testdata;

import java.util.List;
import java.util.Map;

public class InconsistentOverloadsPositiveCasesOverrides {

  class SuperClass {

    void someMethod(String foo, int bar) {}

    // BUG: Diagnostic contains: someMethod(String foo, int bar, List<String> baz)
    void someMethod(int bar, String foo, List<String> baz) {}
  }

  class SubClass extends SuperClass {

    @Override // no bug
    void someMethod(String foo, int bar) {}

    @Override // no bug
    void someMethod(int bar, String foo, List<String> baz) {}

    // BUG: Diagnostic contains: someMethod(String foo, int bar, List<String> baz, Map<String,
    // String> fizz)
    void someMethod(int bar, String foo, List<String> baz, Map<String, String> fizz) {}
  }
}""")
        .doTest();
  }

  @Test
  public void suppressOnMethod() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              public void foo(Object object) {}

              @SuppressWarnings("InconsistentOverloads")
              public void foo(int i, Object object) {}
            }
            """)
        .doTest();
  }
}
