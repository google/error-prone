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

/**
 * @author awturner@google.com (Andy Turner)
 */
@RunWith(JUnit4.class)
public class LossyPrimitiveCompareTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(LossyPrimitiveCompare.class, getClass());

  @Test
  public void doubleCompare() {
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  int[] results = {",
            "    // BUG: Diagnostic contains: Long.compare(0L, 0L)",
            "    Double.compare(0L, 0L),",
            "    // BUG: Diagnostic contains: Long.compare(Long.valueOf(0L), 0L)",
            "    Double.compare(Long.valueOf(0L), 0L),",
            "",
            "    // Not lossy.",
            "    Double.compare((byte) 0, (byte) 0),",
            "    Double.compare((short) 0, (short) 0),",
            "    Double.compare('0', '0'),",
            "    Double.compare(0, 0),",
            "    Double.compare(0.f, 0.f),",
            "    Double.compare(0.0, 0.0),",
            "  };",
            "}")
        .doTest();
  }

  @Test
  public void floatCompare() {
    helper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  int[] results = {",
            "    // BUG: Diagnostic contains: Long.compare(0L, 0L)",
            "    Float.compare(0L, 0L),",
            "    // BUG: Diagnostic contains: Long.compare(Long.valueOf(0L), 0L)",
            "    Float.compare(Long.valueOf(0L), 0L),",
            "    // BUG: Diagnostic contains: Integer.compare(0, 0)",
            "    Float.compare(0, 0),",
            "    // BUG: Diagnostic contains: Integer.compare(0, Integer.valueOf(0))",
            "    Float.compare(0, Integer.valueOf(0)),",
            "",
            "    // Not lossy.",
            "    Float.compare((byte) 0, (byte) 0),",
            "    Float.compare((short) 0, (short) 0),",
            "    Float.compare('0', '0'),",
            "    Float.compare(0.f, 0.f),",
            "  };",
            "}")
        .doTest();
  }
}
