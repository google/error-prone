/*
 * Copyright 2025 The Error Prone Authors.
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

@RunWith(JUnit4.class)
public final class MockIllegalThrowsTest {
  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(MockIllegalThrows.class, getClass());

  @Test
  public void positive() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import static org.mockito.Mockito.when;

            abstract class Test {
              abstract Object foo();

              void test(Test t) {
                // BUG: Diagnostic contains: only unchecked
                when(t.foo()).thenThrow(new Exception());
              }
            }
            """)
        .doTest();
  }

  @Test
  public void positiveWithSpecificType() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import static org.mockito.Mockito.when;

            abstract class Test {
              static class SpecificException extends Exception {}

              abstract Object foo() throws SpecificException;

              void test(Test t) throws Exception {
                // BUG: Diagnostic contains: are SpecificException, or any unchecked
                when(t.foo()).thenThrow(new Exception());
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negative_exceptionTypeViaParameter() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import static org.mockito.Mockito.when;

            abstract class Test {
              static class SpecificException extends Exception {}

              abstract Object foo() throws SpecificException;

              void test(Test t, Exception e) throws Exception {
                when(t.foo()).thenThrow(e);
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
            import static org.mockito.Mockito.when;

            abstract class Test {
              abstract Object foo() throws Exception;

              void test(Test t) throws Exception {
                when(t.foo()).thenThrow(new Exception());
              }
            }
            """)
        .doTest();
  }

  @Test
  public void genericException() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import static org.mockito.Mockito.when;

            abstract class Test {
              interface GenericException<E extends Exception> {
                Object execute() throws E;
              }

              void test(GenericException<Exception> ge) throws Exception {
                when(ge.execute()).thenThrow(new Exception());
              }
            }
            """)
        .doTest();
  }
}
