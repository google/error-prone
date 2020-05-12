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

/** Unit tests for {@link IdentityHashMapBoxing} bug pattern. */
@RunWith(JUnit4.class)
public class IdentityHashMapBoxingTest {
  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(IdentityHashMapBoxing.class, getClass());

  @Test
  public void testPositiveCases() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.IdentityHashMap;",
            "import java.util.Map;",
            "class Test {",
            "  void test() {",
            "    // BUG: Diagnostic contains: IdentityHashMapBoxing",
            "    Map<Integer, String> map1 = new IdentityHashMap<>();",
            "    // BUG: Diagnostic contains: IdentityHashMapBoxing",
            "    Map<Float, String> map2 = new IdentityHashMap<>();",
            "    // BUG: Diagnostic contains: IdentityHashMapBoxing",
            "    Map<Double, String> map3 = new IdentityHashMap<>();",
            "    // BUG: Diagnostic contains: IdentityHashMapBoxing",
            "    Map<Long, String> map4 = new IdentityHashMap<>();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.IdentityHashMap;",
            "import java.util.Map;",
            "class Test {",
            "  void test() {",
            "    Map<String, Integer> map1 = new IdentityHashMap<>();",
            "    Map<String, Float> map2 = new IdentityHashMap<>();",
            "    Map<String, Double> map3 = new IdentityHashMap<>();",
            "    Map<String, Long> map4 = new IdentityHashMap<>();",
            "    Map<String, Object> map5 = new IdentityHashMap<>();",
            "    Map<Object, String> map6 = new IdentityHashMap<>();",
            "  }",
            "}")
        .doTest();
  }
}
