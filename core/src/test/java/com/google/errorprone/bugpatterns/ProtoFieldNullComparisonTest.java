/*
 * Copyright 2013 The Error Prone Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.errorprone.bugpatterns;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import org.junit.Ignore;

/** @author flx@google.com (Felix Berger) */
@Ignore("b/74365407 test proto sources are broken")
@RunWith(JUnit4.class)
public final class ProtoFieldNullComparisonTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(ProtoFieldNullComparison.class, getClass());

  @Test
  public void scalarCases() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.bugpatterns.proto.ProtoTest.TestProtoMessage;",
            "class Test {",
            "  void test() {",
            "    TestProtoMessage message = TestProtoMessage.newBuilder().build();",
            "    // BUG: Diagnostic contains: message.hasMessage()",
            "    if (message.getMessage() != null) {}",
            "    // BUG: Diagnostic contains: !message.hasMessage()",
            "    if (message.getMessage() == null) {}",
            "    // BUG: Diagnostic contains: message.hasMessage()",
            "    if (null != message.getMessage()) {}",
            "    // BUG: Diagnostic contains: message.getMessage().hasField()",
            "    if (message.getMessage().getField() != null) {}",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void listCases() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.bugpatterns.proto.ProtoTest.TestProtoMessage;",
            "import com.google.errorprone.bugpatterns.proto.ProtoTest.TestFieldProtoMessage;",
            "class Test {",
            "  void test() {",
            "    TestProtoMessage message = TestProtoMessage.newBuilder().build();",
            "    TestFieldProtoMessage field = message.getMessage();",
            "    // BUG: Diagnostic contains: !message.getMultiFieldList().isEmpty()",
            "    if (message.getMultiFieldList() != null) {}",
            "    // BUG: Diagnostic contains: message.getMultiFieldList().isEmpty()",
            "    if (null == message.getMultiFieldList()) {}",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void intermediateVariable() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.bugpatterns.proto.ProtoTest.TestProtoMessage;",
            "import com.google.errorprone.bugpatterns.proto.ProtoTest.TestFieldProtoMessage;",
            "import java.util.List;",
            "class Test {",
            "  void test() {",
            "    TestProtoMessage message = TestProtoMessage.newBuilder().build();",
            "    TestFieldProtoMessage field = message.getMessage();",
            "    List<TestFieldProtoMessage> fields = message.getMultiFieldList();",
            "    // BUG: Diagnostic contains: message.hasMessage()",
            "    if (field != null) {}",
            "    // BUG: Diagnostic contains: !message.getMultiFieldList().isEmpty()",
            "    if (fields != null) {}",
            "  }",
            "}")
        .setArgs(ImmutableList.of("-XepOpt:ProtoFieldNullComparison:TrackAssignments"))
        .doTest();
  }

  @Test
  public void intermediateVariable_disabled() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.bugpatterns.proto.ProtoTest.TestProtoMessage;",
            "import com.google.errorprone.bugpatterns.proto.ProtoTest.TestFieldProtoMessage;",
            "import java.util.List;",
            "class Test {",
            "  void test() {",
            "    TestProtoMessage message = TestProtoMessage.newBuilder().build();",
            "    TestFieldProtoMessage field = message.getMessage();",
            "    List<TestFieldProtoMessage> fields = message.getMultiFieldList();",
            "    if (field != null) {}",
            "    if (fields != null) {}",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeCases() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.bugpatterns.proto.ProtoTest.TestProtoMessage;",
            "class Test {",
            "  public void test() {",
            "    TestProtoMessage message = TestProtoMessage.newBuilder().build();",
            "    Object object = new Object();",
            "    if (message.getMessage() != object) {}",
            "    if (object != message.getMessage()) {}",
            "    if (message.getMessage().getField() != object) {}",
            "    if (message.getMultiFieldList() != object) {}",
            "    if (object == message.getMultiFieldList()) {}",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testProto3() {
    compilationHelper
        .addSourceLines(
            "TestProto3.java",
            "import com.google.errorprone.bugpatterns.proto.Proto3Test.TestProto3Message;",
            "public class TestProto3 {",
            "  public boolean doIt(TestProto3Message proto3Message) {",
            "    // BUG: Diagnostic matches: NO_FIX",
            "    return proto3Message.getMyString() == null;",
            "  }",
            "}")
        .expectErrorMessage("NO_FIX", input -> !input.contains("hasMyString()"))
        .doTest();
  }
}
