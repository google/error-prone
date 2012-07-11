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

package com.google.errorprone.bugpatterns.returnvalueignored;

import com.google.errorprone.DiagnosticTestHelper;
import com.google.errorprone.ErrorProneCompiler;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static com.google.errorprone.DiagnosticTestHelper.hasDiagnosticOnAllMatchingLines;
import static java.util.regex.Pattern.compile;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author alexeagle@google.com (Alex Eagle)
 */
public class ReturnValueIgnoredTest {

  private ErrorProneCompiler compiler;
  private DiagnosticTestHelper diagnosticHelper;

  @Before
  public void setUp() {
    diagnosticHelper = new DiagnosticTestHelper();
    compiler = new ErrorProneCompiler.Builder()
        .report(new ReturnValueIgnored.Scanner())
        .listenToDiagnostics(diagnosticHelper.collector)
        .build();
  }

  @Test
  public void testPositiveCase() throws Exception {
    File source = new File(this.getClass().getResource("PositiveCases.java").toURI());
    assertThat(compiler.compile(new String[]{"-Xjcov", source.getAbsolutePath()}), is(1));
    assertThat(diagnosticHelper.getDiagnostics(),
        hasDiagnosticOnAllMatchingLines(source, compile(".*//BUG\\s*$")));
  }

  @Test
  public void testNegativeCase() throws Exception {
    File source = new File(this.getClass().getResource("NegativeCases.java").toURI());
    assertThat(compiler.compile(new String[]{source.getAbsolutePath()}), is(0));
  }

}

