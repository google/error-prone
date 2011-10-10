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

import com.google.errorprone.matchers.ErrorProducingMatcher.AstError;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.JavacTask;
import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Messages;

import javax.tools.*;
import javax.tools.JavaCompiler;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.PropertyResourceBundle;

/**
 * @author alexeagle@google.com (Alex Eagle)
 */
public class ErrorFindingCompiler {
  private final String[] args;
  private final JavaCompiler compiler;
  private static DiagnosticCollector<JavaFileObject> diagnostics;

  public ErrorFindingCompiler(String[] args, DiagnosticCollector<JavaFileObject> diagnostics,
                              JavaCompiler javaCompiler) {
    this.args = args;
    this.compiler = javaCompiler;
  }

  public static void main(String[] args) throws IOException {
    new ErrorFindingCompiler(args, new DiagnosticCollector<JavaFileObject>(),
        ToolProvider.getSystemJavaCompiler()).run();
  }

  public void run() throws IOException {
    StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);

    JavacTask javacTask = (JavacTask) compiler
        .getTask(null, fileManager, diagnostics,
            Collections.<String>emptyList(),
            Collections.<String>emptyList(),
            fileManager.getJavaFileObjects(args[0]));
    Iterable<? extends CompilationUnitTree> compilationUnits = javacTask.parse();
    javacTask.analyze();

    Context context = ((JavacTaskImpl) javacTask).getContext();
    setupMessageBundle(context);
    Log log = Log.instance(context);

    VisitorState visitorState = new VisitorState(javacTask.getTypes(), Symtab.instance(context));

    for (CompilationUnitTree compilationUnitTree : compilationUnits) {
      CommandLineReporter commandLineReporter =
          new CommandLineReporter(log, compilationUnitTree.getSourceFile());
      List<AstError> errors = new ASTVisitor()
          .visitCompilationUnit(compilationUnitTree, visitorState);
      for (AstError error : errors) {
        commandLineReporter.emitError(error);
      }
    }
  }

  static void setupMessageBundle(Context context) {
    try {
      String bundlePath = "/com/google/errorprone/errors.properties";
      InputStream bundleResource = ErrorFindingCompiler.class.getResourceAsStream(bundlePath);
      if (bundleResource == null) {
        throw new IllegalStateException("Resource bundle not found at " + bundlePath);
      }
      Messages.instance(context).add(new PropertyResourceBundle(bundleResource));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
