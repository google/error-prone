/*
 * Copyright 2021 The Error Prone Authors.
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

/** {@link DistinctVarargsChecker}Test */
@RunWith(JUnit4.class)
public class DistinctVarargsCheckerTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(DistinctVarargsChecker.class, getClass());
  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(DistinctVarargsChecker.class, getClass());

  @Test
  public void distinctVarargsChecker_sameVariableInFuturesVaragsMethods_shouldFlag() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.util.concurrent.Futures;",
            "import com.google.common.util.concurrent.ListenableFuture;",
            "public class Test {",
            "  void testFunction() {",
            "    ListenableFuture firstFuture = null, secondFuture = null;",
            "    // BUG: Diagnostic contains: DistinctVarargsChecker",
            "    Futures.whenAllSucceed(firstFuture, firstFuture);",
            "    // BUG: Diagnostic contains: DistinctVarargsChecker",
            "    Futures.whenAllSucceed(firstFuture, firstFuture, secondFuture);",
            "    // BUG: Diagnostic contains: DistinctVarargsChecker",
            "    Futures.whenAllComplete(firstFuture, firstFuture);",
            "    // BUG: Diagnostic contains: DistinctVarargsChecker",
            "    Futures.whenAllComplete(firstFuture, firstFuture, secondFuture);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void distinctVarargsCheckerdifferentVariableInFuturesVaragsMethods_shouldNotFlag() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.util.concurrent.Futures;",
            "import com.google.common.util.concurrent.ListenableFuture;",
            "public class Test {",
            "  void testFunction() {",
            "    ListenableFuture firstFuture = null, secondFuture = null;",
            "    Futures.whenAllComplete(firstFuture);",
            "    Futures.whenAllSucceed(firstFuture, secondFuture);",
            "    Futures.whenAllComplete(firstFuture);",
            "    Futures.whenAllComplete(firstFuture, secondFuture);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void distinctVarargsChecker_sameVariableInGuavaVarargMethods_shouldFlag() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.Ordering;",
            "import com.google.common.collect.ImmutableSortedMap;",
            "import com.google.common.collect.ImmutableSet;",
            "import com.google.common.collect.ImmutableSortedSet;",
            "public class Test {",
            "  void testFunction() {",
            "    int first = 1, second = 2;",
            "    // BUG: Diagnostic contains: DistinctVarargsChecker",
            "    Ordering.explicit(first, first);",
            "    // BUG: Diagnostic contains: DistinctVarargsChecker",
            "    Ordering.explicit(first, first, second);",
            "    // BUG: Diagnostic contains: DistinctVarargsChecker",
            "    ImmutableSortedMap.of(first, second, first, second);",
            "    // BUG: Diagnostic contains: DistinctVarargsChecker",
            "    ImmutableSet.of(first, first);",
            "    // BUG: Diagnostic contains: DistinctVarargsChecker",
            "    ImmutableSet.of(first, first, second);",
            "    // BUG: Diagnostic contains: DistinctVarargsChecker",
            "    ImmutableSortedSet.of(first, first);",
            "    // BUG: Diagnostic contains: DistinctVarargsChecker",
            "    ImmutableSortedSet.of(first, first, second);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void distinctVarargsChecker_differentVariableInGuavaVarargMethods_shouldNotFlag() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.Ordering;",
            "import com.google.common.collect.ImmutableBiMap;",
            "import com.google.common.collect.ImmutableMap;",
            "import com.google.common.collect.ImmutableSortedMap;",
            "import com.google.common.collect.ImmutableSet;",
            "import com.google.common.collect.ImmutableSortedSet;",
            "public class Test {",
            "  void testFunction() {",
            "    int first = 1, second = 2, third = 3, fourth = 4;",
            "    Ordering.explicit(first);",
            "    Ordering.explicit(first, second);",
            "    ImmutableMap.of(first, second);",
            "    ImmutableSortedMap.of(first, second);",
            "    ImmutableBiMap.of(first, second, third, fourth);",
            "    ImmutableSet.of(first);",
            "    ImmutableSet.of(first, second);",
            "    ImmutableSortedSet.of(first);",
            "    ImmutableSortedSet.of(first, second);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void distinctVarargsChecker_sameVariableInImmutableSetVarargsMethod_shouldRefactor() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import com.google.common.collect.ImmutableSet;",
            "import com.google.common.collect.ImmutableSortedSet;",
            "public class Test {",
            "  void testFunction() {",
            "    int first = 1, second = 2;",
            "    ImmutableSet.of(first, first);",
            "    ImmutableSet.of(first, first, second);",
            "    ImmutableSortedSet.of(first, first);",
            "    ImmutableSortedSet.of(first, first, second);",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import com.google.common.collect.ImmutableSet;",
            "import com.google.common.collect.ImmutableSortedSet;",
            "public class Test {",
            "  void testFunction() {",
            "    int first = 1, second = 2;",
            "    ImmutableSet.of(first);",
            "    ImmutableSet.of(first, second);",
            "    ImmutableSortedSet.of(first);",
            "    ImmutableSortedSet.of(first, second);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void distinctVarargsChecker_differentVarsInImmutableSetVarargsMethod_shouldNotRefactor() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import com.google.common.collect.ImmutableSet;",
            "import com.google.common.collect.ImmutableSortedSet;",
            "public class Test {",
            "  void testFunction() {",
            "    int first = 1, second = 2;",
            "    ImmutableSet.of(first);",
            "    ImmutableSet.of(first, second);",
            "    ImmutableSortedSet.of(first);",
            "    ImmutableSortedSet.of(first, second);",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import com.google.common.collect.ImmutableSet;",
            "import com.google.common.collect.ImmutableSortedSet;",
            "public class Test {",
            "  void testFunction() {",
            "    int first = 1, second = 2;",
            "    ImmutableSet.of(first);",
            "    ImmutableSet.of(first, second);",
            "    ImmutableSortedSet.of(first);",
            "    ImmutableSortedSet.of(first, second);",
            "  }",
            "}")
        .doTest();
  }
}
