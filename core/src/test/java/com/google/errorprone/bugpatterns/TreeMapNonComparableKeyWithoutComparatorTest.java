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
import org.junit.Test;

public class TreeMapNonComparableKeyWithoutComparatorTest {
  @Test
  public void testPositive() {
    CompilationTestHelper.newInstance(TreeMapNonComparableKeyWithoutComparator.class, getClass())
        .addSourceLines(
            "test/NoBugs.java",
            "package test;",
            "import java.util.TreeMap;",
            "public class NoBugs { ",
            // Comparable keys
            "  Object o = new TreeMap<String, String>();",
            "  TreeMap<String, String> map = new TreeMap<>();",
            // Uses comparator
            "  Object comp = new TreeMap<Object, Object>((o1, o2) -> 0);",
            "}")
        .doTest();
  }

  @Test
  public void testNegative() {
    CompilationTestHelper.newInstance(TreeMapNonComparableKeyWithoutComparator.class, getClass())
        .addSourceLines(
            "test/Bugs.java",
            "package test;",
            "import java.util.TreeMap;",
            "public class Bugs { ",
            // Not comparable keys
            "  // BUG: Diagnostic contains:",
            "  Object o = new TreeMap<Object, Integer>();",
            "  // BUG: Diagnostic contains:",
            "  Object b = new TreeMap<byte[], Object>();",
            // Chaining
            "  // BUG: Diagnostic contains:",
            "  Object s = new TreeMap<Object, Object>().entrySet();",
            // Raw types
            "  // BUG: Diagnostic contains:",
            "  TreeMap map = new TreeMap();",
            "}")
        .doTest();
  }
}
