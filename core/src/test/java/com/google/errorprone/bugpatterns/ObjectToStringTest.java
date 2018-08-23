/*
 * Copyright 2018 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;

import com.google.common.io.ByteStreams;
import com.google.errorprone.BugPattern;
import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.scanner.ScannerSupplier;
import com.sun.source.tree.ClassTree;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** @author bhagwani@google.com (Sumit Bhagwani) */
@RunWith(JUnit4.class)
public class ObjectToStringTest {

  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper = CompilationTestHelper.newInstance(ObjectToString.class, getClass());
  }

  @Test
  public void testPositiveCase() {
    compilationHelper.addSourceFile("ObjectToStringPositiveCases.java").doTest();
  }

  @Test
  public void testNegativeCase() {
    compilationHelper.addSourceFile("ObjectToStringNegativeCases.java").doTest();
  }

  @Rule public final TemporaryFolder tempFolder = new TemporaryFolder();

  /** A class that will be missing at compile-time for {@link #testIncompleteClasspath}. */
  public static class One {}

  /** A test class for {@link #testIncompleteClasspath}. */
  public abstract static class TestLib {

    /** Another test class for {@link #testIncompleteClasspath}. */
    public static final class Two extends One {
      @Override
      public String toString() {
        return "";
      }
    }

    public abstract Two f();
  }

  static void addClassToJar(JarOutputStream jos, Class<?> clazz) throws IOException {
    String entryPath = clazz.getName().replace('.', '/') + ".class";
    try (InputStream is = clazz.getClassLoader().getResourceAsStream(entryPath)) {
      jos.putNextEntry(new JarEntry(entryPath));
      ByteStreams.copy(is, jos);
    }
  }

  // A bugchecker that eagerly completes the missing symbol for testIncompleteClasspath below,
  // to avoid the CompletionFailure being reported later.
  /** A checker for {@link #testIncompleteClasspath}. */
  @BugPattern(name = "CompletionChecker", summary = "", severity = ERROR)
  public static class CompletionChecker extends BugChecker implements ClassTreeMatcher {
    @Override
    public Description matchClass(ClassTree tree, VisitorState state) {
      state.getSymbolFromString(One.class.getName());
      return Description.NO_MATCH;
    }
  }

  // don't complain if we can't load the type hierarchy of a class that is toString()'d
  @Test
  public void testIncompleteClasspath() throws Exception {
    File libJar = tempFolder.newFile("lib.jar");
    try (FileOutputStream fis = new FileOutputStream(libJar);
        JarOutputStream jos = new JarOutputStream(fis)) {
      addClassToJar(jos, ObjectToStringTest.class);
      addClassToJar(jos, TestLib.class);
      addClassToJar(jos, TestLib.Two.class);
    }
    CompilationTestHelper.newInstance(
            ScannerSupplier.fromBugCheckerClasses(ObjectToString.class, CompletionChecker.class),
            getClass())
        .addSourceLines(
            "Test.java",
            "import " + TestLib.class.getCanonicalName() + ";",
            "class Test {",
            "  String f(TestLib lib) {",
            "    return \"\" + lib.f();",
            "  }",
            "}")
        .setArgs(Arrays.asList("-cp", libJar.toString()))
        .doTest();
  }
}
