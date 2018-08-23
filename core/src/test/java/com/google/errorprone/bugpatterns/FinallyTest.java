/*
 * Copyright 2013 The Error Prone Authors.
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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** @author cushon@google.com (Liam Miller-Cushon) */
@RunWith(JUnit4.class)
public class FinallyTest {

  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper = CompilationTestHelper.newInstance(Finally.class, getClass());
  }

  @Test
  public void testPositiveCase1() {
    compilationHelper.addSourceFile("FinallyPositiveCase1.java").doTest();
  }

  @Test
  public void testPositiveCase2() {
    compilationHelper.addSourceFile("FinallyPositiveCase2.java").doTest();
  }

  @Test
  public void testNegativeCase1() {
    compilationHelper.addSourceFile("FinallyNegativeCase1.java").doTest();
  }

  @Test
  public void testNegativeCase2() {
    compilationHelper.addSourceFile("FinallyNegativeCase2.java").doTest();
  }

  @Test
  public void lambda() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  void f() {",
            "    try {",
            "    } catch (Throwable t) {",
            "  } finally {",
            "      Runnable r = () -> { return; };",
            "    }",
            "  }",
            "}")
        .doTest();
  }
}
