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
public final class ExplicitArrayForVarargsTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(ExplicitArrayForVarargs.class, getClass());

  @Test
  public void positive() {
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              void test() {
                // BUG: Diagnostic contains: String.format("foo %s", "foo")
                String a = String.format("foo %s", new Object[] {"foo"});
                // BUG: Diagnostic contains: String.format("foo %s")
                String b = String.format("foo %s", new Object[] {});
              }
            }
            """)
        .doTest();
  }

  @Test
  public void multidimensionalArray() {
    helper
        .addSourceLines(
            "Test.java",
            """
            import java.util.Arrays;
            import java.util.List;

            class Test {
              List<Object[]> test() {
                return Arrays.asList(new Object[][] {{1, 2}, {3, 4}});
              }
            }
            """)
        .doTest();
  }

  @Test
  public void notFinalArgument() {
    helper
        .addSourceLines(
            "Test.java",
            """
            import java.util.Arrays;
            import java.util.List;

            class Test {
              List<Object[]> test() {
                return Arrays.asList(new Object[] {1, 2}, new Object[] {3, 4});
              }
            }
            """)
        .doTest();
  }

  @Test
  public void emptyInitializers() {
    helper
        .addSourceLines(
            "Test.java",
            """
            import java.util.Arrays;
            import java.util.List;

            class Test {
              List<Object> empty() {
                // BUG: Diagnostic contains: Arrays.asList()
                return Arrays.asList(new Object[0]);
              }

              List<Object> nonempty() {
                // BUG: Diagnostic contains: Arrays.asList(null)
                return Arrays.asList(new Object[1]);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void refactoringWouldCauseRecursion() {
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              Test() {
                this(new String[0]);
              }

              protected Test(String... xs) {}
            }
            """)
        .doTest();
  }
}
