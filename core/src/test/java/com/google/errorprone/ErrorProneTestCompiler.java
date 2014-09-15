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

import static com.google.errorprone.CompilationTestHelper.asJavacList;

import com.sun.tools.javac.util.Context;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

import javax.annotation.processing.Processor;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;

/**
 * Wraps {@link com.google.errorprone.ErrorProneCompiler}.
 */
public class ErrorProneTestCompiler {

  /** Wraps {@link com.google.errorprone.ErrorProneCompiler.Builder} */
  public static class Builder {

    ErrorProneCompiler.Builder wrappedCompilerBuilder = new ErrorProneCompiler.Builder();

    public ErrorProneTestCompiler build() {
      return new ErrorProneTestCompiler(wrappedCompilerBuilder.build());
    }

    public Builder listenToDiagnostics(DiagnosticCollector<JavaFileObject> collector) {
      wrappedCompilerBuilder.listenToDiagnostics(collector);
      return this;
    }

    public Builder report(Scanner errorProneScanner) {
      wrappedCompilerBuilder.report(errorProneScanner);
      return this;
    }

    public Builder named(String test) {
      wrappedCompilerBuilder.named(test);
      return this;
    }

    public Builder redirectOutputTo(PrintWriter printWriter) {
      wrappedCompilerBuilder.redirectOutputTo(printWriter);
      return this;
    }
  }

  private final ErrorProneCompiler compiler;
  private final ErrorProneInMemoryFileManager fileManager = new ErrorProneInMemoryFileManager();

  public ErrorProneInMemoryFileManager fileManager() {
    return fileManager;
  }
  
  private ErrorProneTestCompiler(ErrorProneCompiler compiler) {
    this.compiler = compiler;
  }

  public int compile(List<JavaFileObject> sources) {
    return compile(sources, null);
  }

  public int compile(String[] args, List<JavaFileObject> sources) {
    return compile(args, sources, null);
  }

  public int compile(List<JavaFileObject> sources, List<? extends Processor> processors) {
    return compile(new String[]{}, sources, processors);
  }
  
  public int compile(String[] args, List<JavaFileObject> sources, List<? extends Processor>
      processors) {
    Context context = new Context();
    context.put(JavaFileManager.class, fileManager);
    List<String> processedArgs =
        CompilationTestHelper.disableImplicitProcessing(Arrays.asList(args));
    String[] argsArray = processedArgs.toArray(new String[processedArgs.size()]);
    return compiler.compile(argsArray, context, asJavacList(sources), processors);
  }
}
