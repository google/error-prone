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

package com.google.errorprone.bugpatterns.testparameterinjector;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link SimplifyTestParameter}. */
@RunWith(JUnit4.class)
public final class SimplifyTestParameterTest {

  private final BugCheckerRefactoringTestHelper testHelper =
      BugCheckerRefactoringTestHelper.newInstance(SimplifyTestParameter.class, getClass());

  @Test
  public void primitiveBooleanParameter() {
    testHelper
        .addInputLines(
            "TestType.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameter;
            import org.junit.Test;

            public class TestType {
              @Test
              public void myTest(
                  @TestParameter({"true", "false"}) boolean foo,
                  @TestParameter({"false", "true"}) boolean bar,
                  @TestParameter(value = {"true", "false"}) boolean baz) {}
            }
            """)
        .addOutputLines(
            "TestType.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameter;
            import org.junit.Test;

            public class TestType {
              @Test
              public void myTest(
                  @TestParameter boolean foo, @TestParameter boolean bar, @TestParameter boolean baz) {}
            }
            """)
        .doTest();
  }

  @Test
  public void boxedBooleanParameter() {
    testHelper
        .addInputLines(
            "TestType.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameter;
            import org.junit.Test;

            public class TestType {
              @Test
              public void myTest(
                  @TestParameter({"true", "false"}) Boolean foo,
                  @TestParameter({"false", "true"}) Boolean bar) {}
            }
            """)
        .addOutputLines(
            "TestType.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameter;
            import org.junit.Test;

            public class TestType {
              @Test
              public void myTest(@TestParameter Boolean foo, @TestParameter Boolean bar) {}
            }
            """)
        .doTest();
  }

  @Test
  public void caseInsensitive() {
    testHelper
        .addInputLines(
            "TestType.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameter;
            import org.junit.Test;

            public class TestType {
              @Test
              public void myTest(
                  @TestParameter({"TRUE", "FALSE"}) boolean foo,
                  @TestParameter({"False", "True"}) Boolean bar) {}
            }
            """)
        .addOutputLines(
            "TestType.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameter;
            import org.junit.Test;

            public class TestType {
              @Test
              public void myTest(@TestParameter boolean foo, @TestParameter Boolean bar) {}
            }
            """)
        .doTest();
  }

  @Test
  public void fieldAndConstructorParameter() {
    testHelper
        .addInputLines(
            "TestType.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameter;

            public class TestType {
              @TestParameter({"true", "false"})
              boolean field;

              public TestType(@TestParameter({"false", "true"}) Boolean param) {}
            }
            """)
        .addOutputLines(
            "TestType.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameter;

            public class TestType {
              @TestParameter boolean field;

              public TestType(@TestParameter Boolean param) {}
            }
            """)
        .doTest();
  }

  @Test
  public void enumParameter() {
    testHelper
        .addInputLines(
            "TestType.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameter;
            import org.junit.Test;

            public class TestType {
              enum Color {
                RED,
                GREEN,
                BLUE
              }

              enum Singleton {
                INSTANCE
              }

              @TestParameter({"RED", "GREEN", "BLUE"})
              Color field;

              @Test
              public void myTest(
                  @TestParameter({"RED", "GREEN", "BLUE"}) Color inOrder,
                  @TestParameter({"BLUE", "RED", "GREEN"}) Color outOfOrder,
                  @TestParameter("INSTANCE") Singleton singleton) {}
            }
            """)
        .addOutputLines(
            "TestType.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameter;
            import org.junit.Test;

            public class TestType {
              enum Color {
                RED,
                GREEN,
                BLUE
              }

              enum Singleton {
                INSTANCE
              }

              @TestParameter Color field;

              @Test
              public void myTest(
                  @TestParameter Color inOrder,
                  @TestParameter Color outOfOrder,
                  @TestParameter Singleton singleton) {}
            }
            """)
        .doTest();
  }

  @Test
  public void negativeCases() {
    testHelper
        .addInputLines(
            "TestType.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameter;
            import org.junit.Test;

            public class TestType {
              enum Color {
                RED,
                GREEN,
                BLUE
              }

              @Test
              public void singleValue(@TestParameter("true") boolean foo) {}

              @Test
              public void singleValueInArray(@TestParameter({"false"}) boolean foo) {}

              @Test
              public void duplicateValues(@TestParameter({"true", "true"}) boolean foo) {}

              @Test
              public void includesNull(@TestParameter({"true", "false", "null"}) Boolean foo) {}

              @Test
              public void alreadySimplified(@TestParameter boolean foo, @TestParameter Color bar) {}

              @Test
              public void stringParameter(@TestParameter({"true", "false"}) String foo) {}

              @Test
              public void enumSubset(@TestParameter({"RED", "GREEN"}) Color foo) {}

              @Test
              public void enumDuplicates(@TestParameter({"RED", "RED", "GREEN"}) Color foo) {}

              @Test
              public void enumIncludesNull(@TestParameter({"RED", "GREEN", "BLUE", "null"}) Color foo) {}
            }
            """)
        .expectUnchanged()
        .doTest();
  }
}
