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

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class PreferCharsetOverloadTest {

  private final BugCheckerRefactoringTestHelper helper =
      BugCheckerRefactoringTestHelper.newInstance(PreferCharsetOverload.class, getClass());

  @Test
  public void positiveCustomWriter() {
    helper
        .addInputLines(
            "Test.java",
            """
            import java.io.OutputStream;
            import java.nio.charset.Charset;

            class Test {
              static class MyWriter {
                MyWriter(OutputStream out, String charsetName) {}

                MyWriter(OutputStream out, Charset cs) {}
              }

              void test(OutputStream out) {
                new MyWriter(out, "UTF-8");
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import static java.nio.charset.StandardCharsets.UTF_8;

            import java.io.OutputStream;
            import java.nio.charset.Charset;

            class Test {
              static class MyWriter {
                MyWriter(OutputStream out, String charsetName) {}

                MyWriter(OutputStream out, Charset cs) {}
              }

              void test(OutputStream out) {
                new MyWriter(out, UTF_8);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void positiveCustomString() {
    helper
        .addInputLines(
            "Test.java",
            """
            import java.nio.charset.Charset;

            class Test {
              static class MyString {
                MyString(byte[] bytes, String charsetName) {}

                MyString(byte[] bytes, Charset cs) {}
              }

              void test(byte[] bytes) {
                MyString s = new MyString(bytes, "ISO-8859-1");
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import static java.nio.charset.StandardCharsets.ISO_8859_1;

            import java.nio.charset.Charset;

            class Test {
              static class MyString {
                MyString(byte[] bytes, String charsetName) {}

                MyString(byte[] bytes, Charset cs) {}
              }

              void test(byte[] bytes) {
                MyString s = new MyString(bytes, ISO_8859_1);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void positiveCustomMethod() {
    helper
        .addInputLines(
            "Test.java",
            """
            import java.nio.charset.Charset;

            class Test {
              void log(String msg, String encoding) {}

              void log(String msg, Charset encoding) {}

              void test() {
                log("hello", "US-ASCII");
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import static java.nio.charset.StandardCharsets.US_ASCII;

            import java.nio.charset.Charset;

            class Test {
              void log(String msg, String encoding) {}

              void log(String msg, Charset encoding) {}

              void test() {
                log("hello", US_ASCII);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void positiveFallbackToForName() {
    helper
        .addInputLines(
            "Test.java",
            """
            import java.nio.charset.Charset;

            class Test {
              void log(String msg, String cs) {}

              void log(String msg, Charset cs) {}

              void test(String customEncoding) {
                log("hello", customEncoding);
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.nio.charset.Charset;

            class Test {
              void log(String msg, String cs) {}

              void log(String msg, Charset cs) {}

              void test(String customEncoding) {
                log("hello", Charset.forName(customEncoding));
              }
            }
            """)
        .doTest();
  }

  @Test
  public void positiveDecomposition() {
    helper
        .addInputLines(
            "Test.java",
            """
            import java.nio.charset.Charset;

            class Test {
              void log(String msg, String encoding) {}

              void log(String msg, Charset encoding) {}

              void test(Charset cs) {
                log("hello", cs.name());
                log("hello", cs.displayName());
                log("hello", cs.toString());
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.nio.charset.Charset;

            class Test {
              void log(String msg, String encoding) {}

              void log(String msg, Charset encoding) {}

              void test(Charset cs) {
                log("hello", cs);
                log("hello", cs);
                log("hello", cs);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void positiveMultipleCharsetParameters() {
    helper
        .addInputLines(
            "Test.java",
            """
            import java.nio.charset.Charset;

            class Test {
              void convert(byte[] data, String fromCharset, String toCharset) {}

              void convert(byte[] data, Charset fromCharset, String toCharset) {}

              void convert(byte[] data, String fromCharset, Charset toCharset) {}

              void convert(byte[] data, Charset fromCharset, Charset toCharset) {}

              void test(byte[] data) {
                convert(data, "UTF-8", "ISO-8859-1");
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import static java.nio.charset.StandardCharsets.ISO_8859_1;
            import static java.nio.charset.StandardCharsets.UTF_8;

            import java.nio.charset.Charset;

            class Test {
              void convert(byte[] data, String fromCharset, String toCharset) {}

              void convert(byte[] data, Charset fromCharset, String toCharset) {}

              void convert(byte[] data, String fromCharset, Charset toCharset) {}

              void convert(byte[] data, Charset fromCharset, Charset toCharset) {}

              void test(byte[] data) {
                convert(data, UTF_8, ISO_8859_1);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void positiveAbbreviatedCamelCase() {
    helper
        .addInputLines(
            "Test.java",
            """
            import java.nio.charset.Charset;

            class Test {
              void process(String inputCsName, String destCs) {}

              void process(Charset inputCsName, Charset destCs) {}

              void test() {
                process("UTF-8", "US-ASCII");
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import static java.nio.charset.StandardCharsets.US_ASCII;
            import static java.nio.charset.StandardCharsets.UTF_8;

            import java.nio.charset.Charset;

            class Test {
              void process(String inputCsName, String destCs) {}

              void process(Charset inputCsName, Charset destCs) {}

              void test() {
                process(UTF_8, US_ASCII);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negative() {
    helper
        .addInputLines(
            "Test.java",
            """
            import java.nio.charset.Charset;

            class Test {
              void log(String msg, String format) {}

              void log(String msg, Charset format) {}

              void test() {
                log("hello", "UTF-8");
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void positiveInnerClassConstructor() {
    helper
        .addInputLines(
            "Test.java",
            """
            import java.nio.charset.Charset;

            class Outer {
              class Inner {
                Inner(String charset) {}

                Inner(Charset cs) {}
              }

              void test() {
                new Inner("UTF-8");
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import static java.nio.charset.StandardCharsets.UTF_8;

            import java.nio.charset.Charset;

            class Outer {
              class Inner {
                Inner(String charset) {}

                Inner(Charset cs) {}
              }

              void test() {
                new Inner(UTF_8);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negativeSyntheticParameterName() {
    helper
        .addInputLines(
            "Test.java",
            """
            import java.nio.charset.Charset;

            class Test {
              static class MyClass {
                MyClass(String arg0) {}

                MyClass(Charset cs) {}
              }

              void test() {
                new MyClass("UTF-8");
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void positiveVarargs() {
    helper
        .addInputLines(
            "Test.java",
            """
            import java.nio.charset.Charset;

            class Test {
              void log(String msg, String encoding, Object... args) {}

              void log(String msg, Charset encoding, Object... args) {}

              void test() {
                log("hello", "UTF-8", 1, 2);
                log("hello", "UTF-8");
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import static java.nio.charset.StandardCharsets.UTF_8;

            import java.nio.charset.Charset;

            class Test {
              void log(String msg, String encoding, Object... args) {}

              void log(String msg, Charset encoding, Object... args) {}

              void test() {
                log("hello", UTF_8, 1, 2);
                log("hello", UTF_8);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negativeDelegatingConstructor() {
    helper
        .addInputLines(
            "JavaStringDecoder.java",
            """
            import java.nio.charset.Charset;

            public class JavaStringDecoder {
              private final String charsetName;

              public JavaStringDecoder(Charset charset) {
                this(charset.name());
              }

              public JavaStringDecoder(String charsetName) {
                this.charsetName = charsetName;
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }
}
