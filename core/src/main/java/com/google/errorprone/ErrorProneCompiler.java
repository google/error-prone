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

import com.google.errorprone.scanner.ErrorProneScannerSuppliers;
import com.google.errorprone.scanner.Scanner;
import com.google.errorprone.scanner.ScannerSupplier;

import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.tools.javac.api.MultiTaskListener;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.main.Main;
import com.sun.tools.javac.main.Main.Result;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.JavacMessages;
import com.sun.tools.javac.util.List;

import java.io.PrintWriter;

import javax.annotation.processing.Processor;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;

/**
 * An error-prone compiler that matches the interface of {@link com.sun.tools.javac.main.Main}.
 * Used by plexus-java-compiler-errorprone.
 *
 * TODO(user): Currently matches the interface of javac 6.  Update to match javac 8, and make
 * sure it doesn't break plexus-java-compiler-errorprone.
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
    System.exit(new ErrorProneCompiler.Builder().build().compile(args).exitCode);
  }

  /**
   * Compiles in-process.
   *
   * @param listener listens to the diagnostics produced by error-prone
   * @param args the same args which would be passed to javac on the command line
   * @return exit code from the compiler invocation
   */
  public static Result compile(DiagnosticListener<JavaFileObject> listener, String[] args) {
    ErrorProneCompiler compiler = new ErrorProneCompiler.Builder()
        .listenToDiagnostics(listener)
        .build();
    return compiler.compile(args);
  }

  /**
   * Programmatic interface to the error-prone Java compiler.
   *
   * @param args the same args which would be passed to javac on the command line
   * @param out a {@link PrintWriter} to which to send diagnostic output
   * @return exit code from the compiler invocation
   */
  public static Result compile(String[] args, PrintWriter out) {
    ErrorProneCompiler compiler = new ErrorProneCompiler.Builder()
        .redirectOutputTo(out)
        .build();
    return compiler.compile(args);
  }

  private final DiagnosticListener<? super JavaFileObject> diagnosticListener;
  private final Main main;
  private final PrintWriter printWriter;
  private final SearchResultsPrinter resultsPrinter;
  private final ScannerSupplier scannerSupplier;

  private ErrorProneCompiler(
      String compilerName,
      PrintWriter printWriter,
      DiagnosticListener<? super JavaFileObject> diagnosticListener,
      ScannerSupplier scannerSupplier,
      boolean useResultsPrinter) {
    this.printWriter = checkNotNull(printWriter);
    this.main = new Main(compilerName, printWriter);
    this.diagnosticListener = diagnosticListener;
    this.scannerSupplier = checkNotNull(scannerSupplier);
    this.resultsPrinter = useResultsPrinter ? new SearchResultsPrinter(printWriter) : null;
  }

  public static class Builder {
    private DiagnosticListener<? super JavaFileObject> diagnosticListener = null;
    private PrintWriter out = new PrintWriter(System.err, true);
    private String compilerName = "javac (with error-prone)";
    private boolean useResultsPrinter = false;
    private ScannerSupplier scannerSupplier = ErrorProneScannerSuppliers.matureChecks();

    public ErrorProneCompiler build() {
      return new ErrorProneCompiler(
          compilerName,
          out,
          diagnosticListener,
          scannerSupplier,
          useResultsPrinter);
    }

    public Builder named(String compilerName) {
      this.compilerName = compilerName;
      return this;
    }

    public Builder redirectOutputTo(PrintWriter out) {
      this.out = out;
      return this;
    }

    public Builder listenToDiagnostics(DiagnosticListener<? super JavaFileObject> listener) {
      this.diagnosticListener = listener;
      return this;
    }

    // TODO(user): SearchingResultPrinter interacts awkwardly with everything else and is barely
    // used; consider deleting it.
    public Builder search(ScannerSupplier scannerSupplier) {
      this.scannerSupplier = scannerSupplier;
      this.useResultsPrinter = true;
      return this;
    }

    public Builder report(ScannerSupplier scannerSupplier) {
      this.scannerSupplier = scannerSupplier;
      return this;
    }
  }

  public Result compile(String[] args) {
    return compile(args, List.<JavaFileObject>nil());
  }

  public Result compile(List<JavaFileObject> sources) {
    return compile(new String[]{}, sources);
  }

  public Result compile(String[] args, List<JavaFileObject> sources) {
    Context context = new Context();
    JavacFileManager.preRegister(context);
    return compile(args, context, sources, null);
  }

  public Result compile(Context context, List<JavaFileObject> sources) {
    return compile(new String[]{}, context, sources, null);
  }

  public Result compile(String[] args, Context context, List<JavaFileObject> javaFileObjects,
      Iterable<? extends Processor> processors) {
    ErrorProneOptions epOptions = ErrorProneOptions.processArgs(args);

    if (diagnosticListener != null) {
      context.put(DiagnosticListener.class, diagnosticListener);
    }

    Scanner scanner;
    try {
      scanner = scannerSupplier.applyOverrides(epOptions.getSeverityMap()).get();
    } catch (InvalidCommandLineOptionException e) {
      printWriter.println(e.getMessage());
      printWriter.flush();
      return Result.CMDERR;
    }

    setupMessageBundle(context);
    enableEndPositions(context);
    ErrorProneJavacJavaCompiler.preRegister(context, scanner, resultsPrinter);

    Result result =
        main.compile(epOptions.getRemainingArgs(), context, javaFileObjects, processors);

    if (resultsPrinter != null) {
      resultsPrinter.printMatches();
    }

    return result;
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
