---
title: ElementsCountedInLoop
summary: This code, which counts elements using a loop, can be replaced by a simpler library method
layout: bugpattern
category: JDK
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
This code counts elements using a loop.  You can use various library methods (Guava's Iterables.size(), Collection.size(), array.length) to achieve the same thing in a cleaner way.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("ElementsCountedInLoop")` annotation to the enclosing element.

----------

### Positive examples
__ElementsCountedInLoopPositiveCases.java__

{% highlight java %}
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

import java.util.*;

/**
 * @author amshali@google.com (Amin Shali)
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public class ElementsCountedInLoopPositiveCases {
    
  public int testEnhancedFor(Iterable<Object> iterable, HashSet<Object> set, Object... array) {
    int count = 0;
    // BUG: Diagnostic contains: count += Iterables.size(iterable)
    for (Object item : iterable) {
      count ++;
    }
    // BUG: Diagnostic contains: count += Iterables.size(iterable)
    for (Object item : iterable) {
      count += 1;
    }
    // BUG: Diagnostic contains: count += Iterables.size(iterable)
    for (Object item : iterable) {
      count += 1.0; // float constant 1
    }
    // BUG: Diagnostic contains: count += Iterables.size(iterable)
    for (Object item : iterable) {
      count += 1L; // long constant 1
    }
    // BUG: Diagnostic contains: count += Iterables.size(iterable)
    for (Object item : iterable) {
      count  = count + 1;
    }
    // BUG: Diagnostic contains: count += Iterables.size(iterable)
    for (Object item : iterable) {
      count  = 1 + count;
    }
    // BUG: Diagnostic contains: count += set.size()
    for (Object item : set) {
      count  = 1 + count;
    }
    // BUG: Diagnostic contains: count += array.length
    for (Object item : array) {
      count  = 1 + count;
    }
    return count;
  }
  
  public int testWhileLoop(List<Object> iterable) {
    Iterator<Object> it = iterable.iterator();
    int count = 0;
    // BUG: Diagnostic contains: 
    while (it.hasNext()) {
      count += 1;
    }
    // BUG: Diagnostic contains: 
    while (it.hasNext()) {
      count++;
    }
    // BUG: Diagnostic contains: 
    while (it.hasNext()) {
      count = count + 1;
    }
    return count;
  }
}
{% endhighlight %}

### Negative examples
__ElementsCountedInLoopNegativeCases.java__

{% highlight java %}
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

import java.util.*;

/**
 * @author amshali@google.com (Amin Shali)
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public class ElementsCountedInLoopNegativeCases {
  public int testEnhancedFor(List<Object> iterable) {
    int count = 0;
    // The following cases are considered negative because they are incrementing the counter by more 
    // than 1.
    for (Object item : iterable) {
      count += 2;
    }
    for (Object item : iterable) {
      count  = count + 3;
    }
    for (Object item : iterable) {
      count  = 2 + count;
    }
    return count;
  }

  public int testEnhancedWhileLoop(List<Object> iterable) {
    Iterator<Object> it = iterable.iterator();
    int count = 0;
    // The following case is considered negative because it is incrementing the counter by 2.
    while (it.hasNext()) {
      count += 2;
    }
    // 'this' is not an Iterable type.
    while (this.hasNext()) {
      count += 1;
    }
    // Complicated while body.
    while (it.hasNext()) {
      System.err.println("Not so simple body");
      count++;
    }
    return count;
  }

  public boolean hasNext() {
    return true;
  }
  
  public double testEnhancedForFloats(List<Object> iterable) {
    double count = 0;
    // The following cases are considered negative because they are incrementing the counter by a
    // float value which is not 1.
    for (Object item : iterable) {
      count += 2.0;
    }
    for (Object item : iterable) {
      count  = count + 3.0;
    }
    for (Object item : iterable) {
      count  = 0.1 + count;
    }
    return count;
  }
}
{% endhighlight %}

