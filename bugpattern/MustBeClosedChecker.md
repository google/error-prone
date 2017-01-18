---
title: MustBeClosedChecker
summary: Invocations of methods or constructors annotated with @MustBeClosed must occur within the resource variable initializer of a try-with-resources statement.
layout: bugpattern
category: JDK
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
Methods or constructors annotated with @MustBeClosed require that the returned resource is closed. This is enforced by checking that invocations occur within the resource variable initializer of a try-with-resources statement.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("MustBeClosedChecker")` annotation to the enclosing element.

----------

### Positive examples
__MustBeClosedCheckerPositiveCases.java__

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

import com.google.errorprone.annotations.MustBeClosed;

public class MustBeClosedCheckerPositiveCases {

  class DoesNotImplementAutoCloseable {

    @MustBeClosed
    // BUG: Diagnostic contains: MustBeClosed should only annotate constructors of AutoCloseables.
    DoesNotImplementAutoCloseable() {}

    @MustBeClosed
    // BUG: Diagnostic contains: MustBeClosed should only annotate methods that return an
    // AutoCloseable.
    void doesNotReturnAutoCloseable() {}
  }

  class Closeable implements AutoCloseable {

    @Override
    public void close() {}
  }

  class Foo {

    @MustBeClosed
    Closeable mustBeClosedAnnotatedMethod() {
      return new Closeable();
    }
  }

  class MustBeClosedAnnotatedConstructor extends Closeable {

    @MustBeClosed
    MustBeClosedAnnotatedConstructor() {}
  }

  static interface Lambda {
    Closeable expression();
  }

  void positiveCase1() {
    // BUG: Diagnostic contains: mustBeClosedAnnotatedMethod must be called within the resource
    // variable initializer of a try-with-resources statement.
    new Foo().mustBeClosedAnnotatedMethod();
  }

  void positiveCase2() {
    // BUG: Diagnostic contains: mustBeClosedAnnotatedMethod must be called within the resource
    // variable initializer of a try-with-resources statement.
    Closeable closeable = new Foo().mustBeClosedAnnotatedMethod();
  }

  void positiveCase3() {
    try {
      // BUG: Diagnostic contains: mustBeClosedAnnotatedMethod must be called within the resource
      // variable initializer of a try-with-resources statement.
      new Foo().mustBeClosedAnnotatedMethod();
    } finally {
    }
  }

  void positiveCase4() {
    try (Closeable closeable = new Foo().mustBeClosedAnnotatedMethod()) {
      // BUG: Diagnostic contains: mustBeClosedAnnotatedMethod must be called within the resource
      // variable initializer of a try-with-resources statement.
      new Foo().mustBeClosedAnnotatedMethod();
    }
  }

  void positiveCase5() {
    // BUG: Diagnostic contains: Constructor must be called within the resource variable initializer
    // of a try-with-resources statement.
    new MustBeClosedAnnotatedConstructor();
  }

  Closeable positiveCase6() {
    // BUG: Diagnostic contains: Constructor must be called within the resource variable initializer
    // of a try-with-resources statement.
    return new MustBeClosedAnnotatedConstructor();
  }

  Closeable positiveCase7() {
    // BUG: Diagnostic contains: mustBeClosedAnnotatedMethod must be called within the resource
    // variable initializer of a try-with-resources statement.
    return new Foo().mustBeClosedAnnotatedMethod();
  }

  void positiveCase8() {
    Lambda expression =
        () -> {
          // BUG: Diagnostic contains: mustBeClosedAnnotatedMethod must be called within the resource
          // variable initializer of a try-with-resources statement.
          return new Foo().mustBeClosedAnnotatedMethod();
        };
  }

  void positiveCase9() {
    new Foo() {
      @Override
      public Closeable mustBeClosedAnnotatedMethod() {
        // BUG: Diagnostic contains: Constructor must be called within the resource variable
        // initializer of a try-with-resources statement.
        return new MustBeClosedAnnotatedConstructor();
      }
    };
  }
}
{% endhighlight %}

### Negative examples
__MustBeClosedCheckerNegativeCases.java__

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

import com.google.errorprone.annotations.MustBeClosed;

public class MustBeClosedCheckerNegativeCases {

  class Closeable implements AutoCloseable {

    @Override
    public void close() {}
  }

  class Foo {

    void bar() {}

    @MustBeClosed
    Closeable mustBeClosedAnnotatedMethod() {
      return new Closeable();
    }

    void negativeCase1() {
      // This is fine since it is called from the same class that annotates the method.
      mustBeClosedAnnotatedMethod();
    }
  }

  class MustBeClosedAnnotatedConstructor extends Closeable {

    @MustBeClosed
    MustBeClosedAnnotatedConstructor() {}

    void negativeCase2() {
      // This is fine since it is called from the same class that annotates the constructor.
      new MustBeClosedAnnotatedConstructor();
    }
  }

  void negativeCase3() {
    try (Closeable closeable = new Foo().mustBeClosedAnnotatedMethod()) {}
  }

  void negativeCase4() {
    Foo foo = new Foo();
    try (Closeable closeable = foo.mustBeClosedAnnotatedMethod()) {}
  }

  void negativeCase5() {
    new Foo().bar();
  }

  void negativeCase6() {
    try (MustBeClosedAnnotatedConstructor foo = new MustBeClosedAnnotatedConstructor()) {}
  }

  void negativeCase7() {
    try (MustBeClosedAnnotatedConstructor foo = new MustBeClosedAnnotatedConstructor();
        Closeable closeable = new Foo().mustBeClosedAnnotatedMethod()) {}
  }

  @MustBeClosed
  Closeable positiveCase8() {
    // This is fine since the caller method is annotated.
    return new MustBeClosedAnnotatedConstructor();
  }

  @MustBeClosed
  Closeable positiveCase7() {
    // This is fine since the caller method is annotated.
    return new Foo().mustBeClosedAnnotatedMethod();
  }
}
{% endhighlight %}

