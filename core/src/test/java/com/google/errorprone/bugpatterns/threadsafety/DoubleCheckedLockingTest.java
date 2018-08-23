/*
 * Copyright 2015 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.threadsafety;

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link DoubleCheckedLocking}Test */
@RunWith(JUnit4.class)
public class DoubleCheckedLockingTest {
  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(DoubleCheckedLocking.class, getClass());

  @Test
  public void positive() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "class Test {",
            "  public Object x;",
            "  void m() {",
            "    // BUG: Diagnostic contains: public volatile Object x",
            "    if (x == null) {",
            "      synchronized (this) {",
            "        if (x == null) {",
            "          x = new Object();",
            "        }",
            "      }",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveNoFix() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "class Test {",
            "  static class Inner { static Object x; }",
            "  void m() {",
            "    // BUG: Diagnostic contains:",
            "    if (Inner.x == null) {",
            "      synchronized (this) {",
            "        if (Inner.x == null) {",
            "          Inner.x = new Object();",
            "        }",
            "      }",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void positiveTmpVar() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "class Test {",
            "  Object x;",
            "  void m() {",
            "    Object z = x;",
            "    // BUG: Diagnostic contains: volatile Object",
            "    if (z == null) {",
            "      synchronized (this) {",
            "        z = x;",
            "        if (z == null) {",
            "          x = z = new Object();",
            "        }",
            "      }",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "class Test {",
            "  volatile Object x;",
            "  void m() {",
            "    if (x == null) {",
            "      synchronized (this) {",
            "        if (x == null) {",
            "          x = new Object();",
            "        }",
            "      }",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void immutable_Integer() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "class Test {",
            "  public Integer x;",
            "  void m() {",
            "    if (x == null) {",
            "      synchronized (this) {",
            "        if (x == null) {",
            "          x = 1;",
            "        }",
            "      }",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void immutable_String() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            "package threadsafety;",
            "class Test {",
            "  public String x;",
            "  void m() {",
            "    if (x == null) {",
            "      synchronized (this) {",
            "        if (x == null) {",
            "          x = \"\";",
            "        }",
            "      }",
            "    }",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void b37896333() {
    compilationHelper
        .addSourceLines(
            "tTest.java",
            "class Test {",
            "  public String x;",
            "  String m() {",
            "    String result = x;",
            "    if (result == null) {",
            "      synchronized (this) {",
            "        if (result == null) {",
            "          x = result = \"\";",
            "        }",
            "      }",
            "    }",
            "    return result;",
            "  }",
            "}")
        .doTest();
  }
}
