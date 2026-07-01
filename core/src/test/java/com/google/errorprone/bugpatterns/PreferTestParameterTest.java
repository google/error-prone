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

/** Tests for {@link PreferTestParameter}. */
@RunWith(JUnit4.class)
public final class PreferTestParameterTest {

  private final BugCheckerRefactoringTestHelper testHelper =
      BugCheckerRefactoringTestHelper.newInstance(PreferTestParameter.class, getClass());

  @Test
  public void refactoring() {
    testHelper
        .addInputLines(
            "TestType.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameterInjector;
            import com.google.testing.junit.testparameterinjector.TestParameters;
            import org.junit.Test;
            import org.junit.runner.RunWith;

            @RunWith(TestParameterInjector.class)
            public class TestType {
              enum MyEnum {
                FOO,
                BAR
              }

              @Test
              @TestParameters({"{mode: FOO}", "{mode: BAR}"})
              public void myTest(MyEnum mode) {}
            }
            """)
        .addOutputLines(
            "TestType.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameter;
            import com.google.testing.junit.testparameterinjector.TestParameterInjector;
            import com.google.testing.junit.testparameterinjector.TestParameters;
            import org.junit.Test;
            import org.junit.runner.RunWith;

            @RunWith(TestParameterInjector.class)
            public class TestType {
              enum MyEnum {
                FOO,
                BAR
              }

              @Test
              public void myTest(@TestParameter MyEnum mode) {}
            }
            """)
        .doTest();
  }

  @Test
  public void refactoring_multipleAnnotations() {
    testHelper
        .addInputLines(
            "TestType.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameterInjector;
            import com.google.testing.junit.testparameterinjector.TestParameters;
            import org.junit.Test;
            import org.junit.runner.RunWith;

            @RunWith(TestParameterInjector.class)
            public class TestType {
              enum MyEnum {
                FOO,
                BAR
              }

              @Test
              @TestParameters("{mode: FOO}")
              @TestParameters("{mode: BAR}")
              public void myTest(MyEnum mode) {}
            }
            """)
        .addOutputLines(
            "TestType.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameter;
            import com.google.testing.junit.testparameterinjector.TestParameterInjector;
            import com.google.testing.junit.testparameterinjector.TestParameters;
            import org.junit.Test;
            import org.junit.runner.RunWith;

            @RunWith(TestParameterInjector.class)
            public class TestType {
              enum MyEnum {
                FOO,
                BAR
              }

              @Test
              public void myTest(@TestParameter MyEnum mode) {}
            }
            """)
        .doTest();
  }

  @Test
  public void negative_tooManyParams() {
    testHelper
        .addInputLines(
            "TestType.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameters;
            import org.junit.Test;

            public class TestType {
              enum MyEnum {
                FOO,
                BAR
              }

              @Test
              @TestParameters({"{mode: FOO, b: true}", "{mode: BAR, b: false}"})
              public void myTest(MyEnum mode, boolean b) {}
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void negative_customName_notSupported() {
    testHelper
        .addInputLines(
            "TestType.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameters;
            import org.junit.Test;

            public class TestType {
              enum MyEnum {
                FOO,
                BAR
              }

              @Test
              @TestParameters(
                  customName = "foo",
                  value = {"{mode: FOO}", "{mode: BAR}"})
              public void myTest(MyEnum mode) {}
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void booleanParameter() {
    testHelper
        .addInputLines(
            "TestType.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameters;
            import org.junit.Test;

            public class TestType {
              @Test
              @TestParameters({"{b: true}", "{b: false}"})
              public void myTest(boolean b) {}
            }
            """)
        .addOutputLines(
            "TestType.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameter;
            import com.google.testing.junit.testparameterinjector.TestParameters;
            import org.junit.Test;

            public class TestType {
              @Test
              public void myTest(@TestParameter boolean b) {}
            }
            """)
        .doTest();
  }

  @Test
  public void negative_substringMatch() {
    testHelper
        .addInputLines(
            "TestType.java",
            """
            import com.google.testing.junit.testparameterinjector.TestParameters;
            import org.junit.Test;

            public class TestType {
              enum MyEnum {
                OPEN,
                OPEN_PENDING
              }

              @Test
              @TestParameters({"{mode: OPEN_PENDING}", "{mode: OPEN_PENDING}"})
              public void myTest(MyEnum mode) {}
            }
            """)
        .expectUnchanged()
        .doTest();
  }
}
