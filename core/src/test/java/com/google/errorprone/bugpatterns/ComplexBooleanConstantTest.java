/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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
import java.io.IOException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Unit tests for {@link ComplexBooleanConstantTest}.
 *
 * @author Sumit Bhagwani (bhagwani@google.com)
 */
@RunWith(JUnit4.class)
public class ComplexBooleanConstantTest {
  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(new ComplexBooleanConstant(), getClass());

  @Test
  public void refactorTest() throws IOException {
    refactoringHelper
        .addInputLines(
            "in/Foo.java",
            "class Foo {",
            "  int CONSTANT1 = 1;",
            "  int CONSTANT2 = 1;",
            "  int barNoOp() {",
            "    return 1 - 1;",
            "  }",
            "  boolean barNoOpWithConstants() {",
            "    return CONSTANT1 == CONSTANT2;",
            "  }",
            "  boolean barEquals() {",
            "    return 1 == 1;",
            "  }",
            "  boolean barNotEquals() {",
            "    return 1 != 1;",
            "  }",
            "}")
        .addOutputLines(
            "out/Foo.java",
            "class Foo {",
            "  int CONSTANT1 = 1;",
            "  int CONSTANT2 = 1;",
            "  int barNoOp() {",
            "    return 1 - 1;",
            "  }",
            "  boolean barNoOpWithConstants() {",
            "    return CONSTANT1 == CONSTANT2;",
            "  }",
            "  boolean barEquals() {",
            "    return true;",
            "  }",
            "  boolean barNotEquals() {",
            "    return false;",
            "  }",
            "}")
        .doTest(TestMode.AST_MATCH);
  }
}
