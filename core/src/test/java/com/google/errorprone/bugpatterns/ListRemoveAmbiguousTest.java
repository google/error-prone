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

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link ListRemoveAmbiguous}. */
@RunWith(JUnit4.class)
public class ListRemoveAmbiguousTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(ListRemoveAmbiguous.class, getClass());

  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(ListRemoveAmbiguous.class, getClass());

  @Test
  public void positiveCases() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import java.util.List;

            class Test {
              void foo(List<Integer> list, int i, Integer wrapper) {
                // BUG: Diagnostic contains: Ambiguous call to List.remove
                list.remove(i);
                // BUG: Diagnostic contains: Ambiguous call to List.remove
                list.remove(wrapper);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negativeCases() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import java.util.List;

            class Test {
              void foo(List<String> list, int i) {
                list.remove(i); // No warning, List<String> cannot contain Integers
              }

              void bar(List<Integer> list, int i, Integer wrapper) {
                list.remove(/* index */ i);
                list.remove(/* element */ (Integer) i);
                list.remove(/* element */ wrapper);
                list.remove((Object) wrapper); // Explicit cast to Object, type is Object, no warning
              }
            }
            """)
        .doTest();
  }

  @Test
  public void refactoring() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.util.List;

            class Test {
              void foo(List<Integer> list, int i, Integer wrapper) {
                list.remove(i);
                list.remove(wrapper);
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.util.List;

            class Test {
              void foo(List<Integer> list, int i, Integer wrapper) {
                list.remove(/* index */ i);
                list.remove(/* element */ wrapper);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void refactoringSequenced() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.util.List;

            class Test {
              void foo(List<Integer> list, List<Integer> other) {
                list.remove(0);
                list.remove(list.size() - 1);
                list.remove(other.size() - 1);
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.util.List;

            class Test {
              void foo(List<Integer> list, List<Integer> other) {
                list.removeFirst();
                list.removeLast();
                list.remove(/* index */ other.size() - 1);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void refactoringSequencedImplicitThis() {
    refactoringHelper
        .addInputLines(
            "MyList.java",
            """
            import java.util.AbstractList;

            abstract class MyList extends AbstractList<Integer> {
              void foo() {
                remove(0);
                remove(size() - 1);
              }
            }
            """)
        .addOutputLines(
            "MyList.java",
            """
            import java.util.AbstractList;

            abstract class MyList extends AbstractList<Integer> {
              void foo() {
                removeFirst();
                removeLast();
              }
            }
            """)
        .doTest();
  }

  @Test
  public void positive() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import java.util.List;

            class Test {
              void f(List<Integer> list, int i, Integer ii) {
                // BUG: Diagnostic contains: list.remove(/* index */ i)
                list.remove(i);
                // BUG: Diagnostic contains: list.remove(/* element */ ii)
                list.remove(ii);
                // BUG: Diagnostic contains: list.removeFirst()
                list.remove(0);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void expression() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import java.util.List;

            class Test {
              void f(List<Integer> list, int i, int j) {
                // BUG: Diagnostic contains: list.remove(/* index */ i + j)
                list.remove(i + j);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void subtypes() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import java.util.List;

            class Test {
              void f(List<Number> list, int i, Integer ii) {
                // BUG: Diagnostic contains: list.remove(/* index */ i)
                list.remove(i);
                // BUG: Diagnostic contains: list.remove(/* element */ ii)
                list.remove(ii);
              }

              void g(List<Object> list, int i, Integer ii) {
                list.remove(i);
                list.remove(ii);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void genericMethod() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import java.util.List;

            class Test {
              <T> void f(List<T> list, int i) {
                list.remove(i);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negative() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import java.util.List;

            class Test {
              void f(List<Integer> list, int i, Integer ii) {
                list.remove(/* index */ i);
                list.remove(/* element */ (Integer) i);
                list.remove(/* index */ (int) ii);
                list.remove(/* element */ ii);
              }

              void g(List<String> list, int i) {
                list.remove(i);
              }

              void h(List<Long> list, int i) {
                list.remove(i);
              }

              void i(List<Integer> list, int i, Integer ii) {
                list.remove(Integer.valueOf(i));
                list.remove(ii.intValue());
              }
            }
            """)
        .doTest();
  }

  @Test
  public void castsWarn() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import java.util.List;

            class Test {
              void bar(List<Integer> list, int i, Integer wrapper) {
                // BUG: Diagnostic contains: Ambiguous call to List.remove
                list.remove((int) i);
                // BUG: Diagnostic contains: Ambiguous call to List.remove
                list.remove((Integer) i);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void wrongComments() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import java.util.List;

            class Test {
              void f(List<Integer> list, int i, Integer ii) {
                // BUG: Diagnostic contains: Ambiguous call to List.remove
                list.remove(/* element */ i);
                // BUG: Diagnostic contains: Ambiguous call to List.remove
                list.remove(/* index */ ii);
              }
            }
            """)
        .doTest();
  }
}
