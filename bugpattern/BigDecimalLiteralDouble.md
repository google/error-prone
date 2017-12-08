---
title: BigDecimalLiteralDouble
summary: BigDecimal(double) and BigDecimal.valueOf(double) may lose precision, prefer BigDecimal(String) or BigDecimal(long)
layout: bugpattern
tags: ''
severity: WARNING
providesFix: REQUIRES_HUMAN_ATTENTION
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
BigDecimal has a two mechanisms for converting a double into a BigDecimal, new
BigDecimal(double), and BigDecimal.valueOf(double). These methods are a possible
source of precision loss if the number does not have an exact double
representation. The new BigDecimal(String) and new BigDecimal(long) constructors
should be prefered, as they do not require using a lossy argument.

For example `0.1` cannot be exactly represented a double. Thus
`new BigDecimal(.1)` represents the same bignum as
`new BigDecimal("0.1000000000000000055511151231257827021181583404541015625")`
and not `new BigDecimal(".1").

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("BigDecimalLiteralDouble")` to the enclosing element.

----------

### Positive examples
__BigDecimalLiteralDoublePositiveCases.java__

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

import java.math.BigDecimal;

/** @author endobson@google.com (Eric Dobson) */
public class BigDecimalLiteralDoublePositiveCases {

  public void foo() {
    // BUG: Diagnostic contains: BigDecimal.ZERO
    new BigDecimal(0.0);

    // BUG: Diagnostic contains: new BigDecimal("1.0")
    BigDecimal.valueOf(1.0);

    // BUG: Diagnostic contains: BigDecimal.ONE
    new BigDecimal(1.0);

    // BUG: Diagnostic contains: BigDecimal.TEN
    new BigDecimal(10.0);

    // BUG: Diagnostic contains: new BigDecimal(99L)
    new BigDecimal(99.0);

    // BUG: Diagnostic contains: new BigDecimal(123456L)
    new BigDecimal(123_456.0);

    // BUG: Diagnostic contains: new BigDecimal(".045")
    BigDecimal.valueOf(.045);

    // BUG: Diagnostic contains: new BigDecimal("123456.0E-4")
    new BigDecimal(123456.0E-4);

    // BUG: Diagnostic contains: new BigDecimal("123456.0")
    BigDecimal.valueOf(123456.0D);

    // BUG: Diagnostic contains: new BigDecimal("-0.012")
    new BigDecimal(-0.012);

    // BUG: Diagnostic contains: new BigDecimal("+.034")
    BigDecimal.valueOf(+.034);
  }
}
{% endhighlight %}

### Negative examples
__BigDecimalLiteralDoubleNegativeCases.java__

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

import java.math.BigDecimal;

/** @author endobson@google.com (Eric Dobson) */
public class BigDecimalLiteralDoubleNegativeCases {

  public void foo() {
    new BigDecimal("99");
    new BigDecimal("99.0");
    new BigDecimal(123_459);
    new BigDecimal(123_456L);
    BigDecimal.valueOf(123);
    BigDecimal.valueOf(123L);
  }
}
{% endhighlight %}

