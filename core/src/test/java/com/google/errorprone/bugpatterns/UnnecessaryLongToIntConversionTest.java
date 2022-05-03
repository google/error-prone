/*
 * Copyright 2022 The Error Prone Authors.
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

import static com.google.errorprone.BugCheckerRefactoringTestHelper.FixChoosers.FIRST;
import static com.google.errorprone.BugCheckerRefactoringTestHelper.FixChoosers.SECOND;
import static com.google.errorprone.BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Test for {@link com.google.errorprone.bugpatterns.UnnecessaryLongToIntConversion}. */
@RunWith(JUnit4.class)
public class UnnecessaryLongToIntConversionTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(UnnecessaryLongToIntConversion.class, getClass());

  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(UnnecessaryLongToIntConversion.class, getClass());

  @Test
  public void longParameterLongToIntPositiveCases() {
    compilationHelper.addSourceFile("UnnecessaryLongToIntConversionPositiveCases.java").doTest();
  }

  @Test
  public void longParameterLongToIntNegativeCases() {
    compilationHelper.addSourceFile("UnnecessaryLongToIntConversionNegativeCases.java").doTest();
  }

  // Test the suggested fixes, first removing the conversion and second replacing it with a call to
  // {@code Longs.constrainToRange()} instead.
  @Test
  public void suggestRemovingTypeCast() {
    refactoringHelper
        .addInputLines(
            "in/A.java",
            "public class A {",
            "  void acceptsLong(long value) {}",
            "  void foo() {",
            "    long x = 1L;",
            "    acceptsLong((int) x);",
            "  }",
            "}")
        .addOutputLines(
            "out/A.java",
            "public class A {",
            "  void acceptsLong(long value) {}",
            "  void foo() {",
            "    long x = 1L;",
            "    acceptsLong(x);",
            "  }",
            "}")
        .setFixChooser(FIRST)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void suggestRemovingTypeCastWithoutSpacing() {
    refactoringHelper
        .addInputLines(
            "in/A.java",
            "public class A {",
            "  void acceptsLong(long value) {}",
            "  void foo() {",
            "    long x = 1L;",
            "    acceptsLong((int)x);",
            "  }",
            "}")
        .addOutputLines(
            "out/A.java",
            "public class A {",
            "  void acceptsLong(long value) {}",
            "  void foo() {",
            "    long x = 1L;",
            "    acceptsLong(x);",
            "  }",
            "}")
        .setFixChooser(FIRST)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void suggestReplacingTypeCastWithConstrainToRange() {
    refactoringHelper
        .addInputLines(
            "in/A.java",
            "public class A {",
            "  void acceptsLong(long value) {}",
            "  void foo() {",
            "    long x = 1L;",
            "    acceptsLong((int) x);",
            "  }",
            "}")
        .addOutputLines(
            "out/A.java",
            "import com.google.common.primitives.Longs;",
            "public class A {",
            "  void acceptsLong(long value) {}",
            "  void foo() {",
            "    long x = 1L;",
            "    acceptsLong(Longs.constrainToRange(x, Integer.MIN_VALUE, Integer.MAX_VALUE));",
            "  }",
            "}")
        .setFixChooser(SECOND)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void suggestReplacingTypeCastWithoutSpacingWithConstrainToRange() {
    refactoringHelper
        .addInputLines(
            "in/A.java",
            "public class A {",
            "  void acceptsLong(long value) {}",
            "  void foo() {",
            "    long x = 1L;",
            "    acceptsLong((int)x);",
            "  }",
            "}")
        .addOutputLines(
            "out/A.java",
            "import com.google.common.primitives.Longs;",
            "public class A {",
            "  void acceptsLong(long value) {}",
            "  void foo() {",
            "    long x = 1L;",
            "    acceptsLong(Longs.constrainToRange(x, Integer.MIN_VALUE, Integer.MAX_VALUE));",
            "  }",
            "}")
        .setFixChooser(SECOND)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void suggestRemovingStaticMethod() {
    refactoringHelper
        .addInputLines(
            "in/A.java",
            "import com.google.common.primitives.Ints;",
            "public class A {",
            "  void acceptsLong(long value) {}",
            "  void foo() {",
            "    long x = 1L;",
            "    acceptsLong(Ints.checkedCast(x));",
            "  }",
            "}")
        .addOutputLines(
            "out/A.java",
            "import com.google.common.primitives.Ints;",
            "public class A {",
            "  void acceptsLong(long value) {}",
            "  void foo() {",
            "    long x = 1L;",
            "    acceptsLong(x);",
            "  }",
            "}")
        .setFixChooser(FIRST)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void suggestRemovingStaticMethodWithBoxedLongArgument() {
    refactoringHelper
        .addInputLines(
            "in/A.java",
            "import com.google.common.primitives.Ints;",
            "public class A {",
            "  void acceptsLong(long value) {}",
            "  void foo() {",
            "    Long x = Long.valueOf(1);",
            "    acceptsLong(Ints.checkedCast(x));",
            "  }",
            "}")
        .addOutputLines(
            "out/A.java",
            "import com.google.common.primitives.Ints;",
            "public class A {",
            "  void acceptsLong(long value) {}",
            "  void foo() {",
            "    Long x = Long.valueOf(1);",
            "    acceptsLong(x);",
            "  }",
            "}")
        .setFixChooser(FIRST)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void suggestReplacingStaticMethodWithConstrainToRange() {
    refactoringHelper
        .addInputLines(
            "in/A.java",
            "import java.lang.Math;",
            "public class A {",
            "  void acceptsLong(long value) {}",
            "  void foo() {",
            "    long x = 1L;",
            "    acceptsLong(Math.toIntExact(x));",
            "  }",
            "}")
        .addOutputLines(
            "out/A.java",
            "import com.google.common.primitives.Longs;",
            "import java.lang.Math;",
            "public class A {",
            "  void acceptsLong(long value) {}",
            "  void foo() {",
            "    long x = 1L;",
            "    acceptsLong(Longs.constrainToRange(x, Integer.MIN_VALUE, Integer.MAX_VALUE));",
            "  }",
            "}")
        .setFixChooser(SECOND)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void suggestRemovingInstanceMethod() {
    refactoringHelper
        .addInputLines(
            "in/A.java",
            "public class A {",
            "  void acceptsLong(long value) {}",
            "  void foo() {",
            "    Long x = Long.valueOf(1);",
            "    acceptsLong(x.intValue());",
            "  }",
            "}")
        .addOutputLines(
            "out/A.java",
            "public class A {",
            "  void acceptsLong(long value) {}",
            "  void foo() {",
            "    Long x = Long.valueOf(1);",
            "    acceptsLong(x);",
            "  }",
            "}")
        .setFixChooser(FIRST)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void suggestReplacingInstanceMethodWithConstrainToRange() {
    refactoringHelper
        .addInputLines(
            "in/A.java",
            "public class A {",
            "  void acceptsLong(long value) {}",
            "  void foo() {",
            "    Long x = Long.valueOf(1);",
            "    acceptsLong(x.intValue());",
            "  }",
            "}")
        .addOutputLines(
            "out/A.java",
            "import com.google.common.primitives.Longs;",
            "public class A {",
            "  void acceptsLong(long value) {}",
            "  void foo() {",
            "    Long x = Long.valueOf(1);",
            "    acceptsLong(Longs.constrainToRange(x, Integer.MIN_VALUE, Integer.MAX_VALUE));",
            "  }",
            "}")
        .setFixChooser(SECOND)
        .doTest(TEXT_MATCH);
  }
}
