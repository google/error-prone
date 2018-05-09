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

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.BugCheckerRefactoringTestHelper.TestMode;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import org.junit.Ignore;

/**
 * Tests for {@link ProtoRedundantSet} bugpattern.
 *
 * @author ghm@google.com (Graeme Morgan)
 */
@Ignore("b/74365407 test proto sources are broken")
@RunWith(JUnit4.class)
public final class ProtoRedundantSetTest {
  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(ProtoRedundantSet.class, getClass());

  private static final String[] POSITIVE_LINES =
      new String[] {
        "import com.google.errorprone.bugpatterns.proto.ProtoTest.TestFieldProtoMessage;",
        "import com.google.errorprone.bugpatterns.proto.ProtoTest.TestProtoMessage;",
        "final class ProtoRedundantSetPositiveCases {",
        "  private static final TestFieldProtoMessage foo =",
        "      TestFieldProtoMessage.getDefaultInstance();",
        "  private static final TestFieldProtoMessage bar =",
        "      TestFieldProtoMessage.getDefaultInstance();",
        "  private void singleField() {",
        "    TestProtoMessage twice =",
        "        TestProtoMessage.newBuilder()",
        "            .setMessage(foo)",
        "            .addMultiField(bar)",
        "            .setMessage(foo)",
        "            // BUG: Diagnostic contains: setMessage",
        "            .addMultiField(bar)",
        "            .build();",
        "  }",
        "  private void repeatedField() {",
        "    TestProtoMessage.Builder again =",
        "        TestProtoMessage.newBuilder()",
        "            .setMessage(foo)",
        "            .setMessage(foo)",
        "            .setMessage(foo)",
        "            .setMultiField(0, bar)",
        "            .setMultiField(1, foo)",
        "            // BUG: Diagnostic contains: setMultiField",
        "            .setMultiField(1, bar);",
        "  }",
        "}"
      };

  private static final String[] EXPECTED_LINES =
      new String[] {
        "import com.google.errorprone.bugpatterns.proto.ProtoTest.TestFieldProtoMessage;",
        "import com.google.errorprone.bugpatterns.proto.ProtoTest.TestProtoMessage;",
        "final class ProtoRedundantSetPositiveCases {",
        "  private static final TestFieldProtoMessage foo =",
        "      TestFieldProtoMessage.getDefaultInstance();",
        "  private static final TestFieldProtoMessage bar =",
        "      TestFieldProtoMessage.getDefaultInstance();",
        "  private void singleField() {",
        "    TestProtoMessage twice =",
        "        TestProtoMessage.newBuilder()",
        "            .addMultiField(bar)",
        "            .setMessage(foo)",
        "            // BUG: Diagnostic contains: setMessage",
        "            .addMultiField(bar)",
        "            .build();",
        "  }",
        "  private void repeatedField() {",
        "    TestProtoMessage.Builder again =",
        "        TestProtoMessage.newBuilder()",
        "            .setMessage(foo)",
        "            .setMultiField(0, bar)",
        "            .setMultiField(1, foo)",
        "            // BUG: Diagnostic contains: setMultiField",
        "            .setMultiField(1, bar);",
        "  }",
        "}"
      };

  @Test
  public void testPositiveCase() throws Exception {
    compilationHelper
        .addSourceLines("ProtoRedundantSetPositiveCases.java", POSITIVE_LINES)
        .doTest();
  }

  @Test
  public void singleField() throws Exception {
    compilationHelper
        .addSourceLines(
            "SingleField.java",
            "import com.google.errorprone.bugpatterns.proto.ProtoTest.TestFieldProtoMessage;",
            "import com.google.errorprone.bugpatterns.proto.ProtoTest.TestProtoMessage;",
            "final class ProtoRedundantSetNegativeCases {",
            "  private void singleField() {",
            "    TestProtoMessage.Builder builder =",
            "        TestProtoMessage.newBuilder()",
            "            .setMessage(TestFieldProtoMessage.getDefaultInstance())",
            "            .addMultiField(TestFieldProtoMessage.getDefaultInstance())",
            "            .addMultiField(TestFieldProtoMessage.getDefaultInstance());",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void repeatedField() throws Exception {
    compilationHelper
        .addSourceLines(
            "RepeatedField.java",
            "import com.google.errorprone.bugpatterns.proto.ProtoTest.TestFieldProtoMessage;",
            "import com.google.errorprone.bugpatterns.proto.ProtoTest.TestProtoMessage;",
            "final class ProtoRedundantSetNegativeCases {",
            "  private void repeatedField() {",
            "    TestProtoMessage.Builder builder =",
            "        TestProtoMessage.newBuilder()",
            "            .setMessage(TestFieldProtoMessage.getDefaultInstance())",
            "            .setMultiField(0, TestFieldProtoMessage.getDefaultInstance())",
            "            .setMultiField(1, TestFieldProtoMessage.getDefaultInstance());",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void complexChaining() throws Exception {
    compilationHelper
        .addSourceLines(
            "ComplexChaining.java",
            "import com.google.errorprone.bugpatterns.proto.ProtoTest.TestFieldProtoMessage;",
            "import com.google.errorprone.bugpatterns.proto.ProtoTest.TestProtoMessage;",
            "final class ProtoRedundantSetNegativeCases {",
            "  private void intermediateBuild() {",
            "    TestProtoMessage message =",
            "        TestProtoMessage.newBuilder()",
            "            .setMessage(TestFieldProtoMessage.getDefaultInstance())",
            "            .build()",
            "            .toBuilder()",
            "            .setMessage(TestFieldProtoMessage.getDefaultInstance())",
            "            .build();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testFixes() throws Exception {
    BugCheckerRefactoringTestHelper.newInstance(new ProtoRedundantSet(), getClass())
        .addInputLines("ProtoRedundantSetPositiveCases.java", POSITIVE_LINES)
        .addOutputLines("ProtoRedundantSetExpected.java", EXPECTED_LINES)
        .doTest(TestMode.AST_MATCH);
  }
}
