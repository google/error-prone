---
title: OverridesJavaxInjectableMethod
summary: This method is not annotated with @Inject, but it overrides a method that is  annotated with @javax.inject.Inject. The method will not be Injected.
layout: bugpattern
category: GUICE
severity: ERROR
maturity: EXPERIMENTAL
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
When classes declare that they have an `@javax.inject.Inject`ed method,
dependency injection tools must call those methods after first calling any
`@javax.inject.Inject` constructor, and performing any field injection. These
methods are part of the initialization contract for the object.

When subclasses override methods annotated with `@javax.inject.Inject` and
*don't* also annotate themselves with `@javax.inject.Inject`, the injector will
not call those methods as part of the subclass's initialization. This may
unexpectedly cause assumptions taken in the superclass (e.g.: this
post-initialization routine is finished, meaning that I can safely use this
field) to no longer hold.

This compile error is intended to prevent this unintentional breaking of
assumptions. Possible resolutions to this error include:

*   `@Inject` the overridden method, calling the `super` method to maintain the
    initialization contract.
*   Make the superclass' method `final` to avoid subclasses unintentionally
    masking the injected method.
*   Move the initialization work performed by the superclass method into the
    constructor.
*   Suppress this error, and very carefully inspect the initialization routine
    performed by the superclass, making sure that any work that needs to be done
    there is done in an @Inject method in the subclass. You may want to refactor
    portions of the body of the superclass method into a `protected` method for
    this subclass to use.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("OverridesJavaxInjectableMethod")` annotation to the enclosing element.

----------

### Positive examples
__OverridesJavaxInjectableMethodPositiveCases.java__

{% highlight java %}
/*
 * Copyright 2013 Google Inc. All Rights Reserved.
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
 * @author sgoldfeder@gooogle.com (Steven Goldfeder)
 */
public class OverridesJavaxInjectableMethodPositiveCases {

  /** Class with foo() */
  public class TestClass0 {
    public void foo() {}
  }

  /**
   * Class with a method foo() that is annotated with {@code javax.inject.Inject}. Other test
   * classes will extend this class.
   */
  public class TestClass1 extends TestClass0 {
    @javax.inject.Inject
    public void foo() {}
  }

  /**
   * Class with a method foo() that is not annotated, but overrides a method annotated with
   * @javax.inject.Inject.
   */
  public class TestClass2 extends TestClass1 {
    // BUG: Diagnostic contains: @Inject
    public void foo() {}
  }
  
  /**
   * Class with a method foo() that is not annotated, but overrides a method that in turn overrides
   * a method that is annotated with @javax.inject.Inject.
   */
  public class TestClass3 extends TestClass2 {
    // BUG: Diagnostic contains: @Inject
    public void foo() {}
  }
}
{% endhighlight %}

### Negative examples
__OverridesJavaxInjectableMethodNegativeCases.java__

{% highlight java %}
/*
 * Copyright 2013 Google Inc. All Rights Reserved.
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
 * @author sgoldfeder@gooogle.com (Steven Goldfeder)
 */
public class OverridesJavaxInjectableMethodNegativeCases {
  /**
   * Class with a method foo() with no annotations.
   */
  public class TestClass1 {
    public void foo() {}
  }

  /**
   * Class with a method foo() annotated with @com.google.inject.Inject.
   */
  public class TestClass2 {
    @com.google.inject.Inject
    public void foo() {}
  }

  /**
   * Class with a method foo() annotated with @javax.inject.Inject.
   */
  public class TestClass3 {
    @javax.inject.Inject
    public void foo() {}
  }

  /** OK, as it overrides a Guice-Inject method */
  public class TestClass4 extends TestClass2 {
    @Override
    public void foo() {}
  }

  /** gInject <- jInject */
  public class TestClass5 extends TestClass3 {
    @com.google.inject.Inject
    public void foo() {}
  }

  /** jInject <- gInject */
  public class TestClass6 extends TestClass2 {
    @javax.inject.Inject
    public void foo() {}
  }

  /** OK, as 7 <- jInject <- gInject */
  public class TestClass7 extends TestClass6 {
    public void foo() {}
  }

  /** OK, as 8 <- gInject */
  public class TestClass8 extends TestClass5 {
    public void foo() {}
  }
  
  /** Explicitly suppressed warning */
  public class TestClass9 extends TestClass3 {
    @Override 
    @SuppressWarnings("OverridesJavaxInjectableMethod")
    public void foo() {}
  }
  
}
{% endhighlight %}

