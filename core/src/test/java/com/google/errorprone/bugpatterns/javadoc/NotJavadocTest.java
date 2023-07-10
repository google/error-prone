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

package com.google.errorprone.bugpatterns.javadoc;

import static com.google.errorprone.BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class NotJavadocTest {
  private final BugCheckerRefactoringTestHelper helper =
      BugCheckerRefactoringTestHelper.newInstance(NotJavadoc.class, getClass());

  @Test
  public void notJavadoc() {
    helper
        .addInputLines(
            "Test.java", //
            "class Test {",
            "  void test() {",
            "    /** Not Javadoc. */",
            "  }",
            "}")
        .addOutputLines(
            "Test.java", //
            "class Test {",
            "  void test() {",
            "    /* Not Javadoc. */",
            "  }",
            "}")
        .doTest(TEXT_MATCH);
  }

  @Test
  public void doubleJavadoc() {
    helper
        .addInputLines(
            "Test.java", //
            "class Test {",
            // It would be nice if this were caught.
            "  /** Not Javadoc. */",
            "  /** Javadoc. */",
            "  void test() {",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void notJavadocOnLocalClass() {
    helper
        .addInputLines(
            "Test.java",
            "class Test {",
            "  void test() {",
            "    /** Not Javadoc. */",
            "    class A {}",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "class Test {",
            "  void test() {",
            "    /* Not Javadoc. */",
            "    class A {}",
            "  }",
            "}")
        .doTest(TEXT_MATCH);
  }

  @Test
  public void notJavadocWithLotsOfAsterisks() {
    helper
        .addInputLines(
            "Test.java",
            "class Test {",
            "  void test() {",
            "    /******** Not Javadoc. */",
            "    class A {}",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "class Test {",
            "  void test() {",
            "    /* Not Javadoc. */",
            "    class A {}",
            "  }",
            "}")
        .doTest(TEXT_MATCH);
  }

  @Test
  public void actuallyJavadoc() {
    helper
        .addInputLines(
            "Test.java", //
            "class Test {",
            "  /** Not Javadoc. */",
            "  void test() {",
            "  }",
            "}")
        .expectUnchanged()
        .doTest(TEXT_MATCH);
  }

  @Test
  public void strangeComment() {
    helper
        .addInputLines(
            "Test.java", //
            "class Test {",
            "  void test() {",
            "    /**/",
            "  }",
            "}")
        .expectUnchanged()
        .doTest(TEXT_MATCH);
  }

  @Test
  public void packageLevel() {
    helper
        .addInputLines(
            "package-info.java", //
            "/** Package javadoc */",
            "package foo;")
        .expectUnchanged()
        .doTest(TEXT_MATCH);
  }

  @Test
  public void moduleLevel() {
    helper
        .addInputLines(
            "module-info.java", //
            "/** Module javadoc */",
            "module foo {}")
        .expectUnchanged()
        .doTest(TEXT_MATCH);
  }

  @Test
  public void suppression() {
    helper
        .addInputLines(
            "Test.java", //
            "class Test {",
            "  @SuppressWarnings(\"NotJavadoc\")",
            "  void test() {",
            "    /** Not Javadoc. */",
            "  }",
            "}")
        .expectUnchanged()
        .doTest(TEXT_MATCH);
  }
}
