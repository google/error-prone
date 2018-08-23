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

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.BugCheckerRefactoringTestHelper.TestMode;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link StreamResourceLeakTest}Test */
@RunWith(JUnit4.class)
public class StreamResourceLeakTest {

  private final CompilationTestHelper testHelper =
      CompilationTestHelper.newInstance(StreamResourceLeak.class, getClass());

  @Test
  public void positive() {
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
            "    // BUG: Diagnostic contains: should be closed",
            "    Files.newDirectoryStream(p);",
            "    // BUG: Diagnostic contains: should be closed",
            "    Files.newDirectoryStream(p, /* glob= */ \"*\");",
            "    // BUG: Diagnostic contains: should be closed",
            "    Files.newDirectoryStream(p, /* filter= */ path -> true);",
            "    // BUG: Diagnostic contains: should be closed",
            "    Files.list(p);",
            "    // BUG: Diagnostic contains: should be closed",
            "    Files.walk(p, /* maxDepth= */ 0);",
            "    // BUG: Diagnostic contains: should be closed",
            "    Files.walk(p);",
            "    // BUG: Diagnostic contains: should be closed",
            "    Files.find(p, /* maxDepth= */ 0, (path, a) -> true);",
            "    // BUG: Diagnostic contains: should be closed",
            "    try (Stream<String> stream ="
                + " Files.lines(p).collect(Collectors.toList()).stream()) {",
            "      stream.collect(Collectors.joining(\", \"));",
            "    }",
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
            "    try (Stream<String> stream = Files.lines(p).filter(l -> !l.isEmpty())) {",
            "      stream.collect(Collectors.joining(\", \"));",
            "    }",
            "    try (Stream<String> stream = Files.lines(p)) {",
            "      return stream.collect(Collectors.joining(\", \"));",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void fix() {
    BugCheckerRefactoringTestHelper.newInstance(new StreamResourceLeak(), getClass())
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
  public void fixVariable() {
    BugCheckerRefactoringTestHelper.newInstance(new StreamResourceLeak(), getClass())
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
            "    String r;",
            "    // BUG: Diagnostic contains:",
            "    try (Stream<String> stream = Files.lines(p).count() > 0 ? null : null) {",
            "      r = stream.collect(Collectors.joining(\", \"));",
            "    }",
            "    try (Stream<String> stream = true ? null : Files.lines(p)) {",
            "      r = stream.collect(Collectors.joining(\", \"));",
            "    }",
            "    try (Stream<String> stream = true ? Files.lines(p) : null) {",
            "      r = stream.collect(Collectors.joining(\", \"));",
            "    }",
            "    return r;",
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

  @Test
  public void returnFromMustBeClosedMethodWithChaining() {
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
            "    return Files.list(p).map(Path::toString); // OK due to @MustBeClosed",
            "  }",
            "  Stream<String> g(Path p) throws IOException {",
            "    // BUG: Diagnostic contains: should be closed",
            "    return Files.list(p).map(Path::toString);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void moreRefactorings() {
    BugCheckerRefactoringTestHelper.newInstance(new StreamResourceLeak(), getClass())
        .addInputLines(
            "in/Test.java",
            "import java.io.IOException;",
            "import java.nio.file.DirectoryStream;",
            "import java.nio.file.Files;",
            "import java.nio.file.Path;",
            "class Test {",
            "  void f(Path p) throws IOException {",
            "    DirectoryStream<Path> l = Files.newDirectoryStream(p);",
            "    for (Path x : Files.newDirectoryStream(p)) {",
            "      System.err.println(x);",
            "    }",
            "    System.err.println(l);",
            "    System.err.println(Files.newDirectoryStream(p));",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import java.io.IOException;",
            "import java.nio.file.DirectoryStream;",
            "import java.nio.file.Files;",
            "import java.nio.file.Path;",
            "class Test {",
            "  void f(Path p) throws IOException {",
            "    try (DirectoryStream<Path> l = Files.newDirectoryStream(p)) {",
            "      try (DirectoryStream<Path> stream = Files.newDirectoryStream(p)) {",
            "        for (Path x : stream) {",
            "          System.err.println(x);",
            "        }",
            "      }",
            "      System.err.println(l);",
            "    }",
            "    try (DirectoryStream<Path> stream = Files.newDirectoryStream(p)) {",
            "      System.err.println(stream);",
            "    }",
            "  }",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }
}
