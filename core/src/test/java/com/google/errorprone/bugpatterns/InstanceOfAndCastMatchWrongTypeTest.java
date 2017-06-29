/* Copyright 2016 Google Inc. All Rights Reserved.
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

/**
 * @author sulku@google.com (Marsela Sulku)
 * @author mariasam@google.com (Maria Sam)
 */
@RunWith(JUnit4.class)
public class InstanceOfAndCastMatchWrongTypeTest {
  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper =
        CompilationTestHelper.newInstance(InstanceOfAndCastMatchWrongType.class, getClass());
  }

  @Test
  public void testPositiveCase() {
    compilationHelper.addSourceFile("InstanceOfAndCastMatchWrongTypePositiveCases.java").doTest();
  }

  @Test
  public void testNegativeCase() {
    compilationHelper.addSourceFile("InstanceOfAndCastMatchWrongTypeNegativeCases.java").doTest();
  }

  @Test
  public void regressionTestIssue651() {
    compilationHelper
        .addSourceLines(
            "Foo.java",
            "class Foo {",
            "  void foo() {",
            "    Object[] values = null;",
            "    if (values[0] instanceof Integer) {",
            "      int x = (Integer) values[0];",
            "    } else if (values[0] instanceof Long) {",
            "      long y = (Long) values[0];",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void handlesArrayAccessOnIdentifer() {
    compilationHelper
        .addSourceLines(
            "Foo.java",
            "class Foo {",
            "  void foo() {",
            "    Object[] values = null;",
            "    if (values[0] instanceof Integer) {",
            "      // BUG: Diagnostic contains:",
            "      String s0 = (String) values[0];",
            // OK because indices are different
            "      String s1 = (String) values[1];",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void doesNotHandleArrayAccessOnNonIdentifiers() {
    compilationHelper
        .addSourceLines(
            "Foo.java",
            "class Foo {",
            "  private Object[] getArray() {",
            "    return new Object[0];",
            "  }",
            "  void doIt() {",
            "    if (getArray()[0] instanceof Integer) {",
            "      String s0 = (String) getArray()[0];",
            "    }",
            "  }",
            "}")
        .doTest();
  }
}
