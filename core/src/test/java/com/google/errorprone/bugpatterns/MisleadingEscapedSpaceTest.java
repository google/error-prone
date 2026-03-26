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

package com.google.errorprone.bugpatterns;

import com.google.errorprone.CompilationTestHelper;
import com.google.testing.junit.testparameterinjector.TestParameter;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(TestParameterInjector.class)
public final class MisleadingEscapedSpaceTest {
  private final CompilationTestHelper testHelper =
      CompilationTestHelper.newInstance(MisleadingEscapedSpace.class, getClass());

  private enum LineSeparator {
    CRLF("\r\n"),
    LF("\n");

    final String separator;

    LineSeparator(String separator) {
      this.separator = separator;
    }
  }

  @TestParameter private LineSeparator lineSeparator;

  private void test(String text) {
    testHelper.addSourceLines("Test.class", text.replace("\n", lineSeparator.separator)).doTest();
  }

  @Test
  public void misleadingEscape() {
    test(
        """
        class Test {
          // BUG: Diagnostic contains:
          private static final String FOO = " \\s ";
        }
        """);
  }

  @Test
  public void literalBackslashS() {
    test(
        """
        class Test {
          private static final String FOO = " \\\\s ";
        }
        """);
  }

  @Test
  public void asSingleCharacter_misleading() {
    test(
        """
        class Test {
          // BUG: Diagnostic contains:
          private static final char x = '\\s';
        }
        """);
  }

  @Test
  public void withinTextBlock_notAtEndOfLine_misleading() {
    test(
        """
        class Test {
          private static final String FOO =
              // BUG: Diagnostic contains:
              \"""
          foo   \\s  bar
          \""";
          private static final String BAZ =
              // BUG: Diagnostic contains:
              \"""
          foo   \\s
          bar  \\s baz
          \""";
        }
        """);
  }

  @Test
  public void atEndOfLine_notMisleading() {
    test(
        """
        class Test {
          private static final String FOO =
              \"""
          foo   \\s
          bar     \\s
          \""";
        }
        """);
  }

  @Test
  public void multipleAtEndOfLine_notMisleading() {
    test(
        """
        class Test {
          private static final String FOO =
              \"""
          foo   \\s\\s\\s\\s
          \""";
        }
        """);
  }

  @Test
  public void withinCommentInBrokenUpString_noFinding() {
    test(
        """
        class Test {
          private static final String FOO = "foo" + /* \\s */ " bar";
        }
        """);
  }

  @Test
  public void atEndOfString_noFinding() {
    test(
        """
        class Test {
          private static final String FOO =
              \"""
          foo
          bar\\s\""";
        }
        """);
  }

  @Test
  public void escapedSpaceAtEndOfString() {
    test(
        """
        class Test {
          // BUG: Diagnostic contains:
          private static final String FOO = "foo\\s";
        }
        """);
  }
}
