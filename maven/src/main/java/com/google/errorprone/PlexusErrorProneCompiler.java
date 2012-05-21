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

import org.codehaus.plexus.compiler.CompilerConfiguration;
import org.codehaus.plexus.compiler.CompilerError;
import org.codehaus.plexus.compiler.CompilerException;
import org.codehaus.plexus.compiler.javac.JavacCompiler;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static javax.tools.Diagnostic.Kind.ERROR;

/**
 * @author alexeagle@google.com (Alex Eagle)
 */
public class PlexusErrorProneCompiler extends JavacCompiler {

  /**
   * Based on the implementation in the parent class.
   * @param config
   * @return
   * @throws CompilerException
   */
  @Override
  public List<CompilerError> compile( CompilerConfiguration config ) throws CompilerException {
    if (config.isFork()) {
      throw new UnsupportedOperationException(
          "At present, the error-prone compiler can only be run in-process");
    }

    File destinationDir = new File(config.getOutputLocation());

    if (!destinationDir.exists()) {
      destinationDir.mkdirs();
    }

    String[] sourceFiles = getSourceFiles(config);

    if ((sourceFiles == null) || (sourceFiles.length == 0)) {
      return Collections.emptyList();
    }

    if ((getLogger() != null) && getLogger().isInfoEnabled()) {
      getLogger().info( "Compiling " + sourceFiles.length + " " +
          "source file" + ( sourceFiles.length == 1 ? "" : "s" ) +
          " to " + destinationDir.getAbsolutePath() );
    }

    String[] args = buildCompilerArguments( config, sourceFiles );

    final List<CompilerError> messages = new ArrayList<CompilerError>();
    DiagnosticListener<? super JavaFileObject> listener = new DiagnosticListener<JavaFileObject>() {
      @Override public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
        messages.add(new CompilerError(
            diagnostic.getSource().getName(),
            diagnostic.getKind() == ERROR,
            (int)diagnostic.getLineNumber(),
            (int)diagnostic.getColumnNumber(),
            -1, -1, // end pos line:column is hard to calculate
            diagnostic.getMessage(Locale.getDefault())));
      }
    };
    new ErrorProneCompiler.Builder().listenToDiagnostics(listener).build().compile(args);

    return messages;
  }
}
