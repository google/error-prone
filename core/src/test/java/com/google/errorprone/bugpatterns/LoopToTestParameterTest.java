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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link LoopToTestParameter}. */
@RunWith(JUnit4.class)
public final class LoopToTestParameterTest {

  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(LoopToTestParameter.class, getClass())
          .setArgs("-XepCompilingTestOnlyCode");

  @Test
  public void enumLoop_refactoring() {
    refactoringHelper
        .addInputLines(
            "TestType.java",
            """
            import java.util.concurrent.TimeUnit;
            import org.junit.Test;

            public class TestType {
              @Test
              public void myTest() {
                for (TimeUnit e : TimeUnit.values()) {
                  System.out.println(e);
                }
              }
            }
            """)
        .addOutputLines(
            "TestType.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameter;
            import com.google.testing.junit.testparameterinjector.TestParameterInjector;
            import java.util.concurrent.TimeUnit;
            import org.junit.Test;
            import org.junit.runner.RunWith;

            @RunWith(TestParameterInjector.class)
            public class TestType {
              @Test
              public void myTest(@TestParameter TimeUnit e) {
                System.out.println(e);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void enumSetAllOfLoop_refactoring() {
    refactoringHelper
        .addInputLines(
            "TestType.java",
            """
            import java.util.EnumSet;
            import java.util.concurrent.TimeUnit;
            import org.junit.Test;

            public class TestType {
              @Test
              public void myTest() {
                for (TimeUnit e : EnumSet.allOf(TimeUnit.class)) {
                  System.out.println(e);
                }
              }
            }
            """)
        .addOutputLines(
            "TestType.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameter;
            import com.google.testing.junit.testparameterinjector.TestParameterInjector;
            import java.util.EnumSet;
            import java.util.concurrent.TimeUnit;
            import org.junit.Test;
            import org.junit.runner.RunWith;

            @RunWith(TestParameterInjector.class)
            public class TestType {
              @Test
              public void myTest(@TestParameter TimeUnit e) {
                System.out.println(e);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void multipleEnumLoops_refactoring() {
    refactoringHelper
        .addInputLines(
            "TestType.java",
            """
            import java.util.concurrent.TimeUnit;
            import org.junit.Test;

            public class TestType {
              @Test
              public void test1() {
                for (TimeUnit e : TimeUnit.values()) {
                  System.out.println(e);
                }
              }

              @Test
              public void test2() {
                for (TimeUnit e : TimeUnit.values()) {
                  System.out.println(e);
                }
              }
            }
            """)
        .addOutputLines(
            "TestType.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameter;
            import com.google.testing.junit.testparameterinjector.TestParameterInjector;
            import java.util.concurrent.TimeUnit;
            import org.junit.Test;
            import org.junit.runner.RunWith;

            @RunWith(TestParameterInjector.class)
            public class TestType {
              @Test
              public void test1(@TestParameter TimeUnit e) {
                System.out.println(e);
              }

              @Test
              public void test2(@TestParameter TimeUnit e) {
                System.out.println(e);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void enumListLoop_noMatch() {
    refactoringHelper
        .addInputLines(
            "TestType.java",
            """
            import com.google.common.collect.ImmutableList;
            import java.util.concurrent.TimeUnit;
            import org.junit.Test;

            public class TestType {
              @Test
              public void myTest() {
                for (TimeUnit e : ImmutableList.of(TimeUnit.SECONDS)) {
                  System.out.println(e);
                }
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void nonEnumLoop_noMatch() {
    refactoringHelper
        .addInputLines(
            "TestType.java",
            """
            import java.util.List;
            import org.junit.Test;

            public class TestType {
              @Test
              public void myTest(List<String> strings) {
                for (String s : strings) {
                  System.out.println(s);
                }
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void varLoop_refactoring() {
    refactoringHelper
        .addInputLines(
            "TestType.java",
            """
            import java.util.concurrent.TimeUnit;
            import org.junit.Test;

            public class TestType {
              @Test
              public void myTest() {
                for (var e : TimeUnit.values()) {
                  System.out.println(e);
                }
              }
            }
            """)
        .addOutputLines(
            "TestType.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameter;
            import com.google.testing.junit.testparameterinjector.TestParameterInjector;
            import java.util.concurrent.TimeUnit;
            import org.junit.Test;
            import org.junit.runner.RunWith;

            @RunWith(TestParameterInjector.class)
            public class TestType {
              @Test
              public void myTest(@TestParameter TimeUnit e) {
                System.out.println(e);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void nestedEnumLoop_refactoring() {
    refactoringHelper
        .addInputLines(
            "TestType.java",
            """
            import org.junit.Test;

            public class TestType {
              @Test
              public void myTest() {
                for (Thread.State e : Thread.State.values()) {
                  System.out.println(e);
                }
              }
            }
            """)
        .addOutputLines(
            "TestType.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameter;
            import com.google.testing.junit.testparameterinjector.TestParameterInjector;
            import org.junit.Test;
            import org.junit.runner.RunWith;

            @RunWith(TestParameterInjector.class)
            public class TestType {
              @Test
              public void myTest(@TestParameter Thread.State e) {
                System.out.println(e);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void multipleStatements_noMatch() {
    refactoringHelper
        .addInputLines(
            "TestType.java",
            """
            import java.util.concurrent.TimeUnit;
            import org.junit.Test;

            public class TestType {
              @Test
              public void myTest() {
                System.out.println("Start");
                for (TimeUnit e : TimeUnit.values()) {
                  System.out.println(e);
                }
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void commentsPreserved_noMatch() {
    refactoringHelper
        .addInputLines(
            "TestType.java",
            """
            import java.util.concurrent.TimeUnit;
            import org.junit.Test;

            public class TestType {
              @Test
              public void myTest() {
                for (TimeUnit e : TimeUnit.values()) {
                  // This is a comment
                  System.out.println(e);
                }
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void arraysAsListLoop_refactoring() {
    refactoringHelper
        .addInputLines(
            "TestType.java",
            """
            import java.util.Arrays;
            import java.util.concurrent.TimeUnit;
            import org.junit.Test;

            public class TestType {
              @Test
              public void myTest() {
                for (TimeUnit e : Arrays.asList(TimeUnit.values())) {
                  System.out.println(e);
                }
              }
            }
            """)
        .addOutputLines(
            "TestType.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameter;
            import com.google.testing.junit.testparameterinjector.TestParameterInjector;
            import java.util.Arrays;
            import java.util.concurrent.TimeUnit;
            import org.junit.Test;
            import org.junit.runner.RunWith;

            @RunWith(TestParameterInjector.class)
            public class TestType {
              @Test
              public void myTest(@TestParameter TimeUnit e) {
                System.out.println(e);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void immutableListCopyOfLoop_refactoring() {
    refactoringHelper
        .addInputLines(
            "TestType.java",
            """
            import com.google.common.collect.ImmutableList;
            import java.util.concurrent.TimeUnit;
            import org.junit.Test;

            public class TestType {
              @Test
              public void myTest() {
                for (TimeUnit e : ImmutableList.copyOf(TimeUnit.values())) {
                  System.out.println(e);
                }
              }
            }
            """)
        .addOutputLines(
            "TestType.java",
            """
            import com.google.common.collect.ImmutableList;
            import com.google.testing.junit.testparameterinjector.TestParameter;
            import com.google.testing.junit.testparameterinjector.TestParameterInjector;
            import java.util.concurrent.TimeUnit;
            import org.junit.Test;
            import org.junit.runner.RunWith;

            @RunWith(TestParameterInjector.class)
            public class TestType {
              @Test
              public void myTest(@TestParameter TimeUnit e) {
                System.out.println(e);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void immutableSetCopyOfLoop_refactoring() {
    refactoringHelper
        .addInputLines(
            "TestType.java",
            """
            import com.google.common.collect.ImmutableSet;
            import java.util.concurrent.TimeUnit;
            import org.junit.Test;

            public class TestType {
              @Test
              public void myTest() {
                for (TimeUnit e : ImmutableSet.copyOf(TimeUnit.values())) {
                  System.out.println(e);
                }
              }
            }
            """)
        .addOutputLines(
            "TestType.java",
            """
            import com.google.common.collect.ImmutableSet;
            import com.google.testing.junit.testparameterinjector.TestParameter;
            import com.google.testing.junit.testparameterinjector.TestParameterInjector;
            import java.util.concurrent.TimeUnit;
            import org.junit.Test;
            import org.junit.runner.RunWith;

            @RunWith(TestParameterInjector.class)
            public class TestType {
              @Test
              public void myTest(@TestParameter TimeUnit e) {
                System.out.println(e);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void runWithJUnit4_refactoring() {
    refactoringHelper
        .addInputLines(
            "TestType.java",
            """
            import java.util.concurrent.TimeUnit;
            import org.junit.Test;
            import org.junit.runner.RunWith;
            import org.junit.runners.JUnit4;

            @RunWith(JUnit4.class)
            public class TestType {
              @Test
              public void myTest() {
                for (TimeUnit e : TimeUnit.values()) {
                  System.out.println(e);
                }
              }
            }
            """)
        .addOutputLines(
            "TestType.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameter;
            import com.google.testing.junit.testparameterinjector.TestParameterInjector;
            import java.util.concurrent.TimeUnit;
            import org.junit.Test;
            import org.junit.runner.RunWith;
            import org.junit.runners.JUnit4;

            @RunWith(TestParameterInjector.class)
            public class TestType {
              @Test
              public void myTest(@TestParameter TimeUnit e) {
                System.out.println(e);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void incompatibleRunner_noRefactoring() {
    refactoringHelper
        .addInputLines(
            "TestType.java",
            """
            import java.util.concurrent.TimeUnit;
            import org.junit.Test;
            import org.junit.runner.RunWith;
            import org.junit.runners.Parameterized;

            @RunWith(Parameterized.class)
            public class TestType {
              @Test
              public void myTest() {
                for (TimeUnit e : TimeUnit.values()) {
                  System.out.println(e);
                }
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void alreadyHasTestParameterInjector_refactoring() {
    refactoringHelper
        .addInputLines(
            "TestType.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameterInjector;
            import java.util.concurrent.TimeUnit;
            import org.junit.Test;
            import org.junit.runner.RunWith;

            @RunWith(TestParameterInjector.class)
            public class TestType {
              @Test
              public void myTest() {
                for (TimeUnit e : TimeUnit.values()) {
                  System.out.println(e);
                }
              }
            }
            """)
        .addOutputLines(
            "TestType.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameter;
            import com.google.testing.junit.testparameterinjector.TestParameterInjector;
            import java.util.concurrent.TimeUnit;
            import org.junit.Test;
            import org.junit.runner.RunWith;

            @RunWith(TestParameterInjector.class)
            public class TestType {
              @Test
              public void myTest(@TestParameter TimeUnit e) {
                System.out.println(e);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void partiallyParameterized_refactoring() {
    refactoringHelper
        .addInputLines(
            "TestType.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameter;
            import com.google.testing.junit.testparameterinjector.TestParameterInjector;
            import java.util.concurrent.TimeUnit;
            import org.junit.Test;
            import org.junit.runner.RunWith;

            @RunWith(TestParameterInjector.class)
            public class TestType {
              @Test
              public void myTest(@TestParameter TimeUnit e1) {
                for (TimeUnit e2 : TimeUnit.values()) {
                  System.out.println(e1 + " " + e2);
                }
              }
            }
            """)
        .addOutputLines(
            "TestType.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameter;
            import com.google.testing.junit.testparameterinjector.TestParameterInjector;
            import java.util.concurrent.TimeUnit;
            import org.junit.Test;
            import org.junit.runner.RunWith;

            @RunWith(TestParameterInjector.class)
            public class TestType {
              @Test
              public void myTest(@TestParameter TimeUnit e1, @TestParameter TimeUnit e2) {
                System.out.println(e1 + " " + e2);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void commentsAroundBraces_noMatch() {
    refactoringHelper
        .addInputLines(
            "TestType.java",
            """
            import java.util.concurrent.TimeUnit;
            import org.junit.Test;

            public class TestType {
              @Test
              public void myTest() {
                for (TimeUnit e : TimeUnit.values()) /* before */ { // inside
                  System.out.println(e);
                } // after
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void commentBeforeOpeningBrace_noMatch() {
    refactoringHelper
        .addInputLines(
            "TestType.java",
            """
            import java.util.concurrent.TimeUnit;
            import org.junit.Test;

            public class TestType {
              @Test
              public void myTest() {
                for (TimeUnit e : TimeUnit.values())
                // comment before brace
                {
                  System.out.println(e);
                }
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void enumLoopWithContinue_noMatch() {
    refactoringHelper
        .addInputLines(
            "TestType.java",
            """
            import java.util.concurrent.TimeUnit;
            import org.junit.Test;

            public class TestType {
              @Test
              public void myTest() {
                for (TimeUnit e : TimeUnit.values()) {
                  if (e == TimeUnit.SECONDS) {
                    continue;
                  }
                  System.out.println(e);
                }
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void enumLoopWithBreak_noMatch() {
    refactoringHelper
        .addInputLines(
            "TestType.java",
            """
            import java.util.concurrent.TimeUnit;
            import org.junit.Test;

            public class TestType {
              @Test
              public void myTest() {
                for (TimeUnit e : TimeUnit.values()) {
                  if (e == TimeUnit.SECONDS) {
                    break;
                  }
                  System.out.println(e);
                }
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void enumLoopWithReturn_noMatch() {
    refactoringHelper
        .addInputLines(
            "TestType.java",
            """
            import java.util.concurrent.TimeUnit;
            import org.junit.Test;

            public class TestType {
              @Test
              public void myTest() {
                for (TimeUnit e : TimeUnit.values()) {
                  if (e == TimeUnit.SECONDS) {
                    return;
                  }
                  System.out.println(e);
                }
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void returnInsideLambda_noMatch() {
    // Even though it would be safe to refactor this (the return is inside the lambda), we
    // currently don't.
    refactoringHelper
        .addInputLines(
            "TestType.java",
            """
            import java.util.List;
            import java.util.concurrent.TimeUnit;
            import org.junit.Test;

            public class TestType {
              @Test
              public void myTest(List<String> list) {
                for (TimeUnit e : TimeUnit.values()) {
                  list.forEach(
                      x -> {
                        if (x == null) {
                          return;
                        }
                      });
                }
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void continueInsideInnerLoop_noMatch() {
    // Even though it would be safe to refactor this (the continue is for the inside loop), we
    // currently don't.
    refactoringHelper
        .addInputLines(
            "TestType.java",
            """
            import java.util.concurrent.TimeUnit;
            import org.junit.Test;

            public class TestType {
              @Test
              public void myTest() {
                for (TimeUnit e : TimeUnit.values()) {
                  for (int i = 0; i < 10; i++) {
                    if (i == 5) {
                      continue;
                    }
                  }
                }
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void nestedEnumLoops_refactoring() {
    refactoringHelper
        .addInputLines(
            "TestType.java",
            """
            import java.util.concurrent.TimeUnit;
            import org.junit.Test;

            public class TestType {
              @Test
              public void myTest() {
                for (TimeUnit e1 : TimeUnit.values()) {
                  for (TimeUnit e2 : TimeUnit.values()) {
                    System.out.println(e1 + " " + e2);
                  }
                }
              }
            }
            """)
        .addOutputLines(
            "TestType.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameter;
            import com.google.testing.junit.testparameterinjector.TestParameterInjector;
            import java.util.concurrent.TimeUnit;
            import org.junit.Test;
            import org.junit.runner.RunWith;

            @RunWith(TestParameterInjector.class)
            public class TestType {
              @Test
              public void myTest(@TestParameter TimeUnit e1) {
                for (TimeUnit e2 : TimeUnit.values()) {
                  System.out.println(e1 + " " + e2);
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void annotatedLoopVariable_refactoring() {
    // We currently obliterate annotations on the loop variable. We may want to fix this in the
    // future.
    refactoringHelper
        .addInputLines(
            "TestType.java",
            """
            import java.lang.annotation.ElementType;
            import java.lang.annotation.Target;
            import java.util.concurrent.TimeUnit;
            import org.junit.Test;

            public class TestType {
              @Target(ElementType.TYPE_USE)
              @interface MyAnnotation {}

              @Test
              public void myTest() {
                for (@MyAnnotation TimeUnit e : TimeUnit.values()) {
                  System.out.println(e);
                }
              }
            }
            """)
        .addOutputLines(
            "TestType.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameter;
            import com.google.testing.junit.testparameterinjector.TestParameterInjector;
            import java.lang.annotation.ElementType;
            import java.lang.annotation.Target;
            import java.util.concurrent.TimeUnit;
            import org.junit.Test;
            import org.junit.runner.RunWith;

            @RunWith(TestParameterInjector.class)
            public class TestType {
              @Target(ElementType.TYPE_USE)
              @interface MyAnnotation {}

              @Test
              public void myTest(@TestParameter TimeUnit e) {
                System.out.println(e);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void noBracesLoop_refactoring() {
    refactoringHelper
        .addInputLines(
            "TestType.java",
            """
            import java.util.concurrent.TimeUnit;
            import org.junit.Test;

            public class TestType {
              @Test
              public void myTest() {
                for (TimeUnit e : TimeUnit.values()) System.out.println(e);
              }
            }
            """)
        .addOutputLines(
            "TestType.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameter;
            import com.google.testing.junit.testparameterinjector.TestParameterInjector;
            import java.util.concurrent.TimeUnit;
            import org.junit.Test;
            import org.junit.runner.RunWith;

            @RunWith(TestParameterInjector.class)
            public class TestType {
              @Test
              public void myTest(@TestParameter TimeUnit e) {
                System.out.println(e);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void notTestOnlyTarget_noMatch() {
    BugCheckerRefactoringTestHelper.newInstance(LoopToTestParameter.class, getClass())
        .addInputLines(
            "TestType.java",
            """
            import java.util.concurrent.TimeUnit;
            import org.junit.Test;

            public class TestType {
              @Test
              public void myTest() {
                for (TimeUnit e : TimeUnit.values()) {
                  System.out.println(e);
                }
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void abstractTestMethod_noMatch() {
    refactoringHelper
        .addInputLines(
            "TestType.java",
            """
            import org.junit.Test;

            public abstract class TestType {
              @Test
              public abstract void myTest();
            }
            """)
        .expectUnchanged()
        .doTest();
  }
}
