/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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

package com.google.errorprone.matchers;

import static com.google.errorprone.matchers.Matchers.inLoop;

import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.MatcherChecker;
import com.google.errorprone.bugpatterns.BugChecker;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Unit tests for {@link Matchers}.
 */
@RunWith(JUnit4.class)
public class MatchersTest {

  @Test
  public void inLoopShouldMatchInWhileLoop() {
    CompilationTestHelper compilationHelper = CompilationTestHelper.newInstance(
        inLoopChecker(), getClass());
    compilationHelper
        .addSourceLines("Test.java",
            "public class Test {",
            "  public void doIt() {",
            "    while (true) {",
            "      // BUG: Diagnostic contains:",
            "      System.out.println();",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void inLoopShouldMatchInDoLoop() {
    CompilationTestHelper compilationHelper = CompilationTestHelper.newInstance(
        inLoopChecker(), getClass());
    compilationHelper
        .addSourceLines("Test.java",
            "public class Test {",
            "  public void doIt() {",
            "    do {",
            "      // BUG: Diagnostic contains:",
            "      System.out.println();",
            "    } while (true);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void inLoopShouldMatchInForLoop() {
    CompilationTestHelper compilationHelper = CompilationTestHelper.newInstance(
        inLoopChecker(), getClass());
    compilationHelper
        .addSourceLines("Test.java",
            "public class Test {",
            "  public void doIt() {",
            "    for (; true; ) {",
            "      // BUG: Diagnostic contains:",
            "      System.out.println();",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void inLoopShouldMatchInEnhancedForLoop() {
    CompilationTestHelper compilationHelper = CompilationTestHelper.newInstance(
        inLoopChecker(), getClass());
    compilationHelper
        .addSourceLines("Test.java",
            "import java.util.List;",
            "public class Test {",
            "  public void doIt(List<String> strings) {",
            "    for (String s : strings) {",
            "      // BUG: Diagnostic contains:",
            "      System.out.println();",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void inLoopShouldNotMatchInInitializerWithoutLoop() {
    CompilationTestHelper compilationHelper = CompilationTestHelper.newInstance(
        inLoopChecker(), getClass());
    compilationHelper
        .addSourceLines("Test.java",
            "import java.util.List;",
            "public class Test {",
            "  {",
            "    System.out.println();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void inLoopShouldMatchInInitializerInLoop() {
    CompilationTestHelper compilationHelper = CompilationTestHelper.newInstance(
        inLoopChecker(), getClass());
    compilationHelper
        .addSourceLines("Test.java",
            "import java.util.List;",
            "public class Test {",
            "  {",
            "    int count = 0;",
            "    while (count < 10) {",
            "      // BUG: Diagnostic contains:",
            "      System.out.println();",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void inLoopShouldNotMatchInAnonymousInnerClassDefinedInLoop() {
    CompilationTestHelper compilationHelper = CompilationTestHelper.newInstance(
        inLoopChecker(), getClass());
    compilationHelper
        .addSourceLines("Test.java",
            "import java.util.*;",
            "public class Test {",
            "  public void sort(List<List<String>> stringLists) {",
            "    for (List<String> stringList : stringLists) {",
            "      Collections.sort(stringList, new Comparator<String>() {",
            "          {",
            "            System.out.println();",
            "          }",
            "          public int compare(String s1, String s2) {",
            "            return 0;",
            "          }",
            "          public boolean equals(Object obj) {",
            "            return false;",
            "          }",
            "      });",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  private static BugChecker inLoopChecker() {
    return new MatcherChecker("System.out.println();", inLoop());
  }
}
