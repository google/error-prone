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

import static com.google.errorprone.BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link DefaultCharset}Test */
@RunWith(JUnit4.class)
public class DefaultCharsetTest {

  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper = CompilationTestHelper.newInstance(DefaultCharset.class, getClass());
  }

  private BugCheckerRefactoringTestHelper refactoringTest() {
    return BugCheckerRefactoringTestHelper.newInstance(new DefaultCharset(), getClass());
  }

  @Test
  public void bothFixes() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.io.*;",
            "class Test {",
            "  byte[] f(String s) {",
            "    // BUG: Diagnostic contains: 'return s.getBytes(UTF_8);'"
                + " or 'return s.getBytes(Charset.defaultCharset());'",
            "    return s.getBytes();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positive() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.io.*;",
            "class Test {",
            "  void f(String s, byte[] b, OutputStream out, InputStream in) throws Exception {",
            "    // BUG: Diagnostic contains: s.getBytes(UTF_8);",
            "    s.getBytes();",
            "    // BUG: Diagnostic contains: new String(b, UTF_8);",
            "    new String(b);",
            "    // BUG: Diagnostic contains: new String(b, 0, 0, UTF_8);",
            "    new String(b, 0, 0);",
            "    // BUG: Diagnostic contains: new OutputStreamWriter(out, UTF_8);",
            "    new OutputStreamWriter(out);",
            "    // BUG: Diagnostic contains: new InputStreamReader(in, UTF_8);",
            "    new InputStreamReader(in);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void reader() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.io.*;",
            "class Test {",
            "  void f(String s, File f) throws Exception {",
            "    // BUG: Diagnostic contains: Files.newBufferedReader(Paths.get(s), UTF_8);",
            "    new FileReader(s);",
            "    // BUG: Diagnostic contains: Files.newBufferedReader(f.toPath(), UTF_8);",
            "    new FileReader(f);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void writer() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.io.*;",
            "class Test {",
            "  static final boolean CONST = true;",
            "  void f(File f, String s, boolean flag) throws Exception {",
            "    // BUG: Diagnostic contains: Files.newBufferedWriter(Paths.get(s), UTF_8);",
            "    new FileWriter(s);",
            "    // BUG: Diagnostic contains:"
                + " Files.newBufferedWriter(Paths.get(s), UTF_8, APPEND);",
            "    new FileWriter(s, true);",
            "    // BUG: Diagnostic contains:"
                + " Files.newBufferedWriter(Paths.get(s), UTF_8, APPEND);",
            "    new FileWriter(s, CONST);",
            "    // BUG: Diagnostic contains: Files.newBufferedWriter(f.toPath(), UTF_8);",
            "    new FileWriter(f);",
            "    // BUG: Diagnostic contains: Files.newBufferedWriter(f.toPath(), UTF_8, APPEND);",
            "    new FileWriter(f, true);",
            "    // BUG: Diagnostic contains: Files.newBufferedWriter(f.toPath(), UTF_8);",
            "    new FileWriter(f, false);",
            "    // BUG: Diagnostic contains:"
                + " Files.newBufferedWriter(f.toPath(), UTF_8, flag ? APPEND : CREATE);",
            "    new FileWriter(f, flag);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void buffered() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.io.*;",
            "class Test {",
            "  void f(String s) throws Exception {",
            "    // BUG: Diagnostic contains: "
                + "try (BufferedReader reader = Files.newBufferedReader(Paths.get(s), UTF_8)) {}'",
            "    try (BufferedReader reader = new BufferedReader(new FileReader(s))) {}",
            "    // BUG: Diagnostic contains: "
                + "try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(s), UTF_8)) {}",
            "    try (BufferedWriter writer = new BufferedWriter(new FileWriter(s))) {}",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import static java.nio.charset.StandardCharsets.UTF_8;",
            "import java.io.*;",
            "class Test {",
            "  void f(String s, byte[] b, OutputStream out, InputStream in, File f)",
            "      throws Exception {",
            "    s.getBytes(UTF_8);",
            "    new String(b, UTF_8);",
            "    new String(b, 0, 0, UTF_8);",
            "    new OutputStreamWriter(out, UTF_8);",
            "    new InputStreamReader(in, UTF_8);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void ignoreFileDescriptor() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.io.*;",
            "class Test {",
            "  void f(FileDescriptor fd) throws Exception {",
            "    try (BufferedReader reader = new BufferedReader(new FileReader(fd))) {}",
            "    try (BufferedWriter writer = new BufferedWriter(new FileWriter(fd))) {}",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void guavaReader() throws IOException {
    refactoringTest()
        .addInputLines(
            "in/Test.java",
            "import java.io.*;",
            "import com.google.common.io.Files;",
            "class Test {",
            "  void f(String s, File f) throws Exception {",
            "    new FileReader(s);",
            "    new FileReader(f);",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import static java.nio.charset.StandardCharsets.UTF_8;",
            "import com.google.common.io.Files;",
            "import java.io.*;",
            "import java.io.File;",
            "class Test {",
            "  void f(String s, File f) throws Exception {",
            "    Files.newReader(new File(s), UTF_8);",
            "    Files.newReader(f, UTF_8);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void guavaWriterImportAppend() throws IOException {
    refactoringTest()
        .addInputLines(
            "in/Test.java",
            "import java.io.*;",
            "import com.google.common.io.Files;",
            "class Test {",
            "  void f(String s, File f) throws Exception {",
            "    new FileWriter(s, true);",
            "    new FileWriter(f, true);",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import static java.nio.charset.StandardCharsets.UTF_8;",
            "import static java.nio.file.StandardOpenOption.APPEND;",
            "import com.google.common.io.Files;",
            "import java.io.*;",
            "import java.nio.file.Paths;",
            "class Test {",
            "  void f(String s, File f) throws Exception {",
            "    java.nio.file.Files.newBufferedWriter(Paths.get(s), UTF_8, APPEND);",
            "    java.nio.file.Files.newBufferedWriter(f.toPath(), UTF_8, APPEND);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void guavaWriter() throws IOException {
    refactoringTest()
        .addInputLines(
            "in/Test.java",
            "import java.io.*;",
            "import com.google.common.io.Files;",
            "class Test {",
            "  void f(String s, File f) throws Exception {",
            "    new FileWriter(s);",
            "    new FileWriter(f);",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import static java.nio.charset.StandardCharsets.UTF_8;",
            "import com.google.common.io.Files;",
            "import java.io.*;",
            "import java.io.File;",
            "class Test {",
            "  void f(String s, File f) throws Exception {",
            "    Files.newWriter(new File(s), UTF_8);",
            "    Files.newWriter(f, UTF_8);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void androidReader() throws IOException {
    refactoringTest()
        .addInputLines(
            "in/Test.java",
            "import java.io.*;",
            "class Test {",
            "  void f(String s, File f) throws Exception {",
            "    new FileReader(s);",
            "    new FileReader(f);",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import static com.google.common.base.Charsets.UTF_8;",
            "import com.google.common.io.Files;",
            "import java.io.*;",
            "import java.io.File;",
            "class Test {",
            "  void f(String s, File f) throws Exception {",
            "    Files.newReader(new File(s), UTF_8);",
            "    Files.newReader(f, UTF_8);",
            "  }",
            "}")
        .setArgs("-XDandroidCompatible=true")
        .doTest();
  }

  // this is unfixable without nio.file
  @Test
  public void androidWriterImportAppend() throws IOException {
    refactoringTest()
        .addInputLines(
            "in/Test.java",
            "import java.io.*;",
            "class Test {",
            "  void f(String s, File f) throws Exception {",
            "    new FileWriter(s, true);",
            "    new FileWriter(f, true);",
            "  }",
            "}")
        .expectUnchanged()
        .setArgs("-XDandroidCompatible=true")
        .doTest(TEXT_MATCH);
  }

  @Test
  public void androidWriter() throws IOException {
    refactoringTest()
        .addInputLines(
            "in/Test.java",
            "import java.io.*;",
            "class Test {",
            "  void f(String s, File f) throws Exception {",
            "    new FileWriter(s);",
            "    new FileWriter(f);",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import static com.google.common.base.Charsets.UTF_8;",
            "import com.google.common.io.Files;",
            "import java.io.*;",
            "import java.io.File;",
            "class Test {",
            "  void f(String s, File f) throws Exception {",
            "    Files.newWriter(new File(s), UTF_8);",
            "    Files.newWriter(f, UTF_8);",
            "  }",
            "}")
        .setArgs("-XDandroidCompatible=true")
        .doTest();
  }
}
