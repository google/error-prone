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
import com.google.errorprone.util.RuntimeVersion;
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
            (RuntimeVersion.isAtLeast9()
                ? "import javax.annotation.processing.Generated;"
                : "import javax.annotation.Generated;"),
            "@Generated(\"generator\")",
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
}
