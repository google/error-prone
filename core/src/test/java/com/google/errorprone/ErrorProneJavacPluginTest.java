/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

import static com.google.common.collect.MoreCollectors.onlyElement;
import static com.google.common.truth.Truth.assertThat;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Locale.ENGLISH;

import com.google.common.collect.ImmutableList;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.sun.source.util.JavacTask;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.util.Context;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link ErrorProneJavacPlugin}Test */
@RunWith(JUnit4.class)
public class ErrorProneJavacPluginTest {
  @Test
  public void hello() throws IOException {
    FileSystem fileSystem = Jimfs.newFileSystem(Configuration.unix());
    Path source = fileSystem.getPath("Test.java");
    Files.write(
        source,
        ImmutableList.of(
            "import java.util.HashSet;",
            "import java.util.Set;",
            "class Test {",
            "  public static void main(String[] args) {",
            "    Set<Short> s = new HashSet<>();",
            "    for (short i = 0; i < 100; i++) {",
            "      s.add(i);",
            "      s.remove(i - 1);",
            "    }",
            "    System.out.println(s.size());",
            "  }",
            "}"),
        UTF_8);
    JavacFileManager fileManager = new JavacFileManager(new Context(), false, UTF_8);
    DiagnosticCollector<JavaFileObject> diagnosticCollector = new DiagnosticCollector<>();
    JavacTask task =
        JavacTool.create()
            .getTask(
                null,
                fileManager,
                diagnosticCollector,
                ImmutableList.of("-Xplugin:ErrorProne"),
                ImmutableList.of(),
                fileManager.getJavaFileObjects(source));
    assertThat(task.call()).isFalse();
    Diagnostic<? extends JavaFileObject> diagnostic =
        diagnosticCollector
            .getDiagnostics()
            .stream()
            .filter(d -> d.getKind() == Diagnostic.Kind.ERROR)
            .collect(onlyElement());
    assertThat(diagnostic.getMessage(ENGLISH)).contains("[CollectionIncompatibleType]");
  }
}
