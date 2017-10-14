---
title: PrimitiveArrayPassedToVarargsMethod
summary: Passing a primitive array to a varargs method is usually wrong
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
When you pass a primitive array as the only argument to a varargs method, the primitive array is autoboxed into a single-element Object array. This is usually not what was intended.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("PrimitiveArrayPassedToVarargsMethod")` annotation to the enclosing element.

----------

### Positive examples
__PrimitiveArrayPassedToVarargsMethodPositiveCases.java__

{% highlight java %}
/*
 * Copyright 2014 Google Inc. All Rights Reserved.
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

import java.util.Arrays;

/** @author eaftan@google.com (Eddie Aftandilian) */
public class PrimitiveArrayPassedToVarargsMethodPositiveCases {

  public void objectVarargsMethod(Object... objs) {}

  public <T> void genericVarargsMethod(T... genericArrays) {}

  public void objectVarargsMethodWithMultipleParams(Object obj1, Object... objs) {}

  public void doIt() {
    int[] intArray = {1, 2, 3};

    // BUG: Diagnostic contains:
    objectVarargsMethod(intArray);

    // BUG: Diagnostic contains:
    genericVarargsMethod(intArray);

    // BUG: Diagnostic contains:
    objectVarargsMethodWithMultipleParams(new Object(), intArray);

    // BUG: Diagnostic contains:
    Arrays.asList(intArray);
  }
}
{% endhighlight %}

### Negative examples
__PrimitiveArrayPassedToVarargsMethodNegativeCases.java__

{% highlight java %}
/*
 * Copyright 2014 Google Inc. All Rights Reserved.
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


/** @author eaftan@google.com (Eddie Aftandilian) */
public class PrimitiveArrayPassedToVarargsMethodNegativeCases {

  public void intVarargsMethod(int... ints) {}

  public void intArrayVarargsMethod(int[]... intArrays) {}

  public void objectVarargsMethodWithMultipleParams(Object obj1, Object... objs) {}

  public void doIt() {
    int[] intArray = {1, 2, 3};

    intVarargsMethod(intArray);
    intArrayVarargsMethod(intArray);
    objectVarargsMethodWithMultipleParams(new Object());
  }
}
{% endhighlight %}

