/*
 * Copyright 2024 The Error Prone Authors.
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

@RunWith(JUnit4.class)
public final class NegativeBooleanTest {

  private final CompilationTestHelper testHelper =
      CompilationTestHelper.newInstance(NegativeBoolean.class, getClass());

  @Test
  public void prefix_no_isFlagged() {
    testHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              void foo() {
                // BUG: Diagnostic contains: Prefer positive
                boolean noBar;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void prefix_not_isFlagged() {
    testHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              void foo() {
                // BUG: Diagnostic contains: Prefer positive
                boolean notBar;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void prefix_notable_isNotFlagged() {
    testHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              void foo() {
                boolean notableBar;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void integer_isNotFlagged() {
    testHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              void foo() {
                int notBar;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void parameter_isNotFlagged() {
    testHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              void foo(boolean notBar) {
                return;
              }
            }
            """)
        .doTest();
  }
}
