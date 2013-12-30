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

import static com.google.errorprone.DiagnosticTestHelper.assertHasDiagnosticOnAllMatchingLines;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import com.google.errorprone.bugpatterns.BugChecker;
import com.google.common.collect.Lists;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

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

  public CompilationTestHelper(Class<?> matcherClass) {
    this(ErrorProneScanner.forMatcher(matcherClass));
  }

  public CompilationTestHelper(BugChecker checker) {
    this(new ErrorProneScanner(checker));
  }

  public void assertCompileSucceeds(File source, File... dependencies) {
    List<String> arguments = Lists.newArrayList("-Xjcov", source.getAbsolutePath());
    for (File file : dependencies) {
      arguments.add(file.getAbsolutePath());
    }
    int exitCode = compiler.compile(arguments.toArray(new String[0]));
    assertThat(diagnosticHelper.getDiagnostics().toString(), exitCode, is(0));
    // TODO(eaftan): complain if there are any diagnostics
  }

  /**
   * Assert that the compile fails, and that for each line of the test file that contains
   * the pattern //BUG("foo"), the diagnostic at that line contains "foo".
   */
  public void assertCompileFailsWithMessages(File source, File... dependencies) throws IOException {
    List<String> arguments =
        Lists.newArrayList("-Xjcov", "-encoding", "UTF-8", source.getAbsolutePath());
    for (File file : dependencies) {
      arguments.add(file.getAbsolutePath());
    }
    int exitCode = compiler.compile(arguments.toArray(new String[0]));
    assertThat("Compiler returned an unexpected error code", exitCode, is(1));
    assertHasDiagnosticOnAllMatchingLines(diagnosticHelper.getDiagnostics(), source);
  }

  /**
   * Assert that the compile succeeds, and that for each line of the test file that contains
   * the pattern //BUG("foo"), the diagnostic at that line contains "foo".
   */
  public void assertCompileSucceedsWithMessages(File source) throws IOException {
    assertCompileSucceeds(source);
    assertHasDiagnosticOnAllMatchingLines(diagnosticHelper.getDiagnostics(), source);
  }

  private int compileFileExitCode(File source) {
    return compiler.compile(
        new String[]{"-Xjcov", "-encoding", "UTF-8", source.getAbsolutePath()});
  }

  /**
   * Constructs the absolute paths to the given files, so they can be passed as arguments to the
   * compiler.
   */
  public static String[] sources(Class<?> klass, String... files) throws URISyntaxException {
    String[] result = new String[files.length];
    for (int i = 0; i < result.length; i++) {
      result[i] = new File(klass.getResource("/" + files[i]).toURI()).getAbsolutePath();
    }
    return result;
  }

}
