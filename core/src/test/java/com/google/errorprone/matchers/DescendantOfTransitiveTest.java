/*
 * Copyright 2014 The Error Prone Authors.
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

import static com.google.common.truth.Truth.assertThat;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Locale.ENGLISH;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.BaseErrorProneJavaCompiler;
import com.google.errorprone.scanner.Scanner;
import com.google.errorprone.scanner.ScannerSupplier;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * @author eaftan@google.com (Eddie Aftandilian)
 */
@RunWith(JUnit4.class)
public class DescendantOfTransitiveTest extends DescendantOfAbstractTest {

  @Rule public final TemporaryFolder tempDir = new TemporaryFolder();
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

  private void assertCompilesWithLocalDisk(Scanner scanner) throws IOException {
    BaseErrorProneJavaCompiler compiler =
        new BaseErrorProneJavaCompiler(ScannerSupplier.fromScanner(scanner));
    StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, ENGLISH, UTF_8);
    fileManager.setLocation(StandardLocation.CLASS_PATH, ImmutableList.of(tempDir.getRoot()));
    fileManager.setLocation(StandardLocation.CLASS_OUTPUT, ImmutableList.of(tempDir.getRoot()));
    assertThat(
            compiler
                .getTask(
                    /* out= */ null,
                    fileManager,
                    /* diagnosticListener= */ null,
                    /* options= */ ImmutableList.of(),
                    /* classes= */ null,
                    fileManager.getJavaFileObjects(filesToCompile.toArray(new String[0])))
                .call())
        .isTrue();
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

  @Test
  public void shouldMatchTransitively() throws Exception {
    writeFileToLocalDisk(
        "I1.java", //
        "package i;",
        "public interface I1 {",
        "  void count();",
        "}");
    writeFileToLocalDisk(
        "I2.java", //
        "package i;",
        "public interface I2 extends I1 {",
        "}");
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
        "C.java", //
        "public class C {",
        "  public void test(b.B b) {",
        "    b.count();",
        "  }",
        "}");
    assertCompilesWithLocalDisk(
        memberSelectMatches(/* shouldMatch= */ true, new DescendantOf("i.I1", "count()")));
  }
}
