/*
 * Copyright 2019 The Error Prone Authors.
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
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link LiteProtoToString} bugpattern.
 *
 * @author ghm@google.com (Graeme Morgan)
 */
@RunWith(JUnit4.class)
public final class LiteProtoToStringTest {
  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(LiteProtoToString.class, getClass());

  @Test
  public void positive() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.protobuf.GeneratedMessageLite;",
            "class Test {",
            "  private String test(GeneratedMessageLite message) {",
            "    // BUG: Diagnostic contains:",
            "    return message.toString();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.protobuf.GeneratedMessageLite;",
            "class Test {",
            "  private void test(GeneratedMessageLite message) {",
            "    atVerbose(message.toString());",
            "  }",
            "  public void atVerbose(String s) {}",
            "}")
        .doTest();
  }

  @Test
  public void unknownFieldSet_noMatch() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.protobuf.UnknownFieldSet;",
            "class Test {",
            "  private String test(UnknownFieldSet message) {",
            "    return message.toString();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  @Ignore // TODO(b/130683674,ghm): Support checking toString on proto enums.
  public void enums() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.protobuf.Internal.EnumLite;",
            "import com.google.protobuf.ProtocolMessageEnum;",
            "class Test {",
            "  private String test(EnumLite e) {",
            "    return e.toString();",
            "  }",
            "  private String test2(ProtocolMessageEnum e) {",
            "    return e.toString();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void nestedLogStatement() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.protobuf.GeneratedMessageLite;",
            "class Test {",
            "  private void test(GeneratedMessageLite message) {",
            "    atVerbose().log(message.toString());",
            "  }",
            "  public Test atVerbose() {",
            "    return this;",
            "  }",
            "  public Test log(String s) {",
            "    return this;",
            "  }",
            "}")
        .doTest();
  }


  @Test
  public void customFormatMethod() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.FormatMethod;",
            "import com.google.protobuf.GeneratedMessageLite;",
            "class Test {",
            "  private void test(GeneratedMessageLite message) {",
            "    // BUG: Diagnostic contains:",
            "    format(null, \"%s\", message);",
            "    format(message, \"%s\", 1);",
            "  }",
            "  @FormatMethod",
            "  String format(Object tag, String format, Object... args) {",
            "    return String.format(format, args);",
            "  }",
            "}")
        .doTest();
  }
}
