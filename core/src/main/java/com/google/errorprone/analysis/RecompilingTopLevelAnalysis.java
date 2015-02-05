/*
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.errorprone.analysis;

import com.google.auto.value.AutoValue;
import com.google.common.base.Predicate;
import com.google.common.io.CharStreams;
import com.google.errorprone.DescriptionListener;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.Replacement;
import com.google.errorprone.matchers.Description;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.JavacTask;
import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.util.Context;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.NavigableSet;
import java.util.TreeSet;

import javax.annotation.Nullable;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.SimpleJavaFileObject;

/**
 * Wrapper around a {@code TopLevelAnalysis} that filters analysis results to remove fixes that do
 * not compile.
 *
 * @author Louis Wasserman
 */
@AutoValue
public abstract class RecompilingTopLevelAnalysis implements TopLevelAnalysis {
  public static RecompilingTopLevelAnalysis create(TopLevelAnalysis analysis) {
    return new AutoValue_RecompilingTopLevelAnalysis(analysis);
  }
  
  RecompilingTopLevelAnalysis() {}
  
  abstract TopLevelAnalysis analysis();

  @Override
  public void analyze(final CompilationUnitTree compilationUnit, final Context context,
      AnalysesConfig configuration, final DescriptionListener listener) {
    analysis().analyze(compilationUnit, context, configuration, new DescriptionListener() {

      @Override
      public void onDescribed(Description description) {
        listener.onDescribed(description.filterFixes(new Predicate<Fix>() {
          @Override
          public boolean apply(Fix fix) {
            return compiles(fix, (JCCompilationUnit) compilationUnit, context);
          }
        }));
      }
    });

    
  }

  private enum ByStartPosition implements Comparator<Replacement> {
    INSTANCE {
      @Override
      public int compare(Replacement o1, Replacement o2) {
        return Integer.compare(o1.startPosition(), o2.startPosition());
      }
    };
  }

  private static boolean compiles(Fix fix, JCCompilationUnit compilationUnit, Context context) {
    final NavigableSet<Replacement> replacements = new TreeSet<>(ByStartPosition.INSTANCE);
    replacements.addAll(fix.getReplacements(compilationUnit.endPositions));
    JavaFileObject modifiedFile = compilationUnit.getSourceFile();

    if (replacements.isEmpty()) {
      return true;
    }

    JavacTaskImpl javacTask = (JavacTaskImpl) context.get(JavacTask.class);
    if (javacTask == null) {
      throw new IllegalArgumentException("No JavacTask in context.");
    }
    List<JavaFileObject> fileObjects = new ArrayList<>(fileObjectsForTask(javacTask));
    for (int i = 0; i < fileObjects.size(); i++) {
      final JavaFileObject oldFile = fileObjects.get(i);
      if (modifiedFile.toUri().equals(oldFile.toUri())) {
        fileObjects.set(i, new SimpleJavaFileObject(modifiedFile.toUri(), Kind.SOURCE) {
          @Override
          public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
            StringBuilder builder = new StringBuilder(oldFile.getCharContent(ignoreEncodingErrors));
            for (Replacement replacement : replacements.descendingSet()) {
              builder.replace(replacement.startPosition(), replacement.endPosition(),
                  replacement.replaceWith());
            }
            return builder;
          }
        });
        break;
      }
    }
    JavaCompiler compiler = JavacTool.create();
    DiagnosticCollector<JavaFileObject> diagnosticListener = new DiagnosticCollector<>();
    JavacTask newTask = (JavacTask) compiler.getTask(CharStreams.nullWriter(),
        context.get(JavaFileManager.class),
        diagnosticListener,
        asListOrNull(argsForTask(javacTask)),
        asListOrNull(classesForTask(javacTask)),
        fileObjects);
    try {
      newTask.analyze();
    } catch (Throwable e) {
      return false;
    }
    return countErrors(diagnosticListener) == 0;
  }

  private static int countErrors(DiagnosticCollector<JavaFileObject> diagnosticCollector) {
    int errorCount = 0;
    for (Diagnostic<? extends JavaFileObject> diag : diagnosticCollector.getDiagnostics()) {
      if (diag.getKind() == Diagnostic.Kind.ERROR) {
        errorCount++;
      }
    }
    return errorCount;
  }

  private static Object getFieldReflectively(JavacTaskImpl task, String fieldName) {
    try {
      Field field = JavacTaskImpl.class.getDeclaredField(fieldName);
      field.setAccessible(true);
      return field.get(task);
    } catch (ReflectiveOperationException | SecurityException | IllegalArgumentException e) {
      throw new RuntimeException(e);
    }
  }

  private static <E> List<E> asListOrNull(@Nullable E[] array) {
    return (array == null) ? null : Arrays.asList(array);
  }

  @SuppressWarnings("unchecked")
  private static List<JavaFileObject> fileObjectsForTask(JavacTaskImpl task) {
    return (List<JavaFileObject>) getFieldReflectively(task, "fileObjects");
  }

  private static String[] argsForTask(JavacTaskImpl task) {
    return (String[]) getFieldReflectively(task, "args");
  }

  private static String[] classesForTask(JavacTaskImpl task) {
    return (String[]) getFieldReflectively(task, "classNames");
  }

}
