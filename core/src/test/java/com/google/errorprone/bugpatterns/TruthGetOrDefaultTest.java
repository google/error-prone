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

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Unit tests for {@link TruthGetOrDefault} bug pattern.
 *
 * @author bhagwani@google.com (Sumit Bhagwani)
 */
@RunWith(JUnit4.class)
public class TruthGetOrDefaultTest {
  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(TruthGetOrDefault.class, getClass());
  private final BugCheckerRefactoringTestHelper testHelper =
      BugCheckerRefactoringTestHelper.newInstance(TruthGetOrDefault.class, getClass());

  @Test
  public void testPositiveCases() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "import java.util.HashMap;",
            "import java.util.Map;",
            "class Test {",
            "  void test() {",
            "    Map<String, Integer> map = new HashMap<>();",
            "    // BUG: Diagnostic contains: TruthGetOrDefault",
            "    assertThat(map.getOrDefault(\"key\",  0)).isEqualTo(0);",
            "    Integer expectedVal = 0;",
            "    // BUG: Diagnostic contains: TruthGetOrDefault",
            "    assertThat(map.getOrDefault(\"key\",  expectedVal)).isEqualTo(expectedVal);",
            "    Map<String, Long> longMap = new HashMap<>();",
            "    // BUG: Diagnostic contains: TruthGetOrDefault",
            "    assertThat(longMap.getOrDefault(\"key\",  0L)).isEqualTo(5L);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "import java.util.HashMap;",
            "import java.util.Map;",
            "class Test {",
            "  void test() {",
            "    Map<String, Integer> map = new HashMap<>();",
            "    Integer expectedVal = 10;",
            "    assertThat(map.getOrDefault(\"key\",  0)).isEqualTo(expectedVal);",
            "    assertThat(map.getOrDefault(\"key\",  Integer.valueOf(0)))",
            "      .isEqualTo(Integer.valueOf(1));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testFixGeneration() {
    testHelper
        .addInputLines(
            "in/Test.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "import java.util.HashMap;",
            "import java.util.Map;",
            "class Test {",
            "  void test() {",
            "    Map<String, Integer> map = new HashMap<>();",
            "    assertThat(map.getOrDefault(\"key\",  0)).isEqualTo(1);",
            "    Map<String, Long> longMap = new HashMap<>();",
            "    assertThat(longMap.getOrDefault(\"key\",  0L)).isEqualTo(0L);",
            "    assertThat(longMap.getOrDefault(\"key\",  0L)).isEqualTo(0);",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "import java.util.HashMap;",
            "import java.util.Map;",
            "class Test {",
            "  void test() {",
            "    Map<String, Integer> map = new HashMap<>();",
            "    assertThat(map).containsEntry(\"key\", 1);",
            "    Map<String, Long> longMap = new HashMap<>();",
            "    assertThat(longMap).doesNotContainKey(\"key\");",
            "    assertThat(longMap.getOrDefault(\"key\",  0L)).isEqualTo(0);",
            "  }",
            "}")
        .doTest();
  }
}
