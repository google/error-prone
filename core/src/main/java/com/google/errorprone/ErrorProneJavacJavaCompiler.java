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

import com.google.common.base.Optional;
import com.google.errorprone.scanner.Scanner;

import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskEvent.Kind;
import com.sun.tools.javac.comp.AttrContext;
import com.sun.tools.javac.comp.CompileStates.CompileState;
import com.sun.tools.javac.comp.Env;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Context.Factory;
import com.sun.tools.javac.util.JavacMessages;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Properties;
import java.util.Queue;

/**
 *
 * @author alexeagle@google.com (Alex Eagle)
 */
public class ErrorProneJavacJavaCompiler extends JavaCompiler {

  private final ErrorProneAnalyzer errorProneAnalyzer;

  /**
   * Registers our message bundle.
   */
  public static void setupMessageBundle(Context context) {
    JavacMessages.instance(context).add("com.google.errorprone.errors");
  }

  private ErrorProneJavacJavaCompiler(Context context, Scanner scanner, 
      SearchResultsPrinter resultsPrinter) {
    super(context);
    checkNotNull(scanner);

    // Setup message bundle.
    setupMessageBundle(context);

    // Create ErrorProneAnalyzer.
    errorProneAnalyzer = ErrorProneAnalyzer.create(scanner, resultsPrinter).init(context);
  }

  public static void preRegister(Context context, Scanner scanner) {
    preRegister(context, scanner, null);
  }

  /**
   * Adds an initialization hook to the Context, such that each subsequent
   * request for a JavaCompiler (i.e., a lookup for 'compilerKey' of our
   * superclass, JavaCompiler) will actually construct and return our version.
   * It's necessary since many new JavaCompilers may
   * be requested for later stages of the compilation (annotation processing),
   * within the same Context.
   */
  public static void preRegister(Context context, final Scanner scanner,
      final SearchResultsPrinter resultsPrinter) {
    context.put(compilerKey, new Factory<JavaCompiler>() {
      @Override
      public JavaCompiler make(Context ctx) {
        // Ensure that future processing rounds continue to use the same Scanner.
        return new ErrorProneJavacJavaCompiler(ctx, scanner, resultsPrinter);
      }
    });
  }

  @Override
  protected void flow(Env<AttrContext> env, Queue<Env<AttrContext>> results) {
    if (compileStates.isDone(env, CompileState.FLOW)) {
      return;
    }
    super.flow(env, results);
    try {
      postFlow(env);
    } catch (Throwable e) {
      ByteArrayOutputStream stackTrace = new ByteArrayOutputStream();
      e.printStackTrace(new PrintWriter(stackTrace, true));
      String version = loadVersionFromPom().or("unknown version");
      log.error("error.prone.crash", stackTrace.toString(), version);
    }
  }

  private Optional<String> loadVersionFromPom() {
    InputStream stream = getClass().getResourceAsStream(
        "/META-INF/maven/com.google.errorprone/error_prone_core/pom.properties");
    if (stream == null) {
      return Optional.absent();
    }
    Properties mavenProperties = new Properties();
    try {
      mavenProperties.load(stream);
    } catch (IOException expected) {
      return Optional.absent();
    }
    return Optional.of(mavenProperties.getProperty("version"));
  }

  /**
   * Run Error Prone analysis after performing dataflow checks.
   */
  public void postFlow(Env<AttrContext> env) {
    errorProneAnalyzer.finished(new TaskEvent(Kind.ANALYZE, env.toplevel, env.enclClass.sym));
  }
}
