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
    compilationHelper
        .addSourceLines(
            "UnnecessaryLongToIntConversionPositiveCases.java",
"""
package com.google.errorprone.bugpatterns.testdata;

import com.google.common.primitives.Ints;

/** Positive cases for {@link com.google.errorprone.bugpatterns.UnnecessaryLongToIntConversion}. */
public class UnnecessaryLongToIntConversionPositiveCases {


  static void acceptsLong(long value) {}

  static void acceptsMultipleParams(int intValue, long longValue) {}

  public void longToIntForLongParam() {
    long x = 1;
    // BUG: Diagnostic contains: UnnecessaryLongToIntConversion
    acceptsLong((int) x);
  }

  public void longObjectToIntForLongParam() {
    Long x = Long.valueOf(1);
    // BUG: Diagnostic contains: UnnecessaryLongToIntConversion
    acceptsLong(x.intValue());
  }

  public void convertMultipleArgs() {
    long x = 1;
    // The method expects an int for the first parameter and a long for the second parameter.
    // BUG: Diagnostic contains: UnnecessaryLongToIntConversion
    acceptsMultipleParams(Ints.checkedCast(x), Ints.checkedCast(x));
  }

  // The following test cases test various conversion methods, including an unchecked cast.
  public void castToInt() {
    long x = 1;
    // BUG: Diagnostic contains: UnnecessaryLongToIntConversion
    acceptsLong((int) x);
  }

  public void checkedCast() {
    long x = 1;
    // BUG: Diagnostic contains: UnnecessaryLongToIntConversion
    acceptsLong(Ints.checkedCast(x));
  }

  public void saturatedCast() {
    long x = 1;
    // BUG: Diagnostic contains: UnnecessaryLongToIntConversion
    acceptsLong(Ints.saturatedCast(x));
  }

  public void toIntExact() {
    long x = 1;
    // BUG: Diagnostic contains: UnnecessaryLongToIntConversion
    acceptsLong(Math.toIntExact(x));
  }

  public void toIntExactWithLongObject() {
    Long x = Long.valueOf(1);
    // BUG: Diagnostic contains: UnnecessaryLongToIntConversion
    acceptsLong(Math.toIntExact(x));
  }

  public void intValue() {
    Long x = Long.valueOf(1);
    // BUG: Diagnostic contains: UnnecessaryLongToIntConversion
    acceptsLong(x.intValue());
  }
}\
""")
        .doTest();
  }

  @Test
  public void longParameterLongToIntNegativeCases() {
    compilationHelper
        .addSourceLines(
            "UnnecessaryLongToIntConversionNegativeCases.java",
"""
package com.google.errorprone.bugpatterns.testdata;

import com.google.common.primitives.Ints;

/** Negative cases for {@link com.google.errorprone.bugpatterns.UnnecessaryLongToIntConversion}. */
public class UnnecessaryLongToIntConversionNegativeCases {

  static void acceptsLong(long value) {}

  static void acceptsInt(int value) {}

  static void acceptsMultipleParams(int intValue, long longValue) {}

  // Converting from a long or Long to an Integer type requires first converting to an int. This is
  // out of scope.
  public void longToIntegerForLongParam() {
    long x = 1;
    acceptsLong(Integer.valueOf((int) x));
  }

  public void longObjectToIntegerForLongParam() {
    Long x = Long.valueOf(1);
    acceptsLong(Integer.valueOf(x.intValue()));
  }

  public void longParameterAndLongArgument() {
    long x = 1;
    acceptsLong(x);
  }

  public void longParameterAndIntArgument() {
    int i = 1;
    acceptsLong(i);
  }

  public void longParameterAndIntegerArgument() {
    Integer i = Integer.valueOf(1);
    acceptsLong(i);
  }

  public void castIntToLong() {
    int i = 1;
    acceptsLong((long) i);
  }

  public void castLongToIntForIntParameter() {
    long x = 1;
    acceptsInt((int) x);
  }

  public void longValueOfLongObject() {
    Long x = Long.valueOf(1);
    acceptsLong(x.longValue());
  }

  public void longValueOfInteger() {
    Integer i = Integer.valueOf(1);
    acceptsLong(i.longValue());
  }

  public void intValueOfInteger() {
    Integer i = Integer.valueOf(1);
    acceptsLong(i.intValue());
  }

  public void intValueForIntParameter() {
    Long x = Long.valueOf(1);
    acceptsInt(x.intValue());
  }

  public void checkedCastOnInt() {
    int i = 1;
    acceptsLong(Ints.checkedCast(i));
  }

  public void checkedCastOnInteger() {
    Integer i = Integer.valueOf(1);
    acceptsLong(Ints.checkedCast(i));
  }

  public void checkedCastForIntParameter() {
    long x = 1;
    acceptsInt(Ints.checkedCast(x));
  }

  public void checkedCastMultipleArgs() {
    long x = 1;
    // The method expects an int for the first parameter and a long for the second parameter.
    acceptsMultipleParams(Ints.checkedCast(x), x);
  }

  public void toIntExactOnInt() {
    int i = 1;
    acceptsLong(Math.toIntExact(i));
  }

  public void toIntExactOnInteger() {
    Integer i = Integer.valueOf(1);
    acceptsLong(Math.toIntExact(i));
  }

  public void toIntExactForIntParameter() {
    long x = 1;
    acceptsInt(Math.toIntExact(x));
  }
}\
""")
        .doTest();
  }

  // Test the suggested fixes, first removing the conversion and second replacing it with a call to
  // {@code Longs.constrainToRange()} instead.
  @Test
  public void suggestRemovingTypeCast() {
    refactoringHelper
        .addInputLines(
            "in/A.java",
            """
            public class A {
              void acceptsLong(long value) {}

              void foo() {
                long x = 1L;
                acceptsLong((int) x);
              }
            }
            """)
        .addOutputLines(
            "out/A.java",
            """
            public class A {
              void acceptsLong(long value) {}

              void foo() {
                long x = 1L;
                acceptsLong(x);
              }
            }
            """)
        .setFixChooser(FIRST)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void suggestRemovingTypeCastWithoutSpacing() {
    refactoringHelper
        .addInputLines(
            "in/A.java",
            """
            public class A {
              void acceptsLong(long value) {}

              void foo() {
                long x = 1L;
                acceptsLong((int) x);
              }
            }
            """)
        .addOutputLines(
            "out/A.java",
            """
            public class A {
              void acceptsLong(long value) {}

              void foo() {
                long x = 1L;
                acceptsLong(x);
              }
            }
            """)
        .setFixChooser(FIRST)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void suggestReplacingTypeCastWithConstrainToRange() {
    refactoringHelper
        .addInputLines(
            "in/A.java",
            """
            public class A {
              void acceptsLong(long value) {}

              void foo() {
                long x = 1L;
                acceptsLong((int) x);
              }
            }
            """)
        .addOutputLines(
            "out/A.java",
            """
            import com.google.common.primitives.Longs;

            public class A {
              void acceptsLong(long value) {}

              void foo() {
                long x = 1L;
                acceptsLong(Longs.constrainToRange(x, Integer.MIN_VALUE, Integer.MAX_VALUE));
              }
            }
            """)
        .setFixChooser(SECOND)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void suggestReplacingTypeCastWithoutSpacingWithConstrainToRange() {
    refactoringHelper
        .addInputLines(
            "in/A.java",
            """
            public class A {
              void acceptsLong(long value) {}

              void foo() {
                long x = 1L;
                acceptsLong((int) x);
              }
            }
            """)
        .addOutputLines(
            "out/A.java",
            """
            import com.google.common.primitives.Longs;

            public class A {
              void acceptsLong(long value) {}

              void foo() {
                long x = 1L;
                acceptsLong(Longs.constrainToRange(x, Integer.MIN_VALUE, Integer.MAX_VALUE));
              }
            }
            """)
        .setFixChooser(SECOND)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void suggestRemovingStaticMethod() {
    refactoringHelper
        .addInputLines(
            "in/A.java",
            """
            import com.google.common.primitives.Ints;

            public class A {
              void acceptsLong(long value) {}

              void foo() {
                long x = 1L;
                acceptsLong(Ints.checkedCast(x));
              }
            }
            """)
        .addOutputLines(
            "out/A.java",
            """
            import com.google.common.primitives.Ints;

            public class A {
              void acceptsLong(long value) {}

              void foo() {
                long x = 1L;
                acceptsLong(x);
              }
            }
            """)
        .setFixChooser(FIRST)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void suggestRemovingStaticMethodWithBoxedLongArgument() {
    refactoringHelper
        .addInputLines(
            "in/A.java",
            """
            import com.google.common.primitives.Ints;

            public class A {
              void acceptsLong(long value) {}

              void foo() {
                Long x = Long.valueOf(1);
                acceptsLong(Ints.checkedCast(x));
              }
            }
            """)
        .addOutputLines(
            "out/A.java",
            """
            import com.google.common.primitives.Ints;

            public class A {
              void acceptsLong(long value) {}

              void foo() {
                Long x = Long.valueOf(1);
                acceptsLong(x);
              }
            }
            """)
        .setFixChooser(FIRST)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void suggestReplacingStaticMethodWithConstrainToRange() {
    refactoringHelper
        .addInputLines(
            "in/A.java",
            """
            import java.lang.Math;

            public class A {
              void acceptsLong(long value) {}

              void foo() {
                long x = 1L;
                acceptsLong(Math.toIntExact(x));
              }
            }
            """)
        .addOutputLines(
            "out/A.java",
            """
            import com.google.common.primitives.Longs;
            import java.lang.Math;

            public class A {
              void acceptsLong(long value) {}

              void foo() {
                long x = 1L;
                acceptsLong(Longs.constrainToRange(x, Integer.MIN_VALUE, Integer.MAX_VALUE));
              }
            }
            """)
        .setFixChooser(SECOND)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void suggestRemovingInstanceMethod() {
    refactoringHelper
        .addInputLines(
            "in/A.java",
            """
            public class A {
              void acceptsLong(long value) {}

              void foo() {
                Long x = Long.valueOf(1);
                acceptsLong(x.intValue());
              }
            }
            """)
        .addOutputLines(
            "out/A.java",
            """
            public class A {
              void acceptsLong(long value) {}

              void foo() {
                Long x = Long.valueOf(1);
                acceptsLong(x);
              }
            }
            """)
        .setFixChooser(FIRST)
        .doTest(TEXT_MATCH);
  }

  @Test
  public void suggestReplacingInstanceMethodWithConstrainToRange() {
    refactoringHelper
        .addInputLines(
            "in/A.java",
            """
            public class A {
              void acceptsLong(long value) {}

              void foo() {
                Long x = Long.valueOf(1);
                acceptsLong(x.intValue());
              }
            }
            """)
        .addOutputLines(
            "out/A.java",
            """
            import com.google.common.primitives.Longs;

            public class A {
              void acceptsLong(long value) {}

              void foo() {
                Long x = Long.valueOf(1);
                acceptsLong(Longs.constrainToRange(x, Integer.MIN_VALUE, Integer.MAX_VALUE));
              }
            }
            """)
        .setFixChooser(SECOND)
        .doTest(TEXT_MATCH);
  }
}
