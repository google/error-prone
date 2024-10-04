/*
 * Copyright 2017 The Error Prone Authors.
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

/**
 * Tests for {@link ProtocolBufferOrdinal}.
 *
 * @author bhagwani@google.com (Sumit Bhagwani)
 */
@RunWith(JUnit4.class)
public class ProtocolBufferOrdinalTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(ProtocolBufferOrdinal.class, getClass());

  @Test
  public void positiveCase() {
    compilationHelper
        .addSourceLines(
            "ProtocolBufferOrdinalPositiveCases.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            import com.google.errorprone.bugpatterns.proto.ProtoTest.TestEnum;

            /** Positive test cases for {@link ProtocolBufferOrdinal} check. */
            public class ProtocolBufferOrdinalPositiveCases {

              public static void checkCallOnOrdinal() {
                // BUG: Diagnostic contains: ProtocolBufferOrdinal
                TestEnum.TEST_ENUM_VAL.ordinal();

                // BUG: Diagnostic contains: ProtocolBufferOrdinal
                ProtoLiteEnum.FOO.ordinal();
              }

              enum ProtoLiteEnum implements com.google.protobuf.Internal.EnumLite {
                FOO(1),
                BAR(2);
                private final int number;

                private ProtoLiteEnum(int number) {
                  this.number = number;
                }

                @Override
                public int getNumber() {
                  return number;
                }
              }
            }""")
        .doTest();
  }

  @Test
  public void negativeCase() {
    compilationHelper
        .addSourceLines(
            "ProtocolBufferOrdinalNegativeCases.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            import com.google.errorprone.bugpatterns.proto.ProtoTest.TestEnum;

            /** Negative test cases for {@link ProtocolBufferOrdinal} check. */
            public class ProtocolBufferOrdinalNegativeCases {

              public static void checkProtoEnum() {
                TestEnum.TEST_ENUM_VAL.getNumber();
              }
            }""")
        .doTest();
  }
}
