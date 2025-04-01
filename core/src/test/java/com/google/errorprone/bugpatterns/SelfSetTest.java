/*
 * Copyright 2025 The Error Prone Authors.
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

@RunWith(JUnit4.class)
@Ignore("b/130670719")
public final class SelfSetTest {
  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(SelfSet.class, getClass());

  @Test
  public void positiveCase() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import com.google.errorprone.bugpatterns.proto.ProtoTest.TestProtoMessage;

            final class Test {
              private void test(TestProtoMessage rhs) {
                TestProtoMessage.Builder lhs = TestProtoMessage.newBuilder();
                // BUG: Diagnostic contains:
                lhs.setMessage(lhs.getMessage());
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negativeCase() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import com.google.errorprone.bugpatterns.proto.ProtoTest.TestProtoMessage;

            final class Test {
              private void test(TestProtoMessage rhs) {
                TestProtoMessage.Builder lhs = TestProtoMessage.newBuilder();
                lhs.setMessage(lhs.getFooBuilder());
              }
            }
            """)
        .doTest();
  }
}
