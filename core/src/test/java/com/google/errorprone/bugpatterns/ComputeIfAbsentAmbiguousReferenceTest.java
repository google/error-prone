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
 * Unit tests for {@link ComputeIfAbsentAmbiguousReference} bug pattern.
 *
 * @author bhagwani@google.com (Sumit Bhagwani)
 */
@RunWith(JUnit4.class)
public class ComputeIfAbsentAmbiguousReferenceTest {
  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(ComputeIfAbsentAmbiguousReference.class, getClass());

  @Test
  public void testPositiveCase() {
    compilationHelper
        .addSourceLines(
            "in/Test.java",
            "import java.util.ArrayList;",
            "import java.util.HashMap;",
            "import java.util.List;",
            "import java.util.Map;",
            "import java.util.concurrent.atomic.AtomicLong;",
            "class Test {",
            "  private void doWorkAtomicLong(Map<Long, AtomicLong> map) {",
            "    Long key = 4L;",
            "    // BUG: Diagnostic contains: ComputeIfAbsentAmbiguousReference",
            "    map.computeIfAbsent(key, AtomicLong::new).incrementAndGet();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCase() {
    compilationHelper
        .addSourceLines(
            "in/Test.java",
            "import java.util.Map;",
            "import java.util.concurrent.atomic.AtomicLong;",
            "import java.util.function.Function;",
            "class Test {",
            "  private void doWork(Map<Long, AtomicLong> map) {",
            "    Long key = 4L;",
            "    Function<Long, AtomicLong> longBuilder = AtomicLong::new;",
            "    map.computeIfAbsent(key, k -> new AtomicLong(k));",
            "    map.computeIfAbsent(key, longBuilder);",
            "    map.computeIfAbsent(key, (Function<Long, AtomicLong>) AtomicLong::new);",
            "  }",
            "  private void doWorkStringArray(Map<Integer, String[]> map) {",
            "    Integer key = 4;",
            "    map.computeIfAbsent(key, String[]::new);",
            "  }",
            "  private void doWorkInnerClass1(Map<Long, InnerClass1> map) {",
            "    map.computeIfAbsent(0L, InnerClass1::new);",
            "  }",
            "  /** Class with exactly one 1-argument constructor. **/",
            "  class InnerClass1 {",
            "    InnerClass1(long l) {}",
            "  }",
            "  private void doWorkInnerClass2(Map<Integer, InnerClass2> map) {",
            "    map.computeIfAbsent(0, InnerClass2::new);",
            "  }",
            "  /** Class with two 1-argument constructors. **/",
            "  class InnerClass2 {",
            "    InnerClass2(int i) {}",
            "    InnerClass2(String s) {}",
            "  }",
            "}")
        .doTest();
  }
}
