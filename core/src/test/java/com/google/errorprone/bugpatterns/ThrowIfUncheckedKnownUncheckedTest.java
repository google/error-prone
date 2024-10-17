/*
 * Copyright 2024 The Error Prone Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.errorprone.bugpatterns;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ThrowIfUncheckedKnownUncheckedTest {
  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(ThrowIfUncheckedKnownUnchecked.class, getClass());
  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(ThrowIfUncheckedKnownUnchecked.class, getClass());

  @Test
  public void knownRuntimeException() {
    compilationHelper
        .addSourceLines(
            "Foo.java",
            """
            import static com.google.common.base.Throwables.throwIfUnchecked;

            class Foo {
              void x(IllegalArgumentException e) {
                // BUG: Diagnostic contains:
                throwIfUnchecked(e);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void knownError() {
    compilationHelper
        .addSourceLines(
            "Foo.java",
            """
            import static com.google.common.base.Throwables.throwIfUnchecked;

            class Foo {
              void x(LinkageError e) {
                // BUG: Diagnostic contains:
                throwIfUnchecked(e);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void knownUncheckedMulticatch() {
    compilationHelper
        .addSourceLines(
            "Foo.java",
            """
            import static com.google.common.base.Throwables.throwIfUnchecked;

            class Foo {
              void x() {
                try {
                } catch (RuntimeException | Error e) {
                  // BUG: Diagnostic contains:
                  throwIfUnchecked(e);
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void knownCheckedException() {
    compilationHelper
        .addSourceLines(
            "Foo.java",
            """
            import static com.google.common.base.Throwables.throwIfUnchecked;

            import java.io.IOException;

            class Foo {
              void x(IOException e) {
                throwIfUnchecked(e);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void unknownType() {
    compilationHelper
        .addSourceLines(
            "Foo.java",
            """
            import static com.google.common.base.Throwables.throwIfUnchecked;

            class Foo {
              void x(Exception e) {
                throwIfUnchecked(e);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void refactoring() {
    refactoringHelper
        .addInputLines(
            "in/Foo.java",
            """
            import static com.google.common.base.Throwables.throwIfUnchecked;

            class Foo {
              void x(IllegalArgumentException e) {
                log(e);
                throwIfUnchecked(e);
                throw new RuntimeException(e);
              }

              void log(Throwable t) {}
            }
            """)
        .addOutputLines(
            "out/Foo.java",
            """
            import static com.google.common.base.Throwables.throwIfUnchecked;

            class Foo {
              void x(IllegalArgumentException e) {
                log(e);
                throw e;
              }

              void log(Throwable t) {}
            }
            """)
        .doTest();
  }
}
