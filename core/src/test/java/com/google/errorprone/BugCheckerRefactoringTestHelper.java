/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

import static com.google.common.truth.Truth.assertAbout;
import static com.google.common.truth.Truth.assertThat;
import static com.google.testing.compile.JavaSourceSubjectFactory.javaSource;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.io.CharStreams;
import com.google.errorprone.apply.DescriptionBasedDiff;
import com.google.errorprone.apply.SourceFile;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.scanner.ErrorProneScanner;
import com.google.errorprone.scanner.ErrorProneScannerTransformer;
import com.google.testing.compile.JavaFileObjects;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.util.Context;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;

/**
 * Compare a file transformed as suggested by {@link BugChecker} to an expected source.
 *
 * Inputs are a {@link BugChecker} instance, input file and expected file.
 *
 * @author kurs@google.com (Jan Kurs)
 */
public class BugCheckerRefactoringTestHelper {

  /**
   * Test mode for matching refactored source against expected source.
   */
  public static enum TestMode {
    TEXT_MATCH {
      @Override
      void verifyMatch(JavaFileObject refactoredSource,
          JavaFileObject expectedSource) throws IOException {
        assertThat(refactoredSource.getCharContent(false).toString())
            .isEqualTo(expectedSource.getCharContent(false).toString());
      }
    },
    AST_MATCH {
      @Override
      void verifyMatch(JavaFileObject refactoredSource, JavaFileObject expectedSource) {
        assertAbout(javaSource()).that(refactoredSource).parsesAs(expectedSource);
      }
    };

    abstract void verifyMatch(JavaFileObject refactoredSource,
        JavaFileObject expectedSource) throws IOException;
  }

  private final Map<JavaFileObject, JavaFileObject> sources = new HashMap<>();
  private final BugChecker refactoringBugChecker;
  private final ErrorProneInMemoryFileManager fileManager;

  private BugCheckerRefactoringTestHelper(
      BugChecker refactoringBugChecker, Class<?> clazz) {
    this.refactoringBugChecker = refactoringBugChecker;
    this.fileManager = new ErrorProneInMemoryFileManager(clazz);
  }

  public static BugCheckerRefactoringTestHelper newInstance(
      BugChecker refactoringBugChecker, Class<?> clazz) {
    return new BugCheckerRefactoringTestHelper(refactoringBugChecker, clazz);
  }

  public BugCheckerRefactoringTestHelper.ExpectOutput addInput(String inputFilename) {
    return new ExpectOutput(fileManager.forResource(inputFilename));
  }

  public BugCheckerRefactoringTestHelper.ExpectOutput addInputLines(String path, String... input) {
    assertThat(fileManager.exists(path)).isFalse();
    return new ExpectOutput(fileManager.forSourceLines(path, input));
  }

  public void doTest() throws IOException {
    this.doTest(TestMode.AST_MATCH);
  }

  public void doTest(TestMode testMode) throws IOException {
    for (Map.Entry<JavaFileObject, JavaFileObject> entry : sources.entrySet()) {
      runTestOnPair(entry.getKey(), entry.getValue(), testMode);
    }
  }

  private BugCheckerRefactoringTestHelper addInputAndOutput(
      JavaFileObject input, JavaFileObject output) {
    sources.put(input, output);
    return this;
  }

  private void runTestOnPair(JavaFileObject input, JavaFileObject output, TestMode testMode)
      throws IOException {
    JavacTaskImpl task = createJavacTask();
    JCCompilationUnit tree = parseAndAnalyze(task, input);
    JavaFileObject transformed = applyDiff(input, task, tree);

    testMode.verifyMatch(transformed, output);
  }

  private JavaFileObject applyDiff(JavaFileObject sourceFileObject,
      JavacTaskImpl task, JCCompilationUnit tree) throws IOException {
    DescriptionBasedDiff diff = DescriptionBasedDiff.create(tree);
    transformer(refactoringBugChecker).apply(new TreePath(tree), task.getContext(), diff);
    SourceFile sourceFile = SourceFile.create(sourceFileObject);
    diff.applyDifferences(sourceFile);

    JavaFileObject transformed = JavaFileObjects.forSourceString(
        Iterables.getOnlyElement(Iterables.filter(tree.getTypeDecls(), JCClassDecl.class))
            .sym.getQualifiedName()
            .toString(),
        sourceFile.getSourceText());
    return transformed;
  }

  private JCCompilationUnit parseAndAnalyze(JavacTaskImpl task, final JavaFileObject input) {
    Iterable<? extends CompilationUnitTree> trees = task.parse();
    task.analyze();
    return Iterables.getOnlyElement(
        Iterables.filter(
            Iterables.filter(trees, JCCompilationUnit.class),
            new Predicate<JCCompilationUnit>() {
              @Override
              public boolean apply(JCCompilationUnit compilation) {
                return compilation.getSourceFile() == input;
              }
            }));
  }

  private JavacTaskImpl createJavacTask() {
    JavacTool tool = JavacTool.create();
    DiagnosticCollector<JavaFileObject> diagnosticsCollector = new DiagnosticCollector<>();
    Context context = new Context();

    context.put(ErrorProneOptions.class, ErrorProneOptions.empty());

    JavacTaskImpl task =
        (JavacTaskImpl)
            tool.getTask(
                CharStreams.nullWriter(),
                fileManager,
                diagnosticsCollector,
                ImmutableList.<String>of(),
                null,
                ImmutableList.copyOf(sources.keySet()),
                context);

    return task;
  }

  private ErrorProneScannerTransformer transformer(BugChecker bugChecker) {
    ErrorProneScanner scanner = new ErrorProneScanner(bugChecker);
    return ErrorProneScannerTransformer.create(scanner);
  }

  /**
   * To assert the proper {@code .addInput().addOutput()} chain.
   */
  public class ExpectOutput {
    private final JavaFileObject input;

    public ExpectOutput(JavaFileObject input) {
      this.input = input;
    }

    public BugCheckerRefactoringTestHelper addOutputLines(String path, String ... output) {
      assertThat(fileManager.exists(path)).isFalse();
      return addInputAndOutput(input, fileManager.forSourceLines(path,  output));
    }

    public BugCheckerRefactoringTestHelper addOutput(String outputFilename) {
      return addInputAndOutput(input, fileManager.forResource(outputFilename));
    }
    
    public BugCheckerRefactoringTestHelper expectUnchanged() {
      return addInputAndOutput(input, input);
    }
  }
}
