/*
 * Copyright 2026 The Error Prone Authors.
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

import com.google.common.collect.ImmutableList;
import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Options;
import java.io.Writer;
import java.util.List;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;

/// Shared helpers for configuring compiler options and creating [JavacTaskImpl] instances in Error
/// Prone tests.
///
/// Intended for internal use by Error Prone test helpers only.
public final class TestCompilations {

  private static final String COMPILING_TEST_ONLY_CODE_ARG = "-XepCompilingTestOnlyCode";
  private static final String DEPS_FOR_TEST_INPUTS_PROPERTY =
      "com.google.errorprone.deps_for_test_inputs";

  private static final ImmutableList<String> DEFAULT_TEST_ARGS =
      ImmutableList.of(
          "-encoding",
          "UTF-8",
          // print stack traces for completion failures
          "-XDdev",
          // Enables both writing and reading parameter names (ClassReader.saveParameterNames) from
          // dependency .class files, matching Blaze defaults.
          "-parameters",
          // Don't limit errors/warnings for tests to the default of 100
          "-Xmaxerrs",
          "500",
          "-Xmaxwarns",
          "500");

  /// Returns `extraArgs` prefixed with Error Prone's preferred test defaults and `-proc:none`
  /// (unless annotation processing is explicitly configured in `extraArgs`).
  public static ImmutableList<String> defaultTestArgs(List<String> extraArgs) {
    return defaultTestArgs(extraArgs, /* disableImplicitProcessing= */ true);
  }

  /// Returns `extraArgs` prefixed with Error Prone's preferred test defaults and, when
  /// `disableImplicitProcessing` is `true` and annotation processing is not explicitly configured
  /// in `extraArgs`, `-proc:none`.
  // TODO(cushon): test compilations should be isolated so they can't pick things up from the
  // ambient classpath.
  public static ImmutableList<String> defaultTestArgs(
      List<String> extraArgs, boolean disableImplicitProcessing) {
    // Prepend `-proc:none` and DEFAULT_TEST_ARGS before extraArgs so test-provided flags override
    // the defaults (last-wins) and trailing single-argument flags (e.g., `-cp`) cannot consume
    // them.
    ImmutableList.Builder<String> result = ImmutableList.builder();
    if (disableImplicitProcessing && !hasExplicitAnnotationProcessingOption(extraArgs)) {
      result.add("-proc:none");
    }
    return result.addAll(DEFAULT_TEST_ARGS).addAll(extraArgs).build();
  }

  /// Augments `args` with `-XepCompilingTestOnlyCode` (when `testOnly` is `true`) and the
  /// `com.google.errorprone.deps_for_test_inputs` classpath (when set).
  public static ImmutableList<String> withTestOnlyAndDeps(List<String> args, boolean testOnly) {
    ImmutableList.Builder<String> result = ImmutableList.builder();
    if (testOnly && !args.contains(COMPILING_TEST_ONLY_CODE_ARG)) {
      result.add(COMPILING_TEST_ONLY_CODE_ARG);
    }
    result.addAll(args);
    String depsForTestInputs = System.getProperty(DEPS_FOR_TEST_INPUTS_PROPERTY);
    if (depsForTestInputs != null) {
      // Intentionally appended after args so the system property overrides any prior `-cp` /
      // `withClasspath`.
      result.add("-cp").add(depsForTestInputs);
    }
    return result.build();
  }

  /// Creates a [JavacTaskImpl] using [FileManagers#testFileManager()] with Error Prone's required
  /// production javac options and preferred test defaults layered before `extraJavacArgs`.
  public static JavacTaskImpl getTask(
      Writer out,
      DiagnosticListener<? super JavaFileObject> diagnosticListener,
      List<String> extraJavacArgs,
      Iterable<? extends JavaFileObject> compilationUnits) {
    return getTask(out, diagnosticListener, extraJavacArgs, compilationUnits, new Context());
  }

  /// Creates a [JavacTaskImpl] using [FileManagers#testFileManager()] and the given `context`, with
  /// Error Prone's required production javac options and preferred test defaults layered before
  /// `extraJavacArgs`.
  public static JavacTaskImpl getTask(
      Writer out,
      DiagnosticListener<? super JavaFileObject> diagnosticListener,
      List<String> extraJavacArgs,
      Iterable<? extends JavaFileObject> compilationUnits,
      Context context) {
    ImmutableList<String> javacOpts =
        BaseErrorProneJavaCompiler.applyRequiredJavacOptions(defaultTestArgs(extraJavacArgs));
    JavacTaskImpl task =
        (JavacTaskImpl)
            JavacTool.create()
                .getTask(
                    out,
                    FileManagers.testFileManager(),
                    diagnosticListener,
                    javacOpts,
                    /* classes= */ null,
                    compilationUnits,
                    context);
    BaseErrorProneJavaCompiler.checkRequiredJavacOptions(Options.instance(task.getContext()));
    return task;
  }

  private static boolean hasExplicitAnnotationProcessingOption(List<String> args) {
    return args.stream().anyMatch(TestCompilations::isExplicitAnnotationProcessingOption);
  }

  private static boolean isExplicitAnnotationProcessingOption(String arg) {
    // `-A...` is intentionally omitted: in javac, Option.A stores each `-Akey=value` under its
    // full text rather than under `Option.A`, so `-A` alone does not enable annotation processing
    // on JDK 23+.
    return arg.equals("-processor")
        || arg.equals("-processorpath")
        || arg.equals("--processor-path")
        || arg.startsWith("--processor-path=")
        || arg.equals("--processor-module-path")
        || arg.startsWith("--processor-module-path=")
        || arg.startsWith("-proc:")
        || arg.equals("-Xprint");
  }

  private TestCompilations() {}
}
