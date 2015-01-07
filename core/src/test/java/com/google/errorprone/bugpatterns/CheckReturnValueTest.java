/*
 * Copyright 2012 Google Inc. All Rights Reserved.
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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Arrays;

/**
 * @author eaftan@google.com (Eddie Aftandilian)
 */
@RunWith(JUnit4.class)
public class CheckReturnValueTest {

  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper = CompilationTestHelper.newInstance(new CheckReturnValue());
  }

  @Test
  public void testPositiveCases() throws Exception {
    compilationHelper.assertCompileFailsWithMessages(compilationHelper.fileManager()
        .sources(getClass(), "CheckReturnValuePositiveCases.java"));
  }

  @Test
  public void testNegativeCase() throws Exception {
    compilationHelper.assertCompileSucceeds(compilationHelper.fileManager()
        .sources(getClass(), "CheckReturnValueNegativeCases.java"));
  }

  @Test
  public void testPackageAnnotation() throws Exception {
    compilationHelper.assertCompileFailsWithMessages(Arrays.asList(
        compilationHelper.fileManager().forSourceLines("package-info.java",
            "@javax.annotation.CheckReturnValue",
            "package lib;"),
        compilationHelper.fileManager().forSourceLines("lib/Lib.java",
            "package lib;",
            "public class Lib {",
            "  public static int f() { return 42; }",
            "}"),
        compilationHelper.fileManager().forSourceLines("Test.java",
            "class Test {",
            "  void m() {",
            "    // BUG: Diagnostic contains: Ignored return value",
            "    lib.Lib.f();",
            "  }",
            "}")));
  }

  @Test
  public void testClassAnnotation() throws Exception {
    compilationHelper.assertCompileFailsWithMessages(Arrays.asList(
        compilationHelper.fileManager().forSourceLines("lib/Lib.java",
            "package lib;",
            "@javax.annotation.CheckReturnValue",
            "public class Lib {",
            "  public static int f() { return 42; }",
            "}"),
        compilationHelper.fileManager().forSourceLines("Test.java",
            "class Test {",
            "  void m() {",
            "    // BUG: Diagnostic contains: Ignored return value",
            "    lib.Lib.f();",
            "  }",
            "}")));
  }
}

