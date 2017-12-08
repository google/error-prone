---
title: JUnitAssertSameCheck
summary: An object is tested for reference equality to itself using JUnit library.
layout: bugpattern
tags: ''
severity: ERROR
providesFix: NO_FIX
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem


## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("JUnitAssertSameCheck")` to the enclosing element.

----------

### Positive examples
__JUnitAssertSameCheckPositiveCase.java__

{% highlight java %}
/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

/**
 * Positive test cases for {@link JUnitAssertSameCheck} check.
 *
 * @author bhagwani@google.com (Sumit Bhagwani)
 */
public class JUnitAssertSameCheckPositiveCase {

  public void test(Object obj) {
    // BUG: Diagnostic contains: An object is tested for reference equality to itself using JUnit
    org.junit.Assert.assertSame(obj, obj);

    // BUG: Diagnostic contains: An object is tested for reference equality to itself using JUnit
    org.junit.Assert.assertSame("message", obj, obj);

    // BUG: Diagnostic contains: An object is tested for reference equality to itself using JUnit
    junit.framework.Assert.assertSame(obj, obj);

    // BUG: Diagnostic contains: An object is tested for reference equality to itself using JUnit
    junit.framework.Assert.assertSame("message", obj, obj);
  }
}
{% endhighlight %}

### Negative examples
__JUnitAssertSameCheckNegativeCases.java__

{% highlight java %}
/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

/**
 * Negative test cases for {@link JUnitAssertSameCheck} check.
 *
 * @author bhagwani@google.com (Sumit Bhagwani)
 */
public class JUnitAssertSameCheckNegativeCases {

  public void test(Object obj1, Object obj2) {
    org.junit.Assert.assertSame(obj1, obj2);
    org.junit.Assert.assertSame("message", obj1, obj2);
    junit.framework.Assert.assertSame(obj1, obj2);
    junit.framework.Assert.assertSame("message", obj1, obj2);
  }
}
{% endhighlight %}

