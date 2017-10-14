---
title: TruthConstantAsserts
summary: Truth Library assert is called on a constant.
layout: bugpattern
tags: FragileCode
severity: WARNING
providesFix: NO_FIX
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
The arguments to assertThat method is a constant. It should be a variable or a
method invocation. For eg. switch assertThat(1).isEqualTo(methodCall())
to assertThat(methodCall()).isEqualTo(1).

## Suppression
Suppress false positives by adding an `@SuppressWarnings("TruthConstantAsserts")` annotation to the enclosing element.

----------

### Positive examples
__TruthConstantAssertsPositiveCases.java__

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
 * Positive test cases for TruthConstantAsserts check.
 *
 * @author bhagwani@google.com (Sumit Bhagwani)
 */
public class TruthConstantAssertsPositiveCases {

  public void testAssertThat() {
    // BUG: Diagnostic contains: assertThat(new TruthConstantAssertsPositiveCases()).isEqualTo(1);
    assertThat(1).isEqualTo(new TruthConstantAssertsPositiveCases());

    // BUG: Diagnostic contains: assertThat(someStaticMethod()).isEqualTo("my string");
    assertThat("my string").isEqualTo(someStaticMethod());

    // BUG: Diagnostic contains: assertThat(memberMethod()).isEqualTo(42);
    assertThat(42).isEqualTo(memberMethod());

    // BUG: Diagnostic contains: assertThat(someStaticMethod()).isEqualTo(42L);
    assertThat(42L).isEqualTo(someStaticMethod());

    // BUG: Diagnostic contains: assertThat(new Object()).isEqualTo(4.2);
    assertThat(4.2).isEqualTo(new Object());
  }

  private static TruthConstantAssertsPositiveCases someStaticMethod() {
    return new TruthConstantAssertsPositiveCases();
  }

  private TruthConstantAssertsPositiveCases memberMethod() {
    return new TruthConstantAssertsPositiveCases();
  }
}
{% endhighlight %}

### Negative examples
__TruthConstantAssertsNegativeCases.java__

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
 * Negative test cases for TruthConstantAsserts check.
 *
 * @author bhagwani@google.com (Sumit Bhagwani)
 */
public class TruthConstantAssertsNegativeCases {

  public void testNegativeCases() {
    assertThat(new TruthConstantAssertsNegativeCases()).isEqualTo(Boolean.TRUE);
    assertThat(getObject()).isEqualTo(Boolean.TRUE);

    // assertion called on constant with constant expectation is ignored.
    assertThat(Boolean.FALSE).isEqualTo(4.2);
  }

  private static TruthConstantAssertsNegativeCases getObject() {
    return new TruthConstantAssertsNegativeCases();
  }
}
{% endhighlight %}

