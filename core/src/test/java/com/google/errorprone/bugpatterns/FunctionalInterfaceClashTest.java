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

package com.google.errorprone.bugpatterns;

import com.google.common.io.ByteStreams;
import com.google.errorprone.CompilationTestHelper;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class FunctionalInterfaceClashTest {

  @Rule public final TemporaryFolder tempFolder = new TemporaryFolder();

  private final CompilationTestHelper testHelper =
      CompilationTestHelper.newInstance(FunctionalInterfaceClash.class, getClass());

  @Test
  public void positive() {
    testHelper
        .addSourceLines(
            "Test.java", //
            "import java.util.function.Function;",
            "import java.util.function.Consumer;",
            "public class Test {",
            "  // BUG: Diagnostic contains: disambiguate with: foo(Function<String, String>)",
            "  void foo(Consumer<String> x) {}",
            "  void foo(Function<String, String> c) {}",
            "}")
        .doTest();
  }

  @Test
  public void positiveNullary() {
    testHelper
        .addSourceLines(
            "Test.java", //
            "import java.util.concurrent.Callable;",
            "public class Test {",
            "  interface MyCallable {",
            "    String call();",
            "  }",
            "  // BUG: Diagnostic contains: disambiguate with: foo(MyCallable)",
            "  void foo(Callable<String> x) {}",
            "  void foo(MyCallable c) {}",
            "}")
        .doTest();
  }

  @Test
  public void positiveInherited() {
    testHelper
        .addSourceLines(
            "Super.java", //
            "import java.util.function.Function;",
            "class Super {",
            "  void foo(Function<String, String> x) {}",
            "}")
        .addSourceLines(
            "Test.java", //
            "import java.util.function.Consumer;",
            "public class Test extends Super {",
            "  // BUG: Diagnostic contains: disambiguate with: Super.foo(Function<String, String>)",
            "  void foo(Consumer<String> c) {}",
            "}")
        .doTest();
  }

  @Test
  public void positiveArgs() {
    testHelper
        .addSourceLines(
            "Test.java", //
            "import java.util.function.Function;",
            "import java.util.function.Consumer;",
            "public class Test {",
            "  // BUG: Diagnostic contains: disambiguate with: foo(Function<String, Integer>)",
            "  void foo(Consumer<String> c) {}",
            "  void foo(Function<String, Integer> f) {}",
            "}")
        .doTest();
  }

  @Test
  public void negativeOverride() {
    testHelper
        .addSourceLines(
            "Super.java", //
            "import java.util.function.Consumer;",
            "class Super {",
            "  void foo(Consumer<String> x) {}",
            "}")
        .addSourceLines(
            "Test.java", //
            "import java.util.function.Consumer;",
            "public class Test extends Super {",
            "  void foo(Consumer<String> x) {}",
            "}")
        .doTest();
  }

  @Test
  public void negativeSuperConstructor() {
    testHelper
        .addSourceLines(
            "Super.java", //
            "import java.util.function.Function;",
            "class Super {",
            "  Super(Function<String, String> r) {}",
            "}")
        .addSourceLines(
            "Test.java", //
            "import java.util.function.Consumer;",
            "public class Test extends Super {",
            "  Test(Consumer<String> r) { super(null); }",
            "}")
        .doTest();
  }

  @Test
  public void positiveConstructor() {
    testHelper
        .addSourceLines(
            "Test.java", //
            "import java.util.function.Function;",
            "import java.util.function.Consumer;",
            "public class Test {",
            "  // BUG: Diagnostic contains: disambiguate with: Test(Function<String, String>)",
            "  Test(Consumer<String> r) {}",
            "  Test(Function<String, String> c) {}",
            "}")
        .doTest();
  }

  @Test
  public void positiveStatic() {
    testHelper
        .addSourceLines(
            "Test.java", //
            "import java.util.function.Function;",
            "import java.util.function.Consumer;",
            "public class Test {",
            "  // BUG: Diagnostic contains: disambiguate with: foo(Function<String, String>)",
            "  static void foo(Consumer<String> x) {}",
            "  void foo(Function<String, String> c) {}",
            "}")
        .doTest();
  }

  public static class Other {}

  public static class Super {
    public void f(Other o) {}
  }

  static void addClassToJar(JarOutputStream jos, Class<?> clazz) throws IOException {
    String entryPath = clazz.getName().replace('.', '/') + ".class";
    try (InputStream is = clazz.getClassLoader().getResourceAsStream(entryPath)) {
      jos.putNextEntry(new JarEntry(entryPath));
      ByteStreams.copy(is, jos);
    }
  }

  @Test
  public void incompleteClasspath() throws Exception {
    File libJar = tempFolder.newFile("lib.jar");
    try (FileOutputStream fis = new FileOutputStream(libJar);
        JarOutputStream jos = new JarOutputStream(fis)) {
      addClassToJar(jos, getClass());
      addClassToJar(jos, Super.class);
    }
    testHelper
        .addSourceLines(
            "Test.java",
            "import " + Super.class.getCanonicalName() + ";",
            "class Test extends Super {}")
        .setArgs(Arrays.asList("-cp", libJar.toString()))
        .doTest();
  }

  // TODO(b/38460312): Fix and enable test
  @Test
  @Ignore
  public void suppressWarningsOnMethod() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import java.lang.SuppressWarnings;",
            "import java.util.function.Function;",
            "import java.util.function.Consumer;",
            "public class Test {",
            "  @SuppressWarnings(\"FunctionalInterfaceClash\")",
            "  void foo(Consumer<String> x) {}",
            "  void foo(Function<String, String> c) {}",
            "}")
        .doTest();
  }

  @Test
  public void shouldIgnore_transitiveInheritanceWithExpandedVisibility() {
    testHelper
        .addSourceLines(
            "pkg1/FunctionalInterface.java",
            "package pkg1;",
            "public interface FunctionalInterface {",
            "  String apply(String s);",
            "}")
        .addSourceLines(
            "pkg2/BaseClass.java",
            "package pkg2;",
            "import pkg1.FunctionalInterface;",
            "public abstract class BaseClass {",
            "  abstract String doIt(FunctionalInterface fi);",
            "}")
        .addSourceLines(
            "pkg2/DerivedClass.java",
            "package pkg2;",
            "import pkg1.FunctionalInterface;",
            "public class DerivedClass extends BaseClass {",
            "  @Override public String doIt(FunctionalInterface fi) {",
            "    return null;",
            "  }",
            "}")
        .addSourceLines(
            "pkg3/Test.java",
            "package pkg3;",
            "import pkg1.FunctionalInterface;",
            "import pkg2.DerivedClass;",
            "public class Test {",
            "  DerivedClass getDerivedClass() {",
            "    return new DerivedClass() {",
            "      @Override public String doIt(FunctionalInterface fi) {",
            "        return null;",
            "      }",
            "    };",
            "  }",
            "}")
        .doTest();
  }
}
