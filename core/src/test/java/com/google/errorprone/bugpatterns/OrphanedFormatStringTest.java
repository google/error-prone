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

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link OrphanedFormatString}Test */
@RunWith(JUnit4.class)
public class OrphanedFormatStringTest {

  private final CompilationTestHelper testHelper =
      CompilationTestHelper.newInstance(OrphanedFormatString.class, getClass());

  @Test
  public void positive() {
    testHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void f() {",
            "    // BUG: Diagnostic contains:",
            "    System.err.println(\"%s\");",
            "    // BUG: Diagnostic contains:",
            "    new Exception(\"%s\");",
            "    // BUG: Diagnostic contains:",
            "    new StringBuilder(\"%s\");",
            "    // BUG: Diagnostic contains:",
            "    new StringBuilder().append(\"%s\", 0, 0);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative() {
    testHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  static class FormatException extends Exception {",
            "    FormatException(String f, Object... xs) {",
            "      super(String.format(f, xs));",
            "    }",
            "  }",
            "  void f() {",
            "    String s = \"%s\";",
            "    new FormatException(\"%s\");",
            "    System.err.printf(\"%s\");",
            "  }",
            "  void appendToStringBuilder(StringBuilder b) {",
            "    b.append(\"%s\");",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void formatMethod() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.FormatMethod;",
            "import com.google.errorprone.annotations.FormatString;",
            "class Test {",
            "  static class MyPrintWriter extends java.io.PrintWriter {",
            "    MyPrintWriter() throws java.io.FileNotFoundException {super((String) null);}",
            "    @FormatMethod",
            "    public void println(String first, Object...args) {}",
            "    @FormatMethod",
            "    public void print(String first,"
                + " @FormatString String second, Object...args) {}",
            "  }",
            "  void f(MyPrintWriter pw) {",
            "    pw.println(\"%s %s\", \"\", \"\");",
            "    pw.print(\"\", \"%s\");",
            // Here, %s in the first position is a non-format String arg
            "    // BUG: Diagnostic contains: ",
            "    pw.print(\"%s\", \"%s\");",
            // The first argument to the format string is another format string
            "    // BUG: Diagnostic contains: ",
            "    pw.print(\"\", \"%s\", \"%d\");",
            "  }",
            "}")
        .doTest();
  }
}
