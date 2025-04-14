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

/** {@link AddNullMarkedToPackageInfo}Test */
@RunWith(JUnit4.class)
public class AddNullMarkedToPackageInfoTest {

  private final BugCheckerRefactoringTestHelper refactoringTestHelper =
      BugCheckerRefactoringTestHelper.newInstance(AddNullMarkedToPackageInfo.class, getClass());

  @Test
  public void annotationInserted() {
    refactoringTestHelper
        .addInputLines(
            "in/package-info.java",
            """
            @ObjectiveCName("JBT")
            package com.google.apps.bigtop.sync.client.api.gmailcards;

            import com.google.j2objc.annotations.ObjectiveCName;
            """)
        .addOutputLines(
            "out/package-info.java",
            """
            @NullMarked
            @ObjectiveCName("JBT")
            package com.google.apps.bigtop.sync.client.api.gmailcards;

            import com.google.j2objc.annotations.ObjectiveCName;
            import org.jspecify.annotations.NullMarked;
            """)
        .doTest();
  }

  @Test
  public void annotationNotInserted_alreadyPresent() {
    refactoringTestHelper
        .addInputLines(
            "in/package-info.java",
            """
            @ObjectiveCName("JBT")
            @NullMarked
            package com.google.apps.bigtop.sync.client.api.gmailcards;

            import com.google.j2objc.annotations.ObjectiveCName;
            import org.jspecify.annotations.NullMarked;
            """)
        .addOutputLines(
            "out/package-info.java",
            """
            @ObjectiveCName("JBT")
            @NullMarked
            package com.google.apps.bigtop.sync.client.api.gmailcards;

            import com.google.j2objc.annotations.ObjectiveCName;
            import org.jspecify.annotations.NullMarked;
            """)
        .doTest();
  }

  @Test
  public void annotationNotInserted_notPackageInfo() {
    refactoringTestHelper
        .addInputLines(
            "in/Test.java",
            """
            class T {
              private final Object obj2 = null;

              class Nullable {}
            }
            """)
        .expectUnchanged()
        .doTest();
  }
}
