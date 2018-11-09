/*
 * Copyright 2018 The Error Prone Authors.
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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link DeduplicateConstants}Test */
@RunWith(JUnit4.class)
public class DeduplicateConstantsTest {

  @Test
  public void positive() {
    BugCheckerRefactoringTestHelper.newInstance(new DeduplicateConstants(), getClass())
        .addInputLines(
            "Test.java",
            "class Test {",
            "  static final String C = \"hello world\";",
            "  void f() {",
            "    System.err.println(\"hello world\");",
            "    System.err.println(\"hello world\");",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "class Test {",
            "  static final String C = \"hello world\";",
            "  void f() {",
            "    System.err.println(C);",
            "    System.err.println(C);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void effectivelyFinal() {
    BugCheckerRefactoringTestHelper.newInstance(new DeduplicateConstants(), getClass())
        .addInputLines(
            "Test.java",
            "class Test {",
            "  void f() {",
            "    String C = \"hello world\";",
            "    System.err.println(\"hello world\");",
            "    System.err.println(\"hello world\");",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "class Test {",
            "  void f() {",
            "    String C = \"hello world\";",
            "    System.err.println(C);",
            "    System.err.println(C);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeRecursiveInitializers() {
    BugCheckerRefactoringTestHelper.newInstance(new DeduplicateConstants(), getClass())
        .addInputLines(
            "Test.java",
            "class Test {",
            "  static final String C = \"hello\";",
            "  class One {",
            "    static final String C = \"hello\";",
            "  }",
            "  class Two {",
            "    static final String C = \"hello\";",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void negativeOnlyOneUse() {
    BugCheckerRefactoringTestHelper.newInstance(new DeduplicateConstants(), getClass())
        .addInputLines(
            "Test.java",
            "class Test {",
            "  static final String C = \"hello world\";",
            "  void f() {",
            "    System.err.println(\"hello world\");",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void negativeTooShort() {
    BugCheckerRefactoringTestHelper.newInstance(new DeduplicateConstants(), getClass())
        .addInputLines(
            "Test.java",
            "class Test {",
            "  static final String C = \".\";",
            "  void f() {",
            "    System.err.println(\".\");",
            "    System.err.println(\".\");",
            "    System.err.println(\".\");",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }
}
