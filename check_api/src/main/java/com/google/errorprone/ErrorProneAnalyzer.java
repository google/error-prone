/*
 * Copyright 2012 The Error Prone Authors.
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
import static com.google.common.base.Throwables.getStackTraceAsString;
import static com.google.common.base.Verify.verify;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.ErrorProneOptions.Severity;
import com.google.errorprone.RefactoringCollection.RefactoringResult;
import com.google.errorprone.scanner.ErrorProneScannerTransformer;
import com.google.errorprone.scanner.ScannerSupplier;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskEvent.Kind;
import com.sun.source.util.TaskListener;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.api.ClientCodeWrapper.Trusted;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.code.Symbol.CompletionFailure;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Log.WriterKind;
import com.sun.tools.javac.util.PropagatedException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import javax.tools.JavaFileObject;

/** A {@link TaskListener} that runs Error Prone over attributed compilation units. */
@Trusted
public class ErrorProneAnalyzer implements TaskListener {

  // The set of trees that have already been scanned.
  private final Set<Tree> seen = new HashSet<>();

  private final Supplier<CodeTransformer> transformer;
  private final ErrorProneOptions errorProneOptions;
  private final Context context;
  private final DescriptionListener.Factory descriptionListenerFactory;

  public static ErrorProneAnalyzer createAnalyzer(
      ScannerSupplier scannerSupplier,
      ErrorProneOptions epOptions,
      Context context,
      RefactoringCollection[] refactoringCollection) {
    if (!epOptions.patchingOptions().doRefactor()) {
      return createByScanningForPlugins(scannerSupplier, epOptions, context);
    }
    refactoringCollection[0] = RefactoringCollection.refactor(epOptions.patchingOptions(), context);

    // Refaster refactorer or using builtin checks
    Supplier<CodeTransformer> codeTransformer =
        epOptions
            .patchingOptions()
            .customRefactorer()
            .or(
                Suppliers.memoize(
                    () -> {
                      ImmutableSet<String> namedCheckers =
                          epOptions.patchingOptions().namedCheckers();
                      ScannerSupplier toUse =
                          ErrorPronePlugins.loadPlugins(scannerSupplier, context)
                              .applyOverrides(epOptions)
                              .filter(
                                  bci -> {
                                    String name = bci.canonicalName();
                                    return epOptions.getSeverityMap().get(name) != Severity.OFF
                                        && (namedCheckers.isEmpty()
                                            || namedCheckers.contains(name));
                                  });
                      return ErrorProneScannerTransformer.create(toUse.get());
                    }));

    return createWithCustomDescriptionListener(
        codeTransformer, epOptions, context, refactoringCollection[0]);
  }

  /** A {@link TaskListener} that performs refactorings. */
  public static class RefactoringTask implements TaskListener {

    private final Context context;
    private final RefactoringCollection refactoringCollection;

    public RefactoringTask(Context context, RefactoringCollection refactoringCollection) {
      this.context = context;
      this.refactoringCollection = refactoringCollection;
    }

    @Override
    public void started(TaskEvent event) {}

    @Override
    public void finished(TaskEvent event) {
      if (event.getKind() != Kind.GENERATE) {
        return;
      }
      RefactoringResult refactoringResult;
      try {
        refactoringResult = refactoringCollection.applyChanges(event.getSourceFile().toUri());
      } catch (Exception e) {
        PrintWriter out = Log.instance(context).getWriter(WriterKind.ERROR);
        out.println(e.getMessage());
        out.flush();
        return;
      }
      if (refactoringResult.type() == RefactoringCollection.RefactoringResultType.CHANGED) {
        PrintWriter out = Log.instance(context).getWriter(WriterKind.NOTICE);
        out.println(refactoringResult.message());
        out.flush();
      }
    }
  }

  public static ErrorProneAnalyzer createByScanningForPlugins(
      ScannerSupplier scannerSupplier, ErrorProneOptions errorProneOptions, Context context) {
    return new ErrorProneAnalyzer(
        scansPlugins(scannerSupplier, errorProneOptions, context),
        errorProneOptions,
        context,
        JavacErrorDescriptionListener.provider(context));
  }

  private static Supplier<CodeTransformer> scansPlugins(
      ScannerSupplier scannerSupplier, ErrorProneOptions errorProneOptions, Context context) {
    return Suppliers.memoize(
        () -> {
          // we can't load plugins from the processorpath until the filemanager has been
          // initialized, so do it lazily
          ErrorProneTimings timings = ErrorProneTimings.instance(context);
          try (AutoCloseable unused = timings.initializationTimeSpan()) {
            return ErrorProneScannerTransformer.create(
                ErrorPronePlugins.loadPlugins(scannerSupplier, context)
                    .applyOverrides(errorProneOptions)
                    .get());
          } catch (InvalidCommandLineOptionException e) {
            throw new PropagatedException(e);
          } catch (Exception e) {
            // for the timing span, should be impossible
            throw new AssertionError(e);
          }
        });
  }

  static ErrorProneAnalyzer createWithCustomDescriptionListener(
      Supplier<CodeTransformer> codeTransformer,
      ErrorProneOptions errorProneOptions,
      Context context,
      DescriptionListener.Factory descriptionListenerFactory) {
    return new ErrorProneAnalyzer(
        codeTransformer, errorProneOptions, context, descriptionListenerFactory);
  }

