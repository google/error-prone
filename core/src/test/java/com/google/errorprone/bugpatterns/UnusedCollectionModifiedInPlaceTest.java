/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

/** {@link UnusedCollectionModifiedInPlace}Test */
@RunWith(JUnit4.class)
public class UnusedCollectionModifiedInPlaceTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(UnusedCollectionModifiedInPlace.class, getClass());

  @Test
  public void collectionsMethodCoverage() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.Collections;",
            "import java.util.ArrayList;",
            "import java.util.List;",
            "class Test {",
            "  void doIt(List<String> myList) {",
            "    // BUG: Diagnostic contains:",
            "    Collections.copy(new ArrayList<>(myList), null);",
            "    // BUG: Diagnostic contains:",
            "    Collections.fill(new ArrayList<>(myList), null);",
            "    // BUG: Diagnostic contains:",
            "    Collections.reverse(new ArrayList<>(myList));",
            "    // BUG: Diagnostic contains:",
            "    Collections.rotate(new ArrayList<>(myList), 5);",
            "    // BUG: Diagnostic contains:",
            "    Collections.shuffle(new ArrayList<>(myList));",
            "    // BUG: Diagnostic contains:",
            "    Collections.shuffle(new ArrayList<>(myList), null);",
            "    // BUG: Diagnostic contains:",
            "    Collections.sort(new ArrayList<>(myList));",
            "    // BUG: Diagnostic contains:",
            "    Collections.sort(new ArrayList<>(myList), null);",
            "    // BUG: Diagnostic contains:",
            "    Collections.swap(new ArrayList<>(myList), 1, 2);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void listsNewArrayList() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.Lists;",
            "import java.util.Collections;",
            "import java.util.List;",
            "class Test {",
            "  void doIt(List<String> myList) {",
            "    // BUG: Diagnostic contains:",
            "    Collections.sort(Lists.newArrayList(myList));",
            "    // BUG: Diagnostic contains:",
            "    Collections.sort(Lists.newArrayList(myList), null);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void listsNewLinkedList() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.Lists;",
            "import java.util.Collections;",
            "import java.util.List;",
            "class Test {",
            "  void doIt(List<String> myList) {",
            "    // BUG: Diagnostic contains:",
            "    Collections.sort(Lists.newLinkedList(myList));",
            "    // BUG: Diagnostic contains:",
            "    Collections.sort(Lists.newLinkedList(myList), null);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.Collections;",
            "import java.util.List;",
            "class Test {",
            "  void doIt(List<String> myList) {",
            "    Collections.sort(myList);",
            "  }",
            "}")
        .doTest();
  }
}
