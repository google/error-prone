/*
 * Copyright 2013 Google Inc. All Rights Reserved.
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

import com.google.common.base.Predicate;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** @author flx@google.com (Felix Berger) */
@RunWith(JUnit4.class)
public class ProtoFieldNullComparisonTest {

  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() throws Exception {
    compilationHelper =
        CompilationTestHelper.newInstance(ProtoFieldNullComparison.class, getClass())
            .addSourceFile("proto/ProtoTest.java");
  }

  @Test
  public void testPositiveCase() throws Exception {
    compilationHelper.addSourceFile("ProtoFieldNullComparisonPositiveCases.java").doTest();
  }

  @Test
  public void testNegativeCase() throws Exception {
    compilationHelper.addSourceFile("ProtoFieldNullComparisonNegativeCases.java").doTest();
  }

  @Test
  public void testProto3() {
    CompilationTestHelper.newInstance(ProtoFieldNullComparison.class, getClass())
        .addSourceFile("proto/Proto3Test.java")
        .addSourceLines(
            "TestProto3.java",
            "import com.google.errorprone.bugpatterns.proto.Proto3Test.TestProto3Message;",
            "public class TestProto3 {",
            "  public boolean doIt(TestProto3Message proto3Message) {",
            "    // BUG: Diagnostic matches: NO_FIX",
            "    return proto3Message.getMyString() == null;",
            "  }",
            "}")
        .expectErrorMessage(
            "NO_FIX",
            new Predicate<String>() {
              @Override
              public boolean apply(String input) {
                return !input.contains("hasMyString()");
              }
            })
        .doTest();
  }
}
