/*
 * Copyright 2014 Google Inc. All Rights Reserved.
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

import com.google.errorprone.scanner.ScannerSupplier;
import com.sun.tools.javac.main.Main.Result;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import javax.annotation.processing.Processor;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;

/** Wraps {@link com.google.errorprone.ErrorProneCompiler}. */
public class ErrorProneTestCompiler {

  /** Wraps {@link com.google.errorprone.ErrorProneCompiler.Builder} */
  public static class Builder {

    final BaseErrorProneCompiler.Builder wrappedCompilerBuilder = BaseErrorProneCompiler.builder();

    public ErrorProneTestCompiler build() {
      return new ErrorProneTestCompiler(wrappedCompilerBuilder.build());
    }

    public Builder listenToDiagnostics(DiagnosticListener<? super JavaFileObject> listener) {
      wrappedCompilerBuilder.listenToDiagnostics(listener);
      return this;
    }

    public Builder report(ScannerSupplier scannerSupplier) {
      wrappedCompilerBuilder.report(scannerSupplier);
      return this;
    }

    public Builder redirectOutputTo(PrintWriter printWriter) {
      wrappedCompilerBuilder.redirectOutputTo(printWriter);
      return this;
    }
  }

  private final BaseErrorProneCompiler compiler;
  private final ErrorProneInMemoryFileManager fileManager = new ErrorProneInMemoryFileManager();

  public ErrorProneInMemoryFileManager fileManager() {
    return fileManager;
  }

  private ErrorProneTestCompiler(BaseErrorProneCompiler compiler) {
    this.compiler = compiler;
  }

  public Result compile(List<JavaFileObject> sources) {
    return compile(sources, null);
  }

  public Result compile(String[] args, List<JavaFileObject> sources) {
    return compile(args, sources, null);
  }

  public Result compile(List<JavaFileObject> sources, List<? extends Processor> processors) {
    return compile(new String[] {}, sources, processors);
  }

  public Result compile(
      String[] args, List<JavaFileObject> sources, List<? extends Processor> processors) {
    if (processors == null || processors.isEmpty()) {
      List<String> processedArgs =
          CompilationTestHelper.disableImplicitProcessing(Arrays.asList(args));
      args = processedArgs.toArray(new String[0]);
    }
    return compiler.run(args, fileManager, sources, processors);
  }
}
