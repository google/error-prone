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

/** Tests for {@link ThreadBuilderNameWithPlaceholder}. */
@RunWith(JUnit4.class)
public final class ThreadBuilderNameWithPlaceholderTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(ThreadBuilderNameWithPlaceholder.class, getClass());

  @Test
  public void nameOk() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              public void test() {
                Thread.Builder builder = Thread.ofPlatform();
                builder.name("foo");
                builder.name("foo-", 0);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void nameBad() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              public void test() {
                Thread.Builder builder = Thread.ofPlatform();
                // BUG: Diagnostic contains: Thread.Builder.name() does not accept placeholders
                builder.name("foo%s");
                // BUG: Diagnostic contains: Thread.Builder.name() does not accept placeholders
                builder.name("foo-%s", 0);
                // BUG: Diagnostic contains: Thread.Builder.name() does not accept placeholders
                builder.name("foo%d");
                // BUG: Diagnostic contains: Thread.Builder.name() does not accept placeholders
                builder.name("foo-%d", 0);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void nameBadButNotFlagged() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              public void test() {
                Thread.Builder builder = Thread.ofPlatform();
                String name = "foo%s";
                builder.name(name);
              }
            }
            """)
        .doTest();
  }
}
