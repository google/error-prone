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

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import java.io.IOException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link FilesLinesLeak}Test */
@RunWith(JUnit4.class)
public class FilesLinesLeakTest {

  private final CompilationTestHelper testHelper =
      CompilationTestHelper.newInstance(FilesLinesLeak.class, getClass());

  @Test
  public void positive() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import java.io.IOException;",
            "import java.nio.file.Files;",
            "import java.nio.file.Path;",
            "import java.util.stream.Collectors;",
            "class Test {",
            "  String f(Path p) throws IOException {",
            "    // BUG: Diagnostic contains: should be closed",
            "    return Files.lines(p).collect(Collectors.joining(\", \"));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import java.io.IOException;",
            "import java.nio.file.Files;",
            "import java.nio.file.Path;",
            "import java.util.stream.Collectors;",
            "import java.util.stream.Stream;",
            "class Test {",
            "  String f(Path p) throws IOException {",
            "    try (Stream<String> stream = Files.lines(p)) {",
            "      return stream.collect(Collectors.joining(\", \"));",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void fix() throws IOException {
    BugCheckerRefactoringTestHelper.newInstance(new FilesLinesLeak(), getClass())
        .addInputLines(
            "in/Test.java",
            "import java.io.IOException;",
            "import java.nio.file.Files;",
            "import java.nio.file.Path;",
            "import java.util.stream.Collectors;",
            "class Test {",
            "  String f(Path p) throws IOException {",
            "    return Files.lines(p).collect(Collectors.joining(\", \"));",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import java.io.IOException;",
            "import java.nio.file.Files;",
            "import java.nio.file.Path;",
            "import java.util.stream.Collectors;",
            "import java.util.stream.Stream;",
            "class Test {",
            "  String f(Path p) throws IOException {",
            "    try (Stream<String> stream = Files.lines(p)) {",
            "      return stream.collect(Collectors.joining(\", \"));",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void fixVariable() throws IOException {
    BugCheckerRefactoringTestHelper.newInstance(new FilesLinesLeak(), getClass())
        .addInputLines(
            "in/Test.java",
            "import java.io.IOException;",
            "import java.nio.file.Files;",
            "import java.nio.file.Path;",
            "import java.util.stream.Collectors;",
            "class Test {",
            "  void f(Path p) throws IOException {",
            "    String s = Files.lines(p).collect(Collectors.joining(\", \"));",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import java.io.IOException;",
            "import java.nio.file.Files;",
            "import java.nio.file.Path;",
            "import java.util.stream.Collectors;",
            "import java.util.stream.Stream;",
            "class Test {",
            "  void f(Path p) throws IOException {",
            "    String s;",
            "    try (Stream<String> stream = Files.lines(p)) {",
            "      s = stream.collect(Collectors.joining(\", \"));",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void ternary() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import java.io.IOException;",
            "import java.nio.file.Files;",
            "import java.nio.file.Path;",
            "import java.util.stream.Collectors;",
            "import java.util.stream.Stream;",
            "class Test {",
            "  String f(Path p) throws IOException {",
            "    try (Stream<String> stream = true ? Files.lines(p) : null) {",
            "      return stream.collect(Collectors.joining(\", \"));",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void returnFromMustBeClosedMethod() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.MustBeClosed;",
            "import java.io.IOException;",
            "import java.nio.file.Files;",
            "import java.nio.file.Path;",
            "import java.util.stream.Stream;",
            "class Test {",
            "  @MustBeClosed",
            "  Stream<String> f(Path p) throws IOException {",
            "    return Files.lines(p);",
            "  }",
            "}")
        .doTest();
  }
}
