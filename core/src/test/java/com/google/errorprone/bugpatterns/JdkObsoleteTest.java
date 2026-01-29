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

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import java.util.Enumeration;
import org.junit.Test;
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
            """
            import java.nio.file.Path;

            class Test {
              {
                // BUG: Diagnostic contains:
                new java.util.LinkedList<>();
                // BUG: Diagnostic contains:
                new java.util.Stack<>();
                // BUG: Diagnostic contains:
                new java.util.Vector<>();
                // BUG: Diagnostic contains:
                new java.util.Hashtable<>();
                // BUG: Diagnostic contains:
                new StringBuffer();
                // BUG: Diagnostic contains:
                new java.util.Hashtable<Object, Object>() {};
              }
            }
            """)
        .doTest();
  }

  @Test
  public void stringBuffer_appendReplacement() {
    testHelper
        .addSourceLines(
            "Test.java",
            """
            import java.util.regex.Matcher;

            class Test {
              void f(Matcher m) {
                StringBuffer sb = new StringBuffer();
                m.appendReplacement(sb, null);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void stringBuffer_appendTail() {
    testHelper
        .addSourceLines(
            "Test.java",
            """
            import java.util.regex.Matcher;

            class Test {
              void f(Matcher m) {
                StringBuffer sb = new StringBuffer();
                m.appendTail(sb);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void positiveExtends() {
    testHelper
        .addSourceLines(
            "Test.java",
            """
            import java.nio.file.Path;

            class Test {
              // BUG: Diagnostic contains:
              abstract class A implements java.util.Enumeration<Object> {}

              // BUG: Diagnostic contains:
              abstract class B implements java.util.SortedSet<Object> {}

              // BUG: Diagnostic contains:
              abstract class C implements java.util.SortedMap<Object, Object> {}

              // BUG: Diagnostic contains:
              abstract class D extends java.util.Dictionary<Object, Object> {}
            }
            """)
        .doTest();
  }

  @Test
  public void refactoring() {
    BugCheckerRefactoringTestHelper.newInstance(JdkObsolete.class, getClass())
        .addInputLines(
            "in/Test.java",
            """
            import java.util.*;

            class Test {
              Deque<Object> d = new LinkedList<>();
              List<Object> l = new LinkedList<>();

              {
                l = new LinkedList<>();
              }

              LinkedList<Object> ll = new LinkedList<>();
              List<Object> lll =
                  new LinkedList<Object>() {
                    {
                      add(null); // yikes
                    }
                  };
            }
            """)
        .addOutputLines(
            "out/Test.java",
            """
            import java.util.*;

            class Test {
              Deque<Object> d = new ArrayDeque<>();
              List<Object> l = new ArrayList<>();

              {
                l = new ArrayList<>();
              }

              LinkedList<Object> ll = new LinkedList<>();
              List<Object> lll =
                  new LinkedList<Object>() {
                    {
                      add(null); // yikes
                    }
                  };
            }
            """)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void stringBufferRefactoringTest() {
    BugCheckerRefactoringTestHelper.newInstance(JdkObsolete.class, getClass())
        .addInputLines(
            "in/Test.java",
            """
            class Test {
              String f() {
                StringBuffer sb = new StringBuffer();
                return sb.append(42).toString();
              }
            }
            """)
        .addOutputLines(
            "out/Test.java",
            """
            class Test {
              String f() {
                StringBuilder sb = new StringBuilder();
                return sb.append(42).toString();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void stringBufferRefactoringTest_usingVar() {
    BugCheckerRefactoringTestHelper.newInstance(JdkObsolete.class, getClass())
        .addInputLines(
            "in/Test.java",
            """
            class Test {
              String f() {
                var sb = new StringBuffer();
                return sb.append(42).toString();
              }
            }
            """)
        .addOutputLines(
            "out/Test.java",
            """
            class Test {
              String f() {
                var sb = new StringBuilder();
                return sb.append(42).toString();
              }
            }
            """)
        .doTest();
  }

  /** A test input. */
  public interface Lib {
    Enumeration<Integer> foos();
  }

  @Test
  public void obsoleteOverride() {
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
    BugCheckerRefactoringTestHelper.newInstance(JdkObsolete.class, getClass())
        .addInputLines(
            "in/Test.java",
            """
            import java.util.*;
            import java.util.function.*;

            class Test {
              Supplier<Deque<Object>> a = () -> new LinkedList<>();
              Supplier<Deque<Object>> b =
                  () -> {
                    return new LinkedList<>();
                  };
              Supplier<Deque<Object>> c = LinkedList::new;

              Deque<Object> f() {
                return new LinkedList<>();
              }

              void g(Deque<Object> x) {}

              {
                g(new LinkedList<>());
              }

              {
                List<LinkedList<String>> xs = new ArrayList<>();
                List<List<String>> ys = new ArrayList<>();
                xs.add(new LinkedList<>());
                ys.add(new LinkedList<>());
              }
            }
            """)
        .addOutputLines(
            "out/Test.java",
            """
            import java.util.*;
            import java.util.function.*;

            class Test {
              Supplier<Deque<Object>> a = () -> new ArrayDeque<>();
              Supplier<Deque<Object>> b =
                  () -> {
                    return new ArrayDeque<>();
                  };
              Supplier<Deque<Object>> c = ArrayDeque::new;

              Deque<Object> f() {
                return new ArrayDeque<>();
              }

              void g(Deque<Object> x) {}

              {
                g(new ArrayDeque<>());
              }

              {
                List<LinkedList<String>> xs = new ArrayList<>();
                List<List<String>> ys = new ArrayList<>();
                xs.add(new LinkedList<>());
                ys.add(new ArrayList<>());
              }
            }
            """)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void obsoleteMocking() {
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

  @Test
  public void navigableSetRepro() {
    testHelper
        .addSourceLines(
            "Test.java",
            """
            import java.util.NavigableSet;
            import java.util.Optional;

            class Test {
              Optional<Object> fail1(Optional<NavigableSet<Object>> myOptionalSet) {
                return myOptionalSet.map(NavigableSet::first);
              }

              Optional<Object> fail2(Optional<NavigableSet<Object>> myOptionalSet) {
                return myOptionalSet.map(NavigableSet::last);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void navigableMapInheritedMethod() {
    testHelper
        .addSourceLines(
            "Test.java",
            """
            import java.util.Map;
            import java.util.Set;
            import java.util.NavigableMap;

            class Test {
              void f(NavigableMap<String, Integer> m) {
                for (Integer e : m.values()) {}
              }
            }
            """)
        .doTest();
  }

  @Test
  public void indirect() {
    testHelper
        .addSourceLines(
            "Test.java",
            """
            import com.google.common.collect.SortedSetMultimap;
            import com.google.common.collect.TreeMultimap;

            class Test {
              void f() {
                SortedSetMultimap<String, String> myMultimap = TreeMultimap.create();
                String myValue = myMultimap.get("foo").first();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void preferCharsetAcceptingApis() {
    testHelper
        .addSourceLines(
            "Test.java",
            """
            import static java.nio.charset.StandardCharsets.UTF_8;

            import java.io.*;
            import java.net.*;
            import java.nio.channels.*;
            import java.util.*;

            class Test {
              private static final String UTF8_NAME = UTF_8.name();

              void string(byte[] bytes) throws Exception {
                // BUG: Diagnostic contains: String.getBytes(Charset)
                "foo".getBytes(UTF8_NAME);
                // BUG: Diagnostic contains: new String(byte[], Charset)
                new String(bytes, UTF8_NAME);
                // BUG: Diagnostic contains: new String(byte[], int, int, Charset)
                new String(bytes, 0, 1, UTF8_NAME);
              }

              void byteArrayOutputStream(String UTF8_NAME) throws Exception {
                // BUG: Diagnostic contains: ByteArrayOutputStream.toString(Charset)
                new ByteArrayOutputStream().toString(UTF8_NAME);
              }

              void urlDecoder(String UTF8_NAME) throws Exception {
                // BUG: Diagnostic contains: URLDecoder.decode(String, Charset)
                URLDecoder.decode("foo", UTF8_NAME);
              }

              void urlEncoder(String UTF8_NAME) throws Exception {
                // BUG: Diagnostic contains: URLEncoder.encode(String, Charset)
                URLEncoder.encode("foo", UTF8_NAME);
              }

              void newReader(ReadableByteChannel rbc) throws Exception {
                // BUG: Diagnostic contains: Channels.newReader(ReadableByteChannel, Charset)
                Channels.newReader(rbc, UTF8_NAME);
              }

              void newWriter(WritableByteChannel wbc) throws Exception {
                // BUG: Diagnostic contains: Channels.newWriter(WritableByteChannel, Charset)
                Channels.newWriter(wbc, UTF8_NAME);
              }

              void inputStreamReader(InputStream is) throws Exception {
                // BUG: Diagnostic contains: new InputStreamReader(InputStream, Charset)
                new InputStreamReader(is, UTF8_NAME);
              }

              void outputStreamWriter(OutputStream os) throws Exception {
                // BUG: Diagnostic contains: new OutputStreamWriter(OutputStream, Charset)
                new OutputStreamWriter(os, UTF8_NAME);
              }

              void printStream(OutputStream os, String fileName) throws Exception {
                // BUG: Diagnostic contains: new PrintStream(OutputStream, boolean, Charset)
                new PrintStream(os, false, UTF8_NAME);
                // BUG: Diagnostic contains: new PrintStream(String, Charset)
                new PrintStream(fileName, UTF8_NAME);
                // BUG: Diagnostic contains: new PrintStream(File, Charset)
                new PrintStream(new File(fileName), UTF8_NAME);
              }

              void printWriter(String fileName) throws Exception {
                // BUG: Diagnostic contains: new PrintWriter(String, Charset)
                new PrintWriter(fileName, UTF8_NAME);
                // BUG: Diagnostic contains: new PrintWriter(File, Charset)
                new PrintWriter(new File(fileName), UTF8_NAME);
              }

              void formatter(String fileName, File file, OutputStream os) throws Exception {
                // BUG: Diagnostic contains: new Formatter(String, Charset)
                new Formatter(fileName, UTF8_NAME);
                // BUG: Diagnostic contains: new Formatter(String, Charset, Locale)
                new Formatter(fileName, UTF8_NAME, Locale.US);
                // BUG: Diagnostic contains: new Formatter(File, Charset)
                new Formatter(file, UTF8_NAME);
                // BUG: Diagnostic contains: new Formatter(File, Charset, Locale)
                new Formatter(file, UTF8_NAME, Locale.US);
                // BUG: Diagnostic contains: new Formatter(OutputStream, Charset)
                new Formatter(os, UTF8_NAME);
                // BUG: Diagnostic contains: new Formatter(OutputStream, Charset, Locale)
                new Formatter(os, UTF8_NAME, Locale.US);
              }

              void properties(OutputStream os) throws Exception {
                // BUG: Diagnostic contains: Properties.storeToXML(OutputStream, String, Charset)
                new Properties().storeToXML(os, "comment", UTF8_NAME);
              }
            }
            """)
        .doTest();
  }
}
