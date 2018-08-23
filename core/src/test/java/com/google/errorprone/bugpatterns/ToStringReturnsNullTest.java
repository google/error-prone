/*
 * Copyright 2016 The Error Prone Authors.
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
 * Unit tests for {@link ToStringReturnsNull} bug pattern.
 *
 * @author eleanorh@google.com (Eleanor Harris)
 * @author siyuanl@google.com (Siyuan Liu)
 */
@RunWith(JUnit4.class)
public final class ToStringReturnsNullTest {
  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper = CompilationTestHelper.newInstance(ToStringReturnsNull.class, getClass());
  }

  @Test
  public void positive() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  // BUG: Diagnostic contains: ToStringReturnsNull",
            "  public String toString() {",
            "    return null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void returnsNonNullString() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  public String toString() {",
            "    return \"foo\";",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void nonToStringMethod() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  public String thisIsNotAToStringMethod() {",
            "    return \"bar\";",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void nestedReturnNull() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  public String toString() {",
            "    class InnerTest {",
            "      String getter() {",
            "        return null;",
            "      }",
            "    }",
            "    return \"bar\";",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void lambdaReturnNull() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  interface MyGreeting {",
            "    String processName();",
            "  }",
            "  public String toString() {",
            "    MyGreeting lol = () -> {",
            "      String lolz = \"\";",
            "      return null;",
            "    };",
            "    return \"bar\";",
            "  }",
            "}")
        .doTest();
  }
}
