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

/**
 * Unit tests for {@link DefaultPackage} bug pattern.
 *
 * @author bhagwani@google.com (Sumit Bhagwani)
 */
@RunWith(JUnit4.class)
public class DefaultPackageTest {
  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(DefaultPackage.class, getClass());

  @Test
  public void testPositiveCases() {
    compilationHelper
        .addSourceLines(
            "Test.java", //
            "// BUG: Diagnostic contains: DefaultPackage",
            "class Test {",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases_classWithGenerated() {
    compilationHelper
        .addSourceLines(
            "Test.java", //
            "import javax.annotation.processing.Generated;",
            "@Generated(\"generator\")",
            "class Test {",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases_classWithWarningSuppressed() {
    compilationHelper
        .addSourceLines(
            "in/Test.java", //
            "@SuppressWarnings(\"DefaultPackage\")",
            "class Test {",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases_classWithPackage() {
    compilationHelper
        .addSourceLines(
            "in/Test.java", //
            "package in;",
            "class Test {",
            "}")
        .doTest();
  }

  // see b/2276473
  @Test
  public void trailingComma() {
    compilationHelper
        .addSourceLines(
            "T.java", //
            "package a;",
            "class T {};")
        .doTest();
  }

  @Test
  public void moduleInfo() {

    compilationHelper
        .addSourceLines(
            "module-info.java", //
            "module testmodule {",
            "}")
        .doTest();
  }
}
