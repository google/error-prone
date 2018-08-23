/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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
import com.google.errorprone.BugCheckerRefactoringTestHelper.TestMode;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link CatchFail}Test */
@RunWith(JUnit4.class)
public class CatchFailTest {

  private final BugCheckerRefactoringTestHelper testHelper =
      BugCheckerRefactoringTestHelper.newInstance(new CatchFail(), getClass());

  @Test
  public void positive() {
    testHelper
        .addInputLines(
            "in/Foo.java",
            "import org.junit.Test;",
            "class Foo {",
            "  @Test public void f() {",
            "    try {",
            "      System.err.println();",
            "    } catch (Exception expected) {",
            "      org.junit.Assert.fail();",
            "    }",
            "  }",
            "  public void testFoo() {",
            "    try {",
            "      System.err.println();",
            "    } catch (Exception expected) {",
            "      org.junit.Assert.fail();",
            "    }",
            "  }",
            "}")
        .addOutputLines(
            "out/Foo.java",
            "import org.junit.Test;",
            "class Foo {",
            "  @Test public void f() throws Exception {",
            "    System.err.println();",
            "  }",
            "  public void testFoo() throws Exception {",
            "    System.err.println();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positive_failFail() {
    testHelper
        .addInputLines(
            "in/Foo.java",
            "import org.junit.Test;",
            "class Foo {",
            "  @Test public void f() {",
            "    try {",
            "      System.err.println();",
            "    } catch (Exception expected) {",
            "      org.junit.Assert.fail();",
            "    } catch (Throwable unexpected) {",
            "      org.junit.Assert.fail();",
            "    }",
            "  }",
            "}")
        .addOutputLines(
            "out/Foo.java",
            "import org.junit.Test;",
            "class Foo {",
            "  @Test public void f() throws Exception, Throwable {",
            "    System.err.println();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positive_finally() {
    testHelper
        .addInputLines(
            "in/Test.java",
            "class Test {",
            "  public void test() {",
            "    try {",
            "      if (true) throw new NoSuchMethodException();",
            "      if (true) throw new NoSuchFieldException();",
            "      System.err.println();",
            "    } catch (NoSuchMethodException | NoSuchFieldException expected) {",
            "      org.junit.Assert.fail();",
            "    } finally {}",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "class Test {",
            "  public void test() throws NoSuchMethodException, NoSuchFieldException {",
            "    try {",
            "      if (true) throw new NoSuchMethodException();",
            "      if (true) throw new NoSuchFieldException();",
            "      System.err.println();",
            "    } finally {}",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positive_otherCatch() {
    testHelper
        .addInputLines(
            "in/Test.java",
            "class Test {",
            "  public void test() {",
            "    try {",
            "      System.err.println();",
            "    } catch (Exception expected) {",
            "      org.junit.Assert.fail();",
            "    } catch (Error e) {}",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "class Test {",
            "  public void test() throws Exception {",
            "    try {",
            "      System.err.println();",
            "    } catch (Error e) {}",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative_nonTest() {
    testHelper
        .addInputLines(
            "in/Foo.java",
            "import org.junit.Test;",
            "class Foo {",
            "  public void f() {",
            "    try {",
            "      System.err.println();",
            "    } catch (Exception expected) {",
            "      // BUG: Diagnostic contains:",
            "      org.junit.Assert.fail();",
            "    }",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void useException() {
    testHelper
        .addInputLines(
            "in/Foo.java",
            "import org.junit.Test;",
            "class Foo {",
            "  @Test public void f() {",
            "    try {",
            "      System.err.println();",
            "    } catch (Exception expected) {",
            "      org.junit.Assert.fail(\"oh no \" + expected);",
            "    }",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void failVariations() {
    testHelper
        .addInputLines(
            "in/Foo.java",
            "class Foo {",
            "  public void f() {",
            "    try {",
            "      System.err.println();",
            "    } catch (Exception expected) {",
            "      org.junit.Assert.fail();",
            "    }",
            "    try {",
            "      System.err.println();",
            "    } catch (Exception expected) {",
            "      org.junit.Assert.fail(\"oh no \");",
            "    }",
            "  }",
            "}")
        .addOutputLines(
            "out/Foo.java",
            "class Foo {",
            "  public void f() {",
            "    try {",
            "      System.err.println();",
            "    } catch (Exception expected) {",
            "      org.junit.Assert.fail();",
            "    }",
            "    try {",
            "      System.err.println();",
            "    } catch (Exception expected) {",
            "      throw new AssertionError(\"oh no \", expected);",
            "    }",
            "  }",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void testExpected() {
    testHelper
        .addInputLines(
            "in/Foo.java",
            "import org.junit.Test;",
            "import java.io.IOException;",
            "class Foo {",
            "  @Test(expected = IOException.class)",
            "  public void f() {",
            "    try {",
            "      throw new IOException();",
            "    } catch (IOException expected) {",
            "      org.junit.Assert.fail();",
            "    }",
            "  }",
            "}")
        .addOutputLines(
            "out/Foo.java",
            "import org.junit.Test;",
            "import java.io.IOException;",
            "class Foo {",
            "  @Test(expected = IOException.class)",
            "  public void f() {",
            "    try {",
            "      throw new IOException();",
            "    } catch (IOException expected) {",
            "      org.junit.Assert.fail();",
            "    }",
            "  }",
            "}")
        .doTest();
  }
}
