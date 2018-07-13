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

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import java.io.IOException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link UnnecessaryParentheses}Test */
@RunWith(JUnit4.class)
public class UnnecessaryParenthesesTest {
  private final BugCheckerRefactoringTestHelper testHelper =
      BugCheckerRefactoringTestHelper.newInstance(new UnnecessaryParentheses(), getClass());

  @Test
  public void test() throws IOException {
    testHelper
        .addInputLines(
            "in/Test.java",
            "class Test {",
            "  void f(int x) {",
            "    if (true) System.err.println((x));",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "class Test {",
            "  void f(int x) {",
            "    if (true) System.err.println(x);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void anonymousClass() throws IOException {
    testHelper
        .addInputLines(
            "in/Test.java",
            "import com.google.common.base.Function;",
            "import com.google.common.collect.Iterables;",
            "import java.util.List;",
            "class Test {",
            "  Iterable<Integer> f(List<Integer> l) {",
            "    return Iterables.transform(",
            "        l,",
            "        (new Function<Integer, Integer>() {",
            "          public Integer apply(Integer a) {",
            "              return a * 2;",
            "         }",
            "        }));",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import com.google.common.base.Function;",
            "import com.google.common.collect.Iterables;",
            "import java.util.List;",
            "class Test {",
            "  Iterable<Integer> f(List<Integer> l) {",
            "    return Iterables.transform(",
            "        l,",
            "        new Function<Integer, Integer>() {",
            "          public Integer apply(Integer a) {",
            "              return a * 2;",
            "         }",
            "        });",
            "  }",
            "}")
        .doTest();
  }
}
