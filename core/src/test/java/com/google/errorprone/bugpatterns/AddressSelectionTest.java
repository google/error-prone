/*
 * Copyright 2023 The Error Prone Authors.
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
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class AddressSelectionTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(AddressSelection.class, getClass());

  private final BugCheckerRefactoringTestHelper refactoringTestHelper =
      BugCheckerRefactoringTestHelper.newInstance(AddressSelection.class, getClass());

  @Test
  public void positive() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.net.InetAddress;",
            "import java.net.InetSocketAddress;",
            "import java.net.Socket;",
            "class Test {",
            "  void f() throws Exception{",
            "    // BUG: Diagnostic contains:",
            "    InetAddress.getByName(\"example.com\");",
            "    // BUG: Diagnostic contains:",
            "    new Socket(\"example.com\", 80);",
            "    // BUG: Diagnostic contains:",
            "    new InetSocketAddress(\"example.com\", 80);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative() throws Exception {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.net.InetAddress;",
            "import java.net.InetSocketAddress;",
            "import java.net.Socket;",
            "class Test {",
            "  void f() throws Exception{",
            "    new Socket(InetAddress.getLoopbackAddress(), 80);",
            "    InetAddress.getAllByName(\"example.com\");",
            "    new InetSocketAddress(InetAddress.getLoopbackAddress(), 80);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeLocalhost() throws Exception {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.net.InetAddress;",
            "import java.net.InetSocketAddress;",
            "import java.net.Socket;",
            "class Test {",
            "  void f() throws Exception{",
            "    new Socket(\"localhost\", 80);",
            "    InetAddress.getByName(\"localhost\");",
            "    new InetSocketAddress(\"localhost\", 80);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeNumeric() throws Exception {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.net.InetAddress;",
            "import java.net.InetSocketAddress;",
            "import java.net.Socket;",
            "class Test {",
            "  void f() throws Exception {",
            "    new Socket(\"1.2.3.4\", 80);",
            "    InetAddress.getByName(\"2001:db8:85a3:8d3:1319:8a2e:370:7348\");",
            "    new InetSocketAddress(\"::ffff:192.0.2.128\", 80);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void refactor() throws Exception {
    refactoringTestHelper
        .addInputLines(
            "Test.java",
            "import java.net.InetAddress;",
            "import java.net.InetSocketAddress;",
            "import java.net.Socket;",
            "class Test {",
            "  void f() throws Exception{",
            "    new Socket(\"127.0.0.1\", 80);",
            "    InetAddress.getByName(\"127.0.0.1\");",
            "    new InetSocketAddress(\"127.0.0.1\", 80);",
            "    new Socket(\"::1\", 80);",
            "    InetAddress.getByName(\"::1\");",
            "    new InetSocketAddress(\"::1\", 80);",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import java.net.InetAddress;",
            "import java.net.InetSocketAddress;",
            "import java.net.Socket;",
            "class Test {",
            "  void f() throws Exception{",
            "    new Socket(InetAddress.getLoopbackAddress(), 80);",
            "    InetAddress.getLoopbackAddress();",
            "    new InetSocketAddress(InetAddress.getLoopbackAddress(), 80);",
            "    new Socket(InetAddress.getLoopbackAddress(), 80);",
            "    InetAddress.getLoopbackAddress();",
            "    new InetSocketAddress(InetAddress.getLoopbackAddress(), 80);",
            "  }",
            "}")
        .doTest();
  }
}
