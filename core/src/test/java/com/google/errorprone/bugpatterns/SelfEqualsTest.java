/*
 * Copyright 2016 The Error Prone Authors.
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

/**
 * Unit tests for {@link SelfEquals} bug pattern.
 *
 * @author bhagwani@google.com (Sumit Bhagwani)
 */
@RunWith(JUnit4.class)
public class SelfEqualsTest {
  CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper = CompilationTestHelper.newInstance(SelfEquals.class, getClass());
  }

  @Test
  public void testPositiveCase() {
    compilationHelper.addSourceFile("SelfEqualsPositiveCase.java").doTest();
  }

  @Test
  public void testNegativeCase() {
    compilationHelper.addSourceFile("SelfEqualsNegativeCases.java").doTest();
  }

  @Test
  public void positiveFix() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  <T> boolean f() {",
            "    T t = null;",
            "    int y = 0;",
            "    // BUG: Diagnostic contains:",
            "    return t.equals(t);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testPositiveCase_guava() {
    compilationHelper.addSourceFile("GuavaSelfEqualsPositiveCase.java").doTest();
  }

  @Test
  public void testNegativeCase_guava() {
    compilationHelper.addSourceFile("GuavaSelfEqualsNegativeCases.java").doTest();
  }

  @Test
  public void enclosingStatement() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.base.Objects;",
            "class Test {",
            "  Object a = new Object();",
            "  // BUG: Diagnostic contains:",
            "  boolean b = Objects.equal(a, a);",
            "}")
        .doTest();
  }
}
