/*
 * Copyright 2017 The Error Prone Authors.
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

import static com.google.errorprone.BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH;

import com.google.common.io.ByteStreams;
import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link JdkObsolete}Test */
@RunWith(JUnit4.class)
public class JdkObsoleteTest {
  private final CompilationTestHelper testHelper =
      CompilationTestHelper.newInstance(JdkObsolete.class, getClass());

  @Test
  public void positive() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import java.nio.file.Path;",
            "class Test {",
            "  {",
            "    // BUG: Diagnostic contains:",
            "    new java.util.LinkedList<>();",
            "    // BUG: Diagnostic contains:",
            "    new java.util.Stack<>();",
            "    // BUG: Diagnostic contains:",
            "    new java.util.Vector<>();",
            "    // BUG: Diagnostic contains:",
            "    new java.util.Hashtable<>();",
            "    // BUG: Diagnostic contains:",
            "    new StringBuffer();",
            "    // BUG: Diagnostic contains:",
            "    new java.util.Hashtable<Object, Object>() {};",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void stringBuffer_appendReplacement() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import java.util.regex.Matcher;",
            "class Test {",
            "  void f(Matcher m) {",
            "    StringBuffer sb = new StringBuffer();",
            "    m.appendReplacement(sb, null);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void stringBuffer_appendTail() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import java.util.regex.Matcher;",
            "class Test {",
            "  void f(Matcher m) {",
            "    StringBuffer sb = new StringBuffer();",
            "    m.appendTail(sb);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveExtends() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import java.nio.file.Path;",
            "class Test {",
            "  // BUG: Diagnostic contains:",
            "  abstract class A implements java.util.Enumeration<Object> {}",
            "  // BUG: Diagnostic contains:",
            "  abstract class B implements java.util.SortedSet<Object> {}",
            "  // BUG: Diagnostic contains:",
            "  abstract class C implements java.util.SortedMap<Object, Object> {}",
            "  // BUG: Diagnostic contains:",
            "  abstract class D extends java.util.Dictionary<Object, Object> {}",
            "}")
        .doTest();
  }

  @Test
  public void refactoring() {
    BugCheckerRefactoringTestHelper.newInstance(new JdkObsolete(), getClass())
        .addInputLines(
            "in/Test.java", //
            "import java.util.*;",
            "class Test {",
            "  Deque<Object> d = new LinkedList<>();",
            "  List<Object> l = new LinkedList<>();",
            "  {",
            "    l = new LinkedList<>();",
            "  }",
            "  LinkedList<Object> ll = new LinkedList<>();",
            "  List<Object> lll = new LinkedList<Object>() {{",
            "    add(null); // yikes",
            "  }};",
            "}")
        .addOutputLines(
            "out/Test.java", //
            "import java.util.*;",
            "class Test {",
            "  Deque<Object> d = new ArrayDeque<>();",
            "  List<Object> l = new ArrayList<>();",
            "  {",
            "    l = new ArrayList<>();",
            "  }",
            "  LinkedList<Object> ll = new LinkedList<>();",
            "  List<Object> lll = new LinkedList<Object>() {{",
            "    add(null); // yikes",
            "  }};",
            "}")
        .doTest(TEXT_MATCH);
  }

  @Test
  public void stringBufferRefactoringTest() {
    BugCheckerRefactoringTestHelper.newInstance(new JdkObsolete(), getClass())
        .addInputLines(
            "in/Test.java", //
            "class Test {",
            "  String f() {",
            "    StringBuffer sb = new StringBuffer();",
            "    return sb.append(42).toString();",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java", //
            "class Test {",
            "  String f() {",
            "    StringBuilder sb = new StringBuilder();",
            "    return sb.append(42).toString();",
            "  }",
            "}")
        .doTest();
  }

  @Rule public final TemporaryFolder tempFolder = new TemporaryFolder();

  /** A test input. */
  public interface Lib {
    Enumeration<Integer> foos();
  }

  static void addClassToJar(JarOutputStream jos, Class<?> clazz) throws IOException {
    String entryPath = clazz.getName().replace('.', '/') + ".class";
    try (InputStream is = clazz.getClassLoader().getResourceAsStream(entryPath)) {
      jos.putNextEntry(new JarEntry(entryPath));
      ByteStreams.copy(is, jos);
    }
  }

  @Test
  public void obsoleteOverride() throws IOException {
    File libJar = tempFolder.newFile("lib.jar");
    try (FileOutputStream fis = new FileOutputStream(libJar);
        JarOutputStream jos = new JarOutputStream(fis)) {
      addClassToJar(jos, Lib.class);
    }
    testHelper
        .addSourceLines(
            "Test.java",
            "import " + Lib.class.getCanonicalName() + ";",
            "import java.util.Enumeration;",
            "class Test implements Lib {",
            "  public Enumeration<Integer> foos() {",
            "    return new Enumeration<Integer>() {",
            "      @Override public boolean hasMoreElements() { return false; }",
            "      @Override public Integer nextElement() { return null; }",
            "    };",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void additionalRefactorings() {
    BugCheckerRefactoringTestHelper.newInstance(new JdkObsolete(), getClass())
        .addInputLines(
            "in/Test.java", //
            "import java.util.*;",
            "import java.util.function.*;",
            "class Test {",
            "  Supplier<Deque<Object>> a = () -> new LinkedList<>();",
            "  Supplier<Deque<Object>> b = () -> {",
            "    return new LinkedList<>();",
            "  };",
            "  Supplier<Deque<Object>> c = LinkedList::new;",
            "  Deque<Object> f() {",
            "    return new LinkedList<>();",
            "  }",
            "  void g(Deque<Object> x) {}",
            "  {",
            "    g(new LinkedList<>());",
            "  }",
            "  {",
            "    List<LinkedList<String>> xs = new ArrayList<>();",
            "    List<List<String>> ys = new ArrayList<>();",
            "    xs.add(new LinkedList<>());",
            "    ys.add(new LinkedList<>());",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java", //
            "import java.util.*;",
            "import java.util.function.*;",
            "class Test {",
            "  Supplier<Deque<Object>> a = () -> new ArrayDeque<>();",
            "  Supplier<Deque<Object>> b = () -> {",
            "    return new ArrayDeque<>();",
            "  };",
            "  Supplier<Deque<Object>> c = ArrayDeque::new;",
            "  Deque<Object> f() {",
            "    return new ArrayDeque<>();",
            "  }",
            "  void g(Deque<Object> x) {}",
            "  {",
            "    g(new ArrayDeque<>());",
            "  }",
            "  {",
            "    List<LinkedList<String>> xs = new ArrayList<>();",
            "    List<List<String>> ys = new ArrayList<>();",
            "    xs.add(new LinkedList<>());",
            "    ys.add(new ArrayList<>());",
            "  }",
            "}")
        .doTest(TEXT_MATCH);
  }

  @Test
  public void obsoleteMocking() throws IOException {
    File libJar = tempFolder.newFile("lib.jar");
    try (FileOutputStream fis = new FileOutputStream(libJar);
        JarOutputStream jos = new JarOutputStream(fis)) {
      addClassToJar(jos, Lib.class);
    }
    testHelper
        .addSourceLines(
            "Test.java",
            "import static org.mockito.Mockito.when;",
            "import " + Lib.class.getCanonicalName() + ";",
            "import java.util.Enumeration;",
            "class Test {",
            "  void test(Lib lib) {",
            "    when(lib.foos())",
            "        .thenReturn(",
            "            new Enumeration<Integer>() {",
            "              public boolean hasMoreElements() {",
            "                return false;",
            "              }",
            "              public Integer nextElement() {",
            "                return null;",
            "              }",
            "            });",
            "  }",
            "}")
        .doTest();
  }
}
