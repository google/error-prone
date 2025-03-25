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
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class DeeplyNestedTest {

  private final CompilationTestHelper testHelper =
      CompilationTestHelper.newInstance(DeeplyNested.class, getClass());

  private final BugCheckerRefactoringTestHelper refactoringTestHelper =
      BugCheckerRefactoringTestHelper.newInstance(DeeplyNested.class, getClass());

  @Test
  public void refactoringReturn() {
    refactoringTestHelper
        .addInputLines(
            "Test.java",
            """
            import com.google.common.collect.ImmutableList;

            class Test {
              static ImmutableList<Integer> createXs() {
                return ImmutableList.<Integer>builder()
                    .add(1)
                    .add(2)
                    .add(3)
                    .add(4)
                    // comment
                    .add(5)
                    .add(6)
                    .add(7)
                    .add(8)
                    .add(9)
                    .add(10)
                    .build();
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import com.google.common.collect.ImmutableList;

            class Test {
              static ImmutableList<Integer> createXs() {
                ImmutableList.Builder<Integer> builder = ImmutableList.<Integer>builder();
                builder.add(1);
                builder.add(2);
                builder.add(3);
                builder.add(4);
                // comment
                builder.add(5);
                builder.add(6);
                builder.add(7);
                builder.add(8);
                builder.add(9);
                builder.add(10);
                return builder.build();
              }
            }
            """)
        .setArgs("-XepOpt:DeeplyNested:MaxDepth=10")
        .doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
  }

  @Test
  public void refactoringField() {
    refactoringTestHelper
        .addInputLines(
            "Test.java",
            """
            import com.google.common.collect.ImmutableList;

            class Test {
              static final ImmutableList<Integer> XS =
                  ImmutableList.<Integer>builder()
                      .add(1)
                      .add(2)
                      .add(3)
                      .add(4)
                      .add(5)
                      .add(6)
                      .add(7)
                      .add(8)
                      .add(9)
                      .add(10)
                      .build();
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import com.google.common.collect.ImmutableList;

            class Test {
              static final ImmutableList<Integer> XS = createXs();

              private static ImmutableList<Integer> createXs() {
                ImmutableList.Builder<Integer> builder = ImmutableList.<Integer>builder();
                builder.add(1);
                builder.add(2);
                builder.add(3);
                builder.add(4);
                builder.add(5);
                builder.add(6);
                builder.add(7);
                builder.add(8);
                builder.add(9);
                builder.add(10);
                return builder.build();
              }
            }
            """)
        .setArgs("-XepOpt:DeeplyNested:MaxDepth=10")
        .doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
  }

  @Test
  public void refactoringNewBuilder() {
    refactoringTestHelper
        .addInputLines(
            "Test.java",
            """
            import com.google.common.collect.ImmutableList;

            class Test {
              static final ImmutableList<Integer> XS =
                  new ImmutableList.Builder<Integer>()
                      .add(1)
                      .add(2)
                      .add(3)
                      .add(4)
                      .add(5)
                      .add(6)
                      .add(7)
                      .add(8)
                      .add(9)
                      .add(10)
                      .build();
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import com.google.common.collect.ImmutableList;

            class Test {
              static final ImmutableList<Integer> XS = createXs();

              private static ImmutableList<Integer> createXs() {
                ImmutableList.Builder<Integer> builder = new ImmutableList.Builder<Integer>();
                builder.add(1);
                builder.add(2);
                builder.add(3);
                builder.add(4);
                builder.add(5);
                builder.add(6);
                builder.add(7);
                builder.add(8);
                builder.add(9);
                builder.add(10);
                return builder.build();
              }
            }
            """)
        .setArgs("-XepOpt:DeeplyNested:MaxDepth=10")
        .doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
  }

  @Test
  public void positive() {
    testHelper
        .addSourceLines(
            "Test.java",
            """
            import com.google.common.collect.ImmutableList;

            class Test {
              ImmutableList<Integer> xs =
                  ImmutableList.<Integer>builder()
                      .add(1)
                      .add(2)
                      .add(3)
                      .add(4)
                      .add(5)
                      .add(6)
                      // BUG: Diagnostic contains:
                      .add(7)
                      .add(8)
                      .add(9)
                      .add(10)
                      .build();
            }
            """)
        .setArgs("-XepOpt:DeeplyNested:MaxDepth=10")
        .doTest();
  }

  @Test
  public void negative() {
    testHelper
        .addSourceLines(
            "Test.java",
"""
import com.google.common.collect.ImmutableList;

class Test {
  ImmutableList<Integer> xs = ImmutableList.<Integer>builder().add(1).add(2).add(3).build();
}
""")
        .setArgs("-XepOpt:DeeplyNested:MaxDepth=100")
        .doTest();
  }
}
