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
  public void negatedIf() {
    helper
        .addInputLines(
            "Test.java",
            """
            class Test {
              void test(Object o) {
                if (!(o instanceof Test)) {
                } else {
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
                if (!(o instanceof Test test)) {
                } else {
                  test(test);
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negatedIf_withOrs() {
    helper
        .addInputLines(
            "Test.java",
            """
            class Test {
              void test(Object o) {
                if (!(o instanceof Test) || o.hashCode() == 0) {
                  return;
                }
                Test test = (Test) o;
                test(test);
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              void test(Object o) {
                if (!(o instanceof Test test) || o.hashCode() == 0) {
                  return;
                }
                test(test);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negatedIfWithReturn() {
    helper
        .addInputLines(
            "Test.java",
            """
            class Test {
              void test(Object o) {
                if (!(o instanceof Test)) {
                  return;
                }
                Test test = (Test) o;
                test(test);
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              void test(Object o) {
                if (!(o instanceof Test test)) {
                  return;
                }
                test(test);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negatedIf_butNoDefiniteReturn_noFinding() {
    helper
        .addInputLines(
            "Test.java",
            """
            class Test {
              void test(Object o) {
                if (!(o instanceof Test)) {
                  test(o);
                }
                Test test = (Test) o;
                test(test);
              }
            }
            """)
        .expectUnchanged()
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
  public void notImmediatelyAssignedToVariable() {
    helper
        .addInputLines(
            "Test.java",
            """
            class Test {
              void test(Object o) {
                if (o instanceof Test) {
                  test((Test) o);
                  test(((Test) o).hashCode());
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
                  test(test.hashCode());
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void primitiveType_shortNameChosen() {
    helper
        .addInputLines(
            "Test.java",
            """
            class Test {
              void test(Object o) {
                if (o instanceof Long) {
                  test((Long) o);
                  test(((Long) o).hashCode());
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              void test(Object o) {
                if (o instanceof Long l) {
                  test(l);
                  test(l.hashCode());
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void genericWithUpperBoundedWildcard() {
    helper
        .addInputLines(
            "Test.java",
            """
            import java.util.List;

            class Test {
              void test(Object object) {
                if (object instanceof List) {
                  @SuppressWarnings("unchecked")
                  List<? extends CharSequence> xs = (List) object;
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.util.List;

            class Test {
              void test(Object object) {
                if (object instanceof List list) {
                  @SuppressWarnings("unchecked")
                  List<? extends CharSequence> xs = list;
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void javaKeyword_break() {
    helper
        .addInputLines(
            "Break.java",
            """
            class Break {
              void test(Object o) {
                if (o instanceof Break) {
                  test((Break) o);
                }
              }
            }
            """)
        .addOutputLines(
            "Break.java",
            """
            class Break {
              void test(Object o) {
                if (o instanceof Break b) {
                  test(b);
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void javaKeyword_class() {
    helper
        .addInputLines(
            "Class.java",
            """
            class Class {
              void test(Object o) {
                if (o instanceof Class) {
                  test((Class) o);
                }
              }
            }
            """)
        .addOutputLines(
            "Class.java",
            """
            class Class {
              void test(Object o) {
                if (o instanceof Class c) {
                  test(c);
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void recordPatternMatching() {
    assume().that(Runtime.version().feature()).isAtLeast(21);

    helper
        .addInputLines(
            "Foo.java",
            """
            record Foo(int x, int y) {}
            """)
        .expectUnchanged()
        .addInputLines(
            "Test.java",
            """
            class Test {
              void test(Object o) {
                // No finding here, but also no crash.
                if (o instanceof Foo(int x, int y)) {}
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void newVariableNotInstantlyAssigned_pleasantFix() {
    helper
        .addInputLines(
            "Test.java",
            """
            class Test {
              void test(Object o) {
                if (o instanceof Test) {
                  test((Test) o);
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
                  test(test);
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void reassignedWithinScope_noFinding() {
    helper
        .addInputLines(
            "Test.java",
            """
            class Test {
              public void foo(Object o) {
                if (o instanceof String) {
                  while (((String) o).hashCode() != 0) {
                    o = o.toString();
                  }
                }
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void withinStatement() {
    helper
        .addInputLines(
            "Test.java",
            """
            class Test {
              private final int x = 0;
              private final int y = 1;

              @Override
              public boolean equals(Object o) {
                return o instanceof Test && ((Test) o).x == this.x && ((Test) o).y == this.y;
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              private final int x = 0;
              private final int y = 1;
              @Override
              public boolean equals(Object o) {
                return o instanceof Test test && test.x == this.x && test.y == this.y;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void withinIfCondition_andUsedAfter() {
    helper
        .addInputLines(
            "Test.java",
            """
            class Test {
              private final int x = 0;
              private final int y = 1;

              @Override
              public boolean equals(Object o) {
                if (!(o instanceof Test) || ((Test) o).x != this.x) {
                  return false;
                }
                Test other = (Test) o;
                return other.y == this.y;
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              private final int x = 0;
              private final int y = 1;
              @Override
              public boolean equals(Object o) {
                if (!(o instanceof Test other) || other.x != this.x) {
                  return false;
                }
                return other.y == this.y;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void conditionalExpression() {
    helper
        .addInputLines(
            "Test.java",
            """
            class Test {
              private String val;

              public String stringify(Object o) {
                return o instanceof Test ? ((Test) o).val : "not a test";
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              private String val;

              public String stringify(Object o) {
                return o instanceof Test test ? test.val : "not a test";
              }
            }
            """)
        .doTest();
  }

  @Test
  public void conditionalExpression_negated() {
    helper
        .addInputLines(
            "Test.java",
            """
            class Test {
              private String val;

              public String stringify(Object o) {
                return !(o instanceof Test) ? "not a test" : ((Test) o).val;
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              private String val;

              public String stringify(Object o) {
                return !(o instanceof Test test) ? "not a test" : test.val;
              }
            }
            """)
        .doTest();
  }
}