  private ErrorProneAnalyzer(
      Supplier<CodeTransformer> transformer,
      ErrorProneOptions errorProneOptions,
      Context context,
      DescriptionListener.Factory descriptionListenerFactory) {
    this.transformer = checkNotNull(transformer);
    this.errorProneOptions = checkNotNull(errorProneOptions);
    this.descriptionListenerFactory = checkNotNull(descriptionListenerFactory);

    Context errorProneContext = new SubContext(context);
    errorProneContext.put(ErrorProneOptions.class, errorProneOptions);
    this.context = errorProneContext;
  }

  private int errorProneErrors = 0;

  @Override
  public void finished(TaskEvent taskEvent) {
    if (taskEvent.getKind() != Kind.ANALYZE) {
      return;
    }
    if (JavaCompiler.instance(context).errorCount() > errorProneErrors) {
      return;
    }
    TreePath path = JavacTrees.instance(context).getPath(taskEvent.getTypeElement());
    if (path == null) {
      path = new TreePath(taskEvent.getCompilationUnit());
    }
    // Assert that the event is unique and scan the current tree.
    verify(seen.add(path.getLeaf()), "Duplicate FLOW event for: %s", taskEvent.getTypeElement());
    Log log = Log.instance(context);
    JCCompilationUnit compilation = (JCCompilationUnit) path.getCompilationUnit();
    DescriptionListener descriptionListener =
        descriptionListenerFactory.getDescriptionListener(log, compilation);
    DescriptionListener countingDescriptionListener =
        d -> {
          if (d.severity() == SeverityLevel.ERROR) {
            errorProneErrors++;
          }
          descriptionListener.onDescribed(d);
        };
    JavaFileObject originalSource = log.useSource(compilation.getSourceFile());
    try {
      if (shouldExcludeSourceFile(compilation)) {
        return;
      }
      if (path.getLeaf().getKind() == Tree.Kind.COMPILATION_UNIT) {
        // We only get TaskEvents for compilation units if they contain no package declarations
        // (e.g. package-info.java files).  In this case it's safe to analyze the
        // CompilationUnitTree immediately.
        transformer.get().apply(path, context, countingDescriptionListener);
      } else if (finishedCompilation(path.getCompilationUnit())) {
        // Otherwise this TaskEvent is for a ClassTree, and we can scan the whole
        // CompilationUnitTree once we've seen all the enclosed classes.
        transformer.get().apply(new TreePath(compilation), context, countingDescriptionListener);
      }
    } catch (ErrorProneError e) {
      e.logFatalError(log, context);
      // let the exception propagate to javac's main, where it will cause the compilation to
      // terminate with Result.ABNORMAL
      throw e;
    } catch (LinkageError e) {
      // similar to ErrorProneError
      String version = ErrorProneVersion.loadVersionFromPom().or("unknown version");
      log.error("error.prone.crash", getStackTraceAsString(e), version, "(see stack trace)");
      throw e;
    } catch (CompletionFailure e) {
      // A CompletionFailure can be triggered when error-prone tries to complete a symbol
      // that isn't on the compilation classpath. This can occur when a check performs an
      // instanceof test on a symbol, which requires inspecting the transitive closure of the
      // symbol's supertypes. If javac didn't need to check the symbol's assignability
      // then a normal compilation would have succeeded, and no diagnostics will have been
      // reported yet, but we don't want to crash javac.
      log.error("proc.cant.access", e.sym, getDetailValue(e), getStackTraceAsString(e));
    } finally {
      log.useSource(originalSource);
    }
  }

  private static Object getDetailValue(CompletionFailure completionFailure) {
    try {
      // The return type of getDetailValue() changed from Object to JCDiagnostic in JDK 10,
      // but the rest of the signature is unchanged between the two versions,
      // see https://bugs.openjdk.java.net/browse/JDK-817032,
      return CompletionFailure.class.getMethod("getDetailValue").invoke(completionFailure);
    } catch (ReflectiveOperationException e) {
      throw new LinkageError(e.getMessage(), e);
    }
  }

  /** Returns true if the given source file should be excluded from analysis. */
  private boolean shouldExcludeSourceFile(CompilationUnitTree tree) {
    Pattern excludedPattern = errorProneOptions.getExcludedPattern();
    return excludedPattern != null
        && excludedPattern.matcher(ASTHelpers.getFileName(tree)).matches();
  }

  /** Returns true if all declarations inside the given compilation unit have been visited. */
  private boolean finishedCompilation(CompilationUnitTree tree) {
    OUTER:
    for (Tree decl : tree.getTypeDecls()) {
      switch (decl.getKind()) {
        case EMPTY_STATEMENT -> {
          // ignore ";" at the top level, which counts as an empty type decl
          continue OUTER;
        }
        case IMPORT -> {
          // The spec disallows mixing imports and empty top-level declarations (";"), but
          // javac has a bug that causes it to accept empty declarations interspersed with imports:
          // http://mail.openjdk.java.net/pipermail/compiler-dev/2013-August/006968.html
          //
          // Any import declarations after the first semi are incorrectly added to the list
          // of type declarations, so we have to skip over them here.
          continue OUTER;
        }
        default -> {}
      }
      if (!seen.contains(decl)) {
        return false;
      }
    }
    return true;
  }
}
