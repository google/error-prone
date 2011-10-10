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

import static java.util.Arrays.asList;
import static java.util.Locale.ENGLISH;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;

import junit.framework.TestCase;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URLClassLoader;

import javax.tools.Diagnostic;
import javax.tools.Diagnostic.Kind;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/**
 * @author alexeagle@google.com (Alex Eagle)
 */
public class ErrorProneProcessorIntegrationTest extends TestCase {

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

  // TODO: parameterize the test so each new error type doesn't create a new test method?
  public void testErrorExpectedForPositiveCase1() throws URISyntaxException {
    errorExpectedWithCorrectLineNumber("guava/PositiveCase1.java", 7L, 32L);
  }
  
  public void testErrorExpectedForPositiveCase2() throws URISyntaxException {
    errorExpectedWithCorrectLineNumber("guava/PositiveCase2.java", 10L, 55L);
  }
  
  private void errorExpectedWithCorrectLineNumber(String filename, long lineNum, long colNum) {
    File exampleSource = new File(projectRoot, "error-patterns/" + filename);
    assertTrue(exampleSource.getAbsolutePath() + " should exist", exampleSource.exists());
    assertFalse("Compile should fail", createCompileTask(exampleSource).call());
    boolean found = false;
    for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
      String message = diagnostic.getMessage(ENGLISH);
      if (diagnostic.getKind() == Kind.ERROR && message.contains("Preconditions#checkNotNull")) {
        assertThat(message, containsString("\"string literal\""));
        assertThat(diagnostic.getLineNumber(), is(lineNum));
        assertThat(diagnostic.getColumnNumber(), is(colNum));
        found = true;
        return;

      }
    }
    assertTrue("Warning should be found. Diagnostics: " + diagnostics.getDiagnostics(), found);
  }
  
  public void testNoErrorForNegativeCase1() throws URISyntaxException {
    File exampleSource = new File(projectRoot, "error-patterns/guava/NegativeCase1.java");
    assertTrue(exampleSource.getAbsolutePath() + " should exist", exampleSource.exists());
    assertTrue("Compile should succeed", createCompileTask(exampleSource).call());
    for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
      String message = diagnostic.getMessage(ENGLISH);
      if (diagnostic.getKind() == Kind.ERROR && message.contains("Preconditions#checkNotNull")) {
        fail("Error in negative case");
      }
    }
  }

  private CompilationTask createCompileTask(File exampleSource) {
    CompilationTask task = compiler
        .getTask(null, fileManager, diagnostics,
            asList("-classpath", Joiner.on(File.pathSeparator).join(
              getPath(ErrorProneProcessor.class),
              getPath(Preconditions.class))),
            asList(ErrorProneProcessor.class.getName()),
            fileManager.getJavaFileObjects(exampleSource));
    task.setProcessors(asList(new ErrorProneProcessor()));
    return task;
  }

  private String getPath(Class clazz) {
    String classname = clazz.getName().replace('.', '/') + ".class";
    URLClassLoader classLoader = (URLClassLoader) Thread.currentThread().getContextClassLoader();
    String path = classLoader.findResource(classname).getFile();
    int jarDelimiter = path.indexOf("!");
    if (jarDelimiter >= 0) {
      return path.substring(0, jarDelimiter);
    } else {
      int packageStart = path.indexOf(clazz.getPackage().getName().replace('.', '/'));
      if (packageStart >= 0) {
        return path.substring(0, packageStart);
      } else {
        return path;
      }
    }
  }
}
