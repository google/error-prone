/*
 * Copyright 2012 The Error Prone Authors.
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

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Unit tests for {@link SuppressWarningsDeprecated}.
 *
 * @author sjnickerson@google.com (Simon Nickerson)
 */
@RunWith(JUnit4.class)
public class SuppressWarningsDeprecatedTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(SuppressWarningsDeprecated.class, getClass());

  @Test
  public void positiveCase() {
    compilationHelper
        .addSourceLines(
            "SuppressWarningsDeprecatedPositiveCases.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            /*
             * Copyright 2012 The Error Prone Authors.
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

            /**
             * Positive cases for {@link SuppressWarningsDeprecated}.
             *
             * @author sjnickerson@google.com (Simon Nickerson)
             */
            public class SuppressWarningsDeprecatedPositiveCases {

              // BUG: Diagnostic contains: @SuppressWarnings("deprecation")
              @SuppressWarnings("deprecated")
              public static void positiveCase1() {}

              // BUG: Diagnostic contains: @SuppressWarnings("deprecation")
              @SuppressWarnings({"deprecated"})
              public static void positiveCase2() {}

              // BUG: Diagnostic contains: @SuppressWarnings({"deprecation", "foobarbaz"})
              @SuppressWarnings({"deprecated", "foobarbaz"})
              public static void positiveCase3() {}

              public static void positiveCase4() {
                // BUG: Diagnostic contains: @SuppressWarnings({"deprecation", "foobarbaz"})
                @SuppressWarnings({"deprecated", "foobarbaz"})
                int a = 3;
              }

              public static void positiveCase5() {
                // BUG: Diagnostic contains: @SuppressWarnings("deprecation")
                @SuppressWarnings("deprecated")
                int a = 3;
              }

              public static void positiveCase6() {
                // BUG: Diagnostic contains: @SuppressWarnings("deprecation")
                @SuppressWarnings("deprecated")
                class Foo {}
                ;
              }

              public static void positiveCase7() {
                // BUG: Diagnostic contains: @SuppressWarnings({"deprecation", "foobarbaz"})
                @SuppressWarnings({"deprecated", "foobarbaz"})
                class Foo {}
                ;
              }

              // BUG: Diagnostic contains: @SuppressWarnings(value = "deprecation")
              @SuppressWarnings(value = {"deprecated"})
              public static void positiveCase8() {}

              // BUG: Diagnostic contains: @SuppressWarnings(value = "deprecation")
              @SuppressWarnings(value = "deprecated")
              public static void positiveCase9() {}
            }
            """)
        .doTest();
  }

  @Test
  public void negativeCase() {
    compilationHelper
        .addSourceLines(
            "SuppressWarningsDeprecatedNegativeCases.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            /**
             * Negative cases for {@link SuppressWarningsDeprecated}.
             *
             * @author sjnickerson@google.com (Simon Nickerson)
             */
            public class SuppressWarningsDeprecatedNegativeCases {
              @SuppressWarnings({"deprecation"})
              public static void negativeCase1() {}

              @SuppressWarnings("deprecation")
              public static void negativeCase2() {}

              public static void negativeCase3() {
                @SuppressWarnings({"deprecation"})
                int a = 3;
              }

              public static void negativeCase4() {
                @SuppressWarnings("deprecation")
                int a = 3;
              }

              public static void negativeCase5() {
                @SuppressWarnings({"deprecation"})
                class Foo {}
                Foo a = null;
              }

              public static void negativeCase6() {
                @SuppressWarnings("deprecation")
                class Bar {}
                Bar b = null;
              }
            }\
            """)
        .doTest();
  }
}
