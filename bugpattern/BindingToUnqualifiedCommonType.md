---
title: BindingToUnqualifiedCommonType
summary: This code declares a binding for a common value type without a Qualifier annotation.
layout: bugpattern
tags: ''
severity: WARNING
providesFix: NO_FIX
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
Guice bindings are keyed by a pair of (optional Annotation, Type).

In most cirumstances, one doesn't need the annotation, as there's really just
one active implementation:

```java
bind(CoffeeMaker.class).to(RealCoffeeMaker.class);
...
@Inject Office(CoffeeMaker coffeeMaker) {}
```

However, in other circumstances, you want to bind a simple value (an integer,
String, double, etc.). You should use a Qualifier annotation to allow you to
get the *right* Integer back:

```java
bindConstant().annotatedWith(HttpPort.class).to(80);
...
@Inject MyWebServer(@HttpPort Integer httpPort) {}
```

NOTE: Make sure that your annotation has the [`@Qualifier`] meta-annotation on
it, otherwise injection systems can't see them. Guice users can optionally use
[`@BindingAnnotation`], but Guice also understands `@Qualifier`.

This works great, but if your integer binding *doesn't* include a Qualifier, it
just means that you can ask Guice for "the Integer", and it will give you a
value back:

```java
bind(Integer.class).toInstance(80);
...
@Inject MyWebServer(Integer httpsPort) {}
```

To avoid confusion in these circumstances, please use a Qualifier annotation
when binding simple value types.

[`@Qualifier`]: http://docs.oracle.com/javaee/6/api/javax/inject/Qualifier.html
[`@BindingAnnotation`]: https://github.com/google/guice/wiki/BindingAnnotations

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("BindingToUnqualifiedCommonType")` to the enclosing element.

----------

### Positive examples
__BindingToUnqualifiedCommonTypePositiveCases.java__

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

package com.google.errorprone.bugpatterns.inject.guice.testdata;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

/** Tests for {@code BindingToUnqualifiedCommonType} */
public class BindingToUnqualifiedCommonTypePositiveCases {

  /** Regular module */
  class Module1 extends AbstractModule {
    @Override
    protected void configure() {
      // BUG: Diagnostic contains: BindingToUnqualifiedCommonType
      bind(Integer.class).toInstance(2);

      // BUG: Diagnostic contains: BindingToUnqualifiedCommonType
      bind(String.class).toInstance("Hello");

      // BUG: Diagnostic contains: BindingToUnqualifiedCommonType
      bind(Double.class).toProvider(() -> 42.0);

      // BUG: Diagnostic contains: BindingToUnqualifiedCommonType
      binder().bind(Long.class).toInstance(42L);
    }

    @Provides
    // BUG: Diagnostic contains: BindingToUnqualifiedCommonType
    int providesFoo() {
      return 42;
    }

    @Provides
    // BUG: Diagnostic contains: BindingToUnqualifiedCommonType
    Integer provideBoxedFoo() {
      return 42;
    }

    @Provides
    // BUG: Diagnostic contains: BindingToUnqualifiedCommonType
    String providesGreeting() {
      return "hi";
    }
  }
}
{% endhighlight %}

### Negative examples
__BindingToUnqualifiedCommonTypeNegativeCases.java__

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

package com.google.errorprone.bugpatterns.inject.guice.testdata;

import com.google.common.collect.ImmutableList;
import com.google.inject.AbstractModule;
import com.google.inject.Binder;
import com.google.inject.BindingAnnotation;
import com.google.inject.Module;
import com.google.inject.Provides;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@code BindingToUnqualifiedCommonType} */
public class BindingToUnqualifiedCommonTypeNegativeCases {

  // All of the tagged instances would normally be flagged, but aren't because it's in a JUnit4
  // class
  @RunWith(JUnit4.class)
  static class MyTestClass {
    /** Regular module */
    class Module1 extends AbstractModule {
      @Override
      protected void configure() {
        bind(Integer.class).toInstance(2);
        bind(String.class).toInstance("Hello");
        bind(Double.class).toProvider(() -> 42.0);
        binder().bind(Long.class).toInstance(42L);
      }

      @Provides
      int providesFoo() {
        return 42;
      }

      @Provides
      Integer provideBoxedFoo() {
        return 42;
      }

      @Provides
      String providesGreeting() {
        return "hi";
      }
    }
  }

  /** Regular module */
  class Module1 extends AbstractModule {
    @Override
    protected void configure() {
      // Bindings to unannotated complex instances
      bind(A.class).toInstance(new A());

      // Binding to literals, but with a binding annotation
      bind(Integer.class).annotatedWith(MyBindingAnnotation.class).toInstance(42);
    }

    @Provides
    List<Integer> providesFoo() {
      return ImmutableList.of(42);
    }
  }

  /** implements the Module interface directly */
  class Module2 implements Module {
    @Override
    public void configure(Binder binder) {}

    @Provides
    @MyBindingAnnotation
    int providesFoo() {
      return 42;
    }
  }

  class A {}

  @BindingAnnotation
  @Retention(RetentionPolicy.RUNTIME)
  @interface MyBindingAnnotation {}
}
{% endhighlight %}

