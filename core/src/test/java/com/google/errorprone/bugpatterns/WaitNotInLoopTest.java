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
 * @author eaftan@google.com (Eddie Aftandilian)
 */
@RunWith(JUnit4.class)
public class WaitNotInLoopTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(WaitNotInLoop.class, getClass());

  @Test
  public void positiveCase() {
    compilationHelper
        .addSourceLines(
            "WaitNotInLoopPositiveCases.java",
"""
package com.google.errorprone.bugpatterns.testdata;

import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;

/**
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public class WaitNotInLoopPositiveCases {

  boolean flag = false;

  public void testIfInsteadOfLoop() {
    synchronized (this) {
      if (!flag) {
        try {
          // BUG: Diagnostic contains: wait() must always be called in a loop
          // Did you mean 'while (!flag) {'?
          wait();
        } catch (InterruptedException e) {
        }
      }
    }
  }

  public void testWaitLong() throws InterruptedException {
    // BUG: Diagnostic contains: wait(long) must always be called in a loop
    wait(1000);
  }

  public void testWaitLongInt() throws Exception {
    // BUG: Diagnostic contains: wait(long,int) must always be called in a loop
    wait(1000, 1000);
  }

  public void testAwait(Condition cond) throws Exception {
    // BUG: Diagnostic contains: await() must always be called in a loop
    cond.await();
  }

  public void testAwaitWithFix(Condition cond) throws Exception {
    synchronized (this) {
      if (!flag) {
        try {
          // BUG: Diagnostic contains: await() must always be called in a loop
          // Did you mean 'while (!flag) {'?
          cond.await();
        } catch (InterruptedException e) {
        }
      }
    }
  }

  public void testAwaitLongTimeUnit(Condition cond) throws Exception {
    // BUG: Diagnostic contains:
    // await(long,java.util.concurrent.TimeUnit) must always be called in a loop
    cond.await(1, TimeUnit.SECONDS);
  }

  public void testAwaitNanos(Condition cond) throws Exception {
    // BUG: Diagnostic contains: awaitNanos(long) must always be called in a loop
    cond.awaitNanos(1000000);
  }

  public void testAwaitUninterruptibly(Condition cond) throws Exception {
    // BUG: Diagnostic contains: awaitUninterruptibly() must always be called in a loop
    cond.awaitUninterruptibly();
  }

  public void testAwaitUntil(Condition cond) throws Exception {
    // BUG: Diagnostic contains: awaitUntil(java.util.Date) must always be called in a loop
    cond.awaitUntil(new Date());
  }
}\
""")
        .doTest();
  }

  @Test
  public void negativeCase() {
    compilationHelper
        .addSourceLines(
            "WaitNotInLoopNegativeCases.java",
"""
package com.google.errorprone.bugpatterns.testdata;

/**
 * @author eaftan@google.com (Eddie Aftandilian)
 *     <p>TODO(eaftan): Add test cases for enhanced for loop, loop outside synchronized block.
 */
public class WaitNotInLoopNegativeCases {

  boolean flag = true;

  public void test1() {
    synchronized (this) {
      while (!flag) {
        try {
          wait();
        } catch (InterruptedException e) {
        }
      }
    }
  }

  public void test2() {
    synchronized (this) {
      while (!flag) {
        try {
          wait(1000);
        } catch (InterruptedException e) {
        }
      }
    }
  }

  public void test3() {
    synchronized (this) {
      while (!flag) {
        try {
          wait(1000, 1000);
        } catch (InterruptedException e) {
        }
      }
    }
  }

  // This code is incorrect, but this check should not flag it.
  public void testLoopNotInSynchronized() {
    while (!flag) {
      synchronized (this) {
        try {
          wait();
        } catch (InterruptedException e) {
        }
      }
    }
  }

  public void testDoLoop() {
    synchronized (this) {
      do {
        try {
          wait();
        } catch (InterruptedException e) {
        }
      } while (!flag);
    }
  }

  public void testForLoop() {
    synchronized (this) {
      for (; !flag; ) {
        try {
          wait();
        } catch (InterruptedException e) {
        }
      }
    }
  }

  public void testEnhancedForLoop() {
    int[] arr = new int[100];
    synchronized (this) {
      for (int i : arr) {
        try {
          wait();
        } catch (InterruptedException e) {
        }
      }
    }
  }

  private void wait(Object obj) {}

  public void testNotObjectWait() {
    wait(new Object());
  }
}\
""")
        .doTest();
  }
}
