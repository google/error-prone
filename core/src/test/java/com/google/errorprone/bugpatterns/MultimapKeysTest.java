/*
 * Copyright 2023 The Error Prone Authors.
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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class MultimapKeysTest {
  private final BugCheckerRefactoringTestHelper refactoring =
      BugCheckerRefactoringTestHelper.newInstance(MultimapKeys.class, getClass());

  @Test
  public void positive() {
    refactoring
        .addInputLines(
            "Test.java",
            "import com.google.common.collect.Multimap;",
            "class Test {",
            "  void test(Multimap<String, String> m) {",
            "    for (String k : m.keys()) {}",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import com.google.common.collect.Multimap;",
            "class Test {",
            "  void test(Multimap<String, String> m) {",
            "    for (String k : m.keySet()) {}",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveSubclass() {
    refactoring
        .addInputLines(
            "Test.java",
            "import com.google.common.collect.SetMultimap;",
            "class Test {",
            "  void test(SetMultimap<String, String> m) {",
            "    for (String k : m.keys()) {}",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import com.google.common.collect.SetMultimap;",
            "class Test {",
            "  void test(SetMultimap<String, String> m) {",
            "    for (String k : m.keySet()) {}",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveFunctional() {
    refactoring
        .addInputLines(
            "Test.java",
            "import com.google.common.collect.Multimap;",
            "class Test {",
            "  void test(Multimap<String, String> m) {",
            "     m.keys().forEach(x -> {});",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import com.google.common.collect.Multimap;",
            "class Test {",
            "  void test(Multimap<String, String> m) {",
            "     m.keySet().forEach(x -> {});",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative() {
    refactoring
        .addInputLines(
            "Test.java",
            "import com.google.common.collect.Multimap;",
            "import com.google.common.collect.Multiset;",
            "class Test {",
            "  Multiset<String> test(Multimap<String, String> m) {",
            "    return m.keys();",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }
}
