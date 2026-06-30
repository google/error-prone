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

package com.google.errorprone.bugpatterns.javadoc;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link MissingJavadoc} bug pattern. */
@RunWith(JUnit4.class)
public final class MissingJavadocTest {
  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(MissingJavadoc.class, getClass());
  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(MissingJavadoc.class, getClass());

  @Test
  public void publicClassWithoutJavadoc_warns() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            // BUG: Diagnostic contains: MissingJavadoc
            public class Test {}
            """)
        .doTest();
  }

  @Test
  public void publicClassWithJavadoc_passes() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            /** This is a class doc. */
            public class Test {}
            """)
        .doTest();
  }

  @Test
  public void privateClassWithoutJavadoc_passes() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            /** Class doc. */
            public class Test {
              private static class Inner {}
            }
            """)
        .doTest();
  }

  @Test
  public void publicMethodWithoutJavadoc_warns() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            /** This is class doc. */
            public class Test {
              // BUG: Diagnostic contains: MissingJavadoc
              public void foo() {}
            }
            """)
        .doTest();
  }

  @Test
  public void publicMethodWithJavadoc_passes() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            /** This is class doc. */
            public class Test {
              /** Method doc. */
              public void foo() {}
            }
            """)
        .doTest();
  }

  @Test
  public void simpleGetterAndSetter_passes() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            /** This is class doc. */
            public class Test {
              private int x;

              public int getX() {
                return x;
              }

              public void setX(int x) {
                this.x = x;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void overriddenMethodWithoutJavadoc_passes() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            /** This is class doc. */
            public class Test implements Runnable {
              @Override
              public void run() {}
            }
            """)
        .doTest();
  }

  @Test
  public void builderClassSuggestedFix() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            /** This is class doc. */
            public class Test {
              public static final class Builder {}
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            /** This is class doc. */
            public class Test {
              /** A builder for {@link Test}. */
              public static final class Builder {}
            }
            """)
        .doTest();
  }

  @Test
  public void privateBuilderClass_noSuggestedFix() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            /** This is class doc. */
            public class Test {
              private static final class Builder {}
            }
            """)
        .doTest();
  }
}
