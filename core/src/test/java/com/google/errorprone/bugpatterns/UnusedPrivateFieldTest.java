/*
 * Copyright 2018 Error Prone Authors. All Rights Reserved.
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

/** {@link UnusedPrivateField}Test */
@RunWith(JUnit4.class)
public class UnusedPrivateFieldTest {

  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper = CompilationTestHelper.newInstance(UnusedPrivateField.class, getClass());
  }

  @Test
  public void oneFieldUnused() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "public class Test {",
            "  // BUG: Diagnostic contains: Unused private field",
            "  private Object f;",
            "}")
        .doTest();
  }

  @Test
  public void fieldUsed() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "public class Test {",
            "  private Object f;",
            "  private Object getF() { return this.f; }",
            "}")
        .doTest();
  }

  @Test
  public void usedFromOuter() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "public class Test {",
            "  static class Inner { private Object f; }",
            "  Object getF() { return (new Inner()).f; }",
            "}")
        .doTest();
  }

  @Test
  public void usedFromInner() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "public class Test {",
            "  private Object f;",
            "  class Inner { Inner() { f.toString(); } }",
            "}")
        .doTest();
  }

  @Test
  public void unusedInner() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "public class Test {",
            "  class Inner {",
            "    // BUG: Diagnostic contains: Unused private field",
            "    private Object f;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void multipleInner() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "public class Test {",
            "  class Inner {",
            "    // BUG: Diagnostic contains: Unused private field",
            "    private Object f;",
            "  }",
            "  class Inner2 {",
            "    // BUG: Diagnostic contains: Unused private field",
            "    private Object g;",
            "  }",
            "}")
        .doTest();
  }
}
