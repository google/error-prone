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

import com.google.errorprone.CompilationTestHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Test cases for {@link LiteByteStringUtf8}. */
@RunWith(JUnit4.class)
public class LiteByteStringUtf8Test {

  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper = CompilationTestHelper.newInstance(LiteByteStringUtf8.class, getClass());
  }

  @Test
  public void positiveCase() {
    compilationHelper
        .addSourceLines(
            "Foo.java",
            "import com.google.protobuf.ByteString;",
            "import com.google.protobuf.MessageLite;",
            "class Foo {",
            "  void main(com.google.protobuf.MessageLite m) {",
            "    // BUG: Diagnostic contains: ByteString",
            "    String s = m.toByteString().toStringUtf8();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeCase() {
    compilationHelper
        .addSourceLines(
            "Foo.java",
            "import com.google.protobuf.ByteString;",
            "import com.google.protobuf.MessageLite;",
            "class Foo {",
            "  void main(MessageLite m, ByteString b) {",
            "    ByteString b2 = m.toByteString();",
            "    String s = b2.toStringUtf8();",
            "  }",
            "}")
        .doTest();
  }
}
