---
title: TimeUnitMismatch
summary: An value that appears to be represented in one unit is used where another appears to be required (e.g., seconds where nanos are needed)
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem


## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("TimeUnitMismatch")` to the enclosing element.

----------

### Positive examples
__TimeUnitMismatchPositiveCases.java__

{% highlight java %}
/*
 * Copyright 2014 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.time.testdata;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

import java.util.Optional;

/** @author cpovirk@google.com (Chris Povirk) */
public class TimeUnitMismatchPositiveCases {
  int startMicros;
  int stopMillis;

  void fields() {
    // BUG: Diagnostic contains: expected microseconds but was milliseconds
    startMicros = stopMillis;

    // BUG: Diagnostic contains: If it instead means microseconds
    startMicros = stopMillis;

    // BUG: Diagnostic contains: MILLISECONDS.toMicros(stopMillis)
    startMicros = stopMillis;
  }

  void memberSelect() {
    // BUG: Diagnostic contains: expected microseconds but was milliseconds
    this.startMicros = this.stopMillis;
  }

  void locals() {
    int millis = 0;
    // BUG: Diagnostic contains: expected microseconds but was milliseconds
    startMicros = millis;
  }

  long getMicros() {
    return 0;
  }

  void returns() {
    // BUG: Diagnostic contains: expected nanoseconds but was microseconds
    long fooNano = getMicros();
  }

  void doSomething(double startSec, double endSec) {}

  void setMyMillis(int timeout) {}

  void args() {
    double ms = 0;
    double ns = 0;
    // BUG: Diagnostic contains: expected seconds but was milliseconds
    doSomething(ms, ns);
    // BUG: Diagnostic contains: expected seconds but was nanoseconds
    doSomething(ms, ns);

    // BUG: Diagnostic contains: expected milliseconds but was nanoseconds
    setMyMillis((int) ns);
  }

  void timeUnit() {
    int micros = 0;
    // BUG: Diagnostic contains: expected nanoseconds but was microseconds
    NANOSECONDS.toMillis(micros);
  }

  class Foo {
    Foo(long seconds) {}
  }

  void constructor() {
    int nanos = 0;
    // BUG: Diagnostic contains: expected seconds but was nanoseconds
    new Foo(nanos);
  }

  void boxed() {
    Long nanos = 0L;
    // BUG: Diagnostic contains: expected milliseconds but was nanoseconds
    long millis = nanos;
  }

  void optionalGet() {
    Optional<Long> maybeNanos = Optional.of(0L);
    // BUG: Diagnostic contains: expected milliseconds but was nanoseconds
    long millis = maybeNanos.get();
  }
}
{% endhighlight %}

### Negative examples
__TimeUnitMismatchNegativeCases.java__

{% highlight java %}
/*
 * Copyright 2014 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.time.testdata;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

import java.util.Optional;

/** @author cpovirk@google.com (Chris Povirk) */
public class TimeUnitMismatchNegativeCases {
  static final int THE_MILLIS = 0;
  int startMillis;
  int stopMillis;

  void fields() {
    startMillis = THE_MILLIS;

    startMillis = stopMillis;
  }

  void memberSelect() {
    this.startMillis = this.stopMillis;
  }

  void locals() {
    int millis = 0;
    startMillis = millis;
  }

  long getMicros() {
    return 0;
  }

  void returns() {
    long fooUs = getMicros();
  }

  void doSomething(double startSec, double endSec) {}

  void args() {
    double seconds = 0;
    doSomething(seconds, seconds);
  }

  void timeUnit() {
    int nanos = 0;
    NANOSECONDS.toMillis(nanos);
  }

  class Foo {
    Foo(long seconds) {}
  }

  void constructor() {
    int seconds = 0;
    new Foo(seconds);
  }

  String milliseconds() {
    return "0";
  }

  void nonNumeric() {
    String seconds = milliseconds();
  }

  void boxed() {
    Long startNanos = 0L;
    long endNanos = startNanos;
  }

  void optionalGet() {
    Optional<Long> maybeNanos = Optional.of(0L);
    long nanos = maybeNanos.get();
  }
}
{% endhighlight %}

