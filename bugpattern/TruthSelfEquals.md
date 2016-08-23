---
title: TruthSelfEquals
summary: An object is tested for equality to itself using Truth Libraries.
layout: bugpattern
category: TRUTH
severity: WARNING
maturity: MATURE
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
The arguments to isEqualTo/isNotEqualTo method are the same object, so it either always passes/fails the test.  Either change the arguments to point to different objects or consider using EqualsTester.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("TruthSelfEquals")` annotation to the enclosing element.

----------

### Positive examples
__TruthSelfEqualsPositiveCases.java__

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

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

/**
 * Positive test cases for TruthSelfEquals check.
 *
 * @author bhagwani@google.com (Sumit Bhagwani)
 */
public class TruthSelfEqualsPositiveCases {

  public void testAssertThatEq() {
    String test = Boolean.TRUE.toString();
    // BUG: Diagnostic contains: The arguments to isEqualTo method are the same object
    assertThat(test).isEqualTo(test);
  }

  public void testAssertWithMessageEq() {
    String test = Boolean.TRUE.toString();
    // BUG: Diagnostic contains: The arguments to isEqualTo method are the same object
    assertWithMessage("msg").that(test).isEqualTo(test);
  }

  public void testAssertThatNeq() {
    String test = Boolean.TRUE.toString();
    // BUG: Diagnostic contains: The arguments to isNotEqualTo method are the same object
    assertThat(test).isNotEqualTo(test);
  }

  public void testAssertWithMessageNeq() {
    String test = Boolean.TRUE.toString();
    // BUG: Diagnostic contains: The arguments to isNotEqualTo method are the same object
    assertWithMessage("msg").that(test).isNotEqualTo(test);
  }
}
{% endhighlight %}

### Negative examples
__TruthSelfEqualsNegativeCases.java__

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

import static com.google.common.truth.Truth.assertThat;

/**
 * Negative test cases for TruthSelfEquals check.
 *
 * @author bhagwani@google.com (Sumit Bhagwani)
 */
public class TruthSelfEqualsNegativeCases {

  public void testEq() {
    assertThat(Boolean.TRUE.toString()).isEqualTo(Boolean.FALSE.toString());
  }

  public void testNeq() {
    assertThat(Boolean.TRUE.toString()).isNotEqualTo(Boolean.FALSE.toString());
  }
}
{% endhighlight %}

