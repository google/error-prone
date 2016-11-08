/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

/** Tests for {@link ArgumentParameterMismatch}. */
@RunWith(JUnit4.class)
public class ArgumentParameterMismatchTest {
  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper =
        CompilationTestHelper.newInstance(ArgumentParameterMismatch.class, getClass())
            .addSourceLines(
                "MyObject.java",
                "import java.util.List;",
                "public class MyObject {",
                "  public void setlist(List<?> list) {}",
                "  public void setstring(String string) {}",
                "  public void setInt(int integer) {}",
                "}");
  }

  @Test
  public void otherLocalBetterMatch() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void doIt() {",
            "    int integer = 0, foo = 0;",
            "    // BUG: Diagnostic contains: setInt(integer)",
            "    new MyObject().setInt(foo);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void noBetterMatch() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void doIt() {",
            "    int integer = 0, foo = 0;",
            "    new MyObject().setInt(integer);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void dontSuggestInstanceStateInExplicitThisConstructorInvocation() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  private static final String CONST = \"CONST\";",
            "  private String s1;",
            "  public Test() {",
            "    this(CONST);",
            "  }",
            "  public Test(String s1) {}",
            "}")
        .doTest();
  }

  @Test
  public void dontSuggestInstanceStateInExplicitSuperConstructorInvocation() {
    compilationHelper
        .addSourceLines(
            "Super.java",
            "class Super {",
            "  protected String s1;",
            "  protected Super(String s1) {}",
            "}")
        .addSourceLines(
            "Sub.java",
            "class Sub extends Super {",
            "  private static final String CONST = \"CONST\";",
            "  public Sub() {",
            "    super(CONST);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void skipVarargs() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  public void doIt(String... s1) {",
            "    String correct = \"CORRECT\";",
            "    doIt(correct);",
            "  }",
            "  public void doIt2(String s1) {}",
            "}")
        .doTest();
  }
}
