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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.errorprone.bugpatterns.BugChecker;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Utility class for tests that need to build using error-prone.
 * @author eaftan@google.com (Eddie Aftandilian)
 *
 * TODO(eaftan): Refactor default argument construction to make setup cleaner.
 */
public class CompilationTestHelper {

  private static final List<String> DEFAULT_ARGS = Arrays.asList(
    "-encoding", "UTF-8",
    "-Xjcov");

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

  /**
   * Creates a list of arguments to pass to the compiler, including the list of source files
   * to compile.  Uses DEFAULT_ARGS as the base and appends the extraArgs passed in.
   */
  private static List<String> buildArguments(List<File> sources, List<String> extraArgs) {
    List<String> allArgs = Lists.newArrayList(DEFAULT_ARGS);
    allArgs.addAll(extraArgs);
    for (File file : sources) {
      allArgs.add(file.getAbsolutePath());
    }
    return allArgs;
  }

  /**
   * Asserts that the compilation succeeds (exits with code 0).
   *
   * @param sources The list of {@code File}s to compile
   * @param extraArgs Extra command-line arguments to pass to the compiler
   */
  public void assertCompileSucceeds(List<File> sources, String... extraArgs) {
    List<String> allArgs = buildArguments(sources, Arrays.asList(extraArgs));
    int exitCode = compiler.compile(allArgs.toArray(new String[0]));
    assertThat(diagnosticHelper.getDiagnostics().toString(), exitCode, is(0));
    // TODO(eaftan): complain if there are any diagnostics
  }

  /**
   * Convenience method for the common case of one source file and no extra args.
   */
  public void assertCompileSucceeds(File source) {
    assertCompileSucceeds(ImmutableList.of(source));
  }

  /**
   * Assert that the compile succeeds, and that for each line of the test file that contains
   * the pattern //BUG("foo"), the diagnostic at that line contains "foo".
   */
  public void assertCompileSucceedsWithMessages(List<File> sources, String... extraArgs)
      throws IOException {
    assertCompileSucceeds(sources, extraArgs);
    for (File source : sources) {
      assertHasDiagnosticOnAllMatchingLines(diagnosticHelper.getDiagnostics(), source);
    }
  }

  /**
   * Convenience method for the common case of one source file and no extra args.
   */
  public void assertCompileSucceedsWithMessages(File source) throws IOException {
    assertCompileSucceedsWithMessages(ImmutableList.of(source));
  }

  /**
   * Assert that the compile fails, and that for each line of the test file that contains
   * the pattern //BUG("foo"), the diagnostic at that line contains "foo".
   *
   * @param sources The list of {@code File}s to compile
   * @param extraArgs Extra command-line arguments to pass to the compiler
   */
  public void assertCompileFailsWithMessages(List<File> sources, String... extraArgs)
      throws IOException {
    List<String> allArgs = buildArguments(sources, Arrays.asList(extraArgs));
    int exitCode = compiler.compile(allArgs.toArray(new String[0]));
    assertThat("Compiler returned an unexpected error code", exitCode, is(1));
    for (File source : sources) {
      assertHasDiagnosticOnAllMatchingLines(diagnosticHelper.getDiagnostics(), source);
    }
  }

  /**
   * Convenience method for the common case of one source file and no extra args.
   */
  public void assertCompileFailsWithMessages(File source) throws IOException {
    assertCompileFailsWithMessages(Collections.singletonList(source));
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
