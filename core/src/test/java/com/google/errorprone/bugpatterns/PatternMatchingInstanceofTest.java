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

import static com.google.common.truth.TruthJUnit.assume;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class PatternMatchingInstanceofTest {
  private final BugCheckerRefactoringTestHelper helper =
      BugCheckerRefactoringTestHelper.newInstance(PatternMatchingInstanceof.class, getClass());

  @Test
  public void positive() {
    assume().that(Runtime.version().feature()).isAtLeast(21);
    helper
        .addInputLines(
            "Test.java",
            """
            class Test {
              void test(Object o) {
                if (o instanceof Test) {
                  Test test = (Test) o;
                  test(test);
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              void test(Object o) {
                if (o instanceof Test test) {
                  test(test);
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void notDefinitelyChecked_noFinding() {
    helper
        .addInputLines(
            "Test.java",
            """
            class Test {
              void test(Object o) {
                if (o instanceof Test || o.hashCode() > 0) {
                  Test test = (Test) o;
                  test(test);
                }
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void moreChecksInIf_stillMatches() {
    assume().that(Runtime.version().feature()).isAtLeast(21);
    helper
        .addInputLines(
            "Test.java",
            """
            class Test {
              void test(Object o) {
                if (o instanceof Test && o.hashCode() != 1) {
                  Test test = (Test) o;
                  test(test);
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              void test(Object o) {
                if (o instanceof Test test && o.hashCode() != 1) {
                  test(test);
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void differentTypeToCheck_noFinding() {
    helper
        .addInputLines(
            "Test.java",
            """
            class Test {
              void test(Object o) {
                if (o instanceof Test) {
                  Integer test = (Integer) o;
                  test(test);
                }
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void noInstanceofAtAll_noFinding() {
    helper
        .addInputLines(
            "Test.java",
            """
            class Test {
              void test(Object o) {
                if (o.hashCode() > 0) {
                  Integer test = (Integer) o;
                  test(test);
                }
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void differentVariable() {
    helper
        .addInputLines(
            "Test.java",
            """
            class Test {
              void test(Object x, Object y) {
                if (x instanceof Test) {
                  Test test = (Test) y;
                  test(test, null);
                }
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void generic() {
    helper
        .addInputLines(
            "Test.java",
            """
            import java.util.Map;

            class Test {
              void test(Object x, String k) {
                if (x instanceof Map) {
                  @SuppressWarnings("unchecked")
                  Map<String, Integer> m = (Map<String, Integer>) x;
                  System.err.println(m.get(k));
                }
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void genericWithUpperBoundedWildcard() {
    assume().that(Runtime.version().feature()).isAtLeast(21);
    helper
        .addInputLines(
            "Test.java",
            """
            import java.util.List;

            class Test {
              void test(Object object) {
                if (object instanceof List) {
                  @SuppressWarnings("unchecked")
                  List<? extends CharSequence> list = (List) object;
                  System.err.println(list.get(0));
                }
              }
            }
            """)
        // TODO: b/380054832 - this shouldn't get re-written
        .addOutputLines(
            "Test.java",
            """
            import java.util.List;

            class Test {
              void test(Object object) {
                if (object instanceof List list) {
                  System.err.println(list.get(0));
                }
              }
            }
            """)
        .doTest();
  }
}
