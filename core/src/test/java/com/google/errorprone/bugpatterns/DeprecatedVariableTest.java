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

import static com.google.common.truth.TruthJUnit.assume;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link DeprecatedVariable}Test */
@RunWith(JUnit4.class)
public class DeprecatedVariableTest {
  private final BugCheckerRefactoringTestHelper testHelper =
      BugCheckerRefactoringTestHelper.newInstance(DeprecatedVariable.class, getClass());

  @Test
  public void refactor() {
    testHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              @Deprecated int x;

              void f(@Deprecated int x) {
                @Deprecated int y;
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              @Deprecated int x;

              void f(int x) {
                int y;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void refactorBindingVariables() {
    assume().that(Runtime.version().feature()).isAtLeast(21);

    testHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              void f() {
                if (toString() instanceof @Deprecated String s) {}
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              void f() {
                if (toString() instanceof String s) {}
              }
            }
            """)
        .doTest();
  }
}
