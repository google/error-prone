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

/** Tests for {@link UnnecessarySubstring}. */
@RunWith(JUnit4.class)
public class UnnecessarySubstringTest {
  private final BugCheckerRefactoringTestHelper refactoring =
      BugCheckerRefactoringTestHelper.newInstance(UnnecessarySubstring.class, getClass());
  private final CompilationTestHelper compilation =
      CompilationTestHelper.newInstance(UnnecessarySubstring.class, getClass());

  @Test
  public void twoArgSubstring() {
    refactoring
        .addInputLines(
            "Test.java",
            """
            class Test {
              int f(String s, int begin, int end) {
                return Integer.parseInt(s.substring(begin, end));
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              int f(String s, int begin, int end) {
                return Integer.parseInt(s, begin, end, 10);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void oneArgSubstring() {
    refactoring
        .addInputLines(
            "Test.java",
            """
            class Test {
              int f(String s) {
                return Integer.parseInt(s.substring(1));
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              int f(String s) {
                return Integer.parseInt(s, 1, s.length(), 10);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void explicitRadixIsPreserved() {
    refactoring
        .addInputLines(
            "Test.java",
            """
            class Test {
              int f(String s, int radix) {
                return Integer.parseInt(s.substring(2, 4), radix);
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              int f(String s, int radix) {
                return Integer.parseInt(s, 2, 4, radix);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void allParseFlavours() {
    refactoring
        .addInputLines(
            "Test.java",
            """
            class Test {
              void f(String s) {
                Integer.parseInt(s.substring(1, 2));
                Integer.parseUnsignedInt(s.substring(1, 2));
                Long.parseLong(s.substring(1, 2));
                Long.parseUnsignedLong(s.substring(1, 2));
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              void f(String s) {
                Integer.parseInt(s, 1, 2, 10);
                Integer.parseUnsignedInt(s, 1, 2, 10);
                Long.parseLong(s, 1, 2, 10);
                Long.parseUnsignedLong(s, 1, 2, 10);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void staticImportIsPreserved() {
    refactoring
        .addInputLines(
            "Test.java",
            """
            import static java.lang.Integer.parseInt;

            class Test {
              int f(String s) {
                return parseInt(s.substring(1, 3));
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import static java.lang.Integer.parseInt;

            class Test {
              int f(String s) {
                return parseInt(s, 1, 3, 10);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void charSequenceReceivers() {
    refactoring
        .addInputLines(
            "Test.java",
            """
            class Test {
              void f(StringBuilder sb, StringBuffer buf) {
                Integer.parseInt(sb.substring(1, 2));
                Integer.parseInt(buf.substring(1, 2));
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              void f(StringBuilder sb, StringBuffer buf) {
                Integer.parseInt(sb, 1, 2, 10);
                Integer.parseInt(buf, 1, 2, 10);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void compoundBoundExpressions() {
    refactoring
        .addInputLines(
            "Test.java",
            """
            class Test {
              int f(String s, int i) {
                return Integer.parseInt(s.substring(i + 1, s.indexOf(',')), i > 0 ? 16 : 10);
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              int f(String s, int i) {
                return Integer.parseInt(s, i + 1, s.indexOf(','), i > 0 ? 16 : 10);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void oneArgSubstringOnFieldOrArray() {
    refactoring
        .addInputLines(
            "Test.java",
            """
            class Test {
              String field = "";

              void f(String[] parts, int i) {
                Integer.parseInt(this.field.substring(1));
                Integer.parseInt(parts[i].substring(1));
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              String field = "";

              void f(String[] parts, int i) {
                Integer.parseInt(this.field, 1, this.field.length(), 10);
                Integer.parseInt(parts[i], 1, parts[i].length(), 10);
              }
            }
            """)
        .doTest();
  }

  /** Repeating the target would call {@code g()} twice, so the one-arg form is left alone. */
  @Test
  public void negativeOneArgSubstringOnImpureTarget() {
    compilation
        .addSourceLines(
            "Test.java",
            """
            class Test {
              String g() {
                return "";
              }

              int f() {
                return Integer.parseInt(g().substring(1));
              }
            }
            """)
        .doTest();
  }

  /** Two-arg substring never repeats the target, so an impure target is still fine. */
  @Test
  public void twoArgSubstringOnImpureTarget() {
    refactoring
        .addInputLines(
            "Test.java",
            """
            class Test {
              String g() {
                return "";
              }

              int f() {
                return Integer.parseInt(g().substring(1, 3));
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              String g() {
                return "";
              }

              int f() {
                return Integer.parseInt(g(), 1, 3, 10);
              }
            }
            """)
        .doTest();
  }

  /** Appending {@code .length()} to a ternary would bind to the wrong operand. */
  @Test
  public void negativeOneArgSubstringOnTernary() {
    compilation
        .addSourceLines(
            "Test.java",
            """
            class Test {
              int f(boolean b, String x, String y) {
                return Integer.parseInt((b ? x : y).substring(1));
              }
            }
            """)
        .doTest();
  }

  /** These have no {@code (CharSequence, int, int, int)} overload. */
  @Test
  public void negativeParsesWithoutRegionOverload() {
    compilation
        .addSourceLines(
            "Test.java",
            """
            class Test {
              void f(String s) {
                Double.parseDouble(s.substring(1, 3));
                Float.parseFloat(s.substring(1, 3));
                Boolean.parseBoolean(s.substring(1, 3));
                Integer.valueOf(s.substring(1, 3));
                Integer.decode(s.substring(1, 3));
                Short.parseShort(s.substring(1, 3));
                Byte.parseByte(s.substring(1, 3));
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negativeAlreadyUsesRegionOverload() {
    compilation
        .addSourceLines(
            "Test.java",
            """
            class Test {
              int f(String s) {
                return Integer.parseInt(s, 1, 3, 10);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negativeArgumentIsNotASubstring() {
    compilation
        .addSourceLines(
            "Test.java",
            """
            class Test {
              int f(String s) {
                return Integer.parseInt(s.trim());
              }
            }
            """)
        .doTest();
  }

  /** {@code substring} on an unrelated type returns something the overload cannot consume. */
  @Test
  public void negativeUnrelatedSubstringMethod() {
    compilation
        .addSourceLines(
            "Test.java",
            """
            class Test {
              static class Rope {
                String substring(int begin, int end) {
                  return "";
                }
              }

              int f(Rope rope) {
                return Integer.parseInt(rope.substring(1, 3));
              }
            }
            """)
        .doTest();
  }
}
