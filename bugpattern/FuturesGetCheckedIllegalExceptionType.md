---
title: FuturesGetCheckedIllegalExceptionType
summary: Futures.getChecked requires a checked exception type with a standard constructor.
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
The passed exception type must not be a RuntimeException, and it must expose a public constructor whose only parameters are of type String or Throwable. getChecked will reject any other type with an IllegalArgumentException.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("FuturesGetCheckedIllegalExceptionType")` to the enclosing element.

----------

### Positive examples
__FuturesGetCheckedIllegalExceptionTypePositiveCases.java__

{% highlight java %}
/*
 * Copyright 2015 The Error Prone Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.errorprone.bugpatterns.testdata;

import static com.google.common.util.concurrent.Futures.getChecked;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.concurrent.Future;

/** Positive cases for {@link FuturesGetCheckedIllegalExceptionType}. */
public class FuturesGetCheckedIllegalExceptionTypePositiveCases {
  <T extends RuntimeException> void runtime(
      Future<?> future, Class<? extends RuntimeException> c1, Class<T> c2) throws Exception {
    // BUG: Diagnostic contains: getUnchecked(future)
    getChecked(future, RuntimeException.class);
    // BUG: Diagnostic contains: getUnchecked(future)
    getChecked(future, IllegalArgumentException.class);
    // BUG: Diagnostic contains: getUnchecked(future)
    getChecked(future, RuntimeException.class, 0, SECONDS);
    // BUG: Diagnostic contains: getUnchecked(future)
    getChecked(future, c1);
    // BUG: Diagnostic contains: getUnchecked(future)
    getChecked(future, c2);
  }

  void visibility(Future<?> future) throws Exception {
    // BUG: Diagnostic contains: parameters
    getChecked(future, PrivateConstructorException.class);
    // BUG: Diagnostic contains: parameters
    getChecked(future, PackagePrivateConstructorException.class);
    // BUG: Diagnostic contains: parameters
    getChecked(future, ProtectedConstructorException.class);
  }

  void parameters(Future<?> future) throws Exception {
    // BUG: Diagnostic contains: parameters
    getChecked(future, OtherParameterTypeException.class);
    // TODO(cpovirk): Consider a specialized error message if inner classes prove to be common.
    // BUG: Diagnostic contains: parameters
    getChecked(future, InnerClassWithExplicitConstructorException.class);
    // BUG: Diagnostic contains: parameters
    getChecked(future, InnerClassWithImplicitConstructorException.class);
  }

  public static class PrivateConstructorException extends Exception {
    private PrivateConstructorException() {}
  }

  public static class PackagePrivateConstructorException extends Exception {
    PackagePrivateConstructorException() {}
  }

  public static class ProtectedConstructorException extends Exception {
    protected ProtectedConstructorException() {}
  }

  public class OtherParameterTypeException extends Exception {
    public OtherParameterTypeException(int it) {}
  }

  public class InnerClassWithExplicitConstructorException extends Exception {
    public InnerClassWithExplicitConstructorException() {}
  }

  public class InnerClassWithImplicitConstructorException extends Exception {}
}
{% endhighlight %}

### Negative examples
__FuturesGetCheckedIllegalExceptionTypeNegativeCases.java__

{% highlight java %}
/*
 * Copyright 2015 The Error Prone Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.errorprone.bugpatterns.testdata;

import static com.google.common.util.concurrent.Futures.getChecked;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.io.IOException;
import java.util.concurrent.Future;

/** Negative cases for {@link FuturesGetCheckedIllegalExceptionType}. */
public class FuturesGetCheckedIllegalExceptionTypeNegativeCases {
  <T extends Exception> void runtime(Future<?> future, Class<? extends Exception> c1, Class<T> c2)
      throws Exception {
    getChecked(future, Exception.class);
    getChecked(future, Exception.class, 0, SECONDS);
    getChecked(future, IOException.class);
    // These might or might not be RuntimeExceptions. We can't prove it one way or the other.
    getChecked(future, c1);
    getChecked(future, c2);
    getChecked(future, null);
  }

  <T extends ProtectedConstructorException> void constructor(
      Future<?> future, Class<? extends ProtectedConstructorException> c1, Class<T> c2)
      throws Exception {
    getChecked(future, StaticNestedWithExplicitConstructorException.class);
    getChecked(future, StaticNestedWithImplicitConstructorException.class);
    /*
     * These might be ProtectedConstructorException, but they might be a subtype with a public
     * constructor.
     */
    getChecked(future, c1);
    getChecked(future, c2);
  }

  public static class StaticNestedWithExplicitConstructorException extends Exception {
    public StaticNestedWithExplicitConstructorException() {}
  }

  public static class StaticNestedWithImplicitConstructorException extends Exception {}

  public static class ProtectedConstructorException extends Exception {
    protected ProtectedConstructorException() {}
  }
}
{% endhighlight %}

