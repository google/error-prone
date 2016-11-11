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

/** {@link ImmutableModification}Test */
@RunWith(JUnit4.class)
public class ImmutableModificationTest {
  private final CompilationTestHelper testHelper =
      CompilationTestHelper.newInstance(ImmutableModification.class, getClass());

  @Test
  public void positive() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableList;",
            "class Test {",
            "  void f(ImmutableList<String> xs) {",
            "    // BUG: Diagnostic contains:",
            "    xs.remove(0);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableList;",
            "class Test {",
            "  void f(ImmutableList<String> xs) {",
            "    xs.get(0);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void setView() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.Sets;",
            "import java.util.Set;",
            "class Test {",
            "  void f(Set<String> a, Set<String> b) {",
            "    // BUG: Diagnostic contains:",
            "    Sets.union(a, b).remove(0);",
            "  }",
            "}")
        .doTest();
  }
}
