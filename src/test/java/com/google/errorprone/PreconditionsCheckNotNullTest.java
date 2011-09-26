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

import junit.framework.TestCase;

import javax.tools.*;
import javax.tools.Diagnostic.Kind;
import javax.tools.JavaCompiler.CompilationTask;
import java.io.File;
import java.net.URISyntaxException;

import static java.util.Arrays.asList;
import static java.util.Locale.ENGLISH;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;

/**
 * @author alexeagle@google.com (Alex Eagle)
 */
public class PreconditionsCheckNotNullTest extends TestCase {

  private File projectRoot;
  private javax.tools.JavaCompiler compiler;
  private StandardJavaFileManager fileManager;
  private DiagnosticCollector<JavaFileObject> diagnostics;

  protected void setUp() throws Exception {
    super.setUp();
    // Hacky way to find the project root. This lets us avoid adding the .java example
    // files to the classpath when running the tests, so they don't need to compile.
    String pathToFileInProject = getClass()
        .getResource("/" + this.getClass().getName().replaceAll("\\.", "/") + ".class")
        .toURI().getPath();
    projectRoot = new File(pathToFileInProject
        .substring(0, pathToFileInProject.lastIndexOf("error-prone") + "error-prone".length()));
    compiler = ToolProvider.getSystemJavaCompiler();
    fileManager = compiler.getStandardFileManager(null, null, null);
    diagnostics = new DiagnosticCollector<JavaFileObject>();
  }

  // TODO: parameterize the test so each new error type doesn't create a new class?
  public void testLineNumberAppearsInError() throws URISyntaxException {
    File exampleSource = new File(projectRoot, "error-patterns/guava/PositiveCase1.java");
    assertTrue(exampleSource.exists());
    assertFalse(createCompileTask(exampleSource).call());
    boolean found = false;
    for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
      String message = diagnostic.getMessage(ENGLISH);
      System.out.println("message = " + message);
      if (diagnostic.getKind() == Kind.ERROR && message.contains("Preconditions#checkNotNull")) {
        assertThat(message, containsString("\"string literal\""));
        assertThat(diagnostic.getLineNumber(), is(5L));
        assertThat(diagnostic.getColumnNumber(), is(32L));
        found = true;
        return;

      }
    }
    assertTrue(found);
  }

  //TODO: get the test to pass
  public void testNoErrorForNegativeCase1() throws URISyntaxException {
    File exampleSource = new File(projectRoot, "error-patterns/guava/NegativeCase1.java");
    assertTrue(exampleSource.exists());
    assertTrue(createCompileTask(exampleSource).call());
    for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
      String message = diagnostic.getMessage(ENGLISH);
      if (diagnostic.getKind() == Kind.ERROR && message.contains("Preconditions#checkNotNull")) {
        fail("Error in negative case");
      }
    }
  }

  private CompilationTask createCompileTask(File exampleSource) {
    CompilationTask task = compiler
        .getTask(null, fileManager, diagnostics, null, asList(ErrorProneProcessor.class.getName()),
            fileManager.getJavaFileObjects(exampleSource));
    task.setProcessors(asList(new ErrorProneProcessor()));
    return task;
  }
}
