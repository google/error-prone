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

package com.google.errorprone.bugpatterns.nullness;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link AddNullMarkedToClass}Test */
@RunWith(JUnit4.class)
public final class AddNullMarkedToClassTest {

  private final BugCheckerRefactoringTestHelper refactoringTestHelper =
      BugCheckerRefactoringTestHelper.newInstance(AddNullMarkedToClass.class, getClass());

  @Test
  public void annotationInserted() {
    refactoringTestHelper
        .addInputLines(
            "in/class.java",
            """
            package com.google.foo;

            class Test {
              public void test() {
                System.out.println("Hello World");
              }
            }
            """)
        .addOutputLines(
            "out/class.java",
            """
            package com.google.foo;

            import org.jspecify.annotations.NullMarked;

            @NullMarked
            class Test {
              public void test() {
                System.out.println("Hello World");
              }
            }
            """)
        .doTest();
  }

  @Test
  public void annotationNotInserted_alreadyPresent() {
    refactoringTestHelper
        .addInputLines(
            "in/class.java",
            """
            package com.google.foo;

            import org.jspecify.annotations.NullMarked;

            @NullMarked
            class Test {
              public void test() {
                System.out.println("Hello World");
              }
            }
            """)
        .addOutputLines(
            "out/class.java",
            """
            package com.google.foo;

            import org.jspecify.annotations.NullMarked;

            @NullMarked
            class Test {
              public void test() {
                System.out.println("Hello World");
              }
            }
            """)
        .doTest();
  }

  @Test
  public void annotationNotInserted_notPackageInfo() {
    refactoringTestHelper
        .addInputLines(
            "in/package-info.java",
            """
            @NullMarked
            package com.google.foo;

            import org.jspecify.annotations.NullMarked;
            """)
        .expectUnchanged()
        .doTest();
  }
}
