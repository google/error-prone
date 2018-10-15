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

import com.google.errorprone.CompilationTestHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Verify that there are no usages of Mockito internal implementations.
 *
 * @author tvanderlippe@google.com (Tim van der Lippe)
 */
@RunWith(JUnit4.class)
public class MockitoInternalUsageTest {

  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper = CompilationTestHelper.newInstance(MockitoInternalUsage.class, getClass());
  }

  @Test
  public void testPositiveCases() {
    compilationHelper.addSourceFile("testdata/MockitoInternalUsagePositiveCases.java").doTest();
  }

  // Comments in imports crash "g4 fix". Separate these out in a separate test case. b/74235047
  @Test
  public void testPositiveImportsCases() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "package foo;",
            "// BUG: Diagnostic contains:",
            "import org.mockito.internal.MockitoCore;",
            "// BUG: Diagnostic contains:",
            "import org.mockito.internal.stubbing.InvocationContainer;",
            "// BUG: Diagnostic contains:",
            "import org.mockito.internal.*;",
            "class Test {}")
        .doTest();
  }

  @Test
  public void testNegativeCases() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "package org.mockito;",
            "import org.mockito.internal.MockitoCore;",
            "class Mockito {}")
        .doTest();
  }
}
