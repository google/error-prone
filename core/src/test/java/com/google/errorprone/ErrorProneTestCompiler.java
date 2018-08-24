/*
 * Copyright 2014 The Error Prone Authors.
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
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;

/** Wraps {@link com.google.errorprone.ErrorProneJavaCompiler}. */
public class ErrorProneTestCompiler {

  /** {@link ErrorProneTestCompiler.Builder} */
  public static class Builder {

    private DiagnosticListener<? super JavaFileObject> listener;
    private ScannerSupplier scannerSupplier;
    private PrintWriter printWriter;

    public ErrorProneTestCompiler build() {
      return new ErrorProneTestCompiler(listener, scannerSupplier, printWriter);
    }

    public Builder listenToDiagnostics(DiagnosticListener<? super JavaFileObject> listener) {
      this.listener = listener;
      return this;
    }

    public Builder report(ScannerSupplier scannerSupplier) {
      this.scannerSupplier = scannerSupplier;
      return this;
    }

    public Builder redirectOutputTo(PrintWriter printWriter) {
      this.printWriter = printWriter;
      return this;
    }
  }

  private final DiagnosticListener<? super JavaFileObject> listener;
  private final ScannerSupplier scannerSupplier;
  private final PrintWriter printWriter;

  private final ErrorProneInMemoryFileManager fileManager = new ErrorProneInMemoryFileManager();

  public ErrorProneTestCompiler(
      DiagnosticListener<? super JavaFileObject> listener,
      ScannerSupplier scannerSupplier,
      PrintWriter printWriter) {
    this.listener = listener;
    this.scannerSupplier = scannerSupplier;
    this.printWriter = printWriter;
  }

  public ErrorProneInMemoryFileManager fileManager() {
    return fileManager;
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
    CompilationTask task =
        new BaseErrorProneJavaCompiler(scannerSupplier)
            .getTask(printWriter, fileManager, listener, Arrays.asList(args), null, sources);
    if (processors != null) {
      task.setProcessors(processors);
    }
    return task.call() ? Result.OK : Result.ERROR;
  }
}
