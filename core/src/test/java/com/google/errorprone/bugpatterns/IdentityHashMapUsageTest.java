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

/** Unit tests for {@link IdentityHashMapUsage} bug pattern. */
@RunWith(JUnit4.class)
public class IdentityHashMapUsageTest {
  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(IdentityHashMapUsage.class, getClass());
  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(IdentityHashMapUsage.class, getClass());

  @Test
  public void equals_putAll_positive() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.IdentityHashMap;",
            "import java.util.Map;",
            "class Test {",
            "  boolean test(Map map, IdentityHashMap ihm) {",
            "    // BUG: Diagnostic contains: IdentityHashMapUsage",
            "    return ihm.equals(map);",
            "  }",
            "  void putAll(Map map, IdentityHashMap ihm) {",
            "    // BUG: Diagnostic contains: IdentityHashMapUsage",
            "    ihm.putAll(map);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void equals_putAll_negative() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.IdentityHashMap;",
            "import java.util.Map;",
            "class Test {",
            "  boolean test(Map map, IdentityHashMap ihm) {",
            "    return map.equals(ihm);",
            "  }",
            "  boolean equalsSameType(IdentityHashMap ihm, IdentityHashMap ihm2) {",
            "    return ihm.equals(ihm2);",
            "  }",
            "  void putAll(Map map, IdentityHashMap ihm) {",
            "    map.putAll(ihm);",
            "  }",
            "  void putAllSameType(IdentityHashMap ihm, IdentityHashMap ihm2) {",
            "    ihm.putAll(ihm2);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void assignmentToMap() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.IdentityHashMap;",
            "import java.util.Map;",
            "class Test {",
            "  Map putAll(IdentityHashMap ihm) {",
            "    Map map;",
            "    // BUG: Diagnostic contains: IdentityHashMapUsage",
            "    map = ihm;",
            "    return map;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void variableInitializationToSuperType() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import java.util.IdentityHashMap;",
            "import java.util.Map;",
            "class Test {",
            "  Map putAll(IdentityHashMap ihmArg) {",
            "    Map map = ihmArg;",
            "    return map;",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import java.util.IdentityHashMap;",
            "import java.util.Map;",
            "class Test {",
            "  Map putAll(IdentityHashMap ihmArg) {",
            "    IdentityHashMap map = ihmArg;",
            "    return map;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void ihmInitializationWithNonIhm() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.IdentityHashMap;",
            "import java.util.Map;",
            "class Test {",
            "  IdentityHashMap something(Map mapArg) {",
            "    // BUG: Diagnostic contains: IdentityHashMapUsage",
            "    return new IdentityHashMap(mapArg);",
            "  }",
            "}")
        .doTest();
  }
}
