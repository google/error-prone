/*
 * Copyright 2020 The Error Prone Authors.
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

/** {@link LongFloatConversion}Test */
@RunWith(JUnit4.class)
public class LongFloatConversionTest {

  private final CompilationTestHelper testHelper =
      CompilationTestHelper.newInstance(LongFloatConversion.class, getClass());

  @Test
  public void positive() {
    testHelper
        .addSourceLines(
            "Test.java", //
            "class Test {",
            "  void f(Long l) {}",
            "  void f(float l) {}",
            "  {",
            "    // BUG: Diagnostic contains:",
            "    f(0L);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative() {
    testHelper
        .addSourceLines(
            "Test.java", //
            "class Test {",
            "  void f(float l) {}",
            "  {",
            "    f((float) 0L);",
            "  }",
            "}")
        .doTest();
  }
}
