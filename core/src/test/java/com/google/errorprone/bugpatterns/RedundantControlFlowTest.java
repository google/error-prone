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
public final class RedundantControlFlowTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(RedundantControlFlow.class, getClass());

  @Test
  public void onlyStatementInForLoop() {
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              void test(Iterable<String> xs) {
                for (String x : xs) {
                  // BUG: Diagnostic contains:
                  continue;
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void onlyStatementInTerminalIf() {
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              void test(Iterable<String> xs) {
                for (String x : xs) {
                  if (x.equals("foo")) {
                    // BUG: Diagnostic contains:
                    continue;
                  }
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void onlyStatementInNestedIf() {
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              void test(Iterable<String> xs) {
                for (String x : xs) {
                  if (x.equals("foo")) {
                    if (x.equals("bar")) {
                      // BUG: Diagnostic contains:
                      continue;
                    }
                  }
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void onlyStatementInElse() {
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              void test(Iterable<String> xs) {
                for (String x : xs) {
                  if (x.equals("foo")) {
                  } else {
                    // BUG: Diagnostic contains:
                    continue;
                  }
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negative() {
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              String test(Iterable<String> xs) {
                for (String x : xs) {
                  if (x.equals("foo")) {
                    continue;
                  }
                  return x;
                }
                return null;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void labelledContinue_noFinding() {
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              String test(Iterable<String> xs) {
                outer:
                for (String x : xs) {
                  for (int i = 0; i < x.length(); i++) {
                    if (x.charAt(i) == 'a') {
                      continue outer;
                    }
                  }
                }
                return null;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void withinNestedIfs_statementsAfter() {
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              String test(Iterable<String> xs) {
                for (String x : xs) {
                  if (x.equals("foo")) {
                    if (x.equals("bar")) {
                      continue;
                    }
                  }
                  return x;
                }
                return null;
              }
            }
            """)
        .doTest();
  }
}
