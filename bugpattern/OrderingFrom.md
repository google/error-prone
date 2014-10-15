---
title: OrderingFrom
layout: bugpattern
category: GUAVA
severity: WARNING
maturity: EXPERIMENTAL
---

# Bug pattern: OrderingFrom
__Ordering.from(new Comparator<T>() { }) can be refactored to cleaner form__

## The problem
Calls of the form
`Ordering.from(new Comparator<T>() { ... })`
can be unwrapped to a new anonymous subclass of Ordering
`new Ordering<T>() { ... }`
which is shorter and cleaner (and potentially more efficient).

## Suppression
Suppress false positives by adding an `@SuppressWarnings("OrderingFrom")` annotation to the enclosing element.

----------

# Examples
__OrderingFromNegativeCases.java__
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

package com.google.errorprone.bugpatterns;

import com.google.common.collect.Ordering;

import java.util.Comparator;

/**
 * Negative test cases for theOrdering.from(new Comparator<T>() { ... }) check
 *
 * @author sjnickerson@google.com (Simon Nickerson)
 */
public class OrderingFromNegativeCases {

  public static void negativeCase1() {
    Comparator<String> comparator = new Comparator<String>() {
      @Override
      public int compare(String first, String second) {
        int compare = first.length() - second.length();
        return (compare != 0) ? compare : first.compareTo(second);
      }
    };
    Ordering<String> ord = Ordering.from(comparator);
  }

}

{% endhighlight %}
__OrderingFromPositiveCases.java__
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

package com.google.errorprone.bugpatterns;

import com.google.common.collect.Ordering;

import java.util.Comparator;

/**
 * Positive test cases for theOrdering.from(new Comparator<T>() { ... }) check
 *
 * @author sjnickerson@google.com (Simon Nickerson)
 */
public class OrderingFromPositiveCases {

  public static void positiveCase1() {
    // BUG: Diagnostic contains: new Ordering<String>(
    Ordering<String> ord = Ordering.from(new Comparator<String>() {
      @Override
      public int compare(String first, String second) {
        int compare = first.length() - second.length();
        return (compare != 0) ? compare : first.compareTo(second);
      }
    });
  }

}

{% endhighlight %}
