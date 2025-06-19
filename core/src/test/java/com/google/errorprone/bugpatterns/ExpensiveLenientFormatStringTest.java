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

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ExpensiveLenientFormatStringTest {

  private final CompilationTestHelper testHelper =
      CompilationTestHelper.newInstance(ExpensiveLenientFormatString.class, getClass());

  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(ExpensiveLenientFormatString.class, getClass());

  @Test
  public void refactoring() {
    refactoringHelper
        .addInputLines(
            "PreconditionsExpensiveStringTest.java",
"""
package com.google.devtools.javatools.refactory.refaster.cleanups;

import static com.google.common.base.Preconditions.checkNotNull;

class PreconditionsExpensiveStringTest {
  void f() {
    checkNotNull(this, "%s", "hello");
  }

  void g() {
    checkNotNull(this, "hello");
  }

  void h() {
    checkNotNull(this, String.format("%d", 42));
  }

  void i() {
    checkNotNull(this, "%s", "hello");
  }
}
""")
        .addOutputLines(
            "PreconditionsExpensiveStringTest.java",
"""
package com.google.devtools.javatools.refactory.refaster.cleanups;

import static com.google.common.base.Preconditions.checkNotNull;

class PreconditionsExpensiveStringTest {
  void f() {
    checkNotNull(this, "%s", "hello");
  }

  void g() {
    checkNotNull(this, "hello");
  }

  void h() {
    checkNotNull(this, String.format("%d", 42));
  }

  void i() {
    checkNotNull(this, "%s", "hello");
  }
}
""")
        .doTest();
  }

  @Test
  public void positive() {
    testHelper
        .addSourceLines(
            "PreconditionsExpensiveStringPositiveCase1.java",
"""
package com.google.errorprone.bugpatterns.testdata;

import com.google.common.base.Preconditions;

/**
 * Test for methodIs call involving String.format() and %s
 *
 * @author sjnickerson@google.com (Simon Nickerson)
 */
public class PreconditionsExpensiveStringPositiveCase1 {
  public void error() {
    int foo = 42;
    int bar = 78;
    // BUG: Diagnostic contains: String.format
    Preconditions.checkState(true, String.format("The foo %s (%s) is not a good foo", foo, bar));
  }
}
""")
        .doTest();
  }

  @Test
  public void negative1() {
    testHelper
        .addSourceLines(
            "PreconditionsExpensiveStringNegativeCase1.java",
"""
package com.google.errorprone.bugpatterns.testdata;

import com.google.common.base.Preconditions;

/**
 * Preconditions calls which shouldn't be picked up for expensive string operations
 *
 * @author sjnickerson@google.com (Simon Nickerson)
 */
public class PreconditionsExpensiveStringNegativeCase1 {
  public void error() {
    int foo = 42;
    Preconditions.checkState(true, "The foo %s foo  is not a good foo", foo);

    // This call should not be converted because of the %d, which does some locale specific
    // behaviour. If it were an %s, it would be fair game.
    Preconditions.checkState(true, String.format("The foo %d foo is not a good foo", foo));

    // No format arguments
    Preconditions.checkState(true);
  }
}
""")
        .doTest();
  }

  @Test
  public void negative2() {
    testHelper
        .addSourceLines(
            "PreconditionsExpensiveStringNegativeCase2.java",
"""
package com.google.errorprone.bugpatterns.testdata;

import com.google.common.base.Preconditions;

/**
 * Test for methodIs call including string concatenation. (Not yet supported, so this is a negative
 * case)
 *
 * @author sjnickerson@google.com (Simon Nickerson)
 */
public class PreconditionsExpensiveStringNegativeCase2 {
  public void error() {
    int foo = 42;
    Preconditions.checkState(true, "The foo" + foo + " is not a good foo");
  }
}
""")
        .doTest();
  }
}
