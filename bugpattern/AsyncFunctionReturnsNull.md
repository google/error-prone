---
title: AsyncFunctionReturnsNull
layout: bugpattern
category: GUAVA
severity: ERROR
maturity: EXPERIMENTAL
---

# Bug pattern: AsyncFunctionReturnsNull
__AsyncFunction should not return a null Future, only a Future whose result is null.__

## The problem
Methods like Futures.transformAsync and Futures.catchingAsync will throw a NullPointerException if the provided AsyncFunction returns a null Future. To produce a Future with an output of null, instead return immediateFuture(null).

## Suppression
Suppress false positives by adding an `@SuppressWarnings("AsyncFunctionReturnsNull")` annotation to the enclosing element.

----------

# Examples
__AsyncFunctionReturnsNullNegativeCases.java__
{% highlight java %}
/*
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.errorprone.bugpatterns;

import static com.google.common.util.concurrent.Futures.immediateFuture;

import com.google.common.base.Function;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.ListenableFuture;

import javax.annotation.Nullable;

/**
 * Negative cases for {@link AsyncFunctionReturnsNull}.
 */
public class AsyncFunctionReturnsNullNegativeCases {
  static {
    new AsyncFunction<String, Object>() {
      @Override
      public ListenableFuture<Object> apply(String input) throws Exception {
        return immediateFuture(null);
      }
    };

    new Function<String, Object>() {
      @Override
      public Object apply(String input) {
        return null;
      }
    };

    new AsyncFunction<String, Object>() {
      @Override
      public ListenableFuture<Object> apply(String input) throws Exception {
        return apply(input, input);
      }

      public ListenableFuture<Object> apply(String input1, String input2) {
        return null;
      }
    };

    new MyNonAsyncFunction<String, Object>() {
      @Override
      public ListenableFuture<Object> apply(String input) throws Exception {
        return null;
      }
    };
  }

  interface MyNonAsyncFunction<I, O> {
    ListenableFuture<O> apply(@Nullable I input) throws Exception;
  }
}

{% endhighlight %}
__AsyncFunctionReturnsNullPositiveCases.java__
{% highlight java %}
/*
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.errorprone.bugpatterns;

import static com.google.common.util.concurrent.Futures.immediateFuture;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.ListenableFuture;

/**
 * Positive cases for {@link AsyncFunctionReturnsNull}.
 */
public class AsyncFunctionReturnsNullPositiveCases {
  static {
    new AsyncFunction<String, Object>() {
      @Override
      public ListenableFuture<Object> apply(String input) throws Exception {
        // BUG: Diagnostic contains: immediateFuture(null)
        return null;
      }
    };

    new AsyncFunction<Object, String>() {
      @Override
      public ListenableFuture<String> apply(Object o) {
        if (o instanceof String) {
          return immediateFuture((String) o);
        }
        // BUG: Diagnostic contains: immediateFuture(null)
        return null;
      }
    };

    new AsyncFunction<Object, String>() {
      @Override
      public CheckedFuture<String, Exception> apply(Object o) {
        // BUG: Diagnostic contains: immediateFuture(null)
        return null;
      }
    };

    new MyAsyncFunction() {
      @Override
      public CheckedFuture<String, Exception> apply(Object o) {
        // BUG: Diagnostic contains: immediateFuture(null)
        return null;
      }
    };
  }

  static class MyAsyncFunction implements AsyncFunction<Object, String> {
    @Override
    public ListenableFuture<String> apply(Object input) throws Exception {
      return immediateFuture(input.toString());
    }
  }
}

{% endhighlight %}
