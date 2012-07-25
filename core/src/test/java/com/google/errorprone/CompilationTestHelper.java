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

import static com.google.errorprone.DiagnosticTestHelper.hasDiagnosticOnAllMatchingLines1;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;

/**
 * Utility class for tests that need to build using error-prone.
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public class CompilationTestHelper {

  private DiagnosticTestHelper diagnosticHelper;
  private ErrorProneCompiler compiler;

  public CompilationTestHelper(Scanner scanner) {
    diagnosticHelper = new DiagnosticTestHelper();
    compiler = new ErrorProneCompiler.Builder()
        .report(scanner)
        .listenToDiagnostics(diagnosticHelper.collector)
        .build();
  }

  public void assertCompileSucceeds(File source) {
    assertThat(compiler.compile(new String[]{"-Xjcov", source.getAbsolutePath()}), is(0));
  }

  /**
   * TODO(eaftan): javadoc
   */
  public void assertCompileFails(File source) throws IOException {
    assertThat(compiler.compile(new String[]{"-Xjcov", source.getAbsolutePath()}), is(1));
    assertThat(diagnosticHelper.getDiagnostics(),
        hasDiagnosticOnAllMatchingLines1(source));
  }

}
