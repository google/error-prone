/*
 * Copyright 2024 The Error Prone Authors.
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

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link InvalidSnippet} bug pattern. */
@RunWith(JUnit4.class)
public final class InvalidSnippetTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(InvalidSnippet.class, getClass());

  @Test
  @SuppressWarnings("MisformattedTestData") // sadly intentional, so the BUG line precedes the issue
  public void snippetWithoutColonOrFile() {
    helper
        .addSourceLines(
            "Test.java",
            """
            /**
             * // BUG: Diagnostic contains:
             * {@snippet
             *    I have no colon
             *  }
             */
            interface Test {}
            """)
        .doTest();
  }

  @Test
  @SuppressWarnings("MisformattedTestData") // sadly intentional, so the BUG line precedes the issue
  public void snippetWithoutColon_causingParseErrors() {
    helper
        .addSourceLines(
            "Test.java",
            """
            /**
             * // BUG: Diagnostic contains:
             * {@snippet
             *    List<Foo, Bar> xs = List.of();
             *  }
             */
            interface Test {}
            """)
        .doTest();
  }

  @Test
  public void snippetWithoutBody_butWithFile() {
    helper
        .addSourceLines(
            "Test.java",
            """
            /** {@snippet file="foo.java"} */
            interface Test {}
            """)
        .doTest();
  }

  @Test
  public void snippetWithColon() {
    helper
        .addSourceLines(
            "Test.java",
            """
            /**
             * {@snippet :
             *    I have a colon
             *  }
             */
            interface Test {}
            """)
        .doTest();
  }
}
