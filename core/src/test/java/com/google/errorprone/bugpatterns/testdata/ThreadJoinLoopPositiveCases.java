/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

/** @author mariasam@google.com (Maria Sam) on 7/10/17. */
class ThreadJoinLoopPositiveCases {

  public void basicCase(Thread thread) {
    try {
      // BUG: Diagnostic contains: Uninterruptibles.joinUninterruptibly(thread)
      thread.join();
    } catch (InterruptedException e) {
      // ignore
    }
  }

  public void emptyInterruptedFullException(Thread thread) {
    try {
      // BUG: Diagnostic contains: Uninterruptibles.joinUninterruptibly(thread)
      thread.join();
    } catch (InterruptedException e) {
      // ignore
    }
  }

  public void emptyException(Thread thread) {
    try {
      // BUG: Diagnostic contains: Uninterruptibles.joinUninterruptibly(thread)
      thread.join();
    } catch (Exception e) {
      // ignore
    }
  }

  public void whileLoop(Thread thread) {
    while (true) {
      try {
        // BUG: Diagnostic contains: Uninterruptibles.joinUninterruptibly(thread)
        thread.join();
      } catch (InterruptedException e) {
        // ignore
      }
    }
  }

  public void whileLoopCheck(Thread thread) {
    while (thread != null) {
      try {
        // BUG: Diagnostic contains: Uninterruptibles.joinUninterruptibly(thread)
        thread.join();
      } catch (InterruptedException e) {
        // ignore
      }
    }
  }

  public void whileLoopVariable(Thread thread, boolean threadAlive) {
    while (threadAlive) {
      try {
        // BUG: Diagnostic contains: Uninterruptibles.joinUninterruptibly(thread)
        thread.join();
        threadAlive = false;
      } catch (InterruptedException e) {
        // ignore
      }
    }
  }

  public void basicLoopOtherStatements(Thread thread) {
    while (7 == 7) {
      System.out.println("test");
      try {
        // BUG: Diagnostic contains: Uninterruptibles.joinUninterruptibly(thread)
        thread.join();
      } catch (InterruptedException e) {
        // ignore
      }
    }
  }

  public void breakStatement(Thread thread) {
    while (7 == 7) {
      try {
        // BUG: Diagnostic contains: Uninterruptibles.joinUninterruptibly(thread)
        thread.join();
        break;
      } catch (InterruptedException e) {
        // ignore
      }
    }
  }

  private void whileLoopBreak(Thread thread) {
    while (true) {
      try {
        // BUG: Diagnostic contains: Uninterruptibles.joinUninterruptibly(thread)
        thread.join();
        break;
      } catch (InterruptedException e) {
        /* try again */
      }
    }
  }

  private void whileLoopThreadAlive(Thread thread) {
    while (thread.isAlive()) {
      try {
        // BUG: Diagnostic contains: Uninterruptibles.joinUninterruptibly(thread)
        thread.join();
      } catch (InterruptedException e) {
        // Ignore
      }
    }
  }

  public void multipleStatements(Thread thread, boolean isAlive) {
    try {
      // BUG: Diagnostic contains: Uninterruptibles.joinUninterruptibly(thread)
      thread.join();
      isAlive = false;
    } catch (InterruptedException e) {
      // ignore
    }
  }

  private void arrayJoin(Thread[] threads) {
    for (int i = 0; i < threads.length; i++) {
      try {
        // BUG: Diagnostic contains: Uninterruptibles.joinUninterruptibly(threads[i])
        threads[i].join();
      } catch (InterruptedException e) {
        // ignore
      }
    }
  }

  class MyThread extends Thread {

    public void run() {
      try {
        // BUG: Diagnostic contains: Uninterruptibles.joinUninterruptibly(this)
        join();
      } catch (InterruptedException e) {
        // ignore
      }
    }

    public void whileInThread() {
      while (isAlive()) {
        try {
          // BUG: Diagnostic contains: Uninterruptibles.joinUninterruptibly(this)
          join();
        } catch (InterruptedException e) {
          // Ignore.
        }
      }
    }
  }
}
