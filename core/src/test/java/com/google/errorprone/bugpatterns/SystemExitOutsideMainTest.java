/*
 * Copyright 2018 The Error Prone Authors.
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

/** Tests for {@link SystemExitOutsideMain}. */
@RunWith(JUnit4.class)
public class SystemExitOutsideMainTest {

  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(SystemExitOutsideMain.class, getClass());

  @Test
  public void systemExitNotMain() {
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              void f() {
                // BUG: Diagnostic contains: SystemExitOutsideMain
                System.exit(0);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void systemExitMainLookalikeWithoutParameters() {
    assume().that(Runtime.version().feature()).isLessThan(25);
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              public static void main() {
                // BUG: Diagnostic contains: SystemExitOutsideMain
                System.exit(0);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void systemExitMainNoArgsNegative() {
    assume().that(Runtime.version().feature()).isAtLeast(25);
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              public static void main() {
                System.exit(0);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void systemExitMainLookalikeWithTwoParameters() {
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              public static void main(String[] args, int num) {
                // BUG: Diagnostic contains: SystemExitOutsideMain
                System.exit(0);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void systemExitMainLookalikeWithoutStatic() {
    assume().that(Runtime.version().feature()).isLessThan(25);
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              public void main(String[] args) {
                // BUG: Diagnostic contains: SystemExitOutsideMain
                System.exit(0);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void systemExitInstanceMainNegative() {
    assume().that(Runtime.version().feature()).isAtLeast(25);
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              public void main(String[] args) {
                System.exit(0);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void systemExitMainLookalikeDifferentReturnType() {
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              public static int main(String[] args) {
                // BUG: Diagnostic contains: SystemExitOutsideMain
                System.exit(0);
                return 0;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void systemExitMainLookalikeDifferentVisibility() {
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              private static void main(String[] args) {
                // BUG: Diagnostic contains: SystemExitOutsideMain
                System.exit(0);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void systemExitMainLookalikeDifferentArrayParameter() {
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              private static void main(int[] args) {
                // BUG: Diagnostic contains: SystemExitOutsideMain
                System.exit(0);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void systemExitMainLookalikeStringParameter() {
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              private static void main(String args) {
                // BUG: Diagnostic contains: SystemExitOutsideMain
                System.exit(0);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void systemExitInMethodMainNotInClass() {
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              public static void foo() {
                // BUG: Diagnostic contains: SystemExitOutsideMain
                System.exit(0);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void systemExitInMethodMainInClassNegative() {
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              public static void main(String[] args) {
                foo();
              }

              public static void foo() {
                System.exit(0);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void systemExitMainVarArgsParameterNegative() {
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              public static void main(String... args) {
                System.exit(0);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void systemExitMainNegative() {
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              public static void main(String[] args) {
                System.exit(0);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void systemExitMainLookalikePackagePrivate() {
    assume().that(Runtime.version().feature()).isLessThan(25);
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              static void main(String[] args) {
                // BUG: Diagnostic contains: SystemExitOutsideMain
                System.exit(0);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void systemExitProtectedMainNegative() {
    assume().that(Runtime.version().feature()).isAtLeast(25);
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              protected static void main(String[] args) {
                System.exit(0);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void systemExitPackagePrivateMainNegative() {
    assume().that(Runtime.version().feature()).isAtLeast(25);
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              static void main(String[] args) {
                System.exit(0);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void systemExitInstanceNoArgsMainNegative() {
    assume().that(Runtime.version().feature()).isAtLeast(25);
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              void main() {
                System.exit(0);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void systemExitInMethodNoArgsMainInClassNegative() {
    assume().that(Runtime.version().feature()).isAtLeast(25);
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              void main() {
                foo();
              }

              static void foo() {
                System.exit(0);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void systemExitInMethodInstanceMainInClassNegative() {
    assume().that(Runtime.version().feature()).isAtLeast(25);
    helper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              void main(String[] args) {
                foo();
              }

              static void foo() {
                System.exit(0);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void systemExitImplicitlyDeclaredClassNegative() {
    assume().that(Runtime.version().feature()).isAtLeast(25);
    helper
        .addSourceLines(
            "Test.java",
            """
            void main() {
              System.exit(0);
            }
            """)
        .doTest();
  }

}
