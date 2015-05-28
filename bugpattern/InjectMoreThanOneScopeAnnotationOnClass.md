---
title: InjectMoreThanOneScopeAnnotationOnClass
summary: "A class can be annotated with at most one scope annotation"
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
Annotating a class with more than one scope annotation is invalid according to the JSR-330 specification.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("InjectMoreThanOneScopeAnnotationOnClass")` annotation to the enclosing element.

----------

## Examples
__InjectMoreThanOneScopeAnnotationOnClassNegativeCases.java__

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

import com.google.inject.Singleton;
import com.google.inject.Provides;
/**
 * @author sgoldfeder@google.com(Steven Goldfeder)
 */
public class InjectMoreThanOneScopeAnnotationOnClassNegativeCases {

  /**
   * Class has no annotation. 
   */
  public class TestClass1 {}
  /**
   * Class has a single non scoping annotation. 
   */
  @SuppressWarnings("foo")
  public class TestClass2 {}
  
  /**
   * Class hasa single scoping annotation.
   */
  @Singleton 
  public class TestClass3 {}
  
  /**
   * Class has two annotations, one of which is a scoping annotation.
   */
  @Singleton @SuppressWarnings("foo")
  public class TestClass4 {}
  
  /**
   * Class has two annotations, one of which is a scoping annotation. Class
   * also has a method with a scoping annotation.
   */
   @SuppressWarnings("foo")
  public class TestClass5 {
  @Singleton @Provides
  public void foo(){}
  }
}
{% endhighlight %}

__InjectMoreThanOneScopeAnnotationOnClassPositiveCases.java__

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

import com.google.inject.Singleton;
import com.google.inject.servlet.SessionScoped;

/**
 * @author sgoldfeder@google.com(Steven Goldfeder)
 */
public class InjectMoreThanOneScopeAnnotationOnClassPositiveCases {

  /**
   * Class has two scope annotations
   */  
  // BUG: Diagnostic contains: remove 
  @Singleton 
  // BUG: Diagnostic contains: remove  
  @SessionScoped
  public class TestClass1 {}

  /**
   * Class has three annotations, two of which are scope annotations.
   */
  // BUG: Diagnostic contains: remove 
  @Singleton 
  @SuppressWarnings("foo")
  // BUG: Diagnostic contains: remove  
  @SessionScoped
  public class TestClass2 {}
  
}
{% endhighlight %}

