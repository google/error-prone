---
title: ThreadJoinLoop
summary: Thread.join needs to be surrounded by a loop until it succeeds, as in Uninterruptibles.joinUninterruptibly.
layout: bugpattern
tags: ''
severity: WARNING
providesFix: REQUIRES_HUMAN_ATTENTION
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
Thread.join() can be interrupted, and so requires users to catch InterruptedException. Most users should be looping until the join() actually succeeds.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("ThreadJoinLoop")` to the enclosing element.

----------

### Positive examples
__ThreadJoinLoopPositiveCases.java__

{% highlight java %}
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
{% endhighlight %}

__ThreadJoinLoopPositiveCases_expected.java__

{% highlight java %}
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
{% endhighlight %}

### Negative examples
__ThreadJoinLoopNegativeCases.java__

{% highlight java %}
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
{% endhighlight %}

