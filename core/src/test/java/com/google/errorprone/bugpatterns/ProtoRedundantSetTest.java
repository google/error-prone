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

  @Test
  public void testPositiveCase() {
    compilationHelper
        .addSourceLines(
            "ProtoRedundantSetPositiveCases.java",
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
            "            // BUG: Diagnostic contains: setMessage",
            "            .setMessage(foo)",
            "            .addMultiField(bar)",
            "            .build();",
            "    TestProtoMessage nestedField =",
            "        TestProtoMessage.newBuilder()",
            "            .setMessage(",
            "                // BUG: Diagnostic contains: setField",
            "                TestFieldProtoMessage.newBuilder().setField(foo).setField(foo))",
            "            .addMultiField(bar)",
            "            .build();",
            "  }",
            "  private void repeatedField() {",
            "    TestProtoMessage.Builder again =",
            "        TestProtoMessage.newBuilder()",
            "            .setMessage(foo)",
            "            .setMessage(foo)",
            "            // BUG: Diagnostic contains: setMessage",
            "            .setMessage(foo)",
            "            .setMultiField(0, bar)",
            "            .setMultiField(1, foo)",
            "            // BUG: Diagnostic contains: setMultiField",
            "            .setMultiField(1, bar);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void singleField() {
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
  public void repeatedField() {
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
  public void complexChaining() {
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
  public void testFixes() {
    BugCheckerRefactoringTestHelper.newInstance(new ProtoRedundantSet(), getClass())
        .addInputLines(
            "ProtoRedundantSetPositiveCases.java",
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
            "            .setMultiField(1, bar);",
            "  }",
            "}")
        .addOutputLines(
            "ProtoRedundantSetExpected.java",
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
            "            .addMultiField(bar)",
            "            .build();",
            "  }",
            "  private void repeatedField() {",
            "    TestProtoMessage.Builder again =",
            "        TestProtoMessage.newBuilder()",
            "            .setMessage(foo)",
            "            .setMultiField(0, bar)",
            "            .setMultiField(1, foo)",
            "            .setMultiField(1, bar);",
            "  }",
            "}")
        .doTest(TestMode.AST_MATCH);
  }
}
