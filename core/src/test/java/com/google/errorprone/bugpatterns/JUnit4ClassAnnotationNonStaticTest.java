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

/** Unit test of {@link JUnit4ClassAnnotationNonStatic} */
@RunWith(JUnit4.class)
public class JUnit4ClassAnnotationNonStaticTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(JUnit4ClassAnnotationNonStatic.class, getClass());

  @Test
  public void positive() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import org.junit.runner.RunWith;",
            "import org.junit.runners.JUnit4;",
            "import org.junit.BeforeClass;",
            "import org.junit.AfterClass;",
            "@RunWith(JUnit4.class)",
            "public class Test {",
            "  @BeforeClass",
            "  // BUG: Diagnostic contains: BeforeClass can only be applied to static methods.",
            "  public void shouldDoSomething() {}",
            "",
            "  @AfterClass",
            "  // BUG: Diagnostic contains:  AfterClass can only be applied to static methods.",
            "  public void shouldDoSomethingElse() {}",
            "",
            "  @AfterClass @BeforeClass",
            "  // BUG: Diagnostic contains:  AfterClass and BeforeClass can only be applied to",
            "  public void shouldDoSomethingElseBlah() {}",
            "}")
        .doTest();
  }

  @Test
  public void negative() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import org.junit.runner.RunWith;",
            "import org.junit.runners.JUnit4;",
            "import org.junit.BeforeClass;",
            "import org.junit.AfterClass;",
            "@RunWith(JUnit4.class)",
            "public class Test {",
            "  @BeforeClass",
            "  public static void shouldDoSomething() {}",
            "",
            "  @AfterClass",
            "  public static void shouldDoSomethingElse() {}",
            "}")
        .doTest();
  }
}
