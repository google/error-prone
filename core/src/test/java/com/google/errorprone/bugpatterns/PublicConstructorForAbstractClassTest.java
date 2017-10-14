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
import com.google.errorprone.CompilationTestHelper;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;

@RunWith(JUnit4.class)
public class PublicConstructorForAbstractClassTest {

  @Test
  public void basicRefactoringTest() throws IOException {
    BugCheckerRefactoringTestHelper testHelper =
        BugCheckerRefactoringTestHelper
            .newInstance(new PublicConstructorForAbstractClass(), getClass());
    testHelper.addInputLines(
        "in/Test.java",
        "public abstract class Test {",
        "  public Test() {",
        "  }",
        "}"
    ).addOutputLines(
        "out/Test.java",
        "public abstract class Test {",
        "  protected Test() {",
        "  }",
        "}"
    ).doTest();
  }

  @Test
  public void basicUsageTest() {
    CompilationTestHelper testHelper = CompilationTestHelper
        .newInstance(PublicConstructorForAbstractClass.class, getClass());
    testHelper.addSourceLines(
        "in/Test.java",
        "public abstract class Test {",
        "  // BUG: Diagnostic contains:",
        "  public Test() {",
        "  }",
        "}"
    ).doTest();
  }

  @Test
  public void basicNegativeTest() {
    CompilationTestHelper testHelper = CompilationTestHelper
        .newInstance(PublicConstructorForAbstractClass.class, getClass());
    testHelper.addSourceLines(
        "in/Test.java",
        "public class Test {",
        "  public Test() {",
        "  }",
        "}"
    ).doTest();
  }
}
