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

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link ThreadLocalUsage}Test */
@RunWith(JUnit4.class)
public class ThreadLocalUsageTest {

  private final CompilationTestHelper testHelper =
      CompilationTestHelper.newInstance(ThreadLocalUsage.class, getClass());

  @Test
  public void positive() {
    testHelper
        .addSourceLines(
            "ThreadLocalUsage.java", //
            "class ThreadLocalUsage {",
            "  // BUG: Diagnostic contains:",
            "  ThreadLocal<Object> local = new ThreadLocal<>();",
            "  {",
            "    new ThreadLocal<>();",
            "    new ThreadLocal<Object>() {};",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative() {
    testHelper
        .addSourceLines(
            "Test.java", //
            "class Test {",
            "  static final ThreadLocal<Object> local = new ThreadLocal<>();",
            "}")
        .doTest();
  }

  @Test
  public void negativeWellKnownTypes() {
    testHelper
        .addSourceLines(
            "Test.java", //
            "import java.text.DateFormat;",
            "import java.text.SimpleDateFormat;",
            "class Test {",
            "  final ThreadLocal<Boolean> a = new ThreadLocal<>();",
            "  final ThreadLocal<Long> b = new ThreadLocal<>();",
            "  final ThreadLocal<DateFormat> c = new ThreadLocal<>();",
            "  final ThreadLocal<SimpleDateFormat> d = new ThreadLocal<>();",
            "}")
        .doTest();
  }

  @Test
  public void negativeSingleton() {
    testHelper
        .addSourceLines(
            "Singleton.java", //
            "@interface Singleton {}")
        .addSourceLines(
            "Test.java", //
            "@Singleton",
            "class Test {",
            "  final ThreadLocal<Object> a = new ThreadLocal<>();",
            "}")
        .doTest();
  }
}
