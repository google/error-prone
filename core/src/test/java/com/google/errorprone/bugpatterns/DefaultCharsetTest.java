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
import com.google.errorprone.BugCheckerRefactoringTestHelper.FixChoosers;
import com.google.errorprone.CompilationTestHelper;
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
                + " Files.newBufferedWriter(Paths.get(s), UTF_8, CREATE, APPEND);",
            "    new FileWriter(s, true);",
            "    // BUG: Diagnostic contains:"
                + " Files.newBufferedWriter(Paths.get(s), UTF_8, CREATE, APPEND);",
            "    new FileWriter(s, CONST);",
            "    // BUG: Diagnostic contains: Files.newBufferedWriter(f.toPath(), UTF_8);",
            "    new FileWriter(f);",
            "    // BUG: Diagnostic contains:"
                + " Files.newBufferedWriter(f.toPath(), UTF_8, CREATE, APPEND);",
            "    new FileWriter(f, true);",
            "    // BUG: Diagnostic contains: Files.newBufferedWriter(f.toPath(), UTF_8);",
            "    new FileWriter(f, false);",
            "    // BUG: Diagnostic contains:"
                + " Files.newBufferedWriter(f.toPath(), UTF_8, flag"
                + " ? new StandardOpenOption[] {CREATE, APPEND}"
                + " : new StandardOpenOption[] {CREATE}",
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
  public void guavaReader() {
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
  public void guavaWriterImportAppend() {
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
            "import static java.nio.file.StandardOpenOption.CREATE;",
            "import com.google.common.io.Files;",
            "import java.io.*;",
            "import java.nio.file.Paths;",
            "class Test {",
            "  void f(String s, File f) throws Exception {",
            "    java.nio.file.Files.newBufferedWriter(Paths.get(s), UTF_8, CREATE, APPEND);",
            "    java.nio.file.Files.newBufferedWriter(f.toPath(), UTF_8, CREATE, APPEND);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void guavaWriter() {
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
  public void androidReader() {
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
        .expectUnchanged()
        .setArgs("-XDandroidCompatible=true")
        .doTest();
  }

  @Test
  public void androidWriter() {
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
        .expectUnchanged()
        .setArgs("-XDandroidCompatible=true")
        .doTest();
  }

  @Test
  public void variableFix() {
    refactoringTest()
        .addInputLines(
            "in/Test.java",
            "import java.io.*;",
            "class Test {",
            "  void f(File f) throws Exception {",
            "    FileWriter w = new FileWriter(f);",
            "    FileReader r = new FileReader(f);",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import static java.nio.charset.StandardCharsets.UTF_8;",
            "import java.io.*;",
            "import java.io.Reader;",
            "import java.io.Writer;",
            "import java.nio.file.Files;",
            "class Test {",
            "  void f(File f) throws Exception {",
            "    Writer w = Files.newBufferedWriter(f.toPath(), UTF_8);",
            "    Reader r = Files.newBufferedReader(f.toPath(), UTF_8);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void variableFixAtADistance() {
    refactoringTest()
        .addInputLines(
            "in/Test.java",
            "import java.io.*;",
            "class Test {",
            "  FileWriter w = null;",
            "  FileReader r = null;",
            "  void f(File f) throws Exception {",
            "    w = new FileWriter(f);",
            "    r = new FileReader(f);",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import static java.nio.charset.StandardCharsets.UTF_8;",
            "import java.io.*;",
            "import java.io.Reader;",
            "import java.io.Writer;",
            "import java.nio.file.Files;",
            "class Test {",
            "  Writer w = null;",
            "  Reader r = null;",
            "  void f(File f) throws Exception {",
            "    w = Files.newBufferedWriter(f.toPath(), UTF_8);",
            "    r = Files.newBufferedReader(f.toPath(), UTF_8);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void printWriter() {
    refactoringTest()
        .addInputLines(
            "in/Test.java",
            "import java.io.File;",
            "import java.io.PrintWriter;",
            "class Test {",
            "  void f() throws Exception {",
            "    PrintWriter pw1 = new PrintWriter(System.err, true);",
            "    PrintWriter pw2 = new PrintWriter(System.err);",
            "    PrintWriter pw3 = new PrintWriter(\"test\");",
            "    PrintWriter pw4 = new PrintWriter(new File(\"test\"));",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import static java.nio.charset.StandardCharsets.UTF_8;",
            "import java.io.BufferedWriter;",
            "import java.io.File;",
            "import java.io.OutputStreamWriter;",
            "import java.io.PrintWriter;",
            "class Test {",
            "  void f() throws Exception {",
            "    PrintWriter pw1 = new PrintWriter(",
            "        new BufferedWriter(new OutputStreamWriter(System.err, UTF_8)), true);",
            "    PrintWriter pw2 = new PrintWriter(",
            "        new BufferedWriter(new OutputStreamWriter(System.err, UTF_8)));",
            "    PrintWriter pw3 = new PrintWriter(\"test\", UTF_8.name());",
            "    PrintWriter pw4 = new PrintWriter(new File(\"test\"), UTF_8.name());",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void byteString() {
    refactoringTest()
        .addInputLines(
            "in/Test.java",
            "import com.google.protobuf.ByteString;",
            "class Test {",
            "  void f() throws Exception {",
            "    ByteString.copyFrom(\"hello\".getBytes());",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import com.google.protobuf.ByteString;",
            "class Test {",
            "  void f() throws Exception {",
            "    ByteString.copyFromUtf8(\"hello\");",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void byteStringDefaultCharset() {
    refactoringTest()
        .addInputLines(
            "in/Test.java",
            "import com.google.protobuf.ByteString;",
            "class Test {",
            "  void f() throws Exception {",
            "    ByteString.copyFrom(\"hello\".getBytes());",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import com.google.protobuf.ByteString;",
            "import java.nio.charset.Charset;",
            "class Test {",
            "  void f() throws Exception {",
            "    ByteString.copyFrom(\"hello\", Charset.defaultCharset());",
            "  }",
            "}")
        .setFixChooser(FixChoosers.SECOND)
        .doTest();
  }

  @Test
  public void scannerDefaultCharset() {
    refactoringTest()
        .addInputLines(
            "in/Test.java",
            "import java.util.Scanner;",
            "import java.io.File;",
            "import java.io.InputStream;",
            "import java.nio.channels.ReadableByteChannel;",
            "import java.nio.file.Path;",
            "class Test {",
            "  void f() throws Exception {",
            "    new Scanner((InputStream) null);",
            "    new Scanner((File) null);",
            "    new Scanner((Path) null);",
            "    new Scanner((ReadableByteChannel) null);",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import static java.nio.charset.StandardCharsets.UTF_8;",
            "import java.io.File;",
            "import java.io.InputStream;",
            "import java.nio.channels.ReadableByteChannel;",
            "import java.nio.file.Path;",
            "import java.util.Scanner;",
            "class Test {",
            "  void f() throws Exception {",
            "    new Scanner((InputStream) null, UTF_8.name());",
            "    new Scanner((File) null, UTF_8.name());",
            "    new Scanner((Path) null, UTF_8.name());",
            "    new Scanner((ReadableByteChannel) null, UTF_8.name());",
            "  }",
            "}")
        .doTest();
  }
}
