/*
 * Copyright 2022 The Error Prone Authors.
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

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link IncorrectMainMethod}Test */
@RunWith(JUnit4.class)
public class IncorrectMainMethodTest {
  private final CompilationTestHelper testHelper =
      CompilationTestHelper.newInstance(IncorrectMainMethod.class, getClass());

  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(IncorrectMainMethod.class, getClass());

  @Test
  public void negative() {
    testHelper
        .addSourceLines(
            "Test.java",
            "class Test {", //
            "  public static void main(String[] args) {}",
            "}")
        .doTest();
  }

  @Test
  public void negativeName() {
    testHelper
        .addSourceLines(
            "Test.java",
            "class Test {", //
            "  public static void spain(String[] args) {}",
            "}")
        .doTest();
  }

  // clever but not wrong
  @Test
  public void negativeImplicitPublic() {
    testHelper
        .addSourceLines(
            "Test.java",
            "interface Test {", //
            "  static void main(String[] args) {}",
            "}")
        .doTest();
  }

  // clever but not wrong
  @Test
  public void negativeVarargs() {
    testHelper
        .addSourceLines(
            "Test.java",
            "class Test {", //
            "  public static void main(String... args) {}",
            "}")
        .doTest();
  }

  @Test
  public void positiveNonPublic() {
    testHelper
        .addSourceLines(
            "Test.java",
            "class Test {", //
            "  // BUG: Diagnostic contains:",
            "  static void main(String[] args) {}",
            "}")
        .doTest();
  }

  @Test
  public void positiveNonStatic() {
    testHelper
        .addSourceLines(
            "Test.java",
            "class Test {", //
            "  // BUG: Diagnostic contains:",
            "  public void main(String[] args) {}",
            "}")
        .doTest();
  }

  @Test
  public void positiveNonVoid() {
    testHelper
        .addSourceLines(
            "Test.java",
            "class Test {", //
            "  // BUG: Diagnostic contains:",
            "  public static int main(String[] args) {",
            "    return 0;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeArity() {
    testHelper
        .addSourceLines(
            "Test.java",
            "class Test {", //
            "  static void main(String[] args, String arg) {}",
            "}")
        .doTest();
  }

  @Test
  public void negativeNotArray() {
    testHelper
        .addSourceLines(
            "Test.java",
            "class Test {", //
            "  static void main(String args) {}",
            "}")
        .doTest();
  }

  @Test
  public void negativeNotStringArray() {
    testHelper
        .addSourceLines(
            "Test.java",
            "class Test {", //
            "  static void main(Object[] args) {}",
            "}")
        .doTest();
  }

  @Test
  public void removePrivate() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "class Test {", //
            "  private static void main(String[] args) {}",
            "}")
        .addOutputLines(
            "Test.java",
            "class Test {", //
            "  public static void main(String[] args) {}",
            "}")
        .doTest();
  }

  @Test
  public void removeProtected() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "class Test {", //
            "  protected static void main(String[] args) {}",
            "}")
        .addOutputLines(
            "Test.java",
            "class Test {", //
            "  public static void main(String[] args) {}",
            "}")
        .doTest();
  }
}
