/*
 * Copyright 2017 The Error Prone Authors.
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
import com.google.errorprone.CompilationTestHelper;
import java.io.IOException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link StringSplitter}Test */
@RunWith(JUnit4.class)
public class StringSplitterTest {
  private final BugCheckerRefactoringTestHelper testHelper =
      BugCheckerRefactoringTestHelper.newInstance(new StringSplitter(), getClass());

  @Test
  public void positive() throws IOException {
    testHelper
        .addInputLines(
            "in/Test.java",
            "class Test {",
            "  void f() {",
            "    for (String s : \"\".split(\":\")) {}",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import com.google.common.base.Splitter;",
            "class Test {",
            "  void f() {",
            "    for (String s : Splitter.on(':').split(\"\")) {}",
            "  }",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void varLoop() throws IOException {
    testHelper
        .addInputLines(
            "in/Test.java",
            "class Test {",
            "  void f() {",
            "    String[] pieces = \"\".split(\":\");",
            "    for (String s : pieces) {}",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import com.google.common.base.Splitter;",
            "class Test {",
            "  void f() {",
            "    Iterable<String> pieces = Splitter.on(':').split(\"\");",
            "    for (String s : pieces) {}",
            "  }",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void varLoopLength() throws IOException {
    testHelper
        .addInputLines(
            "in/Test.java",
            "class Test {",
            "  void f() {",
            "    String[] pieces = \"\".split(\":\");",
            "    for (int i = 0; i < pieces.length; i++) {}",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import com.google.common.base.Splitter;",
            "import java.util.List;",
            "class Test {",
            "  void f() {",
            "    List<String> pieces = Splitter.on(':').splitToList(\"\");",
            "    for (int i = 0; i < pieces.size(); i++) {}",
            "  }",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void varList() throws IOException {
    testHelper
        .addInputLines(
            "in/Test.java",
            "class Test {",
            "  void f() {",
            "    String[] pieces = \"\".split(\":\");",
            "    System.err.println(pieces[0]);",
            "    System.err.println(pieces[1]);",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import com.google.common.base.Splitter;",
            "import java.util.List;",
            "class Test {",
            "  void f() {",
            "    List<String> pieces = Splitter.on(':').splitToList(\"\");",
            "    System.err.println(pieces.get(0));",
            "    System.err.println(pieces.get(1));",
            "  }",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void positiveRegex() throws IOException {
    testHelper
        .addInputLines(
            "in/Test.java",
            "class Test {",
            "  void f() {",
            "    for (String s : \"\".split(\".*foo\\\\t\")) {}",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import com.google.common.base.Splitter;",
            "class Test {",
            "  void f() {",
            "    for (String s : Splitter.onPattern(\".*foo\\\\t\").split(\"\")) {}",
            "  }",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void character() throws IOException {
    testHelper
        .addInputLines(
            "in/Test.java",
            "class Test {",
            "  void f() {",
            "    for (String s : \"\".split(\"c\")) {}",
            "    for (String s : \"\".split(\"abc\")) {}",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import com.google.common.base.Splitter;",
            "class Test {",
            "  void f() {",
            "    for (String s : Splitter.on('c').split(\"\")) {}",
            "    for (String s : Splitter.on(\"abc\").split(\"\")) {}",
            "  }",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void negative() throws IOException {
    CompilationTestHelper.newInstance(StringSplitter.class, getClass())
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void f() {",
            "    String[] pieces = \"\".split(\":\");",
            "    System.err.println(pieces);", // escapes
            "  }",
            "}")
        .doTest();
  }
}
