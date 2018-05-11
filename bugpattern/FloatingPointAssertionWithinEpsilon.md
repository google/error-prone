---
title: FloatingPointAssertionWithinEpsilon
summary: This fuzzy equality check is using a tolerance less than the gap to the next number. You may want a less restrictive tolerance, or to assert equality.
layout: bugpattern
tags: Simplification
severity: WARNING
providesFix: REQUIRES_HUMAN_ATTENTION
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
Both JUnit and Truth allow for asserting equality of floating point numbers with
an absolute tolerance. For example, the following statements are equivalent,

```java
double EPSILON = 1e-20;
assertThat(actualValue).isWithin(EPSILON).of(Math.PI);
assertEquals(Math.PI, actualValue, EPSILON);
```

What's not immediately obvious is that both of these assertions are checking
exact equality between `Math.PI` and `actualValue`, because the next `double`
after `Math.PI` is `Math.PI + 4.44e-16`.

This means that using the same tolerance to compare several floating point
values with different magnitude can be prone to error,

```java
float TOLERANCE = 1e-5f;
assertThat(pressure).isWithin(TOLERANCE).of(1f); // GOOD
assertThat(pressure).isWithin(TOLERANCE).of(10f); // GOOD
assertThat(pressure).isWithin(TOLERANCE).of(100f); // BAD -- misleading equals check
```

A larger tolerance should be used if the goal of the test is to allow for some
floating point errors, or, if not, `isEqualTo` makes the intention more clear.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("FloatingPointAssertionWithinEpsilon")` to the enclosing element.

----------

### Positive examples
__FloatingPointAssertionWithinEpsilonPositiveCases.java__

{% highlight java %}
/*
 * Copyright 2018 The Error Prone Authors.
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
import static org.junit.Assert.assertEquals;

/**
 * Positive test cases for FloatingPointAssertionWithinEpsilon check.
 *
 * @author ghm@google.com (Graeme Morgan)
 */
final class FloatingPointAssertionWithinEpsilonPositiveCases {

  private static final float TOLERANCE = 1e-10f;
  private static final double TOLERANCE2 = 1e-20f;
  private static final float VALUE = 1;

  public void testFloat() {
    // BUG: Diagnostic contains: 6.0e-08
    assertThat(1.0f).isWithin(1e-20f).of(1.0f);
    // BUG: Diagnostic contains: 6.0e-08
    assertThat(1f).isWithin(TOLERANCE).of(VALUE);
    // BUG: Diagnostic contains: 1.0e+03
    assertThat(1e10f).isWithin(1).of(1e10f);

    // BUG: Diagnostic contains: 1.2e-07
    assertThat(1f).isNotWithin(1e-10f).of(2);

    // BUG: Diagnostic contains: 6.0e-08
    assertEquals(1f, 1f, TOLERANCE);
    // BUG: Diagnostic contains: 6.0e-08
    assertEquals("equal!", 1f, 1f, TOLERANCE);
  }

  public void testDouble() {
    // BUG: Diagnostic contains: 1.1e-16
    assertThat(1.0).isWithin(1e-20).of(1.0);
    // BUG: Diagnostic contains: 1.1e-16
    assertThat(1.0).isWithin(TOLERANCE2).of(1.0f);
    // BUG: Diagnostic contains: 1.1e-16
    assertThat(1.0).isWithin(TOLERANCE2).of(1);
    // BUG: Diagnostic contains: 1.6e+04
    assertThat(1e20).isWithin(1).of(1e20);

    // BUG: Diagnostic contains: 1.1e-16
    assertEquals(1.0, 1.0, TOLERANCE2);
    // BUG: Diagnostic contains: 1.1e-16
    assertEquals("equal!", 1.0, 1.0, TOLERANCE2);
  }
}
{% endhighlight %}

__FloatingPointAssertionWithinEpsilonPositiveCases_expected.java__

{% highlight java %}
/*
 * Copyright 2018 The Error Prone Authors.
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
import static org.junit.Assert.assertEquals;

/**
 * Expected refactoring output for FloatingPointAssertionWithinEpsilon bugpattern.
 *
 * @author ghm@google.com (Graeme Morgan)
 */
final class FloatingPointAssertionWithinEpsilonPositiveCases {

  private static final float TOLERANCE = 1e-10f;
  private static final double TOLERANCE2 = 1e-20f;
  private static final float VALUE = 1;

  public void testFloat() {
    assertThat(1.0f).isEqualTo(1.0f);
    assertThat(1f).isEqualTo(VALUE);
    assertThat(1e10f).isEqualTo(1e10f);
    assertThat(1f).isNotEqualTo(2f);
    assertEquals(1f, 1f, 0);
    assertEquals("equal!", 1f, 1f, 0);
  }

  public void testDouble() {
    assertThat(1.0).isEqualTo(1.0);
    assertThat(1.0).isEqualTo(1.0);
    assertThat(1.0).isEqualTo(1d);
    assertThat(1e20).isEqualTo(1e20);
    assertEquals(1.0, 1.0, 0);
    assertEquals("equal!", 1.0, 1.0, 0);
  }
}
{% endhighlight %}

### Negative examples
__FloatingPointAssertionWithinEpsilonNegativeCases.java__

{% highlight java %}
/*
 * Copyright 2018 The Error Prone Authors.
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
import static org.junit.Assert.assertEquals;

/**
 * Negative test cases for FloatingPointAssertionWithinEpsilon check.
 *
 * @author ghm@google.com (Graeme Morgan)
 */
final class FloatingPointAssertionWithinEpsilonNegativeCases {

  private static final float TOLERANCE = 1e-5f;
  private static final double TOLERANCE2 = 1e-10f;
  private static final float VALUE = 1;

  public void testFloat() {
    String test = Boolean.TRUE.toString();
    assertThat(1.0f).isWithin(1e-5f).of(1.0f);
    assertThat(1f).isWithin(TOLERANCE).of(VALUE);
    assertThat(1f).isWithin(1).of(1);

    assertThat(1f).isNotWithin(0).of(2f);

    assertThat(1f).isNotWithin(.5f).of(2f);

    assertEquals(1f, 1f, TOLERANCE);
  }

  public void testDouble() {
    String test = Boolean.TRUE.toString();
    assertThat(1.0).isWithin(1e-10).of(1.0);
    assertThat(1.0).isWithin(TOLERANCE2).of(1f);
    assertThat(1.0).isWithin(TOLERANCE2).of(1);

    assertEquals(1.0, 1.0, TOLERANCE);
  }

  public void testZeroCases() {
    assertThat(1.0).isWithin(0.0).of(1.0);
    assertThat(1f).isWithin(0f).of(1f);
    assertThat(1f).isWithin(0).of(1f);

    assertEquals(1f, 1f, 0f);
  }
}
{% endhighlight %}

