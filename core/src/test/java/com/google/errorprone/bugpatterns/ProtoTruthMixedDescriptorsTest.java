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

import org.junit.Ignore;

/**
 * Tests for {@link ProtoTruthMixedDescriptors} bugpattern.
 *
 * @author ghm@google.com (Graeme Morgan)
 */
@Ignore("b/74365407 test proto sources are broken")
@RunWith(JUnit4.class)
public final class ProtoTruthMixedDescriptorsTest {
  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(ProtoTruthMixedDescriptors.class, getClass());

  @Test
  public void positive() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import static com.google.common.truth.extensions.proto.ProtoTruth.assertThat;",
            "import com.google.errorprone.bugpatterns.proto.ProtoTest.TestFieldProtoMessage;",
            "import com.google.errorprone.bugpatterns.proto.ProtoTest.TestProtoMessage;",
            "final class Test {",
            "  void test() {",
            "    assertThat(TestFieldProtoMessage.getDefaultInstance())",
            "        // BUG: Diagnostic contains:",
            "        .ignoringFields(",
            "            TestProtoMessage.MESSAGE_FIELD_NUMBER);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positive_wrongType() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import static com.google.common.truth.extensions.proto.ProtoTruth.assertThat;",
            "import com.google.errorprone.bugpatterns.proto.ProtoTest.TestFieldProtoMessage;",
            "import com.google.errorprone.bugpatterns.proto.ProtoTest.TestProtoMessage;",
            "final class Test {",
            "  void test() {",
            "    assertThat(TestFieldProtoMessage.getDefaultInstance())",
            "        // BUG: Diagnostic contains:",
            "        .ignoringFields(",
            "            TestProtoMessage.MULTI_FIELD_FIELD_NUMBER,",
            "            TestProtoMessage.MESSAGE_FIELD_NUMBER);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positive_fluentChain() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import static com.google.common.truth.extensions.proto.ProtoTruth.assertThat;",
            "import com.google.errorprone.bugpatterns.proto.ProtoTest.TestFieldProtoMessage;",
            "import com.google.errorprone.bugpatterns.proto.ProtoTest.TestProtoMessage;",
            "final class Test {",
            "  void test() {",
            "    assertThat(TestFieldProtoMessage.getDefaultInstance())",
            "        .ignoringFields(TestFieldProtoMessage.FIELD_FIELD_NUMBER)",
            "        // BUG: Diagnostic contains:",
            "        .ignoringFields(TestProtoMessage.MESSAGE_FIELD_NUMBER);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import static com.google.common.truth.extensions.proto.ProtoTruth.assertThat;",
            "import com.google.errorprone.bugpatterns.proto.ProtoTest.TestFieldProtoMessage;",
            "import com.google.errorprone.bugpatterns.proto.ProtoTest.TestProtoMessage;",
            "import com.google.protobuf.Message;",
            "final class Test {",
            "  void test() {",
            "    assertThat(TestProtoMessage.getDefaultInstance())",
            "        .ignoringFields(",
            "            TestProtoMessage.MULTI_FIELD_FIELD_NUMBER,",
            "            TestProtoMessage.MESSAGE_FIELD_NUMBER);",
            "    assertThat((Message) TestFieldProtoMessage.getDefaultInstance())",
            "        .ignoringFields(TestProtoMessage.MULTI_FIELD_FIELD_NUMBER);",
            "  }",
            "}")
        .doTest();
  }
}
