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

/** {@link JdkImmutableCollections}Test */
@RunWith(JUnit4.class)
public class JdkImmutableCollectionsTest {
  private final BugCheckerRefactoringTestHelper testHelper =
      BugCheckerRefactoringTestHelper.newInstance(JdkImmutableCollections.class, getClass());

  @Test
  public void factories() {
    testHelper
        .addInputLines(
            "Test.java",
            """
            import java.util.List;
            import java.util.Map;
            import java.util.Set;

            class Test {
              void f() {
                List.of(42);
                Map.of(42, true);
                Set.of(42);
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import com.google.common.collect.ImmutableList;
            import com.google.common.collect.ImmutableMap;
            import com.google.common.collect.ImmutableSet;
            import java.util.List;
            import java.util.Map;
            import java.util.Set;

            class Test {
              void f() {
                ImmutableList.of(42);
                ImmutableMap.of(42, true);
                ImmutableSet.of(42);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void copyOf() {
    testHelper
        .addInputLines(
            "Test.java",
            """
            import java.util.List;
            import java.util.Map;
            import java.util.Set;

            class Test {
              void f(List<?> l, Map<?, ?> m, Set<?> s) {
                List.copyOf(l);
                Map.copyOf(m);
                Set.copyOf(s);
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import com.google.common.collect.ImmutableList;
            import com.google.common.collect.ImmutableMap;
            import com.google.common.collect.ImmutableSet;
            import java.util.List;
            import java.util.Map;
            import java.util.Set;

            class Test {
              void f(List<?> l, Map<?, ?> m, Set<?> s) {
                ImmutableList.copyOf(l);
                ImmutableMap.copyOf(m);
                ImmutableSet.copyOf(s);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void enhancedForLoop() {
    testHelper
        .addInputLines(
            "Test.java",
            """
            import java.util.List;

            class Test {
              void f() {
                for (int x : List.of(42)) {}
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import com.google.common.collect.ImmutableList;
            import java.util.List;

            class Test {
              void f() {
                for (int x : ImmutableList.of(42)) {}
              }
            }
            """)
        .doTest();
  }

  @Test
  public void arrays() {
    testHelper
        .addInputLines(
            "Test.java",
            """
            import java.util.List;
            import java.util.Set;

            class Test {
              void f(Object[] o, int[] x) {
                List.of(o);
                List.of(x);
                Set.of(o);
                Set.of(x);
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import com.google.common.collect.ImmutableList;
            import com.google.common.collect.ImmutableSet;
            import java.util.List;
            import java.util.Set;

            class Test {
              void f(Object[] o, int[] x) {
                ImmutableList.copyOf(o);
                ImmutableList.of(x);
                ImmutableSet.copyOf(o);
                ImmutableSet.of(x);
              }
            }
            """)
        .doTest();
  }
}
