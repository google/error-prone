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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Objects;
import com.google.errorprone.scanner.Scanner;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskEvent.Kind;
import com.sun.source.util.TaskListener;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.api.MultiTaskListener;
import com.sun.tools.javac.code.Symbol.CompletionFailure;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Set;

/**
 * Used to run an error-prone analysis as a phase in the javac compiler.
 */
public class ErrorProneAnalyzer implements TaskListener {

  public static ErrorProneAnalyzer create(Scanner scanner) {
    return create(scanner, null);
  }

  public static ErrorProneAnalyzer create(
      Scanner scanner, SearchResultsPrinter searchResultsPrinter) {
    checkNotNull(scanner);
    return new ErrorProneAnalyzer(scanner, searchResultsPrinter);
  }

  /**
   * Initializes the analyzer with the current compilation context. (E.g. for the current
   * annotation processing round.)
   */
  public ErrorProneAnalyzer init(Context context) {
    this.initialized = true;
    this.context = context;
    this.log = Log.instance(context);
    this.compiler = JavaCompiler.instance(context);
    return this;
  }

  public ErrorProneAnalyzer register(Context context) {
    init(context);
    MultiTaskListener.instance(context).add(this);
    return this;
  }

  private final Scanner errorProneScanner;
  // If matchListener != null, then we are in search mode.
  private final SearchResultsPrinter resultsPrinter;
  // The set of trees that have already been scanned.
  private final Set<Tree> seen = new HashSet<>();

  private Context context;
  private Log log;
  private JavaCompiler compiler;
  private boolean initialized = false;

  private ErrorProneAnalyzer(Scanner scanner, SearchResultsPrinter resultsPrinter) {
    this.errorProneScanner = scanner;
    this.resultsPrinter = resultsPrinter;
  }

  private static class DeclFreeCompilationUnitWrapper extends JCCompilationUnit {
    protected DeclFreeCompilationUnitWrapper(JCCompilationUnit original) {
      super(
          original.packageAnnotations,
          original.pid,
          com.sun.tools.javac.util.List.<JCTree>nil(),
          original.sourcefile,
          original.packge,
          original.namedImportScope,
          original.starImportScope);
    }
  }

  @Override
  public void started(TaskEvent taskEvent) {
    checkState(initialized);
  }

  @Override
  public void finished(TaskEvent taskEvent) {
    if (taskEvent.getKind() == Kind.ANALYZE) {
      // One TaskEvent is created per class declaration after FLOW is finished, but the TaskEvent
      // only provides the CompilationUnitTree and the symbol (element) for the class declaration.
      // We have to search for the class decl manually...
      // TODO(user): suggest to upstream that TaskEvents provide a TreePath?
      JCClassDecl currentClassTree = null;
      for (Tree declTree : taskEvent.getCompilationUnit().getTypeDecls()) {
        if (declTree instanceof JCClassDecl) {
          JCClassDecl classTree = (JCClassDecl) declTree;
          if (Objects.equal(classTree.sym, taskEvent.getTypeElement())) {
            currentClassTree = classTree;
          }
        }
      }

      TreePath path = currentClassTree != null
          ? TreePath.getPath(taskEvent.getCompilationUnit(), currentClassTree)
          : new TreePath(taskEvent.getCompilationUnit());
      reportReadyForAnalysis(taskEvent, path, compiler.errorCount() > 0);
    }
  }

  /**
   * Reports that a class is ready for error-prone to analyze.
   *
   * @param path the path from the compilation unit to the class declaration
   * @param hasErrors true if errors have been reported during the compilation
   */
  public void reportReadyForAnalysis(TaskEvent taskEvent, TreePath path, boolean hasErrors) {
    try {
      if (seen.add(path.getCompilationUnit())) {
        // Visit the compilation unit separately from the enclosed class declarations, and
        // prevent scanners from accessing the (incomplete) class declarations.
        TreePath rootPath = new TreePath(new DeclFreeCompilationUnitWrapper(
            (JCCompilationUnit) path.getCompilationUnit()));
        errorProneScanner.scan(rootPath, createVisitorState(path.getCompilationUnit()));
      }

      if (path.getLeaf().getKind() == Tree.Kind.COMPILATION_UNIT) {
        // If we're visiting, e.g., a package-info.java file with no class decls, then we're done.
        return;
      }

      if (!seen.add(path.getLeaf())) {
        throw new IllegalStateException("Duplicate FLOW event for: " + taskEvent.getTypeElement());
      }

      errorProneScanner.scan(path, createVisitorState(path.getCompilationUnit()));

    } catch (CompletionFailure e) {
      // A CompletionFailure can be triggered when error-prone tries to complete a symbol
      // that isn't on the compilation classpath. This can occur when a check performs an
      // instanceof test on a symbol, which requires inspecting the transitive closure of the
      // symbol's supertypes. If javac didn't need to check the symbol's assignability
      // then a normal compilation would have succeeded, and no diagnostics will have been
      // reported yet, but we don't want to crash javac.
      StringWriter message = new StringWriter();
      e.printStackTrace(new PrintWriter(message));
      log.error("proc.cant.access", e.sym, e.getDetailValue(), message.toString());
    } catch (RuntimeException e) {
      // If there is a RuntimeException in an analyzer, swallow it if there are other compiler
      // errors.  This prevents javac from exiting with code 4, Abnormal Termination.
      if (!hasErrors) {
        throw e;
      }
    }
  }

  /**
   * Create a VisitorState object from a compilation unit.
   */
  private VisitorState createVisitorState(CompilationUnitTree compilation) {
    if (resultsPrinter != null) {
      resultsPrinter.setCompilationUnit(compilation.getSourceFile());
      return new VisitorState(context, resultsPrinter);
    } else {
      DescriptionListener logReporter = new JavacErrorDescriptionListener(
          log,
          ((JCCompilationUnit) compilation).endPositions,
          compilation.getSourceFile());
      return new VisitorState(context, logReporter);
    }
  }
}
