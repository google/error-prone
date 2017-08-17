---
title: IncrementInForLoopAndHeader
summary: This for loop increments the same variable in the header and in the body
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem


## Suppression
Suppress false positives by adding an `@SuppressWarnings("IncrementInForLoopAndHeader")` annotation to the enclosing element.

----------

### Positive examples
__IncrementInForLoopAndHeaderPositiveCases.java__

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

/** @author mariasam@google.com (Maria Sam) */
public class IncrementInForLoopAndHeaderPositiveCases {

  public void basicTest() {
    // BUG: Diagnostic contains: increment
    for (int i = 0; i < 10; i++) {
      i++;
    }
  }

  public void decrement() {
    // BUG: Diagnostic contains: increment
    for (int i = 0; i < 10; i++) {
      i--;
    }
  }

  public void preInc() {
    // BUG: Diagnostic contains: increment
    for (int i = 0; i < 10; i++) {
      --i;
    }
  }

  public void multipleStatements() {
    // BUG: Diagnostic contains: increment
    for (int i = 0; i < 10; i++) {
      --i;
      int a = 0;
    }
  }

  public void multipleUpdates() {
    // BUG: Diagnostic contains: increment
    for (int i = 0, a = 1; i < 10; i++, a++) {
      a++;
    }
  }

  public void multipleUpdatesOtherVar() {
    // BUG: Diagnostic contains: increment
    for (int i = 0, a = 1; i < 10; i++, a++) {
      i++;
    }
  }

  public void multipleUpdatesBothVars() {
    // BUG: Diagnostic contains: increment
    for (int i = 0, a = 1; i < 10; i++, a++) {
      a++;
      i++;
    }
  }

  public void nestedFor() {
    for (int i = 0; i < 10; i++) {
      // BUG: Diagnostic contains: increment
      for (int a = 0; a < 10; a++) {
        a--;
      }
    }
  }

  public void nestedForBoth() {
    // BUG: Diagnostic contains: increment
    for (int i = 0; i < 10; i++) {
      i++;
      // BUG: Diagnostic contains: increment
      for (int a = 0; a < 10; a++) {
        a--;
      }
    }
  }
}
{% endhighlight %}

### Negative examples
__IncrementInForLoopAndHeaderNegativeCases.java__

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

/** Created by mariasam on 7/20/17. */
public class IncrementInForLoopAndHeaderNegativeCases {

  public void arrayInc() {
    for (int[] level = {}; level[0] > 10; level[0]--) {
      System.out.println("test");
    }
  }

  public void emptyForLoop() {
    for (int i = 0; i < 2; i++) {}
  }

  public void inIf() {
    for (int i = 0; i < 20; i++) {
      if (i == 7) {
        i++;
      }
    }
  }

  public void inWhile() {
    for (int i = 0; i < 20; i++) {
      while (i == 7) {
        i++;
      }
    }
  }

  public void inDoWhile() {
    for (int i = 0; i < 20; i++) {
      do {
        i++;
      } while (i == 7);
    }
  }

  public void inFor() {
    for (int i = 0; i < 20; i++) {
      for (int a = 0; a < i; a++) {
        i++;
      }
    }
  }

  public void inForEach(List<String> list) {
    for (int i = 0; i < 10; i++) {
      for (String s : list) {
        i++;
      }
    }
  }

  public void otherVarInc() {
    for (int i = 0; i < 2; i++) {
      int a = 0;
      a++;
    }
  }
}
{% endhighlight %}

