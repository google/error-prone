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

import com.google.errorprone.checkers.ErrorChecker.AstError;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.JavacTask;
import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Messages;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.PropertyResourceBundle;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/**
 * @author alexeagle@google.com (Alex Eagle)
 */
public class ErrorFindingCompiler {
  private final String[] args;
  private final JavaCompiler compiler;
  private final DiagnosticCollector<JavaFileObject> diagnostics;

  public ErrorFindingCompiler(String[] args, DiagnosticCollector<JavaFileObject> diagnostics,
                              JavaCompiler javaCompiler) {
    this.args = args;
    this.diagnostics = diagnostics;
    this.compiler = javaCompiler;
  }

  public static void main(String[] args) throws IOException {
    System.exit(new ErrorFindingCompiler(
        args,
        null, // Null diagnostics means they are printed to the console
        ToolProvider.getSystemJavaCompiler())
        .run(new ErrorProneScanner()) ? 0 : 1);
  }

  public boolean run(ErrorCollectingTreeScanner scanner) throws IOException {
    StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);

    //TODO: setup the compiler using all the flags, or extend the javac.main.Main class.
    JavacTask javacTask = (JavacTask) compiler
        .getTask(null, fileManager, diagnostics,
            Arrays.asList("-Xjcov"), // Instruct javac to maintain a table of endpositions for AST
            // TODO: need valid classpath for symbol table lookup to work in Preconditions checker
            //"-classpath",
            //"/Users/alexeagle/.m2/repository/com/google/guava/guava/r09/guava-r09.jar"),
            Collections.<String>emptyList(),
            fileManager.getJavaFileObjects(args));
    Iterable<? extends CompilationUnitTree> compilationUnits = javacTask.parse();
    javacTask.analyze();

    Context context = ((JavacTaskImpl) javacTask).getContext();
    setupMessageBundle(context);
    Log log = Log.instance(context);
    VisitorState visitorState = new VisitorState(javacTask.getTypes(), context);

    boolean hasErrors = false;
    for (CompilationUnitTree compilationUnitTree : compilationUnits) {
      LogReporter logReporter =
          new LogReporter(log, compilationUnitTree.getSourceFile());
      List<AstError> errors = scanner.scan(compilationUnitTree, visitorState);
      for (AstError error : errors) {
        logReporter.emitError(error);
        hasErrors = true;
      }
    }
    return !hasErrors;
  }

  public static void setupMessageBundle(Context context) {
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
