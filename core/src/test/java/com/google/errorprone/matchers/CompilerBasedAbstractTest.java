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

import static com.google.errorprone.CompilationTestHelper.forSourceLines;

import com.google.common.io.Files;
import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.Scanner;

import org.junit.After;

import java.util.ArrayList;
import java.util.List;

import javax.tools.JavaFileObject;

/**
 * @author alexeagle@google.com (Alex Eagle)
 */
public class CompilerBasedAbstractTest {
  List<JavaFileObject> filesToCompile = new ArrayList<JavaFileObject>();

  @After
  public void clearSourceFiles() throws Exception {
    filesToCompile.clear();
  }
  protected void writeFile(String fileName, String... lines) {
    filesToCompile.add(forSourceLines(Files.getNameWithoutExtension(fileName),
        lines));
  }

  protected void assertCompiles(Scanner scanner) {
    new CompilationTestHelper(scanner).assertCompileSucceeds(filesToCompile);
  }
}
