/*
 * Copyright 2013 Google Inc. All Rights Reserved.
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
}
