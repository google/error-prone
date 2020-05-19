---
title: ImplementAssertionWithChaining
summary: Prefer check(...), which usually generates more readable failure messages.
layout: bugpattern
tags: ''
severity: SUGGESTION
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem


## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("ImplementAssertionWithChaining")` to the enclosing element.


----------

### Positive examples
__ImplementAssertionWithChainingPositiveCases.java__

{% highlight java %}
/*
 * Copyright 2019 The Error Prone Authors.
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

import com.google.common.truth.FailureMetadata;
import com.google.common.truth.Subject;

/** @author cpovirk@google.com (Chris Povirk) */
public class ImplementAssertionWithChainingPositiveCases {
  static final class FooSubject extends Subject {
    private final Foo actual;

    private FooSubject(FailureMetadata metadata, Foo actual) {
      super(metadata, actual);
      this.actual = actual;
    }

    void hasString(String expected) {
      // BUG: Diagnostic contains: check("string()").that(actual.string()).isEqualTo(expected)
      if (!actual.string().equals(expected)) {
        failWithActual("expected to have string", expected);
      }
    }

    void hasStringGuavaObjectsEqual(String expected) {
      // BUG: Diagnostic contains: check("string()").that(actual.string()).isEqualTo(expected)
      if (!com.google.common.base.Objects.equal(actual.string(), expected)) {
        failWithActual("expected to have string", expected);
      }
    }

    void hasStringJavaObjectsEquals(String expected) {
      // BUG: Diagnostic contains: check("string()").that(actual.string()).isEqualTo(expected)
      if (!java.util.Objects.equals(actual.string(), expected)) {
        failWithActual("expected to have string", expected);
      }
    }

    void hasInteger(int expected) {
      // BUG: Diagnostic contains: check("integer()").that(actual.integer()).isEqualTo(expected)
      if (actual.integer() != expected) {
        failWithActual("expected to have integer", expected);
      }
    }

    void hasKind(Kind expected) {
      // BUG: Diagnostic contains: check("kind()").that(actual.kind()).isEqualTo(expected)
      if (actual.kind() != expected) {
        failWithActual("expected to have kind", expected);
      }
    }

    void hasOtherFooInteger(int expected) {
      // BUG: Diagnostic contains:
      // check("otherFoo().integer()").that(actual.otherFoo().integer()).isEqualTo(expected)
      if (actual.otherFoo().integer() != expected) {
        failWithActual("expected to have other foo with integer", expected);
      }
    }
  }

  private static final class Foo {
    final String string;
    final int integer;
    final Kind kind;

    Foo(String string, int integer, Kind kind) {
      this.string = string;
      this.integer = integer;
      this.kind = kind;
    }

    String string() {
      return string;
    }

    int integer() {
      return integer;
    }

    Kind kind() {
      return kind;
    }

    Foo otherFoo() {
      return this;
    }
  }

  private enum Kind {}
}
{% endhighlight %}

### Negative examples
__ImplementAssertionWithChainingNegativeCases.java__

{% highlight java %}
/*
 * Copyright 2019 The Error Prone Authors.
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

import com.google.common.truth.FailureMetadata;
import com.google.common.truth.Subject;

/** @author cpovirk@google.com (Chris Povirk) */
public class ImplementAssertionWithChainingNegativeCases {
  static final class FooSubject extends Subject {
    private final Foo actual;

    private FooSubject(FailureMetadata metadata, Foo actual) {
      super(metadata, actual);
      this.actual = actual;
    }

    void doesNotHaveString(String other) {
      if (actual.string().equals(other)) {
        failWithActual("expected not to have string", other);
      }
    }

    void doesNotHaveInteger(int other) {
      if (actual.integer() == other) {
        failWithActual("expected not to have integer", other);
      }
    }

    void hasBoxedIntegerSameInstance(Integer expected) {
      if (actual.boxedInteger() != expected) {
        failWithActual("expected to have boxed integer", expected);
      }
    }
  }

  private static final class Foo {
    final String string;
    final int integer;
    final Integer boxedInteger;

    Foo(String string, int integer, Integer boxedInteger) {
      this.string = string;
      this.integer = integer;
      this.boxedInteger = boxedInteger;
    }

    String string() {
      return string;
    }

    int integer() {
      return integer;
    }

    Integer boxedInteger() {
      return boxedInteger;
    }

    Foo otherFoo() {
      return this;
    }
  }
}
{% endhighlight %}

