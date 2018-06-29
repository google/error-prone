---
title: TruthAssertExpected
summary: The actual and expected values appear to be swapped, which results in poor assertion failure messages. The actual value should come first.
layout: bugpattern
tags: Style
severity: WARNING
providesFix: REQUIRES_HUMAN_ATTENTION
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
Arguments to a fluent [Truth](go/truth) assertion appear to be reversed based on
the argument names.

```java {.bad}
  int expected = 1;
  assertThat(expected).isEqualTo(codeUnderTest());
```

This is problematic as the quality of Truth's error message depends on the
argument order. If `codeUnderTest()` returns `2`, this code will output:

```
expected: 2
but was : 1
```

Which will likely make debugging the problem harder. Truth assertions should
follow the opposite order to JUnit assertions. Compare:

```java {.good}
  assertThat(actual).isEqualTo(expected);
  assertEquals(expected, actual);
```

See https://google.github.io/truth/faq#order for more details.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("TruthAssertExpected")` to the enclosing element.

----------

### Positive examples
__TruthAssertExpectedPositiveCases.java__

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
import static com.google.common.truth.Truth.assertWithMessage;

import com.google.common.collect.ImmutableList;

/**
 * Positive test cases for TruthAssertExpected check.
 *
 * @author ghm@google.com (Graeme Morgan)
 */
final class TruthAssertExpectedPositiveCases {
  private static final ImmutableList<Object> EXPECTED_LIST = ImmutableList.of();
  private static final float EXPECTED_FLOAT = 1f;

  private float actualFloat() {
    return 3.14f;
  }

  private void simple() {
    Object expected = new Object();
    Object actual = new Object();
    Object foo = new Object();
    // BUG: Diagnostic contains: assertThat(foo).isEqualTo(expected)
    assertThat(expected).isEqualTo(foo);
    // BUG: Diagnostic contains: assertThat(foo).isNotEqualTo(expected)
    assertThat(expected).isNotEqualTo(foo);

    // BUG: Diagnostic contains: assertWithMessage("reversed!").that(actual).isEqualTo(expected)
    assertWithMessage("reversed!").that(expected).isEqualTo(actual);

    // BUG: Diagnostic contains: assertThat(actual.hashCode()).isEqualTo(expected.hashCode())
    assertThat(expected.hashCode()).isEqualTo(actual.hashCode());
  }

  private void tolerantFloats() {
    // BUG: Diagnostic contains: assertThat(actualFloat()).isWithin(1f).of(EXPECTED_FLOAT)
    assertThat(EXPECTED_FLOAT).isWithin(1f).of(actualFloat());
  }

  private void lists() {
    // BUG: Diagnostic contains:
    // assertThat(ImmutableList.of(this)).containsExactlyElementsIn(EXPECTED_LIST);
    assertThat(EXPECTED_LIST).containsExactlyElementsIn(ImmutableList.of(this));
    // BUG: Diagnostic contains:
    // assertThat(ImmutableList.of(this)).containsExactlyElementsIn(EXPECTED_LIST).inOrder();
    assertThat(EXPECTED_LIST).containsExactlyElementsIn(ImmutableList.of(this)).inOrder();
  }
}
{% endhighlight %}

### Negative examples
__TruthAssertExpectedNegativeCases.java__

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

import com.google.common.collect.ImmutableList;

/**
 * Negative test cases for TruthAssertExpected check.
 *
 * @author ghm@google.com (Graeme Morgan)
 */
final class TruthAssertExpectedNegativeCases {
  private static final Object expected = new Object();
  private static final Object actual = new Object();
  private static final Object foo = new Object();
  private static final long CONSTANT = 1L;

  private enum Enum {
    A,
    B;
  }

  private void simple() {
    assertThat(foo).isEqualTo(expected);

    assertThat(expected.hashCode()).isEqualTo(expected.hashCode());
    assertThat(hashCode()).isEqualTo(foo);
  }

  private void expectedExceptions() {
    Exception expectedException = new Exception("Oh no.");
    assertThat(expectedException).hasMessageThat().isEqualTo("Oh no.");
    assertThat(expectedException.getClass()).isEqualTo(hashCode());
  }

  private void staticFactoryMethod() {
    assertThat(expected).isEqualTo(Long.valueOf(10L));
    assertThat(expected).isEqualTo(ImmutableList.of(1));
  }

  private void constantValues() {
    assertThat(expected).isEqualTo(Enum.A);
    assertThat(expected).isEqualTo(10L);
    assertThat(expected).isEqualTo(CONSTANT);
  }
}
{% endhighlight %}

