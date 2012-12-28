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

package com.google.errorprone;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

/**
 * Tests that automated fixes ("Did you mean...") are generated even when the -Xjcov compiler
 * option is not set.
 *
 * @author Eddie Aftandilian (eaftan@google.com)
 */
public class EndPosTest extends IntegrationTest {

  private PrintWriter printWriter;
  private ByteArrayOutputStream outputStream;
  ErrorProneCompiler compiler;

  @Before
  public void setUp() {
    outputStream = new ByteArrayOutputStream();
    printWriter = new PrintWriter(new OutputStreamWriter(outputStream));
    compiler = new ErrorProneCompiler.Builder()
        .named("test")
        .redirectOutputTo(printWriter)
        .build();
  }

  @Test
  public void fileWithError() throws Exception {
    int exitCode = compiler.compile(sources(
        "com/google/errorprone/bugpatterns/SelfAssignmentPositiveCases1.java"));
    outputStream.flush();
    assertThat("Compiler should have exited with exit code 1", exitCode, is(1));
    assertThat("Compiler error message should include suggested fix", outputStream.toString(),
        containsString("Did you mean 'this.a = b;'?"));

  }
}
