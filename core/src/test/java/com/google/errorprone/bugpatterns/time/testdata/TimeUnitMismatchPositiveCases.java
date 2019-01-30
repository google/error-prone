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

  void args() {
    double ms = 0;
    double ns = 0;
    // BUG: Diagnostic contains: expected seconds but was milliseconds
    doSomething(ms, ns);
    // BUG: Diagnostic contains: expected seconds but was nanoseconds
    doSomething(ms, ns);
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
