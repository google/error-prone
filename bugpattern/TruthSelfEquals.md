---
title: TruthSelfEquals
summary: isEqualTo should not be used to test an object for equality with itself; the assertion will never fail.
layout: bugpattern
tags: ''
severity: ERROR
providesFix: REQUIRES_HUMAN_ATTENTION
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
If a test subject and the argument to `isEqualTo` are the same instance (e.g.
`assertThat(x).isEqualTo(x)`), then the assertion will always pass. Truth
implements `isEqualTo` using [`Objects#equal`] , which tests its arguments for
reference equality and returns true without calling `equals()` if both arguments
are the same instance.

[`Objects#equals`]: https://google.github.io/guava/releases/21.0/api/docs/com/google/common/base/Objects.html#equal-java.lang.Object-java.lang.Object-

To test the implementation of an `equals` method, use [Guava's
EqualsTester][javadoc].

[javadoc]: http://static.javadoc.io/com.google.guava/guava-testlib/21.0/com/google/common/testing/EqualsTester.html

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("TruthSelfEquals")` to the enclosing element.

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
    // BUG: Diagnostic contains: new EqualsTester().addEqualityGroup(test).testEquals()
    assertThat(test).isEqualTo(test);
  }

  public void testAssertWithMessageEq() {
    String test = Boolean.TRUE.toString();
    // BUG: Diagnostic contains: new EqualsTester().addEqualityGroup(test).testEquals()
    assertWithMessage("msg").that(test).isEqualTo(test);
  }

  public void testAssertThatSame() {
    String test = Boolean.TRUE.toString();
    // BUG: Diagnostic contains: new EqualsTester().addEqualityGroup(test).testEquals()
    assertThat(test).isSameAs(test);
  }

  public void testAssertWithMessageSame() {
    String test = Boolean.TRUE.toString();
    // BUG: Diagnostic contains: new EqualsTester().addEqualityGroup(test).testEquals()
    assertWithMessage("msg").that(test).isSameAs(test);
  }

  public void testAssertThatNeq() {
    String test = Boolean.TRUE.toString();
    // BUG: Diagnostic contains: isNotEqualTo method are the same object
    assertThat(test).isNotEqualTo(test);
  }

  public void testAssertThatNotSame() {
    String test = Boolean.TRUE.toString();
    // BUG: Diagnostic contains: isNotSameAs method are the same object
    assertThat(test).isNotSameAs(test);
  }

  public void testAssertWithMessageNeq() {
    String test = Boolean.TRUE.toString();
    // BUG: Diagnostic contains: isNotEqualTo method are the same object
    assertWithMessage("msg").that(test).isNotEqualTo(test);
  }

  public void testAssertWithMessageNotSame() {
    String test = Boolean.TRUE.toString();
    // BUG: Diagnostic contains: isNotSameAs method are the same object
    assertWithMessage("msg").that(test).isNotSameAs(test);
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

