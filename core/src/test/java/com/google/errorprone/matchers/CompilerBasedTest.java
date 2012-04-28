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

import com.google.errorprone.ErrorProneCompiler;
import com.google.errorprone.Scanner;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import static com.google.common.io.Files.deleteRecursively;
import static org.hamcrest.CoreMatchers.is;

/**
 * @author alexeagle@google.com (Alex Eagle)
 */
public class CompilerBasedTest {
  @Rule public TestName name = new TestName();
  protected File tempDir;

  @Before
  public void setUp() throws IOException {
    tempDir = new File(System.getProperty("java.io.tmpdir"),
        getClass().getCanonicalName() + "." + name.getMethodName());
    tempDir.mkdirs();
  }

  @After
  public void tearDown() throws Exception {
    deleteRecursively(tempDir.getCanonicalFile());
  }

  protected void writeFile(String fileName, String... lines) throws IOException {
    File source = new File(tempDir, fileName);
    PrintWriter writer = new PrintWriter(new FileWriter(source));
    for (String line : lines) {
      writer.println(line);
    }
    writer.close();
  }

  protected void assertCompiles(Scanner scanner) throws IOException {
    ErrorProneCompiler compiler = new ErrorProneCompiler.Builder()
        .refactor(scanner)
        .build();

    File[] files = tempDir.listFiles();
    String[] args = new String[files.length];
    for (int i = 0; i < args.length; i++) {
      args[i] = files[i].getAbsolutePath();
    }
    Assert.assertThat(compiler.compile(args), is(0));
  }
}
