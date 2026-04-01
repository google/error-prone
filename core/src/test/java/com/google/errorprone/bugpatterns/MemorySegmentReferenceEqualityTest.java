/*
 * Copyright 2026 The Error Prone Authors.
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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * @author kak@google.com (Kurt Alfred Kluever)
 */
@RunWith(JUnit4.class)
public class MemorySegmentReferenceEqualityTest {

  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(MemorySegmentReferenceEquality.class, getClass());

  @Before
  public void setUp() {
    assume().that(Runtime.version().feature()).isAtLeast(22);
  }

  @Test
  public void positiveCase_equal() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.lang.foreign.MemorySegment;

            class Test {
              boolean f(MemorySegment a, MemorySegment b) {
                return a == b;
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.lang.foreign.MemorySegment;
            import java.util.Objects;

            class Test {
              boolean f(MemorySegment a, MemorySegment b) {
                return Objects.equals(a, b);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void positiveCase_notEqual() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.lang.foreign.MemorySegment;

            class Test {
              boolean f(MemorySegment a, MemorySegment b) {
                return a != b;
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.lang.foreign.MemorySegment;
            import java.util.Objects;

            class Test {
              boolean f(MemorySegment a, MemorySegment b) {
                return !Objects.equals(a, b);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negativeCase_null() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.lang.foreign.MemorySegment;

            class Test {
              boolean f(MemorySegment b) {
                return b == null;
              }

              boolean g(MemorySegment b) {
                return b != null;
              }

              boolean h(MemorySegment b) {
                return null == b;
              }

              boolean i(MemorySegment b) {
                return null != b;
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void compareToNullConstant() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.lang.foreign.MemorySegment;

            class Test {
              boolean f(MemorySegment a) {
                return a == MemorySegment.NULL;
              }

              boolean g(MemorySegment a) {
                return a != MemorySegment.NULL;
              }

              boolean h(MemorySegment a) {
                return MemorySegment.NULL == a;
              }

              boolean i(MemorySegment a) {
                return MemorySegment.NULL != a;
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.lang.foreign.MemorySegment;
            import java.util.Objects;

            class Test {
              boolean f(MemorySegment a) {
                return Objects.equals(a, MemorySegment.NULL);
              }

              boolean g(MemorySegment a) {
                return !Objects.equals(a, MemorySegment.NULL);
              }

              boolean h(MemorySegment a) {
                return Objects.equals(MemorySegment.NULL, a);
              }

              boolean i(MemorySegment a) {
                return !Objects.equals(MemorySegment.NULL, a);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void compareToNullConstant_notEqual() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.lang.foreign.MemorySegment;

            class Test {
              boolean f(MemorySegment a) {
                return a != MemorySegment.NULL;
              }

              boolean g(MemorySegment a) {
                return MemorySegment.NULL != a;
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.lang.foreign.MemorySegment;
            import java.util.Objects;

            class Test {
              boolean f(MemorySegment a) {
                return !Objects.equals(a, MemorySegment.NULL);
              }

              boolean g(MemorySegment a) {
                return !Objects.equals(MemorySegment.NULL, a);
              }
            }
            """)
        .doTest();
  }
}
