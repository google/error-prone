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

import com.google.errorprone.CompilationTestHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** @author scottjohnson@google.com (Scott Johnson) */
@RunWith(JUnit4.class)
public class WrongParameterPackageTest {

  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper = CompilationTestHelper.newInstance(WrongParameterPackage.class, getClass());
  }

  @Test
  public void testPositiveCase() {
    compilationHelper
        .addSourceFile("WrongParameterPackageNegativeCases.java") // used as a dependency
        .addSourceFile("WrongParameterPackagePositiveCases.java")
        .doTest();
  }

  @Test
  public void testNegativeCase() {
    compilationHelper.addSourceFile("WrongParameterPackageNegativeCases.java").doTest();
  }

  // regression test for https://github.com/google/error-prone/issues/330
  @Test
  public void testNPE() {
    compilationHelper
        .addSourceLines(
            "foo/Bar.java", "package foo;", "public interface Bar {", "    void bar();", "}")
        .doTest();
  }

  // regression test for https://github.com/google/error-prone/issues/356
  @Test
  public void testCompleteParams() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "package org.gaul.mypackage;",
            "import java.io.IOException;",
            "import java.io.InputStream;",
            "class MyInputStream extends InputStream {",
            "  @Override",
            "  public int read() throws IOException {",
            "    return 0;",
            "  }",
            "}")
        .doTest();
  }
}
