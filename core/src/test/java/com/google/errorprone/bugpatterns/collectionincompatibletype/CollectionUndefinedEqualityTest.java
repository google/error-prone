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

package com.google.errorprone.bugpatterns.collectionincompatibletype;

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link CollectionUndefinedEquality}. */
@RunWith(JUnit4.class)
public final class CollectionUndefinedEqualityTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(CollectionUndefinedEquality.class, getClass());

  @Test
  public void collectionOfCollections() {
    helper
        .addSourceLines(
            "Test.java",
            "import java.util.Collection;",
            "class Test {",
            "  boolean foo(Collection<Collection<Integer>> xs, Collection<Integer> x) {",
            "    // BUG: Diagnostic contains:",
            "    return xs.contains(x);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void collectionOfLists_noFinding() {
    helper
        .addSourceLines(
            "Test.java",
            "import java.util.Collection;",
            "import java.util.List;",
            "class Test {",
            "  boolean foo(Collection<List<Integer>> xs, List<Integer> x) {",
            "    return xs.contains(x);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void treeMap_noFinding() {
    helper
        .addSourceLines(
            "Test.java",
            "import java.util.Collection;",
            "import java.util.TreeMap;",
            "class Test {",
            "  boolean foo(TreeMap<Collection<Integer>, Integer> xs, Collection<Integer> x) {",
            "    return xs.containsKey(x);",
            "  }",
            "}")
        .doTest();
  }
}
