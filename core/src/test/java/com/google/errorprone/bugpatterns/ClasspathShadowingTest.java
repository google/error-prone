/*
 * Copyright 2026 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.CompilationTestHelper;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/// [ClasspathShadowing]Test
public class ClasspathShadowingTest {

  @Rule public final TemporaryFolder tempFolder = new TemporaryFolder();

  private final CompilationTestHelper testHelper =
      CompilationTestHelper.newInstance(ClasspathShadowing.class, getClass());

  @Test
  public void positive_shadowsClasspathClass() {
    testHelper
        .withClasspath(ImmutableList.class)
        .addSourceLines(
            "com/google/common/collect/ImmutableList.java",
            """
            package com.google.common.collect;

            // BUG: Diagnostic contains: Top-level class shadows a class on the compilation classpath
            public class ImmutableList<E> {}
            """)
        .doTest();
  }

  @Test
  public void positive_modulePath() throws Exception {
    Path classesDir = tempFolder.newFolder("dep_classes").toPath();
    Path depSourceDir = tempFolder.newFolder("dep_src").toPath();
    Files.writeString(
        depSourceDir.resolve("module-info.java"),
        """
        module dep.mod {
          exports com.example.dep to other.mod;
        }
        """);
    Path pkgDir = Files.createDirectories(depSourceDir.resolve("com/example/dep"));
    Files.writeString(
        pkgDir.resolve("Dep.java"),
        """
        package com.example.dep;

        public class Dep {}
        """);
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    int exitCode =
        compiler.run(
            /* in= */ null,
            /* out= */ null,
            /* err= */ null,
            "-d",
            classesDir.toString(),
            depSourceDir.resolve("module-info.java").toString(),
            pkgDir.resolve("Dep.java").toString());
    assertThat(exitCode).isEqualTo(0);

    Path moduleJar = tempFolder.newFile("dep.mod.jar").toPath();
    try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(moduleJar))) {
      for (String entry : ImmutableList.of("module-info.class", "com/example/dep/Dep.class")) {
        jos.putNextEntry(new JarEntry(entry));
        Files.copy(classesDir.resolve(entry), jos);
      }
    }

    testHelper
        .withClasspath()
        .addSourceLines(
            "module-info.java",
            """
            module app.mod {
              requires dep.mod;
            }
            """)
        .addSourceLines(
            "com/example/dep/Dep.java",
            """
            package com.example.dep;

            // BUG: Diagnostic contains: Top-level class shadows a class on the compilation classpath
            public class Dep {}
            """)
        .addSourceLines(
            "com/example/dep/Other.java",
            """
            package com.example.dep;

            public class Other {}
            """)
        .setArgs("--module-path", moduleJar.toString())
        .doTest();
  }

  @Test
  public void negative_namedModuleSelfCompilation() {
    testHelper
        .withClasspath()
        .addSourceLines(
            "module-info.java",
            """
            module app.mod {
              exports com.example.app;
            }
            """)
        .addSourceLines(
            "com/example/app/App.java",
            """
            package com.example.app;

            public class App {}
            """)
        .doTest();
  }

  @Test
  public void negative_platformClassesNotFlagged() {
    testHelper
        .withClasspath()
        .addSourceLines(
            "javax/crypto/Cipher.java",
            """
            package javax.crypto;

            public class Cipher {}
            """)
        .setArgs("-source", "8", "-target", "8")
        .doTest();
  }

  @Test
  public void negative_systemModulesNotFlagged() {
    testHelper
        .withClasspath()
        .addSourceLines(
            "sun/nio/ch/DirectBuffer.java",
            """
            package sun.nio.ch;

            public interface DirectBuffer {}
            """)
        .doTest();
  }

  @Test
  public void negative_distinctClassInSamePackage() {
    testHelper
        .withClasspath(ImmutableList.class)
        .addSourceLines(
            "com/google/common/collect/NotOnClasspath.java",
            """
            package com.google.common.collect;

            public class NotOnClasspath {}
            """)
        .doTest();
  }

  @Test
  public void negative_nestedClassWithSameSimpleName() {
    testHelper
        .withClasspath(ImmutableList.class)
        .addSourceLines(
            "com/google/common/collect/NotOnClasspath.java",
            """
            package com.google.common.collect;

            public class NotOnClasspath {
              public static class ImmutableList {}
            }
            """)
        .doTest();
  }

  @Test
  public void negative_packageInfo() {
    testHelper
        .withClasspath(ImmutableList.class)
        .addSourceLines(
            "com/google/common/collect/package-info.java",
            """
            package com.google.common.collect;
            """)
        .doTest();
  }

  @Test
  public void negative_classOutputOnClasspath() throws Exception {
    Path classesDir = tempFolder.newFolder("classes").toPath();
    Path srcDir = tempFolder.newFolder("src").toPath();
    Path pkgDir = Files.createDirectories(srcDir.resolve("com/example"));
    Files.writeString(
        pkgDir.resolve("App.java"),
        """
        package com.example;

        public class App {}
        """);
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    int exitCode =
        compiler.run(
            /* in= */ null,
            /* out= */ null,
            /* err= */ null,
            "-d",
            classesDir.toString(),
            pkgDir.resolve("App.java").toString());
    assertThat(exitCode).isEqualTo(0);
    assertThat(Files.exists(classesDir.resolve("com/example/App.class"))).isTrue();

    testHelper
        .withClasspath()
        .addSourceLines(
            "com/example/App.java",
            """
            package com.example;

            public class App {}
            """)
        .setArgs("-d", classesDir.toString(), "-cp", classesDir.toString())
        .doTest();
  }

  @Test
  public void positive_classOutputOnClasspathAheadOfShadowedJar() throws Exception {
    Path classesDir = tempFolder.newFolder("classes").toPath();
    Path srcDir = tempFolder.newFolder("src").toPath();
    Path srcPkgDir = Files.createDirectories(srcDir.resolve("com/example"));
    Files.writeString(
        srcPkgDir.resolve("App.java"),
        """
        package com.example;

        public class App {}
        """);
    Files.writeString(
        srcPkgDir.resolve("LocalOnly.java"),
        """
        package com.example;

        public class LocalOnly {}
        """);
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    int exitCode =
        compiler.run(
            /* in= */ null,
            /* out= */ null,
            /* err= */ null,
            "-d",
            classesDir.toString(),
            srcPkgDir.resolve("App.java").toString(),
            srcPkgDir.resolve("LocalOnly.java").toString());
    assertThat(exitCode).isEqualTo(0);

    Path depClassesDir = tempFolder.newFolder("dep_classes").toPath();
    Path depSrcDir = tempFolder.newFolder("dep_src").toPath();
    Path depPkgDir = Files.createDirectories(depSrcDir.resolve("com/example"));
    Files.writeString(
        depPkgDir.resolve("App.java"),
        """
        package com.example;

        public class App {}
        """);
    Files.writeString(
        depPkgDir.resolve("Unrelated.java"),
        """
        package com.example;

        public class Unrelated {}
        """);
    exitCode =
        compiler.run(
            /* in= */ null,
            /* out= */ null,
            /* err= */ null,
            "-d",
            depClassesDir.toString(),
            depPkgDir.resolve("App.java").toString(),
            depPkgDir.resolve("Unrelated.java").toString());
    assertThat(exitCode).isEqualTo(0);

    Path depJar = tempFolder.newFile("dep.jar").toPath();
    try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(depJar))) {
      for (String entry :
          ImmutableList.of("com/example/App.class", "com/example/Unrelated.class")) {
        jos.putNextEntry(new JarEntry(entry));
        Files.copy(depClassesDir.resolve(entry), jos);
      }
    }

    testHelper
        .withClasspath()
        .addSourceLines(
            "com/example/App.java",
            """
            package com.example;

            // BUG: Diagnostic contains: Top-level class shadows a class on the compilation classpath
            public class App {}
            """)
        .addSourceLines(
            "com/example/LocalOnly.java",
            """
            package com.example;

            public class LocalOnly {}
            """)
        .setArgs("-d", classesDir.toString(), "-cp", classesDir + File.pathSeparator + depJar)
        .doTest();
  }

  @Test
  public void suppressed() {
    testHelper
        .withClasspath(ImmutableList.class)
        .addSourceLines(
            "com/google/common/collect/ImmutableList.java",
            """
            package com.google.common.collect;

            @SuppressWarnings("ClasspathShadowing")
            public class ImmutableList<E> {}
            """)
        .doTest();
  }
}
