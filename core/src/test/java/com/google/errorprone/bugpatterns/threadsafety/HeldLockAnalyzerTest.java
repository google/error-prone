/*
 * Copyright 2014 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;

import com.google.errorprone.BugPattern;
import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.Tree;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link GuardedByLockSetAnalyzer}Test */
@RunWith(JUnit4.class)
public class HeldLockAnalyzerTest {
  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(GuardedByLockSetAnalyzer.class, getClass());

  @Test
  public void instance() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            """
            package threadsafety;

            import com.google.errorprone.annotations.concurrent.GuardedBy;
            import java.util.concurrent.locks.Lock;

            class Test {
              final Lock lock = null;

              @GuardedBy("lock")
              int x;

              void m() {
                lock.lock();
                try {
                  // BUG: Diagnostic contains:
                  // [(SELECT (THIS) lock)]
                  x++;
                } finally {
                  lock.unlock();
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void twoInstances() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            """
            package threadsafety;

            import com.google.errorprone.annotations.concurrent.GuardedBy;
            import java.util.concurrent.locks.Lock;

            class Test {
              final Lock lock = null;

              @GuardedBy("lock")
              int x;

              void m(Lock lock2) {
                lock.lock();
                lock2.lock();
                try {
                  // BUG: Diagnostic contains:
                  // [(LOCAL_VARIABLE lock2), (SELECT (THIS) lock)]
                  x++;
                } finally {
                  lock.unlock();
                  lock2.unlock();
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void synchronizedMethod() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            """
            package threadsafety;

            import com.google.errorprone.annotations.concurrent.GuardedBy;
            import java.util.concurrent.locks.Lock;

            class Test {
              @GuardedBy("this")
              int x;

              synchronized void m() {
                // BUG: Diagnostic contains:  [(THIS)]
                x++;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void synchronizedThis() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            """
            package threadsafety;

            import com.google.errorprone.annotations.concurrent.GuardedBy;
            import java.util.concurrent.locks.Lock;

            class Test {
              @GuardedBy("this")
              int x;

              void m() {
                synchronized (this) {
                  // BUG: Diagnostic contains:  [(THIS)]
                  x++;
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void synchronizedField() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            """
            package threadsafety;

            import com.google.errorprone.annotations.concurrent.GuardedBy;

            class Lock {
              final Object lock = null;
            }

            class Test {
              final Lock mu = new Lock();

              @GuardedBy("this")
              int x;

              void m() {
                synchronized (mu.lock) {
                  // BUG: Diagnostic contains:
                  // [(SELECT (SELECT (THIS) mu) lock)]
                  x++;
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void synchronizedClass() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            """
            package threadsafety;

            import com.google.errorprone.annotations.concurrent.GuardedBy;

            class Lock {}

            class Test {
              final Lock mu = new Lock();

              @GuardedBy("this")
              int x;

              void m() {
                synchronized (Lock.class) {
                  // BUG: Diagnostic contains:  [(CLASS_LITERAL threadsafety.Lock)]
                  x++;
                }
              }
            }
            """)
        .doTest();
  }

  @Test
  public void locked() {
    compilationHelper
        .addSourceLines(
            "threadsafety/Test.java",
            """
            package threadsafety;

            import com.google.errorprone.annotations.concurrent.GuardedBy;
            import java.util.concurrent.locks.Lock;

            class Test {
              final Lock mu = null;
              final Lock lock = null;

              @GuardedBy("lock")
              int x;

              void m() {
                mu.lock();
                // BUG: Diagnostic contains:  []
                x++;
                try {
                  // BUG: Diagnostic contains:
                  // [(SELECT (THIS) mu)]
                  x++;
                } finally {
                  mu.unlock();
                }
                // BUG: Diagnostic contains:  []
                x++;
              }
            }
            """)
        .doTest();
  }

  /** A customized {@link GuardedByChecker} that prints more test-friendly diagnostics. */
  @BugPattern(name = "GuardedByLockSet", summary = "", explanation = "", severity = ERROR)
  public static class GuardedByLockSetAnalyzer extends GuardedByChecker {
    @Inject
    GuardedByLockSetAnalyzer() {}

    @Override
    protected Description checkGuardedAccess(
        Tree tree, GuardedByExpression guard, HeldLockSet live, VisitorState state) {
      List<String> toSort = new ArrayList<>();
      for (GuardedByExpression node : live.allLocks()) {
        toSort.add(node.debugPrint());
      }
      Collections.sort(toSort);
      return buildDescription(tree).setMessage("Holding: " + toSort).build();
    }
  }
}
