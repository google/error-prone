/*
 * Copyright 2025 The Error Prone Authors.
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

/** Unit tests for {@link StringJoin} check. */
@RunWith(JUnit4.class)
public class StringJoinTest {
  private final BugCheckerRefactoringTestHelper testHelper =
      BugCheckerRefactoringTestHelper.newInstance(StringJoin.class, getClass());

  @Test
  public void oneCharSequenceParam() {
    testHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              private static final String JOINED = String.join(",");
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              private static final String JOINED = "";
            }
            """)
        .doTest();
  }

  @Test
  public void twoCharSequenceParams() {
    testHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              private static final String JOINED = String.join(",", "foo");
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              private static final String JOINED = "foo";
            }
            """)
        .doTest();
  }

  @Test
  public void threeCharSequenceParamsAreUnchanged() {
    testHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              private static final String JOINED = String.join(",", "foo", "bar");
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void iterableElementsAreUnchanged() {
    testHelper
        .addInputLines(
            "Test.java",
            """
            import java.util.List;

            class Test {
              private static final String JOINED = String.join(",", List.of("foo", "bar"));
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void stringArrayElementsAreUnchanged() {
    testHelper
        .addInputLines(
            "Test.java",
            """
            import java.util.List;

            class Test {
              private static final String[] FOO_BAR = {"foo", "bar"};
              private static final String JOINED = String.join(",", FOO_BAR);
            }
            """)
        .expectUnchanged()
        .doTest();
  }
}
