/*
 * Copyright 2013 The Error Prone Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.errorprone.refaster;


import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.CharStreams;
import com.google.errorprone.FileManagers;
import com.google.errorprone.apply.SourceFile;
import com.google.testing.compile.JavaFileObjects;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.TreeInfo;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.util.Context;
import java.io.IOException;
import java.util.Map;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;

/**
 * Abstract skeleton for tests that run the compiler on the fly.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
public class CompilerBasedTest {
  protected Context context;
  protected SourceFile sourceFile;
  protected Iterable<JCCompilationUnit> compilationUnits;
  private Map<String, JCMethodDecl> methods;

  protected void compile(TreeScanner scanner, JavaFileObject fileObject) {
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
                ImmutableList.of(fileObject));
    try {
      this.sourceFile = SourceFile.create(fileObject);
      Iterable<? extends CompilationUnitTree> trees = task.parse();
      task.analyze();
      for (CompilationUnitTree tree : trees) {
        scanner.scan((JCCompilationUnit) tree);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    this.context = task.getContext();
  }

  protected void compile(TreeScanner scanner, String... lines) {
    compile(scanner, JavaFileObjects.forSourceLines("CompilerBasedTestInput", lines));
  }

  protected void compile(JavaFileObject fileObject) {
    final ImmutableMap.Builder<String, JCMethodDecl> methodsBuilder = ImmutableMap.builder();
    final ImmutableList.Builder<JCCompilationUnit> compilationUnitsBuilder =
        ImmutableList.builder();
    compile(
        new TreeScanner() {
          @Override
          public void visitMethodDef(JCMethodDecl tree) {
            if (!TreeInfo.isConstructor(tree)) {
              methodsBuilder.put(tree.getName().toString(), tree);
            }
          }

          @Override
          public void visitTopLevel(JCCompilationUnit tree) {
            compilationUnitsBuilder.add(tree);
            super.visitTopLevel(tree);
          }
        },
        fileObject);
    this.methods = methodsBuilder.build();
    this.compilationUnits = compilationUnitsBuilder.build();
  }

  protected void compile(String... lines) {
    compile(JavaFileObjects.forSourceLines("CompilerBasedTestInput", lines));
  }

  protected JCMethodDecl getMethodDeclaration(String name) {
    return methods.get(name);
  }
}
