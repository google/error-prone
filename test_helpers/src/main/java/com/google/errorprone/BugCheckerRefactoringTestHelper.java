/*
 * Copyright 2016 The Error Prone Authors.
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
import static org.junit.Assert.fail;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.io.CharStreams;
import com.google.errorprone.apply.DescriptionBasedDiff;
import com.google.errorprone.apply.ImportOrganizer;
import com.google.errorprone.apply.SourceFile;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.scanner.ErrorProneScanner;
import com.google.errorprone.scanner.ErrorProneScannerTransformer;
import com.google.googlejavaformat.java.Formatter;
import com.google.googlejavaformat.java.FormatterException;
import com.google.testing.compile.JavaFileObjects;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.util.Context;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileAlreadyExistsException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;

/**
 * Compare a file transformed as suggested by {@link BugChecker} to an expected source.
 *
 * <p>Inputs are a {@link BugChecker} instance, input file and expected file.
 *
 * @author kurs@google.com (Jan Kurs)
 */
public class BugCheckerRefactoringTestHelper {

  /** Test mode for matching refactored source against expected source. */
  public enum TestMode {
    TEXT_MATCH {
      @Override
      void verifyMatch(JavaFileObject refactoredSource, JavaFileObject expectedSource)
          throws IOException {
        assertThat(maybeFormat(refactoredSource.getCharContent(false).toString()))
            .isEqualTo(maybeFormat(expectedSource.getCharContent(false).toString()));
      }

      private String maybeFormat(String input) {
        try {
          return new Formatter().formatSource(input);
        } catch (FormatterException e) {
          return input;
        }
      }
    },
    AST_MATCH {
      @Override
      void verifyMatch(JavaFileObject refactoredSource, JavaFileObject expectedSource) {
        assertAbout(javaSource()).that(refactoredSource).parsesAs(expectedSource);
      }
    };

    abstract void verifyMatch(JavaFileObject refactoredSource, JavaFileObject expectedSource)
        throws IOException;
  }

  /**
   * For checks that provide multiple possible fixes, chooses the one that will be applied for the
   * test.
   */
  public interface FixChooser {
    Fix choose(List<Fix> fixes);
  }

  /** Predefined FixChoosers for selecting a fix by its position in the list */
  public enum FixChoosers implements FixChooser {
    FIRST {
      @Override
      public Fix choose(List<Fix> fixes) {
        return fixes.get(0);
      }
    },
    SECOND {
      @Override
      public Fix choose(List<Fix> fixes) {
        return fixes.get(1);
      }
    },
    THIRD {
      @Override
      public Fix choose(List<Fix> fixes) {
        return fixes.get(2);
      }
    },
    FOURTH {
      @Override
      public Fix choose(List<Fix> fixes) {
        return fixes.get(3);
      }
    }
  }

  private final Map<JavaFileObject, JavaFileObject> sources = new HashMap<>();
  private final BugChecker refactoringBugChecker;
  private final ErrorProneInMemoryFileManager fileManager;

  private FixChooser fixChooser = FixChoosers.FIRST;
  private List<String> options = ImmutableList.of();
  private boolean allowBreakingChanges = false;
  private String importOrder = "static-first";

  private BugCheckerRefactoringTestHelper(BugChecker refactoringBugChecker, Class<?> clazz) {
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
    String inputPath = getPath("in/", path);
    assertThat(fileManager.exists(inputPath)).isFalse();
    return new ExpectOutput(fileManager.forSourceLines(inputPath, input));
  }

  public BugCheckerRefactoringTestHelper setFixChooser(FixChooser chooser) {
    this.fixChooser = chooser;
    return this;
  }

  public BugCheckerRefactoringTestHelper setArgs(String... args) {
    this.options = ImmutableList.copyOf(args);
    return this;
  }

  /** If set, fixes that produce output that doesn't compile are allowed. Off by default. */
  public BugCheckerRefactoringTestHelper allowBreakingChanges() {
    allowBreakingChanges = true;
    return this;
  }

  public BugCheckerRefactoringTestHelper setImportOrder(String importOrder) {
    this.importOrder = importOrder;
    return this;
  }

  public void doTest() {
    this.doTest(TestMode.AST_MATCH);
  }

