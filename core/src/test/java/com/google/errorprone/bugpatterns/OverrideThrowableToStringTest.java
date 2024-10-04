/*
 * Copyright 2017 The Error Prone Authors.
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
import com.google.errorprone.BugCheckerRefactoringTestHelper.TestMode;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * @author mariasam@google.com (Maria Sam)
 */
@RunWith(JUnit4.class)
public class OverrideThrowableToStringTest {
  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(OverrideThrowableToString.class, getClass());

  @Test
  public void positiveCases() {
    compilationHelper
        .addSourceLines(
            "OverrideThrowableToStringPositiveCases.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            /**
             * @author mariasam@google.com (Maria Sam)
             */
            class OverrideThrowableToStringPositiveCases {

              class BasicTest extends Throwable {

                @Override
                // BUG: Diagnostic contains: override
                public String toString() {
                  return "";
                }
              }

              class MultipleMethods extends Throwable {

                public MultipleMethods() {
                  ;
                }

                @Override
                // BUG: Diagnostic contains: override
                public String toString() {
                  return "";
                }
              }

              class NoOverride extends Throwable {

                // BUG: Diagnostic contains: override
                public String toString() {
                  return "";
                }
              }
            }""")
        .doTest();
  }

  @Test
  public void negativeCases() {
    compilationHelper
        .addSourceLines(
            "OverrideThrowableToStringNegativeCases.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            /**
             * @author mariasam@google.com (Maria Sam)
             */
            public class OverrideThrowableToStringNegativeCases {

              class BasicTest extends Throwable {}

              class OtherToString {
                public String toString() {
                  return "";
                }
              }

              class NoToString extends Throwable {
                public void test() {
                  System.out.println("test");
                }
              }

              class GetMessage extends Throwable {
                public String getMessage() {
                  return "";
                }
              }

              class OverridesBoth extends Throwable {
                public String toString() {
                  return "";
                }

                public String getMessage() {
                  return "";
                }
              }
            }""")
        .doTest();
  }

  @Test
  public void fixes() {
    BugCheckerRefactoringTestHelper.newInstance(OverrideThrowableToString.class, getClass())
        .addInputLines(
            "OverrideThrowableToStringPositiveCases.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            /**
             * @author mariasam@google.com (Maria Sam)
             */
            class OverrideThrowableToStringPositiveCases {

              class BasicTest extends Throwable {

                @Override
                // BUG: Diagnostic contains: override
                public String toString() {
                  return "";
                }
              }

              class MultipleMethods extends Throwable {

                public MultipleMethods() {
                  ;
                }

                @Override
                // BUG: Diagnostic contains: override
                public String toString() {
                  return "";
                }
              }

              class NoOverride extends Throwable {

                // BUG: Diagnostic contains: override
                public String toString() {
                  return "";
                }
              }
            }""")
        .addOutputLines(
            "OverrideThrowableToStringPositiveCases_expected.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            /**
             * @author mariasam@google.com (Maria Sam)
             */
            class OverrideThrowableToStringPositiveCases {

              // BUG: Diagnostic contains: override
              class BasicTest extends Throwable {

                @Override
                public String getMessage() {
                  return "";
                }
              }

              class MultipleMethods extends Throwable {

                public MultipleMethods() {
                  ;
                }

                @Override
                public String getMessage() {
                  return "";
                }
              }

              class NoOverride extends Throwable {

                public String getMessage() {
                  return "";
                }
              }
            }""")
        .doTest(TestMode.AST_MATCH);
  }
}
