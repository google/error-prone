/*
 * Copyright 2026 The Error Prone Authors.
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

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class AvoidValueSetterTest {

  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(AvoidValueSetter.class, getClass());

  @Test
  public void positive() {
    helper
        .addSourceLines(
            "Test.java",
            """
            import com.google.errorprone.bugpatterns.proto.Proto3Test;
            import com.google.errorprone.bugpatterns.proto.Proto3Test.TestProto3Message;

            class Test {
              void f(TestProto3Message.Builder m) {
                // BUG: Diagnostic contains: m.setMyEnum(Proto3Test.TestProto3Enum.VALUE_1);
                m.setMyEnumValue(1);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void confusinglyNamedField_noFinding() {
    helper
        .addSourceLines(
            "Test.java",
            """
            import com.google.errorprone.bugpatterns.proto.ProtoTest.TestProtoWithConfusingNames;

            class Test {
              void f(TestProtoWithConfusingNames.Builder m) {
                m.setBarFieldValue(1);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void zerothElement_finding() {
    helper
        .addSourceLines(
            "Test.java",
            """
            import com.google.errorprone.bugpatterns.proto.Proto3Test;
            import com.google.errorprone.bugpatterns.proto.Proto3Test.TestProto3Message;

            class Test {
              void f(TestProto3Message.Builder m) {
                // BUG: Diagnostic contains: m.setMyEnum(Proto3Test.TestProto3Enum.UNKNOWN);
                m.setMyEnumValue(0);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void unrecognisedNumber_noFinding() {
    helper
        .addSourceLines(
            "Test.java",
            """
            import com.google.errorprone.bugpatterns.proto.Proto3Test;
            import com.google.errorprone.bugpatterns.proto.Proto3Test.TestProto3Message;

            class Test {
              void f(TestProto3Message.Builder m) {
                m.setMyEnumValue(99);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void repeatedField() {
    helper
        .addSourceLines(
            "Test.java",
            """
            import com.google.errorprone.bugpatterns.proto.Proto3Test;
            import com.google.errorprone.bugpatterns.proto.Proto3Test.TestProto3Message;

            class Test {
              void f(TestProto3Message.Builder m) {
                // BUG: Diagnostic contains: m.addRepeatedEnum(Proto3Test.TestProto3Enum.VALUE_1);
                m.addRepeatedEnumValue(1);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void mapField() {
    helper
        .addSourceLines(
            "Test.java",
            """
            import com.google.errorprone.bugpatterns.proto.Proto3Test;
            import com.google.errorprone.bugpatterns.proto.Proto3Test.TestProto3Message;

            class Test {
              void f(TestProto3Message.Builder m) {
                // BUG: Diagnostic contains: m.putMapEnum("a", Proto3Test.TestProto3Enum.VALUE_1);
                m.putMapEnumValue("a", 1);
              }
            }
            """)
        .doTest();
  }
}
