---
title: JavaxInjectOnAbstractMethod
summary: Abstract methods are not injectable with javax.inject.Inject.
layout: bugpattern
category: INJECT
severity: ERROR
maturity: EXPERIMENTAL
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
The javax.inject.Inject annotation cannot go on an abstract method as per the JSR-330 spec. This is in line with the fact that if a class overrides a method that was annotated with javax.inject.Inject, and the subclass methodis not annotated, the subclass method will not be injected.

See http://docs.oracle.com/javaee/6/api/javax/inject/Inject.html
and https://code.google.com/p/google-guice/wiki/JSR330

## Suppression
Suppress false positives by adding an `@SuppressWarnings("JavaxInjectOnAbstractMethod")` annotation to the enclosing element.

----------

### Positive examples
__JavaxInjectOnAbstractMethodPositiveCases.java__

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

package com.google.errorprone.bugpatterns.inject.testdata;

/**
 * @author sgoldfeder@google.com (Steven Goldfeder)
 */
public class JavaxInjectOnAbstractMethodPositiveCases {

  /**
   * Abstract class has an injectable(javax.inject.Inject) abstract method.
   */
  public abstract class TestClass1 {
    // BUG: Diagnostic contains: remove  
    @javax.inject.Inject
    abstract void abstractMethod();
  }

  /**
   * Abstract class has an injectable(javax.inject.Inject) abstract method and 
   * an unrelated concrete method.
   */
  public abstract class TestClass2 {
    // BUG: Diagnostic contains: remove  
    @javax.inject.Inject
    abstract void abstractMethod();
    public void foo(){}
  }
  
  /**
   * Abstract class has an injectable(javax.inject.Inject) abstract method and 
   * an unrelated abstarct method.
   */
  public abstract class TestClass3 {
    // BUG: Diagnostic contains: remove  
    @javax.inject.Inject
    abstract void abstractMethod1();
    abstract void abstarctMethod2();
  }
}
{% endhighlight %}

### Negative examples
__JavaxInjectOnAbstractMethodNegativeCases.java__

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

package com.google.errorprone.bugpatterns.inject.testdata;

import javax.inject.Inject;

/**
 * @author sgoldfeder@google.com (Steven Goldfeder)
 */
public class JavaxInjectOnAbstractMethodNegativeCases {

  /**
   * Concrete class has no methods or annotations.
   */
  public class TestClass1 {
  }

  /**
   * Abstract class has a single abstract method with no annotation.
   */
  public abstract class TestClass2 {
    abstract void abstractMethod();
  }

  /**
   * Concrete class has an injectable method.
   */
  public class TestClass3 {
    @Inject
    public void foo() {}
  }

  /**
   * Abstract class has an injectable concrete method.
   */
  public abstract class TestClass4 {
    abstract void abstractMethod();

    @Inject
    public void concreteMethod() {}
  }
  
  /**
   * Abstract class has an com.google.inject.Inject abstract method (This is allowed;
   * Injecting abstract methods is only forbidden with javax.inject.Inject). 
   */
  public abstract class TestClass5 {
    @com.google.inject.Inject    
    abstract void abstractMethod();
  }
  
  /**
   * Abstract class has an injectable(javax.inject.Inject) abstract method. Error is suppressed.
   */
  public abstract class TestClass6 {
    @SuppressWarnings("JavaxInjectOnAbstractMethod")  
    @javax.inject.Inject
    abstract void abstractMethod();
  }
}
{% endhighlight %}

