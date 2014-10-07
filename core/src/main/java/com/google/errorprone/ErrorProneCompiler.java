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

import static com.google.errorprone.ErrorProneScanner.EnabledPredicate.DEFAULT_CHECKS;

import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.main.Main;
import com.sun.tools.javac.main.Main.Result;
import com.sun.tools.javac.util.Context;
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
    System.exit(new ErrorProneCompiler.Builder().build().compile(args));
  }

  /**
   * Compiles in-process.
   *
   * @param listener listens to the diagnostics produced by error-prone
   * @param args the same args which would be passed to javac on the command line
   * @return exit code from the compiler invocation
   */
  public static int compile(DiagnosticListener<JavaFileObject> listener, String[] args) {
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
  public static int compile(String[] args, PrintWriter out) {
    ErrorProneCompiler compiler = new ErrorProneCompiler.Builder()
        .redirectOutputTo(out)
        .build();
    return compiler.compile(args);
  }

  private final DiagnosticListener<? super JavaFileObject> diagnosticListener;
  private final Class<? extends JavaCompiler> compilerClass;
  private final Main main;
  private final PrintWriter printWriter;

  /**
   * A custom Scanner to use if we want to use a non-default set of error-prone checks, e.g.
   * for testing.  Null if we want to use the default set of checks.
   */
  private final Scanner errorProneScanner;

  private ErrorProneCompiler(String s, PrintWriter printWriter,
      DiagnosticListener<? super JavaFileObject> diagnosticListener,
      Scanner errorProneScanner,
      Class<? extends JavaCompiler> compilerClass) {
    this.printWriter = printWriter;
    this.main = new Main(s, printWriter);
    this.diagnosticListener = diagnosticListener;
    this.errorProneScanner = errorProneScanner;
    this.compilerClass = compilerClass;
  }

  public static class Builder {
    DiagnosticListener<? super JavaFileObject> diagnosticListener = null;
    PrintWriter out = new PrintWriter(System.err, true);
    String compilerName = "javac (with error-prone)";
    Class<? extends JavaCompiler> compilerClass = ErrorReportingJavaCompiler.class;

    /**
     * A custom Scanner to use if we want to use a non-default set of error-prone checks, e.g.
     * for testing.  Null if we want to use the default set of checks.
     */
    Scanner scanner;

    public ErrorProneCompiler build() {
      return new ErrorProneCompiler(compilerName, out, diagnosticListener, scanner, compilerClass);
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

    public Builder search(Scanner scanner) {
      this.compilerClass = SearchingJavaCompiler.class;
      this.scanner = scanner;
      return this;
    }

    public Builder report(Scanner scanner) {
      this.compilerClass = ErrorReportingJavaCompiler.class;
      this.scanner = scanner;
      return this;
    }
  }

  public int compile(String[] args) {
    return compile(args, List.<JavaFileObject>nil());
  }

  public int compile(List<JavaFileObject> sources) {
    return compile(new String[]{}, sources);
  }

  public int compile(String[] args, List<JavaFileObject> sources) {
    Context context = new Context();
    JavacFileManager.preRegister(context);
    return compile(args, context, sources, null);
  }

  public int compile(Context context, List<JavaFileObject> sources) {
    return compile(new String[]{}, context, sources, null);
  }

  public int compile(String[] args, Context context, List<JavaFileObject> javaFileObjects,
      Iterable<? extends Processor> processors) {
    ErrorProneOptions epOptions = ErrorProneOptions.processArgs(args);

    if (diagnosticListener != null) {
      context.put(DiagnosticListener.class, diagnosticListener);
    }

    Scanner scannerInContext = context.get(Scanner.class);
    if (scannerInContext == null) {
      if (errorProneScanner == null) {
        scannerInContext = new ErrorProneScanner(DEFAULT_CHECKS);
      } else {
        scannerInContext = errorProneScanner;
      }
      context.put(Scanner.class, scannerInContext);
    }
    try {
      scannerInContext.setDisabledChecks(epOptions.getDisabledChecks());
    } catch (InvalidCommandLineOptionException e) {
      printWriter.println(e.getMessage());
      return 2;     // Main.EXIT_CMDERR
    }

    try {
      compilerClass.getMethod("preRegister", Context.class).invoke(null, context);
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException("The JavaCompiler used must have the preRegister static method. "
          + "We are very sorry.", e);
    }
    Result result =
        main.compile(epOptions.getRemainingArgs(), context, javaFileObjects, processors);
    return result.exitCode;
  }
}