  public void doTest(TestMode testMode) {
    for (Map.Entry<JavaFileObject, JavaFileObject> entry : sources.entrySet()) {
      try {
        runTestOnPair(entry.getKey(), entry.getValue(), testMode);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
  }

  private BugCheckerRefactoringTestHelper addInputAndOutput(
      JavaFileObject input, JavaFileObject output) {
    sources.put(input, output);
    return this;
  }

  private void runTestOnPair(JavaFileObject input, JavaFileObject output, TestMode testMode)
      throws IOException {
    Context context = new Context();
    JCCompilationUnit tree = doCompile(input, sources.keySet(), context);
    JavaFileObject transformed = applyDiff(input, context, tree);
    testMode.verifyMatch(transformed, output);
    if (!allowBreakingChanges) {
      doCompile(output, sources.values(), new Context());
    }
  }

  private JCCompilationUnit doCompile(
      final JavaFileObject input, Iterable<JavaFileObject> files, Context context)
      throws IOException {
    JavacTool tool = JavacTool.create();
    DiagnosticCollector<JavaFileObject> diagnosticsCollector = new DiagnosticCollector<>();
    context.put(ErrorProneOptions.class, ErrorProneOptions.empty());
    JavacTaskImpl task =
        (JavacTaskImpl)
            tool.getTask(
                CharStreams.nullWriter(),
                fileManager,
                diagnosticsCollector,
                options,
                /*classes=*/ null,
                files,
                context);
    Iterable<? extends CompilationUnitTree> trees = task.parse();
    task.analyze();
    JCCompilationUnit tree =
        Iterables.getOnlyElement(
            Iterables.filter(
                Iterables.filter(trees, JCCompilationUnit.class),
                compilationUnit -> compilationUnit.getSourceFile() == input));
    Iterable<Diagnostic<? extends JavaFileObject>> errorDiagnostics =
        Iterables.filter(
            diagnosticsCollector.getDiagnostics(), d -> d.getKind() == Diagnostic.Kind.ERROR);
    if (!Iterables.isEmpty(errorDiagnostics)) {
      fail("compilation failed unexpectedly: " + errorDiagnostics);
    }
    return tree;
  }

  private JavaFileObject applyDiff(
      JavaFileObject sourceFileObject, Context context, JCCompilationUnit tree) throws IOException {
    ImportOrganizer importOrganizer = ImportOrderParser.getImportOrganizer(importOrder);
    final DescriptionBasedDiff diff = DescriptionBasedDiff.create(tree, importOrganizer);
    transformer(refactoringBugChecker)
        .apply(
            new TreePath(tree),
            context,
            new DescriptionListener() {
              @Override
              public void onDescribed(Description description) {
                if (!description.fixes.isEmpty()) {
                  diff.handleFix(fixChooser.choose(description.fixes));
                }
              }
            });
    SourceFile sourceFile = SourceFile.create(sourceFileObject);
    diff.applyDifferences(sourceFile);

    JavaFileObject transformed =
        JavaFileObjects.forSourceString(getFullyQualifiedName(tree), sourceFile.getSourceText());
    return transformed;
  }

  private static String getFullyQualifiedName(JCCompilationUnit tree) {
    Iterator<JCClassDecl> types =
        Iterables.filter(tree.getTypeDecls(), JCClassDecl.class).iterator();
    if (types.hasNext()) {
      return Iterators.getOnlyElement(types).sym.getQualifiedName().toString();
    }

    // Fallback: if no class is declared, then assume we're looking at a `package-info.java`.
    return tree.getPackage().packge.package_info.toString();
  }

  private ErrorProneScannerTransformer transformer(BugChecker bugChecker) {
    ErrorProneScanner scanner = new ErrorProneScanner(bugChecker);
    return ErrorProneScannerTransformer.create(scanner);
  }

  /** To assert the proper {@code .addInput().addOutput()} chain. */
  public class ExpectOutput {
    private final JavaFileObject input;

    public ExpectOutput(JavaFileObject input) {
      this.input = input;
    }

    public BugCheckerRefactoringTestHelper addOutputLines(String path, String... output) {
      String outputPath = getPath("out/", path);
      if (fileManager.exists(outputPath)) {
        throw new UncheckedIOException(new FileAlreadyExistsException(outputPath));
      }
      return addInputAndOutput(input, fileManager.forSourceLines(outputPath, output));
    }

    public BugCheckerRefactoringTestHelper addOutput(String outputFilename) {
      return addInputAndOutput(input, fileManager.forResource(outputFilename));
    }

    public BugCheckerRefactoringTestHelper expectUnchanged() {
      return addInputAndOutput(input, input);
    }
  }

  private String getPath(String prefix, String path) {
    // return prefix + path;
    int insertAt = path.lastIndexOf('/');
    insertAt = insertAt == -1 ? 0 : insertAt + 1;
    return new StringBuilder(path).insert(insertAt, prefix + "/").toString();
  }
}
