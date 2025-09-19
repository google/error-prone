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
  public void seesThroughParens() {
    helper
        .addInputLines(
            "Test.java",
            """
            class Test {
              void test(Object o) {
                if (o instanceof Test) {
                  Test test = ((((Test) o)));
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
  public void withinIf_elseCannotCompleteNormally_variableInScopeForStatementsAfter() {
    helper
        .addInputLines(
            "Test.java",
            """
            class Test {
              void test(Object o) {
                if (o instanceof Test) {
                } else if (true) {
                  throw new AssertionError();
                } else {
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
                if (o instanceof Test test) {
                } else if (true) {
                  throw new AssertionError();
                } else {
                  return;
                }
                test(test);
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
  public void rawType_findingAvoided() {
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
        .expectUnchanged()
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

  @Test
  public void generics_includeWildcards() {
    helper
        .addInputLines(
            "Test.java",
            """
            class Test<T> {
              private String val;

              public String stringify(Object o) {
                return !(o instanceof Test) ? "not a test" : ((Test) o).val;
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test<T> {
              private String val;

              public String stringify(Object o) {
                return !(o instanceof Test<?> test) ? "not a test" : test.val;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void returnTarget() {
    helper
        .addInputLines(
            "Test.java",
            """
            class Test<T> {
              private String val;

              public Class stringify(Object o) {
                if (o instanceof Class<?>) {
                  return (Class) o;
                }
                return null;
              }
            }
            """)
        .expectUnchanged()
        .doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
  }

  @Test
  public void switchExpression() {
    helper
        .addInputLines(
            "Test.java",
            """
            class T {
              interface Filter {
                Object child();
              }

              int f(Object o) {
                return switch (o) {
                  case Filter filter -> {
                    if (!(filter.child() instanceof Integer)) {
                      yield 0;
                    }
                    yield 1;
                  }
                  default -> 2;
                };
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void constantExpression() {
    helper
        .addInputLines(
            "Test.java",
            """
            import com.google.common.collect.ImmutableList;

            class Test {
              int f(ImmutableList<Object> xs) {
                if (xs.get(0) instanceof Integer) {
                  return (Integer) xs.get(0);
                }
                return 0;
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import com.google.common.collect.ImmutableList;

            class Test {
              int f(ImmutableList<Object> xs) {
                if (xs.get(0) instanceof Integer i) {
                  return i;
                }
                return 0;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void nonFinalIdentifier() {
    // NOTE(ghm): Ideally we could match this, but ConstantExpressions won't regard a non-final
    // identifier as a constant (correctly!)
    helper
        .addInputLines(
            "Test.java",
            """
            import com.google.common.collect.ImmutableList;

            class Test {
              String f(Object o) {
                o = o.toString();
                if (o instanceof String) {
                  return (String) o;
                }
                return null;
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  // https://github.com/google/error-prone/issues/4923
  @Test
  public void requiredParentheses_retainedInFix() {
    helper
        .addInputLines(
            "Test1.java",
            """
            public class Test1 {

              int test_switch() {
                Object o = 1;
                if (o instanceof Integer) {
                  // Next line will be turned into "return switch i {".
                  return switch ((Integer) o) {
                    case 0 -> 0;
                    default -> 1;
                  };
                }
                return 0;
              }

              boolean test_if() {
                Object o = false;
                if (o instanceof Boolean) {
                  // Next line will be turned into "if b {".
                  if ((Boolean) o) {
                    return (Boolean) o;
                  }
                }
                return false;
              }
            }
            """)
        .addOutputLines(
            "Test1.java",
            """
            public class Test1 {

              int test_switch() {
                Object o = 1;
                if (o instanceof Integer i) {
                  // Next line will be turned into "return switch i {".
                  return switch (i) {
                    case 0 -> 0;
                    default -> 1;
                  };
                }
                return 0;
              }

              boolean test_if() {
                Object o = false;
                if (o instanceof Boolean b) {
                  // Next line will be turned into "if b {".
                  if (b) {
                    return b;
                  }
                }
                return false;
              }
            }
            """)
        .doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
  }

  // https://github.com/google/error-prone/issues/4921
  @Test
  public void castToSupertypeOfInstanceofCheck_noFinding() {
    helper
        .addInputLines(
            "Test.java",
            """
            import java.nio.file.Path;
            import java.util.ArrayList;

            public class Test {
              void superinterface() {
                Object o = Path.of(".");
                if (o instanceof Path) {
                  f((Iterable<?>) o);
                }
              }

              void f(Comparable<?> c) {}

              void f(Iterable<?> c) {}

              void f(Path p) {}

              void rawtypes() {
                Object o = new ArrayList<Integer>();
                if (o instanceof ArrayList<?>) {
                  @SuppressWarnings("rawtypes")
                  ArrayList list = (ArrayList) o;
                  rawTypeNecessary(list);
                }
              }

              void rawTypeNecessary(ArrayList<Integer> l) {}
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.nio.file.Path;
            import java.util.ArrayList;

            public class Test {
              void superinterface() {
                Object o = Path.of(".");
                if (o instanceof Path) {
                  f((Iterable<?>) o);
                }
              }

              void f(Comparable<?> c) {}

              void f(Iterable<?> c) {}

              void f(Path p) {}

              void rawtypes() {
                Object o = new ArrayList<Integer>();
                if (o instanceof ArrayList<?>) {
                  @SuppressWarnings("rawtypes")
                  ArrayList list = (ArrayList) o;
                  rawTypeNecessary(list);
                }
              }

              void rawTypeNecessary(ArrayList<Integer> l) {}
            }
            """)
        .doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
  }
}
