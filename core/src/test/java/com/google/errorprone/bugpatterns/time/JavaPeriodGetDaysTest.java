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
package com.google.errorprone.bugpatterns.time;

import static org.junit.Assume.assumeFalse;

import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.util.RuntimeVersion;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link JavaPeriodGetDays}.
 *
 * @author kak@google.com (Kurt Alfred Kluever)
 */
@RunWith(JUnit4.class)
public class JavaPeriodGetDaysTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(JavaPeriodGetDays.class, getClass());

  @Test
  public void getBoth() {
    compilationHelper
        .addSourceLines(
            "test/TestCase.java",
            "package test;",
            "import java.time.Period;",
            "public class TestCase {",
            "  public static void foo(Period period) {",
            "    int days = period.getDays();",
            "    int months = period.getMonths();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void getDaysInReturn() {
    compilationHelper
        .addSourceLines(
            "test/TestCase.java",
            "package test;",
            "import java.time.Period;",
            "public class TestCase {",
            "  public static int foo(Period period) {",
            "    // BUG: Diagnostic contains: JavaPeriodGetDays",
            "    return period.getDays();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void getDaysInReturnWithConsultingMonths() {
    compilationHelper
        .addSourceLines(
            "test/TestCase.java",
            "package test;",
            "import com.google.common.collect.ImmutableMap;",
            "import java.time.Period;",
            "public class TestCase {",
            "  public static ImmutableMap<String, Object> foo(Period period) {",
            "    return ImmutableMap.of(",
            "        \"months\", period.getMonths(), \"days\", period.getDays());",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void getDaysWithMonthsInDifferentScope() {
    // Ideally we would also catch cases like this, but it requires scanning "too much" of the class
    compilationHelper
        .addSourceLines(
            "test/TestCase.java",
            "package test;",
            "import java.time.Period;",
            "public class TestCase {",
            "  public static void foo(Period period) {",
            "    long months = period.getMonths();",
            "    if (true) {",
            "      // BUG: Diagnostic contains: JavaPeriodGetDays",
            "      int days = period.getDays();",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void getDaysAndGetMonthsInDifferentMethods() {
    compilationHelper
        .addSourceLines(
            "test/TestCase.java",
            "package test;",
            "import java.time.Period;",
            "public class TestCase {",
            "  public static void foo(Period period) {",
            "    long months = period.getMonths();",
            "  }",
            "  public static void bar(Period period) {",
            "    // BUG: Diagnostic contains: JavaPeriodGetDays",
            "    int days = period.getDays();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void getMonths() {
    compilationHelper
        .addSourceLines(
            "test/TestCase.java",
            "package test;",
            "import java.time.Period;",
            "public class TestCase {",
            "  public static void foo(Period period) {",
            "    long months = period.getMonths();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void getDaysOnly() {
    compilationHelper
        .addSourceLines(
            "test/TestCase.java",
            "package test;",
            "import java.time.Period;",
            "public class TestCase {",
            "  public static void foo(Period period) {",
            "    // BUG: Diagnostic contains: JavaPeriodGetDays",
            "    int days = period.getDays();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void getMonthsInVariableDetachedFromDays() {
    compilationHelper
        .addSourceLines(
            "test/TestCase.java",
            "package test;",
            "import java.time.Period;",
            "public class TestCase {",
            "  private static final Period PERIOD = Period.ZERO;",
            "  private static final long months = PERIOD.getMonths();",
            "  public static void foo() {",
            "    // BUG: Diagnostic contains: JavaPeriodGetDays",
            "    int days = PERIOD.getDays();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void getMonthsInStaticBlock() {
    compilationHelper
        .addSourceLines(
            "test/TestCase.java",
            "package test;",
            "import java.time.Period;",
            "public class TestCase {",
            "  static {",
            "    long months = Period.ZERO.getMonths();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void getDaysInStaticBlock() {
    compilationHelper
        .addSourceLines(
            "test/TestCase.java",
            "package test;",
            "import java.time.Period;",
            "public class TestCase {",
            "  static {",
            "    // BUG: Diagnostic contains: JavaPeriodGetDays",
            "    int days = Period.ZERO.getDays();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void getDaysAndMonthsInParallelInitializers() {
    compilationHelper
        .addSourceLines(
            "test/TestCase.java",
            "package test;",
            "import java.time.Period;",
            "public class TestCase {",
            "  private final long months = Period.ZERO.getMonths();",
            "  private final int days = Period.ZERO.getDays();",
            "}")
        .doTest();
  }

  @Test
  public void getDaysInFieldInitializer() {
    compilationHelper
        .addSourceLines(
            "test/TestCase.java",
            "package test;",
            "import java.time.Period;",
            "public class TestCase {",
            "  // BUG: Diagnostic contains: JavaPeriodGetDays",
            "  private final int days = Period.ZERO.getDays();",
            "}")
        .doTest();
  }

  @Test
  public void getDaysInInnerClass() {
    compilationHelper
        .addSourceLines(
            "test/TestCase.java",
            "package test;",
            "import java.time.Period;",
            "public class TestCase {",
            "  private static final Period PERIOD = Period.ZERO;",
            "  public static void foo() {",
            "    long months = PERIOD.getMonths();",
            "    Object obj = new Object() {",
            "      @Override public String toString() {",
            "        // BUG: Diagnostic contains: JavaPeriodGetDays",
            "        return String.valueOf(PERIOD.getDays()); ",
            "      }",
            "    };",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void getDaysInInnerClassField() {
    compilationHelper
        .addSourceLines(
            "test/TestCase.java",
            "package test;",
            "import java.time.Period;",
            "public class TestCase {",
            "  Period PERIOD = Period.ZERO;",
            "  long months = PERIOD.getMonths();",
            "  Object obj = new Object() {",
            "    // BUG: Diagnostic contains: JavaPeriodGetDays",
            "    long days = PERIOD.getDays();",
            "  };",
            "}")
        .doTest();
  }

  @Test
  public void getDaysWithMonthInLambdaContext() {
    compilationHelper
        .addSourceLines(
            "test/TestCase.java",
            "package test;",
            "import java.time.Period;",
            "public class TestCase {",
            "  private static final Period PERIOD = Period.ZERO;",
            "  public static void foo() {",
            "    Runnable r = () -> PERIOD.getMonths();",
            "    // BUG: Diagnostic contains: JavaPeriodGetDays",
            "    int days = PERIOD.getDays();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void getDaysWithMonthInMethodArgumentLambda() {
    compilationHelper
        .addSourceLines(
            "test/TestCase.java",
            "package test;",
            "import java.time.Period;",
            "import java.util.function.Supplier;",
            "public class TestCase {",
            "  private static final Period PERIOD = Period.ZERO;",
            "  public void foo() {",
            "    doSomething(() -> PERIOD.getMonths());",
            "    // BUG: Diagnostic contains: JavaPeriodGetDays",
            "    int days = PERIOD.getDays();",
            "  }",
            "  public void doSomething(Supplier<Integer> supplier) {",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void getDaysInLambdaWithMonthOutside() {
    compilationHelper
        .addSourceLines(
            "test/TestCase.java",
            "package test;",
            "import java.time.Period;",
            "public class TestCase {",
            "  private static final Period PERIOD = Period.ZERO;",
            "  public static void foo() {",
            "    // BUG: Diagnostic contains: JavaPeriodGetDays",
            "    Runnable r = () -> PERIOD.getDays();",
            "    long months = PERIOD.getMonths();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testByJavaTime() {
    assumeFalse(RuntimeVersion.isAtLeast9());
    compilationHelper
        .addSourceLines(
            "test/TestCase.java",
            "package java.time;",
            "public class TestCase {",
            "  private static final Period PERIOD = Period.ZERO;",
            "  public static void foo() {",
            "    int nanos = PERIOD.getDays();",
            "  }",
            "}")
        .doTest();
  }
}
