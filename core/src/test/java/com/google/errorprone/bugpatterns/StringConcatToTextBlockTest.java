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

import static com.google.errorprone.BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class StringConcatToTextBlockTest {
  private final CompilationTestHelper testHelper =
      CompilationTestHelper.newInstance(StringConcatToTextBlock.class, getClass());
  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(StringConcatToTextBlock.class, getClass());

  @Test
  public void negative() {
    testHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              String s = new Object() + "world\\n";
              String oneToken = "hello\\nworld";
              String comment =
                  "hello\\\\n"
                      // comment
                      + "world\\\\n";
              String noNewline = "hello" + "world\\n";
              String extra = "prefix" + s + "hello\\n" + "world\\n";
              String noTrailing = "hello\\n" + "world";
            }
            """)
        .doTest();
  }

  @Test
  public void positive() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              String s = "hello\\n" + "world\\n";
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              String s =
                  \"""
                  hello
                  world
                  \""";
            }
            """)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void noTrailing() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              String s = "hello\\n" + "foo\\n" + "world";
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              String s =
                  \"""
                  hello
                  foo
                  world\\
                  \""";
            }
            """)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void escape() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              String s = "\\n" + "\\nhello\\n" + "foo\\n\\n" + "world";
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              String s =
                  \"""


                  hello
                  foo

                  world\\
                  \""";
            }
            """)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void guavaJoiner() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import com.google.common.base.Joiner;

            class Test {
              String s = Joiner.on('\\n').join("string", "literals");
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import com.google.common.base.Joiner;

            class Test {
              String s =
                  \"""
                  string
                  literals\\
                  \""";
            }
            """)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void stringJoiner() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.util.StringJoiner;

            class Test {
              String s = new StringJoiner("\\n").add("string").add("literals").toString();
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.util.StringJoiner;

            class Test {
              String s =
                  \"""
                  string
                  literals\\
                  \""";
            }
            """)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void stringJoin() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.util.StringJoiner;

            class Test {
              String s = String.join("\\n", "string", "literals");
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.util.StringJoiner;

            class Test {
              String s =
                  \"""
                  string
                  literals\\
                  \""";
            }
            """)
        .doTest(TEXT_MATCH);
  }

  // b/396965922
  @Test
  public void trailingSpacesInMultilineString() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              private static final String FOO =
                  "Lorem ipsum dolor sit amet, consectetur adipiscing elit: \\n"
                      + "- Lorem ipsum dolor sit amet, consectetur adipiscing elit?   \\n"
                      + "- Lorem ipsum dolor sit amet, consectetur adipiscing elit?\\n"
                      + "- Lorem ipsum dolor sit amet, consectetur adipiscing elit?\\n";
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              private static final String FOO =
                  \"""
                  Lorem ipsum dolor sit amet, consectetur adipiscing elit:\\s
                  - Lorem ipsum dolor sit amet, consectetur adipiscing elit?  \\s
                  - Lorem ipsum dolor sit amet, consectetur adipiscing elit?
                  - Lorem ipsum dolor sit amet, consectetur adipiscing elit?
                  \""";
            }
            """)
        .doTest(TEXT_MATCH);
  }
}
