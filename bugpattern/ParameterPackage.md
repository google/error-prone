---
title: ParameterPackage
summary: Method parameter has wrong package
layout: bugpattern
tags: ''
severity: ERROR
providesFix: REQUIRES_HUMAN_ATTENTION
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
Method does not override method in superclass due to wrong package for parameter. For example, defining a method void foo(alpha.Foo x) when the superclass contains a method void foo(beta.Foo x). The defined method was probably meant to override the superclass method.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("ParameterPackage")` annotation to the enclosing element.

----------

### Positive examples
__WrongParameterPackagePositiveCases.java__

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

package com.google.errorprone.bugpatterns.testdata;

/** @author scottjohnson@google.com (Scott Johnson) */
public class WrongParameterPackagePositiveCases {

  public void testParameter(WrongParameterPackageNegativeCases.Integer x) {}

  public void testParameter(Integer x, Integer y) {}

  public void testParameter2(java.lang.Integer x, Integer y) {}

  public void testParameter3(Integer x, Integer y) {}

  /** Test overrides */
  public static class Subclass extends WrongParameterPackagePositiveCases {

    // BUG: Diagnostic contains: public void
    // testParameter(com.google.errorprone.bugpatterns.testdata.WrongParameterPackageNegativeCases.Integer x) {}
    public void testParameter(Integer x) {}

    // BUG: Diagnostic contains: public void
    // testParameter(com.google.errorprone.bugpatterns.testdata.WrongParameterPackagePositiveCases.Integer x, com.google.errorprone.bugpatterns.testdata.WrongParameterPackagePositiveCases.Integer y) {}
    public void testParameter(WrongParameterPackageNegativeCases.Integer x, Integer y) {}

    // BUG: Diagnostic contains: public void testParameter2(java.lang.Integer x,
    // com.google.errorprone.bugpatterns.testdata.WrongParameterPackagePositiveCases.Integer y) {}
    public void testParameter2(WrongParameterPackageNegativeCases.Integer x, java.lang.Integer y) {}

    // BUG: Diagnostic contains: public void
    // testParameter3(com.google.errorprone.bugpatterns.testdata.WrongParameterPackagePositiveCases.Integer x, com.google.errorprone.bugpatterns.testdata.WrongParameterPackagePositiveCases.Integer y) {}
    public void testParameter3(java.lang.Integer x, java.lang.Integer y) {}

    /** Ambiguous Integer class */
    public static class Integer {}
  }

  /** Ambiguous Integer class */
  public static class Integer {}
}
{% endhighlight %}

### Negative examples
__WrongParameterPackageNegativeCases.java__

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

package com.google.errorprone.bugpatterns.testdata;

/** @author scottjohnson@google.com (Scott Johnson) */
public class WrongParameterPackageNegativeCases {

  public void testParameter(Integer x) {}

  public void testParameter(Integer x, Integer y) {}

  public void testParameter2(Integer x, Integer y) {}

  /** Test overrides */
  public static class Subclass extends WrongParameterPackageNegativeCases {

    @Override
    public void testParameter(Integer x) {}

    @Override
    public void testParameter(Integer x, Integer y) {}

    public void testParameter(Boolean x, Integer y) {}

    public void testParameter(Boolean x) {}

    @Override
    public void testParameter2(WrongParameterPackageNegativeCases.Integer x, Integer y) {}

    @SuppressWarnings("ParameterPackage")
    public void testParameter(java.lang.Integer x) {}
  }

  /** Ambiguous Integer class */
  public static class Integer {}
}
{% endhighlight %}

