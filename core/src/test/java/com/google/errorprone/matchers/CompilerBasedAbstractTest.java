/*
 * Copyright 2012 Google Inc. All Rights Reserved.
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

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.scanner.Scanner;
import com.google.errorprone.scanner.ScannerSupplier;

import org.junit.After;

import java.util.ArrayList;
import java.util.List;

import javax.tools.JavaFileObject;

/**
 * @author alexeagle@google.com (Alex Eagle)
 */
public class CompilerBasedAbstractTest {

  private static class FileToCompile {
    final String name;
    final String[] lines;

    FileToCompile(String name, String... lines) {
      this.name = name;
      this.lines = lines;
    }
  }

  List<FileToCompile> filesToCompile = new ArrayList<>();

  @After
  public void clearSourceFiles() throws Exception {
    filesToCompile.clear();
  }

  protected void writeFile(String fileName, String... lines) {
    filesToCompile.add(new FileToCompile(fileName, lines));
  }

  private void assertCompiles(ScannerSupplier scannerSupplier) {
    final CompilationTestHelper compilationHelper =
        CompilationTestHelper.newInstance(scannerSupplier);
    List<JavaFileObject> fileObjects =
        Lists.transform(filesToCompile, new Function<FileToCompile, JavaFileObject>() {
          @Override
          public JavaFileObject apply(FileToCompile file) {
            return compilationHelper.fileManager().forSourceLines(file.name, file.lines);
          }
        });
    compilationHelper.assertCompileSucceeds(fileObjects);
  }

  protected void assertCompiles(final Scanner scanner) {
    assertCompiles(ScannerSupplier.fromScanner(scanner));
  }
}
