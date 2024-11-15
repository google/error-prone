/*
 * Copyright 2013 The Error Prone Authors.
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

/**
 * @author cushon@google.com (Liam Miller-Cushon)
 */
@RunWith(JUnit4.class)
public class FinallyTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(Finally.class, getClass());

  @Test
  public void positiveCase1() {
    compilationHelper
        .addSourceLines(
            "FinallyPositiveCase1.java",
            """
package com.google.errorprone.bugpatterns.testdata;

/**
 * When a finally statement is exited because of a return, throw, break, or continue statement,
 * unintuitive behaviour can occur. Consider:
 *
 * <pre>
 * {@code
 * finally foo() {
 *   try {
 *     return true;
 *   } finally {
 *     return false;
 *   }
 * }
 * </pre>
 *
 * Because the finally block always executes, the first return statement has no effect and the
 * method will return false.
 *
 * @author eaftan@google.com (Eddie Aftandilian)
 * @author cushon@google.com (Liam Miller-Cushon)
 */
public class FinallyPositiveCase1 {

  public static void test1() {
    while (true) {
      try {
      } finally {
        // BUG: Diagnostic contains:
        break;
      }
    }
  }

  public static void test2() {
    while (true) {
      try {
      } finally {
        // BUG: Diagnostic contains:
        continue;
      }
    }
  }

  public static void test3() {
    try {
    } finally {
      // BUG: Diagnostic contains:
      return;
    }
  }

  public static void test4() throws Exception {
    try {
    } finally {
      // BUG: Diagnostic contains:
      throw new Exception();
    }
  }

  /**
   * break statement jumps to outer labeled while, not inner one.
   */
  public void test5() {
  label:
    while (true) {
      try {
      } finally {
        while (true) {
          // BUG: Diagnostic contains:
          break label;
        }
      }
    }
  }

  /**
   * continue statement jumps to outer labeled for, not inner one.
   */
  public void test6() {
  label:
    for (;;) {
      try {
      } finally {
        for (;;) {
          // BUG: Diagnostic contains:
          continue label;
        }
      }
    }
  }

  /**
   * continue statement jumps to while, not switch.
   */
  public void test7() {
    int i = 10;
    while (true) {
      try {
      } finally {
        switch (i) {
          case 10:
            // BUG: Diagnostic contains:
            continue;
        }
      }
    }
  }

  public void test8() {
    try {
    } finally {
    // BUG: Diagnostic contains:
      { { { { { { { { { { return; } } } } } } } } } }
    }
  }

  // Don't assume that completion statements occur inside methods:
  static boolean flag = false;
  static {
    while (flag) {
      try {
      } finally {
        // BUG: Diagnostic contains:
        break;
      }
    }
  }
}\
""")
        .doTest();
  }

  @Test
  public void positiveCase2() {
    compilationHelper
        .addSourceLines(
            "FinallyPositiveCase2.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            import java.io.IOException;

            /**
             * @author cushon@google.com (Liam Miller-Cushon)
             */
            public class FinallyPositiveCase2 {
              public void completeWithReturn(boolean flag) {
                try {

                } finally {
                  // BUG: Diagnostic contains:
                  return;
                }
              }

              public void completeWithThrow(boolean flag) throws Exception {
                try {

                } finally {
                  // BUG: Diagnostic contains:
                  throw new Exception();
                }
              }

              public void unreachableThrow(boolean flag) throws Exception {
                try {

                } finally {
                  if (flag) {
                    // BUG: Diagnostic contains:
                    throw new Exception();
                  }
                }
              }

              public void nestedBlocks(int i, boolean flag) throws Exception {
                try {

                } finally {
                  switch (i) {
                    default:
                      {
                        while (flag) {
                          do {
                            if (flag) {
                            } else {
                              // BUG: Diagnostic contains:
                              throw new Exception();
                            }
                          } while (flag);
                        }
                      }
                  }
                }
              }

              public void nestedFinally() throws Exception {
                try {

                } finally {
                  try {
                  } finally {
                    // BUG: Diagnostic contains:
                    throw new IOException();
                  }
                }
              }

              public void returnFromTryNestedInFinally() {
                try {
                } finally {
                  try {
                    // BUG: Diagnostic contains:
                    return;
                  } finally {
                  }
                }
              }

              public void returnFromCatchNestedInFinally() {
                try {
                } finally {
                  try {
                  } catch (Exception e) {
                    // BUG: Diagnostic contains:
                    return;
                  } finally {
                  }
                }
              }

              public void throwUncaughtFromNestedTryInFinally() throws Exception {
                try {
                } finally {
                  try {
                    // BUG: Diagnostic contains:
                    throw new Exception();
                  } finally {
                  }
                }
              }

              public void throwFromNestedCatchInFinally() throws Exception {
                try {
                } finally {
                  try {
                  } catch (Exception e) {
                    // BUG: Diagnostic contains:
                    throw new Exception();
                  } finally {
                  }
                }
              }
            }\
            """)
        .doTest();
  }

  @Test
  public void negativeCase1() {
    compilationHelper
        .addSourceLines(
            "FinallyNegativeCase1.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            /**
             * @author eaftan@google.com (Eddie Aftandilian)
             */
            public class FinallyNegativeCase1 {

              public static void test1() {
                while (true) {
                  try {
                    break;
                  } finally {
                  }
                }
              }

              public static void test2() {
                while (true) {
                  try {
                    continue;
                  } finally {
                  }
                }
              }

              public static void test3() {
                try {
                  return;
                } finally {
                }
              }

              public static void test4() throws Exception {
                try {
                  throw new Exception();
                } catch (Exception e) {
                } finally {
                }
              }

              /** break inner loop. */
              public void test5() {
                label:
                while (true) {
                  try {
                  } finally {
                    while (true) {
                      break;
                    }
                  }
                }
              }

              /** continue statement jumps out of inner for. */
              public void test6() {
                label:
                for (; ; ) {
                  try {
                  } finally {
                    for (; ; ) {
                      continue;
                    }
                  }
                }
              }

              /** break statement jumps out of switch. */
              public void test7() {
                int i = 10;
                while (true) {
                  try {
                  } finally {
                    switch (i) {
                      case 10:
                        break;
                    }
                  }
                }
              }
            }\
            """)
        .doTest();
  }

  @Test
  public void negativeCase2() {
    compilationHelper
        .addSourceLines(
            "FinallyNegativeCase2.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            import java.io.IOException;

            /**
             * @author cushon@google.com (Liam Miller-Cushon)
             */
            public class FinallyNegativeCase2 {
              public void test1(boolean flag) {
                try {
                  return;
                } finally {
                }
              }

              public void test2() throws Exception {
                try {
                } catch (Exception e) {
                  throw new Exception();
                } finally {
                }
              }

              public void returnInAnonymousClass(boolean flag) {
                try {
                } finally {
                  new Object() {
                    void foo() {
                      return;
                    }
                  };
                }
              }

              public void throwFromNestedTryInFinally() throws Exception {
                try {
                } finally {
                  try {
                    throw new Exception();
                  } catch (Exception e) {
                  } finally {
                  }
                }
              }

              public void nestedTryInFinally2() throws Exception {
                try {
                } finally {
                  try {
                    // This exception will propagate out through the enclosing finally,
                    // but we don't do exception analysis and have no way of knowing that.
                    // Xlint:finally doesn't handle this either, since it only reports
                    // situations where the end of a finally block is unreachable as
                    // defined by JLS 14.21.
                    throw new IOException();
                  } catch (Exception e) {
                  }
                }
              }
            }\
            """)
        .doTest();
  }

  @Test
  public void lambda() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            class Test {
              void f() {
                try {
                } catch (Throwable t) {
                } finally {
                  Runnable r =
                      () -> {
                        return;
                      };
                }
              }
            }
            """)
        .doTest();
  }
}
