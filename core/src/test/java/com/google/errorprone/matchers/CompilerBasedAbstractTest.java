/*
 * Copyright 2012 The Error Prone Authors.
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

import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.scanner.Scanner;
import com.google.errorprone.scanner.ScannerSupplier;
import com.sun.tools.javac.main.Main.Result;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;

/** @author alexeagle@google.com (Alex Eagle) */
public class CompilerBasedAbstractTest {

  private static class FileToCompile {
    final String name;
    final String[] lines;

    FileToCompile(String name, String... lines) {
      this.name = name;
      this.lines = lines;
    }
  }

  final List<FileToCompile> filesToCompile = new ArrayList<>();

  @After
  public void clearSourceFiles() {
    filesToCompile.clear();
  }

  protected void writeFile(String fileName, String... lines) {
    filesToCompile.add(new FileToCompile(fileName, lines));
  }

  private void assertCompiles(ScannerSupplier scannerSupplier) {
    CompilationTestHelper compilationHelper =
        CompilationTestHelper.newInstance(scannerSupplier, getClass()).expectResult(Result.OK);
    for (FileToCompile fileToCompile : filesToCompile) {
      compilationHelper.addSourceLines(fileToCompile.name, fileToCompile.lines);
    }
    compilationHelper.doTest();
  }

  protected void assertCompiles(final Scanner scanner) {
    assertCompiles(ScannerSupplier.fromScanner(scanner));
  }
}
