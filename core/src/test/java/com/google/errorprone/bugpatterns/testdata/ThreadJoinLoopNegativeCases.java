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

import java.util.List;

/** @author mariasam@google.com (Maria Sam) on 7/10/17. */
public class ThreadJoinLoopNegativeCases {

  public void basicCase(Thread thread) throws InterruptedException {
    thread.join();
  }

  public void inIf(Thread thread) {
    try {
      if (7 == 7) {
        thread.join();
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  public void basicCaseTry(Thread thread) {
    try {
      thread.join();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  public void basicCaseWhile(Thread thread, List<String> list) {
    while (list.size() == 7) {
      try {
        thread.join();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  public void basicCaseFor(Thread thread, List<String> list) {
    for (int i = 0; i < list.size(); i++) {
      try {
        thread.join();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  public void basicCaseForEach(Thread thread, List<String> list) {
    for (String str : list) {
      try {
        thread.join();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  public void multipleCatches(Thread thread, int[] arr) {
    try {
      thread.join();
      int test = arr[10];
    } catch (ArrayIndexOutOfBoundsException e) {
      // ignore
    } catch (InterruptedException e) {
      System.out.println("test");
    }
  }

  public void fullInterruptedFullException(Thread thread) {
    try {
      thread.join();
    } catch (InterruptedException e) {
      e.printStackTrace();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void justException(Thread thread) {
    try {
      thread.join();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void multipleMethodInvocations(Thread thread, Thread thread2) {
    try {
      thread.join();
      thread2.join();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void tryFinally(Thread thread) {
    try {
      thread.join();
    } catch (InterruptedException e) {
      // ignore
    } finally {
      System.out.println("test finally");
    }
  }

  public void tryAssigningThread(Thread thread) {
    while (true) {
      try {
        thread.join();
        thread = null;
      } catch (InterruptedException e) {
        // ignore
      }
    }
  }
}
