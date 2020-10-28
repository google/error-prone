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
import com.google.errorprone.BugCheckerRefactoringTestHelper.TestMode;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link UseEnumSwitch}Test */
@RunWith(JUnit4.class)
public class UseEnumSwitchTest {

  @Test
  public void refactoring() {
    BugCheckerRefactoringTestHelper.newInstance(new UseEnumSwitch(), getClass())
        .addInputLines(
            "Test.java",
            "class Test {",
            "  enum E { ONE, TWO, THREE }",
            "  int f(E e) {",
            "    if (e.equals(E.ONE)) {",
            "      return 1;",
            "    } else if (e.equals(E.TWO)) {",
            "      return 2;",
            "    } else {",
            "      return 3;",
            "    }",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "class Test {",
            "  enum E { ONE, TWO, THREE }",
            "  int f(E e) {",
            "    switch (e) {",
            "      case ONE:",
            "        return 1;",
            "      case TWO:",
            "        return 2;",
            "      default:",
            "        return 3;",
            "    }",
            "  }",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void nonConstantEnum() {
    BugCheckerRefactoringTestHelper.newInstance(new UseEnumSwitch(), getClass())
        .addInputLines(
            "Test.java",
            "class Test {",
            "  enum E {",
            "    ONE, TWO, THREE;",
            "    E one() {",
            "      return ONE;",
            "    }",
            "  }",
            "  int f(E e) {",
            "    if (e == e.one()) {",
            "      return 1;",
            "    } else if (e == E.TWO) {",
            "      return 2;",
            "    } else {",
            "      return 3;",
            "    }",
            "  }",
            "}")
        .expectUnchanged()
        .doTest(TestMode.TEXT_MATCH);
  }
}
