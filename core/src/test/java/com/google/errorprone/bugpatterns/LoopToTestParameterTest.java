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
          .setTestOnly();

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
  public void enumListLoop_refactoring() {
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
              public void myTest(@TestParameter({"SECONDS"}) TimeUnit e) {
                System.out.println(e);
              }
            }
            """)
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

  @Test
  public void stringArrayLoop_refactoring() {
    refactoringHelper
        .addInputLines(
            "TestType.java",
            """
            import org.junit.Test;

            public class TestType {
              @Test
              public void compileQuantified_repetitionCounts_compileSingleCopyOfElement() {
                for (String regex :
                    new String[] {
                      "a{0,3}", "a{2,4}", "a{3}", "a{2147483607,}", "((a{3}){3}){3}",
                    }) {
                  System.out.println(regex);
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
              public void compileQuantified_repetitionCounts_compileSingleCopyOfElement(
                  @TestParameter({"a{0,3}", "a{2,4}", "a{3}", "a{2147483607,}", "((a{3}){3}){3}"})
                      String regex) {
                System.out.println(regex);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void asListStringLoop_refactoring() {
    refactoringHelper
        .addInputLines(
            "TestType.java",
            """
            import static java.util.Arrays.asList;

            import org.junit.Test;

            public class TestType {
              @Test
              public void myTest() {
                for (String s : asList("a", "b", "c")) {
                  System.out.println(s);
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
              public void myTest(@TestParameter({"a", "b", "c"}) String s) {
                System.out.println(s);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void collectionFactoriesLoop_refactoring() {
    refactoringHelper
        .addInputLines(
            "TestType.java",
            """
            import static java.util.concurrent.TimeUnit.SECONDS;

            import com.google.common.collect.ImmutableSet;
            import com.google.testing.junit.testparameterinjector.TestParameter;
            import java.util.EnumSet;
            import java.util.List;
            import java.util.Set;
            import java.util.concurrent.TimeUnit;
            import org.junit.Test;

            public class TestType {
              @Test
              public void listOfTest() {
                for (var s : List.of("true", "false")) {
                  System.out.println(s);
                }
              }

              @Test
              public void setOfTest() {
                for (int x : Set.of(+1, (-2), -(3))) {
                  System.out.println(x);
                }
              }

              @Test
              public void immutableSetOfTest() {
                for (double d : ImmutableSet.of(1.5, -2.5)) {
                  System.out.println(d);
                }
              }

              @Test
              public void enumSetOfTest() {
                for (TimeUnit u : EnumSet.of(SECONDS, TimeUnit.MINUTES)) {
                  System.out.println(u);
                }
              }

              @Test
              public void parenthesizedEnumValuesTest() {
                for (TimeUnit u : (TimeUnit.values())) {
                  System.out.println(u);
                }
              }

              @Test
              public void existingParam(@TestParameter int a) {
                for (int b : List.of(1, 2)) {
                  System.out.println(a + b);
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
              public void listOfTest(@TestParameter({"true", "false"}) String s) {
                System.out.println(s);
              }

              @Test
              public void setOfTest(@TestParameter({"1", "-2", "-3"}) int x) {
                System.out.println(x);
              }

              @Test
              public void immutableSetOfTest(@TestParameter({"1.5", "-2.5"}) double d) {
                System.out.println(d);
              }

              @Test
              public void enumSetOfTest(@TestParameter({"SECONDS", "MINUTES"}) TimeUnit u) {
                System.out.println(u);
              }

              @Test
              public void parenthesizedEnumValuesTest(@TestParameter TimeUnit u) {
                System.out.println(u);
              }

              @Test
              public void existingParam(@TestParameter int a, @TestParameter({"1", "2"}) int b) {
                System.out.println(a + b);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void primitiveArraysAndNullElementsLoop_refactoring() {
    refactoringHelper
        .addInputLines(
            "TestType.java",
            """
            import static java.util.Arrays.asList;

            import java.util.concurrent.TimeUnit;
            import org.junit.Test;

            public class TestType {
              @Test
              public void longArrayTest() {
                for (long l : new long[] {10L, -20L}) {
                  System.out.println(l);
                }
              }

              @Test
              public void floatArrayTest() {
                for (float f : new float[] {1.0f, 2.5f}) {
                  System.out.println(f);
                }
              }

              @Test
              public void booleanArrayTest() {
                for (boolean b : new boolean[] {true, false}) {
                  System.out.println(b);
                }
              }

              @Test
              public void boxedBooleanListTest() {
                for (Boolean b : asList(false, true)) {
                  System.out.println(b);
                }
              }

              @Test
              public void nullableBooleanTest() {
                for (Boolean b : asList(true, false, null)) {
                  System.out.println(b);
                }
              }

              @Test
              public void singleBooleanTest() {
                for (boolean b : new boolean[] {true}) {
                  System.out.println(b);
                }
              }

              @Test
              public void enumArrayTest() {
                for (TimeUnit u : new TimeUnit[] {TimeUnit.SECONDS, TimeUnit.MINUTES}) {
                  System.out.println(u);
                }
              }

              @Test
              public void nullableElementsTest() {
                for (String s : asList("a", null)) {
                  System.out.println(s);
                }
              }

              @Test
              public void nullableEnumTest() {
                for (TimeUnit u : asList(TimeUnit.SECONDS, null)) {
                  System.out.println(u);
                }
              }

              @Test
              public void nullableBoxedIntTest() {
                for (Integer i : asList(1, null)) {
                  System.out.println(i);
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
              public void longArrayTest(@TestParameter({"10", "-20"}) long l) {
                System.out.println(l);
              }

              @Test
              public void floatArrayTest(@TestParameter({"1.0", "2.5"}) float f) {
                System.out.println(f);
              }

              @Test
              public void booleanArrayTest(@TestParameter boolean b) {
                System.out.println(b);
              }

              @Test
              public void boxedBooleanListTest(@TestParameter Boolean b) {
                System.out.println(b);
              }

              @Test
              public void nullableBooleanTest(@TestParameter({"true", "false", "null"}) Boolean b) {
                System.out.println(b);
              }

              @Test
              public void singleBooleanTest(@TestParameter({"true"}) boolean b) {
                System.out.println(b);
              }

              @Test
              public void enumArrayTest(@TestParameter({"SECONDS", "MINUTES"}) TimeUnit u) {
                System.out.println(u);
              }

              @Test
              public void nullableElementsTest(@TestParameter({"a", "null"}) String s) {
                System.out.println(s);
              }

              @Test
              public void nullableEnumTest(@TestParameter({"SECONDS", "null"}) TimeUnit u) {
                System.out.println(u);
              }

              @Test
              public void nullableBoxedIntTest(@TestParameter({"1", "null"}) Integer i) {
                System.out.println(i);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void unsupportedArrayAndListLoops_noMatch() {
    refactoringHelper
        .addInputLines(
            "TestType.java",
            """
            import static java.util.Arrays.asList;

            import com.google.common.collect.ImmutableList;
            import org.junit.Test;

            public class TestType {
              private static final String CONST = "foo";

              @Test
              public void nonLoopSingleStatement() {
                System.out.println(CONST);
              }

              @Test
              public void nullStringLiteral() {
                for (String s : new String[] {"a", "null"}) {
                  System.out.println(s);
                }
              }

              @Test
              public void nonLiteralElement() {
                for (String s : asList(CONST, "bar")) {
                  System.out.println(s);
                }
              }

              @Test
              public void binaryExpressionElement() {
                for (int x : asList(1 + 2, 3)) {
                  System.out.println(x);
                }
              }

              @Test
              public void primitiveWithNullElement() {
                for (int x : asList(1, null)) {
                  System.out.println(x);
                }
              }

              @Test
              public void unsupportedPrimitiveArrays() {
                for (char c : new char[] {'a', 'b'}) {
                  System.out.println(c);
                }
              }

              @Test
              public void byteArray() {
                for (byte b : new byte[] {1, 2}) {
                  System.out.println(b);
                }
              }

              @Test
              public void shortArray() {
                for (short s : new short[] {1, 2}) {
                  System.out.println(s);
                }
              }

              @Test
              public void unsupportedVarType() {
                for (Object o : asList("a", "b")) {
                  System.out.println(o);
                }
              }

              @Test
              public void emptyList() {
                for (String s : ImmutableList.<String>of()) {
                  System.out.println(s);
                }
              }

              @Test
              public void emptyArray() {
                for (String s : new String[] {}) {
                  System.out.println(s);
                }
              }

              @Test
              public void uninitializedArray() {
                for (int x : new int[3]) {
                  System.out.println(x);
                }
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }
}
