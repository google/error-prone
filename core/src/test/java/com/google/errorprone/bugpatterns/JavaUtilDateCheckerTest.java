/*
 * Copyright 2020 The Error Prone Authors.
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

/** {@link JavaUtilDateChecker}Test */
@RunWith(JUnit4.class)
public class JavaUtilDateCheckerTest {

  private final CompilationTestHelper testHelper =
      CompilationTestHelper.newInstance(JavaUtilDateChecker.class, getClass());

  @Test
  public void javaUtilDate_constructors() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import java.util.Date;",
            "class Test {",
            "  // BUG: Diagnostic contains: Date has a bad API",
            "  private static final Date date1 = new Date();",
            "  // BUG: Diagnostic contains: Date has a bad API",
            "  private static final Date date2 = new Date(123456789L);",
            "}")
        .doTest();
  }

  @Test
  public void javaUtilDate_staticMethods() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import java.util.Date;",
            "class Test {",
            "  // BUG: Diagnostic contains: Date has a bad API",
            "  private static final long date = Date.parse(\"Sat, 12 Aug 1995 13:30:00 GMT\");",
            "}")
        .doTest();
  }

  @Test
  public void javaUtilDate_instanceMethods() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import java.util.Date;",
            "class Test {",
            "  public void doSomething(Date date) {",
            "    // BUG: Diagnostic contains: Date has a bad API",
            "    long time = date.getTime();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void javaUtilDate_allowedApis() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import java.time.Instant;",
            "import java.util.Date;",
            "class Test {",
            "  public void doSomething(Date date) {",
            "    Instant instant = date.toInstant();",
            "    Date date2 = Date.from(instant);",
            "  }",
            "}")
        .doTest();
  }
}
