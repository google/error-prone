---
title: DeadException
summary: Exception created but not thrown
layout: bugpattern
tags: ''
severity: ERROR
providesFix: REQUIRES_HUMAN_ATTENTION
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

_Alternate names: ThrowableInstanceNeverThrown_

## The problem
The exception is created with new, but is not thrown, and the reference is lost.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("DeadException")` annotation to the enclosing element.

----------

### Positive examples
__DeadExceptionPositiveCases.java__

{% highlight java %}
/*
 * Copyright 2011 Google Inc. All Rights Reserved.
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

public class DeadExceptionPositiveCases {
  public void error() {
    // BUG: Diagnostic contains: throw new RuntimeException
    new RuntimeException("Not thrown, and reference lost");
  }

  public void fixIsToDeleteTheFirstStatement() {
    // BUG: Diagnostic contains: remove this line
    new IllegalArgumentException("why is this here?");
    int i = 1;
    System.out.println("i = " + i);

    if (true) {
      // BUG: Diagnostic contains: remove this line
      new RuntimeException("oops");
      System.out.println("another statement after exception");
    }

    switch (0) {
      default:
        // BUG: Diagnostic contains: remove this line
        new RuntimeException("oops");
        System.out.println("another statement after exception");
    }
  }

  public void firstStatementWithNoSurroundingBlock() {
    if (true)
      // BUG: Diagnostic contains: throw new InterruptedException
      new InterruptedException("this should be thrown");

    if (true) return;
    else
      // BUG: Diagnostic contains: throw new ArithmeticException
      new ArithmeticException("should also be thrown");

    switch (4) {
      case 4:
        System.out.println("4");
        break;
      default:
        // BUG: Diagnostic contains: throw new IllegalArgumentException
        new IllegalArgumentException("should be thrown");
    }
  }

  public void testLooksLikeAJunitTestMethod() {
    // BUG: Diagnostic contains: throw new RuntimeException
    new RuntimeException("Not thrown, and reference lost");
  }

  {
    // BUG: Diagnostic contains: throw new Exception
    new Exception();
  }
}
{% endhighlight %}

### Negative examples
__DeadExceptionNegativeCases.java__

{% highlight java %}
/*
 * Copyright 2011 Google Inc. All Rights Reserved.
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

public class DeadExceptionNegativeCases {
  public void noError() {
    Exception e = new RuntimeException("stored");
    e = new UnsupportedOperationException("also stored");
    throw new IllegalArgumentException("thrown");
  }

  public Exception returnsException() {
    return new RuntimeException("returned");
  }
}
{% endhighlight %}

