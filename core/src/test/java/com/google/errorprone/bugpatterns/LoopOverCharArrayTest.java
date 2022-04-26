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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link LoopOverCharArray}Test */
@RunWith(JUnit4.class)
public class LoopOverCharArrayTest {
  @Test
  public void refactor() {
    BugCheckerRefactoringTestHelper.newInstance(LoopOverCharArray.class, getClass())
        .addInputLines(
            "Test.java",
            "class T {",
            "  void f(String s) {",
            "    for (char c : s.toCharArray()) {",
            "      System.err.print(c);",
            "    }",
            "    for (char i : s.toCharArray()) {",
            "      System.err.print(i);",
            "    }",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "class T {",
            "  void f(String s) {",
            "    for (int i = 0; i < s.length(); i++) {",
            "      char c = s.charAt(i);",
            "      System.err.print(c);",
            "    }",
            "    for (char i : s.toCharArray()) {",
            "      System.err.print(i);",
            "    }",
            "  }",
            "}")
        .doTest();
  }
}
