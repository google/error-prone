/*
 * Copyright 2014 The Error Prone Authors.
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

import static com.google.common.truth.TruthJUnit.assume;

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@code CannotMockFinalClass}.
 *
 * @author Louis Wasserman
 */
@RunWith(JUnit4.class)
public class CannotMockFinalClassTest {
  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(CannotMockFinalClass.class, getClass());

  @Test
  public void positiveCase() {
    compilationHelper
        .addSourceLines(
            "CannotMockFinalClassPositiveCases.java",
            """
            import org.junit.runner.RunWith;
            import org.junit.runners.JUnit4;
            import org.mockito.Mock;
            import org.mockito.Mockito;

            /** Test for CannotMockFinalClass. */
            @RunWith(JUnit4.class)
            public class CannotMockFinalClassPositiveCases {
              static final class FinalClass {}

              // BUG: Diagnostic contains: Mockito cannot mock
              @Mock FinalClass impossible;

              public void method() {
                // BUG: Diagnostic contains: Mockito cannot mock
                FinalClass local = Mockito.mock(FinalClass.class);
              }
            }\
            """)
        .doTest();
  }

  @Test
  public void positiveCase_record() {
    assume().that(Runtime.version().feature()).isAtLeast(16);

    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import org.junit.runner.RunWith;
            import org.junit.runners.JUnit4;
            import org.mockito.Mock;
            import org.mockito.Mockito;

            @RunWith(JUnit4.class)
            public class Test {
              record Record() {}

              // BUG: Diagnostic contains:
              @Mock Record record;
            }
            """)
        .doTest();
  }

  @Test
  public void negativeCase() {
    compilationHelper
        .addSourceLines(
            "CannotMockFinalClassNegativeCases.java",
            """
            import org.junit.runner.RunWith;
            import org.junit.runners.JUnit4;
            import org.mockito.Mock;
            import org.mockito.Mockito;

            /** Test for CannotMockFinalClass. */
            @RunWith(JUnit4.class)
            public class CannotMockFinalClassNegativeCases {
              static class NonFinalClass {}

              @Mock NonFinalClass okToMock;

              public void method() {
                NonFinalClass local = Mockito.mock(NonFinalClass.class);
              }
            }\
            """)
        .doTest();
  }

  @Test
  public void negativeCase2() {
    compilationHelper
        .addSourceLines(
            "CannotMockFinalClassNegativeCases2.java",
            """
            import org.mockito.Mock;
            import org.mockito.Mockito;

            /** Test for CannotMockFinalClass in the absence of @RunWith(JUnit4.class). */
            public class CannotMockFinalClassNegativeCases2 {
              static final class FinalClass {}

              @Mock FinalClass impossible;

              public void method() {
                FinalClass local = Mockito.mock(FinalClass.class);
              }
            }\
            """)
        .doTest();
  }
}
