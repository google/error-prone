/*
 * Copyright 2014 Google Inc. All Rights Reserved.
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

package com.google.errorprone.matchers;

import static org.hamcrest.CoreMatchers.is;

import com.google.errorprone.BaseErrorProneCompiler;
import com.google.errorprone.scanner.Scanner;
import com.google.errorprone.scanner.ScannerSupplier;
import com.sun.tools.javac.main.Main.Result;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * NOTE(cushon): This test does two rounds of compilation and relies on the first round producing
 * classes that appear on the second round's class path. I'm not aware of any good ways to do this
 * without creating actual files on disk.
 *
 * @author eaftan@google.com (Eddie Aftandilian)
 */
@RunWith(JUnit4.class)
public class DescendantOfTransitiveTest extends DescendantOfAbstractTest {

  @Rule public final TemporaryFolder tempDir = new TemporaryFolder();
  final List<ScannerTest> tests = new ArrayList<ScannerTest>();
  final List<String> filesToCompile = new ArrayList<String>();

  private void writeFileToLocalDisk(String fileName, String... lines) throws IOException {
    File source = new File(tempDir.getRoot(), fileName);
    new File(source.getParent()).mkdirs();
    PrintWriter writer = new PrintWriter(new FileWriter(source));
    for (String line : lines) {
      writer.println(line);
    }
    writer.close();
    filesToCompile.add(source.getAbsolutePath());
  }

  private void assertCompilesWithLocalDisk(Scanner scanner) {
    List<String> args = new ArrayList<String>();
    args.add("-cp");
    args.add(tempDir.getRoot().getAbsolutePath());
    args.add("-d");
    args.add(tempDir.getRoot().getAbsolutePath());
    args.addAll(filesToCompile);

    BaseErrorProneCompiler compiler =
        BaseErrorProneCompiler.builder().report(ScannerSupplier.fromScanner(scanner)).build();
    Assert.assertThat(compiler.run(args.toArray(new String[0])), is(Result.OK));
  }

  @Override
  @Before
  public void setUp() throws IOException {
    writeFileToLocalDisk(
        "A.java",
        "package com.google;",
        "public class A { ",
        "  public int count() {",
        "    return 1;",
        "  }",
        "  public static int staticCount() {",
        "    return 2;",
        "  }",
        "}");
  }

  @Override
  @After
  public void tearDown() {
    for (ScannerTest test : tests) {
      test.assertDone();
    }
  }

  @Test
  public void shouldMatchTransitively() throws Exception {
    writeFileToLocalDisk("I1.java", "package i;", "public interface I1 {", "  void count();", "}");
    writeFileToLocalDisk("I2.java", "package i;", "public interface I2 extends I1 {", "}");
    writeFileToLocalDisk(
        "B.java",
        "package b;",
        "public class B implements i.I2 {",
        "  public void count() {",
        "  }",
        "}");
    assertCompilesWithLocalDisk(new Scanner());
    clearSourceFiles();
    writeFileToLocalDisk(
        "C.java", "public class C {", "  public void test(b.B b) {", "    b.count();", "  }", "}");
    assertCompilesWithLocalDisk(
        memberSelectMatches(/* shouldMatch= */ true, new DescendantOf("i.I1", "count()")));
  }
}
