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
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class InterruptedInCatchBlockTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(InterruptedInCatchBlock.class, getClass());
  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(InterruptedInCatchBlock.class, getClass());

  @Test
  public void positive() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            package com.google.errorprone.bugpatterns;

            public class Test {
              void f() {
                try {
                  Thread.sleep(10);
                } catch (InterruptedException e) {
                  // BUG: Diagnostic contains: InterruptedInCatchBlock
                  Thread.interrupted();
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negativeOtherException() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            package com.google.errorprone.bugpatterns;

            public class Test {
              void f() {
                try {
                  throw new RuntimeException();
                } catch (RuntimeException e) {
                  Thread.interrupted();
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negativeIsInterrupted() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            package com.google.errorprone.bugpatterns;

            public class Test {
              void f() {
                try {
                  Thread.sleep(10);
                } catch (InterruptedException e) {
                  Thread.currentThread().isInterrupted();
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negativeInterrupt() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            package com.google.errorprone.bugpatterns;

            public class Test {
              void f() {
                try {
                  Thread.sleep(10);
                } catch (InterruptedException e) {
                  Thread.currentThread().interrupt();
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void positiveNested() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            package com.google.errorprone.bugpatterns;

            public class Test {
              void f() {
                try {
                  Thread.sleep(10);
                } catch (InterruptedException e) {
                  if (true) {
                    // BUG: Diagnostic contains: InterruptedInCatchBlock
                    Thread.interrupted();
                  }
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void refactoring() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            package com.google.errorprone.bugpatterns;

            public class Test {
              void f() {
                try {
                  Thread.sleep(10);
                } catch (InterruptedException e) {
                  Thread.interrupted();
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            package com.google.errorprone.bugpatterns;

            public class Test {
              void f() {
                try {
                  Thread.sleep(10);
                } catch (InterruptedException e) {
                  Thread.currentThread().interrupt();
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void refactoringReturnValueUsed() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            package com.google.errorprone.bugpatterns;

            public class Test {
              void f() {
                try {
                  Thread.sleep(10);
                } catch (InterruptedException e) {
                  boolean interrupted = Thread.interrupted();
                }
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            package com.google.errorprone.bugpatterns;

            public class Test {
              void f() {
                try {
                  Thread.sleep(10);
                } catch (InterruptedException e) {
                  boolean interrupted = Thread.interrupted();
                }
              }
            }
            """)
        .doTest();
  }
}
