---
title: OverridesJavaxInjectableMethod
summary: This method is not annotated with @Inject, but it overrides a  method that is  annotated with @javax.inject.Inject.
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
According to the JSR-330 spec, a method that overrides a method annotated with javax.inject.Inject will not be injected unless it iself is annotated with  @Inject

## Suppression
Suppress false positives by adding an `@SuppressWarnings("OverridesJavaxInjectableMethod")` annotation to the enclosing element.

----------

### Positive examples
__GuiceOverridesJavaxInjectableMethodPositiveCases.java__

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

package com.google.errorprone.bugpatterns;

/**
 * @author sgoldfeder@gooogle.com (Steven Goldfeder)
 */
public class GuiceOverridesJavaxInjectableMethodPositiveCases {

  /**
   * Class with a method foo() that is annotated with @javax.inject.Inject. Other test classes will
   * extend this class.
   */
  public class TestClass1 {
    @javax.inject.Inject
    public void foo() {}
  }

  /**
   * Class with a method foo() that is not annotated, but overrides a method annotated with
   * @javax.inject.Inject.
   */
  public class TestClass2 extends TestClass1 {
    @Override 
    // BUG: Diagnostic contains: @Inject
    public void foo() {}
  }
  
  /**
   * Class with a method foo() that is not annotated, but overrides a method that in turn overrides
   * a method that is annotated with @javax.inject.Inject.
   */
  public class TestClass3 extends TestClass2 {
    @Override 
    // BUG: Diagnostic contains: @Inject
    public void foo() {}
  }
}
{% endhighlight %}

### Negative examples
__GuiceOverridesJavaxInjectableMethodNegativeCases.java__

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

package com.google.errorprone.bugpatterns;

/**
 * @author sgoldfeder@gooogle.com (Steven Goldfeder)
 */
public class GuiceOverridesJavaxInjectableMethodNegativeCases {
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

  /**
   * Class with a method foo() that is not annotated with @Inject, but overrides a method that is
   * annotated with @com.google.inject.Inject.
   */
  public class TestClass4 extends TestClass2 {
    @Override
    public void foo() {}
  }

  /**
   * Class with a method foo() annotated with @com.google.inject.Inject that overrides a method
   * annotated with @javax.inject.Inject.
   */
  public class TestClass5 extends TestClass3 {
    @com.google.inject.Inject
    public void foo() {}
  }

  /**
   * Class with a method foo() annotated with @javax.inject.Inject that overrides a method 
   * annotated with @com.google.inject.Inject.
   */
  public class TestClass6 extends TestClass2 {
    @javax.inject.Inject
    public void foo() {}
  }

  /**
   * Class with a method foo() that is not annotated, but overrides a method that is annotated with
   * @javax.inject.Inject, and that method in turn overrides a method annotated with
   * @com.google.inject.Inject
   */
  public class TestClass7 extends TestClass6 {
    public void foo() {}
  }

  /**
   * Class with a method foo() that is not annotated, but overrides a method that is annotated with
   * @com.google.inject.Inject, and that method in turn overrides a method annotated with
   * @javax.inject.Inject
   */
  public class TestClass8 extends TestClass5 {
    public void foo() {}
  }
  
  /**
   * Class with a method foo() that is not annotated, but overrides a method annotated with
   * @javax.inject.Inject. Error is suppressed.
   */
  public class TestClass9 extends TestClass3 {
    @Override 
    @SuppressWarnings("OverridesJavaxInjectableMethod")
    public void foo() {}
  }
  
}
{% endhighlight %}

