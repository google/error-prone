---
title: OverridesGuiceInjectableMethod
summary: This method is not annotated with @Inject, but it overrides a method that is annotated with @com.google.inject.Inject. Guice will inject this method, and it is recommended to annotate it explicitly.
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
Unlike with `@javax.inject.Inject`, if a method overrides a method annotated
with `@com.google.inject.Inject`, Guice will inject it even if it itself is not
annotated. This differs from the behavior of methods that override
`@javax.inject.Inject` methods since according to the JSR-330 spec, a method
that overrides a method annotated with `@javax.inject.Inject` will not be
injected unless it iself is annotated with `@Inject`. Because of this
difference, it is recommended that you annotate this method explicitly.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("OverridesGuiceInjectableMethod")` to the enclosing element.

----------

### Positive examples
__OverridesGuiceInjectableMethodPositiveCases.java__

{% highlight java %}
/*
 * Copyright 2013 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.inject.guice.testdata;

/**
 * @author sgoldfeder@google.com (Steven Goldfeder)
 */
public class OverridesGuiceInjectableMethodPositiveCases {

  /**
   * Class with a method foo() annotated with @com.google.inject.Inject.
   */
  public class TestClass1 {
    @com.google.inject.Inject
    public void foo() {}
  }

  /**
   * Class with a method foo() that is not annotated with @Inject, but overrides a method that is
   * annotated with @com.google.inject.Inject
   */
  public class TestClass2 extends TestClass1 {
    // BUG: Diagnostic contains: @Inject
    public void foo() {}
  }

  /**
   * Class with a method foo() that is not annotated with @Inject, but overrides a method that in
   * turn is overrides a method that is annotated with @com.google.inject.Inject
   */
  public class TestClass3 extends TestClass2 {
    // BUG: Diagnostic contains: @Inject
    public void foo() {}
  }

  /**
   * Class with a method foo() that is not annotated with @javax.inject.Inject and overrides a
   * method that is annotated with @com.google.inject.Inject. This class does not contain an error,
   * but it is extended in the next test class.
   */
  public class TestClass4 extends TestClass1 {
    @javax.inject.Inject
    public void foo() {}
  }

  /**
   * Class with a method foo() that is not annotated with @Inject and overrides a method that is is
   * annotated with @javax.inject.Inject. This super method in turn overrides a method that is
   * annoatated with @com.google.inject.Inject.
   */
  public class TestClass5 extends TestClass4 {
    // BUG: Diagnostic contains: @Inject
    public void foo() {}
  }

  /** Class that extends a class with an injected method, but doesn't override it. */
  public class TestClass6 extends TestClass1 {}

  /**
   * Class that extends a class with an injected method, but it was declared in a supertype that
   * isn't a direct supertype.
   */
  public class TestClass7 extends TestClass1 {
    // BUG: Diagnostic contains: @Inject
    public void foo() {}
  }
}
{% endhighlight %}

### Negative examples
__OverridesGuiceInjectableMethodNegativeCases.java__

{% highlight java %}
/*
 * Copyright 2013 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.inject.guice.testdata;

/** @author sgoldfeder@google.com (Steven Goldfeder) */
public class OverridesGuiceInjectableMethodNegativeCases {

  /** Class with a method foo() annotated with @com.google.inject.Inject. */
  public class TestClass1 {
    @com.google.inject.Inject
    public void foo() {}
  }

  /** Class with a method foo() annotated with @javax.inject.Inject. */
  public class TestClass2 {
    @javax.inject.Inject
    public void foo() {}
  }

  /**
   * Class with a method foo() annotated with @javax.inject.Inject that overrides a method annotated
   * with @com.google.inject.Inject.
   */
  public class TestClass3 extends TestClass1 {
    @javax.inject.Inject
    public void foo() {}
  }

  /**
   * Class with a method foo() annotated with @com.google.inject.Inject that overrides a method
   * annoted with @javax.inject.Inject.
   */
  public class TestClass4 extends TestClass2 {
    @com.google.inject.Inject
    public void foo() {}
  }

  /**
   * Class with a method foo() annotated with @javax.inject.Inject that overrides a method annotated
   * with @com.google.inject.Inject
   */
  public class TestClass5 extends TestClass1 {
    @javax.inject.Inject
    public void foo() {}
  }

  /**
   * Class with a method foo() that is not annotated with @Inject, but overrides a method that is
   * annotated with @com.google.inject.Inject. Warning is suppressed.
   */
  public class TestClass6 extends TestClass1 {
    @SuppressWarnings("OverridesGuiceInjectableMethod")
    @Override
    public void foo() {}
  }

  /** Class that extends a class with an injected method, but doesn't override it. */
  public class TestClass7 extends TestClass1 {}
}
{% endhighlight %}

