---
title: MultipleUnaryOperatorsInMethodCall
summary: Avoid having multiple unary operators acting on the same variable in a method call
layout: bugpattern
tags: ''
severity: SUGGESTION
providesFix: NO_FIX
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
Increment operators in method calls are dubious and while argument lists are evaluated left-to-right, documentation suggests that code not rely on this specification. In addition, code is clearer when each expression contains at most one side effect.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("MultipleUnaryOperatorsInMethodCall")` annotation to the enclosing element.

----------

### Positive examples
__MultipleUnaryOperatorsInMethodCallPositiveCases.java__

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

/** @author sulku@google.com (Marsela Sulku) */
public class MultipleUnaryOperatorsInMethodCallPositiveCases {
  public static void tests(int a, int b) {
    /** these cases do not have suggested fixes */

    // BUG: Diagnostic contains: Avoid having multiple unary operators acting
    twoArgs(a++, a--);
    // BUG: Diagnostic contains: Avoid having multiple unary operators acting
    twoArgs(a--, ++a);
    // BUG: Diagnostic contains: Avoid having multiple unary operators acting
    twoArgs(++a, a++);
    // BUG: Diagnostic contains: Avoid having multiple unary operators acting
    twoArgs(--a, --a);

    // BUG: Diagnostic contains: Avoid having multiple unary operators acting
    threeArgs(a++, b++, b++);
    // BUG: Diagnostic contains: Avoid having multiple unary operators acting
    threeArgs(a++, b, a++);
    // BUG: Diagnostic contains: Avoid having multiple unary operators acting
    threeArgs(++a, b++, --b);
    // BUG: Diagnostic contains: Avoid having multiple unary operators acting
    threeArgs(++a, a++, b);
    // BUG: Diagnostic contains: Avoid having multiple unary operators acting
    threeArgs(++a, a++, a);
    // BUG: Diagnostic contains: Avoid having multiple unary operators acting
    threeArgs(++a, a++, a--);
  }

  public static void twoArgs(int a, int b) {}

  public static void threeArgs(int a, int b, int c) {}

  public static int someFunction(int a) {
    return 0;
  }
}
{% endhighlight %}

### Negative examples
__MultipleUnaryOperatorsInMethodCallNegativeCases.java__

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

/** @author sulku@google.com (Marsela Sulku) */
public class MultipleUnaryOperatorsInMethodCallNegativeCases {
  public static void tests(int a, int b) {
    testMethod(a, b);
    testMethod(a + 1, b);
    testMethod(b, a + 1);
    testMethod(a++, b);
    testMethod(--a, b);
    testMethod(a, b--);
    testMethod(a, ++b);
  }

  public static void testMethod(int one, int two) {}
}
{% endhighlight %}

