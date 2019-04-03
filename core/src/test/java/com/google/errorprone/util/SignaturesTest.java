/*
 * Copyright 2019 The Error Prone Authors.
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

package com.google.errorprone.util;

import static com.google.common.truth.Truth.assertThat;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.collect.ImmutableList;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.sun.source.tree.NewClassTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskEvent.Kind;
import com.sun.source.util.TaskListener;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.util.Context;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link Signatures}Test */
@RunWith(JUnit4.class)
public class SignaturesTest {

  @Test
  public void prettyMethodSignature() throws Exception {
    FileSystem fileSystem = Jimfs.newFileSystem(Configuration.unix());
    Path source = fileSystem.getPath("Test.java");
    Files.write(
        source,
        ImmutableList.of(
            "class Test {", //
            "  void f() {",
            "    new Test();",
            "    new Test() {};",
            "  }",
            "}"),
        UTF_8);
    JavacFileManager fileManager = new JavacFileManager(new Context(), false, UTF_8);
    List<String> signatures = new ArrayList<>();
    JavacTask task =
        JavacTool.create()
            .getTask(
                /* out= */ null,
                fileManager,
                /* diagnosticListener= */ null,
                /* options= */ ImmutableList.of(),
                /* classes= */ ImmutableList.of(),
                fileManager.getJavaFileObjects(source));
    task.addTaskListener(
        new TaskListener() {
          @Override
          public void finished(TaskEvent e) {
            if (e.getKind() != Kind.ANALYZE) {
              return;
            }
            new TreePathScanner<Void, Void>() {
              @Override
              public Void visitNewClass(NewClassTree node, Void unused) {
                signatures.add(
                    Signatures.prettyMethodSignature(
                        (ClassSymbol) e.getTypeElement(), ASTHelpers.getSymbol(node)));
                return super.visitNewClass(node, null);
              }
            }.scan(e.getCompilationUnit(), null);
          }
        });
    assertThat(task.call()).isTrue();
    assertThat(signatures).containsExactly("Test()", "Test()");
  }
}
