/*
 * Copyright 2011 The Error Prone Authors.
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

import com.google.errorprone.scanner.BuiltInCheckerSuppliers;
import com.google.errorprone.scanner.ScannerSupplier;
import com.sun.tools.javac.main.Main.Result;
import java.io.PrintWriter;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;

/**
 * An Error Prone compiler that matches the interface of {@link com.sun.tools.javac.main.Main}.
 *
 * <p>Unlike {@link BaseErrorProneCompiler}, it enables all built-in Error Prone checks by default.
 *
 * <p>Used by plexus-java-compiler-errorprone.
 *
 * @author alexeagle@google.com (Alex Eagle)
 * @deprecated prefer {@link ErrorProneJavaCompiler}, which implements the standard {@code
 *     javax.tools.JavacCompiler} interface.
 */
@Deprecated
public class ErrorProneCompiler {

  /**
   * Entry point for compiling Java code with error-prone enabled. All default checks are run, and
   * the compile fails if they find a bug.
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
    return ErrorProneCompiler.builder().listenToDiagnostics(listener).build().run(args);
  }

  /**
   * Programmatic interface to the error-prone Java compiler.
   *
   * @param args the same args which would be passed to javac on the command line
   * @return result from the compiler invocation
   */
  public static Result compile(String[] args) {
    return builder().build().run(args);
  }

  /**
   * Programmatic interface to the error-prone Java compiler.
   *
   * @param args the same args which would be passed to javac on the command line
   * @param out a {@link PrintWriter} to which to send diagnostic output
   * @return result from the compiler invocation
   */
  public static Result compile(String[] args, PrintWriter out) {
    return ErrorProneCompiler.builder().redirectOutputTo(out).build().run(args);
  }

  private final BaseErrorProneCompiler compiler;

  private ErrorProneCompiler(BaseErrorProneCompiler compiler) {
    this.compiler = compiler;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private final BaseErrorProneCompiler.Builder builder =
        new BaseErrorProneCompiler.Builder().report(BuiltInCheckerSuppliers.defaultChecks());

    public ErrorProneCompiler build() {
      return new ErrorProneCompiler(builder.build());
    }

    public Builder redirectOutputTo(PrintWriter errOutput) {
      builder.redirectOutputTo(errOutput);
      return this;
    }

    public Builder listenToDiagnostics(DiagnosticListener<? super JavaFileObject> listener) {
      builder.listenToDiagnostics(listener);
      return this;
    }

    public Builder report(ScannerSupplier scannerSupplier) {
      builder.report(scannerSupplier);
      return this;
    }

    /** @deprecated prefer {@link #builder()} */
    @Deprecated
    public Builder() {}
  }

  public Result run(String[] args) {
    return compiler.run(args);
  }
}
