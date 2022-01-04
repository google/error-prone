/*
 * Copyright 2021 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.apidiff;

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link Java8ApiChecker}Test */
@RunWith(JUnit4.class)
public class Java8ApiCheckerTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(Java8ApiChecker.class, getClass());

  @Test
  public void positive() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.Optional;",
            "class Test {",
            "  boolean f(Optional<String> o) {",
            "    // BUG: Diagnostic contains: java.util.Optional#isEmpty() is not available",
            "    return o.isEmpty();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void bufferPositive() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.nio.ByteBuffer;",
            "class Test {",
            "  void f(ByteBuffer b, int i) {",
            "    // BUG: Diagnostic contains: ByteBuffer#position(int) is not available",
            "    b.position(i);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void bufferNegative() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.nio.ByteBuffer;",
            "class Test {",
            "  void f(ByteBuffer b, int i) {",
            "    b.position(i);",
            "  }",
            "}")
        .setArgs("-XepOpt:Java8ApiChecker:checkBuffer=false")
        .doTest();
  }

  @Test
  public void checksumPositive() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.zip.CRC32;",
            "class Test {",
            "  void f(CRC32 c, byte[] b) {",
            "    // BUG: Diagnostic contains: Checksum#update(byte[]) is not available",
            "    c.update(b);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void checksumNegative() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.zip.CRC32;",
            "class Test {",
            "  void f(CRC32 c, byte[] b) {",
            "    c.update(b);",
            "  }",
            "}")
        .setArgs("-XepOpt:Java8ApiChecker:checkChecksum=false")
        .doTest();
  }
}
