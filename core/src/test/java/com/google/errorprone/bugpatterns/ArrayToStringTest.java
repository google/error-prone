/*
 * Copyright 2012 The Error Prone Authors.
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

/** {@link ArrayToString}Test */
@RunWith(JUnit4.class)
public class ArrayToStringTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(ArrayToString.class, getClass());

  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(ArrayToString.class, getClass());

  @Test
  public void positiveCase() {
    compilationHelper
        .addSourceLines(
            "ArrayToStringPositiveCases.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            import java.util.*;

            /**
             * @author adgar@google.com (Mike Edgar)
             */
            public class ArrayToStringPositiveCases {

              public void intArray() {
                int[] a = {1, 2, 3};

                // BUG: Diagnostic contains: Arrays.toString(a)
                if (a.toString().isEmpty()) {
                  System.out.println("int array string is empty!");
                } else {
                  System.out.println("int array string is nonempty!");
                }
              }

              public void objectArray() {
                Object[] a = new Object[3];

                // BUG: Diagnostic contains: Arrays.toString(a)
                if (a.toString().isEmpty()) {
                  System.out.println("object array string is empty!");
                } else {
                  System.out.println("object array string is nonempty!");
                }
              }

              public void firstMethodCall() {
                String s = "hello";

                // BUG: Diagnostic contains: Arrays.toString(s.toCharArray())
                if (s.toCharArray().toString().isEmpty()) {
                  System.out.println("char array string is empty!");
                } else {
                  System.out.println("char array string is nonempty!");
                }
              }

              public void secondMethodCall() {
                char[] a = new char[3];

                // BUG: Diagnostic contains: Arrays.toString(a)
                if (a.toString().isEmpty()) {
                  System.out.println("array string is empty!");
                } else {
                  System.out.println("array string is nonempty!");
                }
              }

              public void throwable() {
                Exception e = new RuntimeException();
                // BUG: Diagnostic contains: Throwables.getStackTraceAsString(e)
                System.out.println(e.getStackTrace().toString());
              }

              public void arrayOfArrays() {
                int[][] a = {};
                // BUG: Diagnostic contains: Arrays.deepToString(a)
                System.out.println(a);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negativeCase() {
    compilationHelper
        .addSourceLines(
            "ArrayToStringNegativeCases.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            import java.util.*;

            /**
             * @author adgar@google.com (Mike Edgar)
             */
            public class ArrayToStringNegativeCases {
              public void objectEquals() {
                Object a = new Object();

                if (a.toString().isEmpty()) {
                  System.out.println("string is empty!");
                } else {
                  System.out.println("string is not empty!");
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void stringConcat() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              void f(int[] xs) {
                // BUG: Diagnostic contains: ("" + Arrays.toString(xs));
                System.err.println("" + xs);
                String s = "";
                // BUG: Diagnostic contains: s += Arrays.toString(xs);
                s += xs;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void printString() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              int[] g() {
                return null;
              }

              void f(int[] xs) {
                System.err.println(xs);
                System.err.println(String.valueOf(xs));
                System.err.println(String.valueOf(g()));
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            import java.util.Arrays;

            class Test {
              int[] g() {
                return null;
              }

              void f(int[] xs) {
                System.err.println(Arrays.toString(xs));
                System.err.println(Arrays.toString(xs));
                System.err.println(Arrays.toString(g()));
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negativePrintString() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              void f(char[] xs) {
                System.err.println(String.valueOf(xs));
              }
            }
            """)
        .doTest();
  }

  @Test
  public void stringBuilder() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              void f(int[] xs) {
                // BUG: Diagnostic contains: append(Arrays.toString(xs))
                new StringBuilder().append(xs);
              }
            }
            """)
        .doTest();
  }

  /**
   * Don't complain about {@code @FormatMethod}s; there's a chance they're handling arrays
   * correctly.
   */
  @Test
  public void customFormatMethod() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import com.google.errorprone.annotations.FormatMethod;

            class Test {
              private void test(Object[] arr) {
                format("%s %s", arr, 2);
              }

              @FormatMethod
              String format(String format, Object... args) {
                return String.format(format, args);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void methodReturningArray() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              private void test() {
                // BUG: Diagnostic contains:
                String.format("%s %s", arr(), 1);
              }

              Object[] arr() {
                return null;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void throwableToString() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              void test(Exception e) {
                // BUG: Diagnostic contains: Throwables.getStackTraceAsString(e)
                String.format("%s, %s", 1, e.getStackTrace());
              }
            }
            """)
        .doTest();
  }

  @Test
  public void positiveCompoundAssignment() {
    compilationHelper
        .addSourceLines(
            "ArrayToStringCompoundAssignmentPositiveCases.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            import java.util.*;

            /**
             * @author adgar@google.com (Mike Edgar)
             */
            public class ArrayToStringCompoundAssignmentPositiveCases {

              private static final int[] a = {1, 2, 3};

              public void stringVariableAddsArrayAndAssigns() {
                String b = "a string";
                // BUG: Diagnostic contains: += Arrays.toString(a)
                b += a;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negativeCompoundAssignment() {
    compilationHelper
        .addSourceLines(
            "ArrayToStringCompoundAssignmentNegativeCases.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            /**
             * @author adgar@google.com (Mike Edgar)
             */
            public class ArrayToStringCompoundAssignmentNegativeCases {
              public void concatenateCompoundAssign_object() {
                Object a = new Object();
                String b = " a string";
                b += a;
              }

              public void concatenateCompoundAssign_int() {
                int a = 5;
                String b = " a string ";
                b += a;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void positiveConcat() {
    compilationHelper
        .addSourceLines(
            "ArrayToStringConcatenationPositiveCases.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            import java.util.*;

            /**
             * @author adgar@google.com (Mike Edgar)
             */
            public class ArrayToStringConcatenationPositiveCases {

              private static final int[] a = {1, 2, 3};

              public void stringLiteralLeftOperandIsArray() {
                // BUG: Diagnostic contains: Arrays.toString(a) +
                String b = a + " a string";
              }

              public void stringLiteralRightOperandIsArray() {
                // BUG: Diagnostic contains: + Arrays.toString(a)
                String b = "a string" + a;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negativeConcat() {
    compilationHelper
        .addSourceLines(
            "ArrayToStringConcatenationNegativeCases.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            /**
             * @author adgar@google.com (Mike Edgar)
             */
            public class ArrayToStringConcatenationNegativeCases {
              public void notArray() {
                Object a = new Object();
                String b = a + " a string";
              }

              public void notArray_refactored() {
                Object a = new Object();
                String b = " a string";
                String c = a + b;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void arrayPassedToJoiner() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import com.google.common.base.Joiner;

            class Test {
              String test(Joiner j, Object[] a) {
                return j.join(a);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void arrayPassedToJoiner_firstSecondRest_negative() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import com.google.common.base.Joiner;

            class Test {
              String test(Joiner j, Object first, Object second, Object[] rest) {
                return j.join(first, second, rest);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void arrayPassedToJoiner_firstSecondRest_positive() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            import com.google.common.base.Joiner;

            class Test {
              String test(Joiner j, Object first, Object second, Object third, Object[] rest) {
                // BUG: Diagnostic contains:
                return j.join(first, second, third, rest);
              }
            }
            """)
        .doTest();
  }
}
