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

package org.codehaus.plexus.compiler.javac;

import com.google.errorprone.ErrorProneCompiler;
import org.codehaus.plexus.compiler.CompilerConfiguration;
import org.codehaus.plexus.compiler.CompilerException;
import org.codehaus.plexus.compiler.CompilerMessage;
import org.codehaus.plexus.compiler.CompilerResult;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * @author alexeagle@google.com (Alex Eagle)
 */
public class PlexusErrorProneCompiler extends JavacCompiler {

  @Override
  CompilerResult compileOutOfProcess(CompilerConfiguration config, String executable, String[] args) throws CompilerException {
    throw new UnsupportedOperationException(
        "At present, the error-prone compiler can only be run in-process");
  }

  @Override
  CompilerResult compileInProcess(String[] args, CompilerConfiguration config) throws CompilerException {
    final List<CompilerMessage> messages = new ArrayList<CompilerMessage>();
    DiagnosticListener<? super JavaFileObject> listener = new DiagnosticListener<JavaFileObject>() {
      @Override public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
        messages.add(new CompilerMessage(
            diagnostic.getSource().getName(),
            convertKind(diagnostic.getKind()),
                (int)diagnostic.getLineNumber(),
            (int)diagnostic.getColumnNumber(),
            -1, -1, // end pos line:column is hard to calculate
            diagnostic.getMessage(Locale.getDefault())));
      }
    };
    int result = new ErrorProneCompiler.Builder().listenToDiagnostics(listener).build().compile(args);

    return new CompilerResult(result == 0, messages);
  }

  private org.codehaus.plexus.compiler.CompilerMessage.Kind convertKind(Diagnostic.Kind kind) {
    switch(kind) {
      case ERROR:
        return CompilerMessage.Kind.ERROR;
      case MANDATORY_WARNING:
        return CompilerMessage.Kind.MANDATORY_WARNING;
      case NOTE:
        return CompilerMessage.Kind.NOTE;
      case WARNING:
        return CompilerMessage.Kind.WARNING;
      case OTHER:
        return CompilerMessage.Kind.OTHER;
      default:
        return CompilerMessage.Kind.OTHER;
    }
  }
}
