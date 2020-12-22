/*
 * Copyright 2014 The Error Prone Authors.
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

package com.google.errorprone.refaster;

import static com.google.testing.compile.JavaFileObjects.forSourceString;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.io.CharStreams;
import com.google.errorprone.CodeTransformer;
import com.google.errorprone.FileManagers;
import com.google.errorprone.apply.DescriptionBasedDiff;
import com.google.errorprone.apply.ImportOrganizer;
import com.google.errorprone.apply.SourceFile;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import java.io.IOException;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;

/**
 * Test helper for {@link CodeTransformer} implementations.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@AutoValue
public abstract class CodeTransformerTestHelper {
  public static CodeTransformerTestHelper create(CodeTransformer transformer) {
    return new AutoValue_CodeTransformerTestHelper(transformer);
  }

  abstract CodeTransformer transformer();

  public JavaFileObject transform(JavaFileObject original) {
    JavaCompiler compiler = JavacTool.create();
    DiagnosticCollector<JavaFileObject> diagnosticsCollector = new DiagnosticCollector<>();
    StandardJavaFileManager fileManager = FileManagers.testFileManager();
    JavacTaskImpl task =
        (JavacTaskImpl)
            compiler.getTask(
                CharStreams.nullWriter(),
                fileManager,
                diagnosticsCollector,
                ImmutableList.<String>of(),
                null,
                ImmutableList.of(original));

    try {
      SourceFile sourceFile = SourceFile.create(original);
      Iterable<? extends CompilationUnitTree> trees = task.parse();
      task.analyze();
      JCCompilationUnit tree =
          Iterables.getOnlyElement(Iterables.filter(trees, JCCompilationUnit.class));
      DescriptionBasedDiff diff =
          DescriptionBasedDiff.create(tree, ImportOrganizer.STATIC_FIRST_ORGANIZER);
      transformer().apply(new TreePath(tree), task.getContext(), diff);
      diff.applyDifferences(sourceFile);

      return forSourceString(
          Iterables.getOnlyElement(Iterables.filter(tree.getTypeDecls(), JCClassDecl.class))
              .sym
              .getQualifiedName()
              .toString(),
          sourceFile.getSourceText());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
