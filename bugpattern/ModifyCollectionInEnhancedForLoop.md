---
title: ModifyCollectionInEnhancedForLoop
summary: Modifying a collection while iterating over it in a loop may cause a ConcurrentModificationException to be thrown.
layout: bugpattern
tags: ''
severity: WARNING
providesFix: NO_FIX
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
From the javadoc for
[`Iterator.remove`](https://docs.oracle.com/javase/9/docs/api/java/util/Iterator.html#remove--):

> The behavior of an iterator is unspecified if the underlying collection is
> modified while the iteration is in progress in any way other than by calling
> this method, unless an overriding class has specified a concurrent
> modification policy.

That is, prefer this:

```java {.good}
Iterator<String> it = ids.iterator();
while (it.hasNext()) {
  if (shouldRemove(it.next())) {
    it.remove();
  }
}
```

to this:

```java {.bad}
for (String id : ids) {
  if (shouldRemove(id)) {
    ids.remove(id); // will cause a ConcurrentModificationException!
  }
}
```

TIP: This pattern is simpler with Java 8's
[`Collection.removeIf`](https://docs.oracle.com/javase/8/docs/api/java/util/Collection.html#removeIf-java.util.function.Predicate-):

    ```java {.good}
    ids.removeIf(id -> shouldRemove(id));
    ```

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("ModifyCollectionInEnhancedForLoop")` to the enclosing element.

----------

### Positive examples
__ModifyCollectionInEnhancedForLoopPositiveCases.java__

{% highlight java %}
/*
 * Copyright 2017 The Error Prone Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.errorprone.bugpatterns;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;

/** @author anishvisaria98@gmail.com (Anish Visaria) */
public class ModifyCollectionInEnhancedForLoopPositiveCases {

  public static void testBasic(ArrayList<Integer> arr, HashSet<Integer> set) {
    for (Integer a : arr) {
      // BUG: Diagnostic contains:
      arr.add(new Integer("42"));
      // BUG: Diagnostic contains:
      arr.addAll(set);
      // BUG: Diagnostic contains:
      arr.clear();
      // BUG: Diagnostic contains:
      arr.remove(a);
      // BUG: Diagnostic contains:
      arr.removeAll(set);
      // BUG: Diagnostic contains:
      arr.retainAll(set);
    }
  }

  public static void testNested(ArrayList<Integer> arr, LinkedList<Integer> list) {
    for (Integer x : arr) {
      for (Integer y : list) {
        // BUG: Diagnostic contains:
        arr.add(y);
        // BUG: Diagnostic contains:
        arr.addAll(list);
        // BUG: Diagnostic contains:
        arr.clear();
        // BUG: Diagnostic contains:
        arr.remove(x);
        // BUG: Diagnostic contains:
        arr.removeAll(list);
        // BUG: Diagnostic contains:
        arr.retainAll(list);
        // BUG: Diagnostic contains:
        list.add(x);
        // BUG: Diagnostic contains:
        list.addAll(arr);
        // BUG: Diagnostic contains:
        list.clear();
        // BUG: Diagnostic contains:
        list.remove(y);
        // BUG: Diagnostic contains:
        list.removeAll(arr);
        // BUG: Diagnostic contains:
        list.retainAll(arr);
      }
    }
  }
}
{% endhighlight %}

### Negative examples
__ModifyCollectionInEnhancedForLoopNegativeCases.java__

{% highlight java %}
/*
 * Copyright 2017 The Error Prone Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.errorprone.bugpatterns;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;

/** @author anishvisaria98@gmail.com (Anish Visaria) */
public class ModifyCollectionInEnhancedForLoopNegativeCases {

  public static void testBasic(ArrayList<Integer> arr, HashSet<Integer> set) {
    for (Integer a : arr) {
      set.add(a);
      set.addAll(arr);
      set.clear();
      set.removeAll(arr);
      set.retainAll(arr);
    }

    for (Integer i : set) {
      arr.add(i);
      arr.addAll(set);
      arr.clear();
      arr.removeAll(set);
      arr.retainAll(set);
    }
  }

  public static void testNested(ArrayList<Integer> arr, LinkedList<Integer> list) {
    for (Integer x : arr) {
      for (Integer y : list) {}

      list.add(x);
      list.addAll(arr);
      list.clear();
      list.removeAll(arr);
      list.retainAll(arr);
    }
  }

  public static void testBreakOutOfLoop(ArrayList<Integer> xs) {
    for (Integer x : xs) {
      xs.remove(x);
      return;
    }
    for (Integer x : xs) {
      xs.remove(x);
      System.err.println();
      break;
    }
  }

  private static void concurrent() {
    CopyOnWriteArrayList<Integer> cowal = new CopyOnWriteArrayList<>();
    for (int i : cowal) {
      cowal.remove(i);
    }
  }

  interface MyBlockingQueue<T> extends BlockingQueue<T> {}

  private static void customConcurrent(MyBlockingQueue<Integer> mbq) {
    for (Integer i : mbq) {
      mbq.add(i);
    }
  }
}
{% endhighlight %}

