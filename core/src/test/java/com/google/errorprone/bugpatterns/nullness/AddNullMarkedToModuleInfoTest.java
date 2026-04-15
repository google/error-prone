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

/** {@link AddNullMarkedToModuleInfo}Test */
@RunWith(JUnit4.class)
public class AddNullMarkedToModuleInfoTest {

  private final BugCheckerRefactoringTestHelper refactoringTestHelper =
      BugCheckerRefactoringTestHelper.newInstance(AddNullMarkedToModuleInfo.class, getClass())
          .setArgs("--add-reads=com.google.test=ALL-UNNAMED");

  @Test
  public void annotationInserted() {
    refactoringTestHelper
        .addInputLines(
            "in/module-info.java",
            """
            module com.google.test {
            }
            """)
        .addOutputLines(
            "out/module-info.java",
            """
            import org.jspecify.annotations.NullMarked;

            @NullMarked
            module com.google.test {
            }
            """)
        .doTest();
  }

  @Test
  public void annotationInserted_withOtherAnnotation() {
    refactoringTestHelper
        .addInputLines(
            "in/module-info.java",
            """
            @Deprecated
            module com.google.test {
            }
            """)
        .addOutputLines(
            "out/module-info.java",
            """
            import org.jspecify.annotations.NullMarked;

            @NullMarked
            @Deprecated
            module com.google.test {
            }
            """)
        .doTest();
  }

  @Test
  public void annotationNotInserted_alreadyPresent() {
    refactoringTestHelper
        .addInputLines(
            "in/module-info.java",
            """
            import org.jspecify.annotations.NullMarked;

            @NullMarked
            module com.google.test {
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void annotationNotInserted_notModuleInfo() {
    refactoringTestHelper
        .addInputLines(
            "in/Test.java",
            """
            class T {
            }
            """)
        .expectUnchanged()
        .doTest();
  }
}
