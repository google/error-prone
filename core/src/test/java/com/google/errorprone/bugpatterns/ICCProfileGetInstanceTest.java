/*
 * Copyright 2023 The Error Prone Authors.
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

@RunWith(JUnit4.class)
public class ICCProfileGetInstanceTest {
  private final CompilationTestHelper testHelper =
      CompilationTestHelper.newInstance(ICCProfileGetInstance.class, getClass());

  @Test
  public void positive() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import java.awt.color.ICC_Profile;",
            "class Test {",
            "  void f(String s) throws Exception {",
            "    // BUG: Diagnostic contains:",
            "    ICC_Profile.getInstance(s);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import java.awt.color.ICC_Profile;",
            "class Test {",
            "  void f(byte[] b) throws Exception {",
            "    ICC_Profile.getInstance(b);",
            "  }",
            "}")
        .doTest();
  }
}
