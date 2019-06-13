---
title: ChainedAssertionLosesContext
summary: Inside a Subject, use check(...) instead of assert*() to preserve user-supplied messages and other settings.
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
Assertions made _inside the implementation of another [Truth] assertion_ should
use [`check`], not `assertThat`.

Before:

```
class MyProtoSubject {
  public void hasFoo(Foo expected) {
    assertThat(actual.foo()).isEqualTo(expected);
  }
}
```

After:

```
class MyProtoSubject {
  public void hasFoo(Foo expected) {
    check("foo()").that(actual.foo()).isEqualTo(expected);
  }
}
```

Benefits of `check` include:

-   When the assertion fails, the failure message includes more context: The
    message specifies that the assertion was performed on `myProto.foo()`, and
    it includes the full value of `myProto` for reference.
-   If the user of the assertion called `assertWithMessage`, that message, which
    is lost in the `assertThat` version, is shown by the `check` version.
-   `check` makes it possible to test the assertion with [`ExpectFailure`] and
    to use [`Expect`] or [`assume`]. `assertThat`, by contrast, overrides any
    user-specified failure behavior.

[Truth]: https://github.com/google/truth
[`check`]: https://google.github.io/truth/api/latest/com/google/common/truth/Subject.html#check-java.lang.String-java.lang.Object...-
[`ExpectFailure`]: https://google.github.io/truth/api/latest/com/google/common/truth/ExpectFailure.html
[`Expect`]: https://google.github.io/truth/api/latest/com/google/common/truth/Expect.html
[`assume`]: https://google.github.io/truth/api/latest/com/google/common/truth/TruthJUnit.html#assume--

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("ChainedAssertionLosesContext")` to the enclosing element.

----------

### Positive examples
__ChainedAssertionLosesContextPositiveCases.java__

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

import static com.google.common.truth.Truth.assertAbout;
import static com.google.common.truth.Truth.assertWithMessage;
import static com.google.common.truth.Truth.assert_;

import com.google.common.truth.FailureMetadata;
import com.google.common.truth.Subject;
import com.google.common.truth.Truth;

/** @author cpovirk@google.com (Chris Povirk) */
public class ChainedAssertionLosesContextPositiveCases {
  static final class FooSubject extends Subject {
    private final Foo actual;

    static Factory<FooSubject, Foo> foos() {
      return FooSubject::new;
    }

    static FooSubject assertThat(Foo foo) {
      return assertAbout(foos()).that(foo);
    }

    private FooSubject(FailureMetadata metadata, Foo actual) {
      super(metadata, actual);
      this.actual = actual;
    }

    void hasString(String expected) {
      // BUG: Diagnostic contains: check("string()").that(actual.string()).isEqualTo(expected)
      Truth.assertThat(actual.string()).isEqualTo(expected);
    }

    void hasOtherFooInteger(int expected) {
      // BUG: Diagnostic contains:
      // check("otherFoo().integer()").that(actual.otherFoo().integer()).isEqualTo(expected)
      Truth.assertThat(actual.otherFoo().integer()).isEqualTo(expected);
    }

    FooSubject otherFooAbout() {
      // BUG: Diagnostic contains: check("otherFoo()").about(foos()).that(actual.otherFoo())
      return assertAbout(foos()).that(actual.otherFoo());
    }

    FooSubject otherFooThat() {
      // BUG: Diagnostic contains: check("otherFoo()").about(foos()).that(actual.otherFoo())
      return assertThat(actual.otherFoo());
    }

    void withMessage(String expected) {
      // BUG: Diagnostic contains:
      // check("string()").withMessage("blah").that(actual.string()).isEqualTo(expected)
      assertWithMessage("blah").that(actual.string()).isEqualTo(expected);
    }

    void withMessageWithArgs(String expected) {
      // BUG: Diagnostic contains:
      // check("string()").withMessage("%s", "blah").that(actual.string()).isEqualTo(expected)
      assertWithMessage("%s", "blah").that(actual.string()).isEqualTo(expected);
    }

    void plainAssert(String expected) {
      // BUG: Diagnostic contains:
      // check("string()").that(actual.string()).isEqualTo(expected)
      assert_().that(actual.string()).isEqualTo(expected);
    }
  }

  private static final class Foo {
    final String string;
    final int integer;

    Foo(String string, int integer) {
      this.string = string;
      this.integer = integer;
    }

    String string() {
      return string;
    }

    int integer() {
      return integer;
    }

    Foo otherFoo() {
      return this;
    }
  }
}
{% endhighlight %}

### Negative examples
__ChainedAssertionLosesContextNegativeCases.java__

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

import static com.google.common.truth.Truth.assertAbout;
import static com.google.common.truth.Truth.assertThat;

import com.google.common.truth.FailureMetadata;
import com.google.common.truth.Subject;

/** @author cpovirk@google.com (Chris Povirk) */
public class ChainedAssertionLosesContextNegativeCases {
  static final class FooSubject extends Subject {
    private final Foo actual;

    private FooSubject(FailureMetadata metadata, Foo actual) {
      super(metadata, actual);
      this.actual = actual;
    }

    static Factory<FooSubject, Foo> foos() {
      return FooSubject::new;
    }

    static FooSubject assertThat(Foo foo) {
      return assertAbout(foos()).that(foo);
    }
  }

  void someTestMethod() {
    assertThat("").isNotNull();
  }

  private static final class Foo {
    final String string;
    final int integer;

    Foo(String string, int integer) {
      this.string = string;
      this.integer = integer;
    }

    String string() {
      return string;
    }

    int integer() {
      return integer;
    }

    Foo otherFoo() {
      return this;
    }
  }
}
{% endhighlight %}

