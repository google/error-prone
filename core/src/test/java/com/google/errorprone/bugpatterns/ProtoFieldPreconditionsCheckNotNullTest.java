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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import org.junit.Ignore;

/** @author awturner@google.com (Andy Turner) */
@Ignore("b/74365407 test proto sources are broken")
@RunWith(JUnit4.class)
public class ProtoFieldPreconditionsCheckNotNullTest {

  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper =
        CompilationTestHelper.newInstance(ProtoFieldPreconditionsCheckNotNull.class, getClass());
  }

  @Test
  public void testPositiveCase() {
    compilationHelper
        .addSourceFile("ProtoFieldPreconditionsCheckNotNullPositiveCases.java")
        .doTest();
  }

  @Test
  public void testNegativeCase() {
    compilationHelper
        .addSourceFile("ProtoFieldPreconditionsCheckNotNullNegativeCases.java")
        .doTest();
  }

  @Test
  public void disabled() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import static com.google.common.base.Preconditions.checkNotNull;",
            "import com.google.errorprone.bugpatterns.proto.ProtoTest.TestProtoMessage;",
            "import java.util.List;",
            "class Test {",
            "  void test() {",
            "    TestProtoMessage message = TestProtoMessage.newBuilder().build();",
            "    checkNotNull(message.getMessage());",
            "  }",
            "}")
        .setArgs(ImmutableList.of("-XepOpt:ProtoFieldNullComparison:MatchCheckNotNull"))
        .doTest();
  }
}
