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

import com.google.common.util.concurrent.Uninterruptibles;

/** @author mariasam@google.com (Maria Sam) on 7/10/17. */
class ThreadJoinLoopPositiveCases {

  public void basicCase(Thread thread) {
    Uninterruptibles.joinUninterruptibly(thread);
  }

  public void emptyInterruptedFullException(Thread thread) {
    Uninterruptibles.joinUninterruptibly(thread);
  }

  public void emptyException(Thread thread) {
    Uninterruptibles.joinUninterruptibly(thread);
  }

  public void whileLoop(Thread thread) {
    Uninterruptibles.joinUninterruptibly(thread);
  }

  public void whileLoopCheck(Thread thread) {
    Uninterruptibles.joinUninterruptibly(thread);
  }

  public void whileLoopVariable(Thread thread, boolean threadAlive) {
    Uninterruptibles.joinUninterruptibly(thread);
  }

  public void basicLoopOtherStatements(Thread thread) {
    while (7 == 7) {
      System.out.println("test");
      Uninterruptibles.joinUninterruptibly(thread);
    }
  }

  public void breakStatement(Thread thread) {
    Uninterruptibles.joinUninterruptibly(thread);
  }

  private void whileLoopBreak(Thread thread) {
    Uninterruptibles.joinUninterruptibly(thread);
  }

  private void whileLoopThreadAlive(Thread thread) {
    Uninterruptibles.joinUninterruptibly(thread);
  }

  public void multipleStatements(Thread thread, boolean isAlive) {
    Uninterruptibles.joinUninterruptibly(thread);
  }

  private void arrayJoin(Thread[] threads) {
    for (int i = 0; i < threads.length; i++) {
      Uninterruptibles.joinUninterruptibly(threads[i]);
    }
  }

  class MyThread extends Thread {

    public void run() {
      Uninterruptibles.joinUninterruptibly(this);
    }

    public void whileInThread() {
      Uninterruptibles.joinUninterruptibly(this);
    }
  }
}
