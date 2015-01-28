/*
 * Copyright 2011 Google Inc. All Rights Reserved.
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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.StandardSystemProperty.JAVA_SPECIFICATION_VERSION;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.scanner.BuiltInCheckerSuppliers;
import com.google.errorprone.scanner.Scanner;
import com.google.errorprone.scanner.ScannerSupplier;

import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.api.MultiTaskListener;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.main.Main;
import com.sun.tools.javac.main.Main.Result;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.JavacMessages;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.annotation.processing.Processor;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;

/**
 * An error-prone compiler that matches the interface of {@link com.sun.tools.javac.main.Main}.
 * Used by plexus-java-compiler-errorprone.
 *
 * @author alexeagle@google.com (Alex Eagle)
 */
public class ErrorProneCompiler {

  /**
   * Entry point for compiling Java code with error-prone enabled.
   * All default checks are run, and the compile fails if they find a bug.
   *
   * @param args the same args which could be passed to javac on the command line
   */
  public static void main(String[] args) {
    System.exit(compile(args).exitCode);
  }

  /**
   * Compiles in-process.
   *
   * @param listener listens to the diagnostics produced by error-prone
   * @param args the same args which would be passed to javac on the command line
   * @return result from the compiler invocation
   */
  public static Result compile(DiagnosticListener<JavaFileObject> listener, String[] args) {
    ErrorProneCompiler compiler = new ErrorProneCompiler.Builder()
        .listenToDiagnostics(listener)
        .build();
    return compiler.run(args);
  }

  /**
   * Programmatic interface to the error-prone Java compiler.
   *
   * @param args the same args which would be passed to javac on the command line
   * @return result from the compiler invocation
   */
  public static Result compile(String[] args) {
    return new Builder().build().run(args);
  }

  /**
   * Programmatic interface to the error-prone Java compiler.
   *
   * @param args the same args which would be passed to javac on the command line
   * @param out a {@link PrintWriter} to which to send diagnostic output
   * @return result from the compiler invocation
   */
  public static Result compile(String[] args, PrintWriter out) {
    ErrorProneCompiler compiler = new ErrorProneCompiler.Builder()
        .redirectOutputTo(out)
        .build();
    return compiler.run(args);
  }

  private final DiagnosticListener<? super JavaFileObject> diagnosticListener;
  private final PrintWriter errOutput;
  private final String compilerName;
  private final ScannerSupplier scannerSupplier;

  private ErrorProneCompiler(
      String compilerName,
      PrintWriter errOutput,
      DiagnosticListener<? super JavaFileObject> diagnosticListener,
      ScannerSupplier scannerSupplier) {
    this.errOutput = errOutput;
    this.compilerName = compilerName;
    this.diagnosticListener = diagnosticListener;
    this.scannerSupplier = checkNotNull(scannerSupplier);
  }

  public static class Builder {
    private DiagnosticListener<? super JavaFileObject> diagnosticListener = null;
    private PrintWriter errOutput = new PrintWriter(System.err, true);
    private String compilerName = "javac (with error-prone)";
    private ScannerSupplier scannerSupplier = BuiltInCheckerSuppliers.matureChecks();

    public ErrorProneCompiler build() {
      return new ErrorProneCompiler(
          compilerName,
          errOutput,
          diagnosticListener,
          scannerSupplier);
    }

    public Builder named(String compilerName) {
      this.compilerName = compilerName;
      return this;
    }

    public Builder redirectOutputTo(PrintWriter errOutput) {
      this.errOutput = errOutput;
      return this;
    }

    public Builder listenToDiagnostics(DiagnosticListener<? super JavaFileObject> listener) {
      this.diagnosticListener = listener;
      return this;
    }

    public Builder report(ScannerSupplier scannerSupplier) {
      this.scannerSupplier = scannerSupplier;
      return this;
    }
  }

  public Result run(String[] args) {
    Context context = new Context();
    JavacFileManager.preRegister(context);
    return run(args, context);
  }

  /**
   * Default to compiling with the same -source and -target as the host's javac.
   *
   * <p>This prevents, e.g., targeting Java 8 by default when using error-prone on JDK7.
   */
  private List<String> defaultToLatestSupportedLanguageLevel(String[] argv) {

    String overrideLanguageLevel;
    switch (JAVA_SPECIFICATION_VERSION.value()) {
      case "1.7":
        overrideLanguageLevel = "7";
        break;
      case "1.8":
        overrideLanguageLevel = "8";
        break;
      default:
        return ImmutableList.copyOf(argv);
    }

    return ImmutableList.<String>builder()
        // suppress xlint 'options' warnings to avoid diagnostics like:
        // 'bootstrap class path not set in conjunction with -source 1.7'
        .add("-Xlint:-options")
        .add("-source").add(overrideLanguageLevel)
        .add("-target").add(overrideLanguageLevel)
        .add(argv)
        .build();
  }

  private String[] prepareCompilation(String[] argv, Context context)
      throws InvalidCommandLineOptionException {

    List<String> newArgs = defaultToLatestSupportedLanguageLevel(argv);
    ErrorProneOptions epOptions = ErrorProneOptions.processArgs(newArgs);

    argv = epOptions.getRemainingArgs();

    if (diagnosticListener != null) {
      context.put(DiagnosticListener.class, diagnosticListener);
    }

    Scanner scanner = scannerSupplier.applyOverrides(epOptions).get();

    setupMessageBundle(context);
    enableEndPositions(context);
    ErrorProneJavacJavaCompiler.preRegister(context, scanner);

    return argv;
  }

  private Result run(String[] argv, Context context) {
    try {
      argv = prepareCompilation(argv, context);
    } catch (InvalidCommandLineOptionException e) {
      errOutput.println(e.getMessage());
      errOutput.flush();
      return Result.CMDERR;
    }

    return new Main(compilerName, errOutput).compile(argv, context);
  }

  public Result run(
    String[] argv,
    List<JavaFileObject> javaFileObjects) {

    Context context = new Context();
    return run(argv, context, null, javaFileObjects, Collections.<Processor>emptyList());
  }

  public Result run(
      String[] argv,
      Context context,
      JavaFileManager fileManager,
      List<JavaFileObject> javaFileObjects,
      Iterable<? extends Processor> processors) {

    try {
      argv = prepareCompilation(argv, context);
    } catch (InvalidCommandLineOptionException e) {
      errOutput.println(e.getMessage());
      errOutput.flush();
      return Result.CMDERR;
    }

    JavacTool tool = JavacTool.create();
    JavacTaskImpl task = (JavacTaskImpl) tool.getTask(
        errOutput,
        fileManager,
        null,
        Arrays.asList(argv),
        null,
        javaFileObjects,
        context);
    if (processors != null) {
      task.setProcessors(processors);
    }
    return task.doCall();
  }

  /**
   * Registers our message bundle.
   */
  public static void setupMessageBundle(Context context) {
    JavacMessages.instance(context).add("com.google.errorprone.errors");
  }

  private static final TaskListener EMPTY_LISTENER = new TaskListener() {
    @Override public void started(TaskEvent e) {}
    @Override public void finished(TaskEvent e) {}
  };

  /** Convinces javac to run in 'API mode', and collect end position information. */
  private static void enableEndPositions(Context context) {
    MultiTaskListener.instance(context).add(EMPTY_LISTENER);
  }
}
