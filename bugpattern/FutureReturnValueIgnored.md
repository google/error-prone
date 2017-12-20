---
title: FutureReturnValueIgnored
summary: Return value of methods returning Future must be checked. Ignoring returned Futures suppresses exceptions thrown from the code that completes the Future.
layout: bugpattern
tags: FragileCode
severity: WARNING
providesFix: NO_FIX
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
Methods that return `java.util.concurrent.Future` and its subclasses generally indicate errors by returning a future that eventually fails.

If you don’t check the return value of these methods, you will never find out if they threw an exception.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("FutureReturnValueIgnored")` to the enclosing element.

----------

### Positive examples
__FutureReturnValueIgnoredPositiveCases.java__

{% highlight java %}
/*
 * Copyright 2012 The Error Prone Authors.
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

import static com.google.common.util.concurrent.Futures.immediateFuture;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.Future;

/** @author eaftan@google.com (Eddie Aftandilian) */
public class FutureReturnValueIgnoredPositiveCases {

  IntValue intValue = new IntValue(0);

  private Future<Integer> increment(int bar) {
    return null;
  }

  public <T extends Future> T returnFutureType(T input) {
    return input;
  }

  public void testFutureGenerics() {
    // BUG: Diagnostic contains: Future must be checked
    returnFutureType(Futures.immediateCancelledFuture());
  }

  public void foo() {
    int i = 1;
    // BUG: Diagnostic contains: Future must be checked
    increment(i);
    System.out.println(i);
  }

  public void bar() {
    // BUG: Diagnostic contains: Future must be checked
    this.intValue.increment();
  }

  public void testIntValue() {
    IntValue value = new IntValue(10);
    // BUG: Diagnostic contains: Future must be checked
    value.increment();
  }

  public void testFunction() {
    new Function<Object, ListenableFuture<?>>() {
      @Override
      public ListenableFuture<?> apply(Object input) {
        return immediateFuture(null);
      }
      // BUG: Diagnostic contains: Future must be checked
    }.apply(null);
  }

  private class IntValue {

    final int i;

    public IntValue(int i) {
      this.i = i;
    }

    public ListenableFuture<IntValue> increment() {
      return immediateFuture(new IntValue(i + 1));
    }

    public void increment2() {
      // BUG: Diagnostic contains: Future must be checked
      this.increment();
    }

    public void increment3() {
      // BUG: Diagnostic contains: Future must be checked
      increment();
    }
  }
}
{% endhighlight %}

### Negative examples
__FutureReturnValueIgnoredNegativeCases.java__

{% highlight java %}
/*
 * Copyright 2017 The Error Prone Authors.
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

import static com.google.common.util.concurrent.Futures.immediateFuture;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.netty.channel.ChannelFuture;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.Future;
import java.util.concurrent.RecursiveAction;

/** */
public class FutureReturnValueIgnoredNegativeCases {

  public FutureReturnValueIgnoredNegativeCases() {}

  static ListenableFuture<Object> getFuture() {
    return immediateFuture(null);
  }

  interface CanIgnoreMethod {
    @CanIgnoreReturnValue
    Future<Object> getFuture();
  }

  public static class CanIgnoreImpl implements CanIgnoreMethod {
    @Override
    public Future<Object> getFuture() {
      return null;
    }
  }

  static void callIgnoredInterfaceMethod() {
    new CanIgnoreImpl().getFuture();
  }

  @CanIgnoreReturnValue
  static ListenableFuture<Object> getFutureIgnore() {
    return immediateFuture(null);
  }

  static void putInMap() {
    Map<Object, Future<?>> map = new HashMap<>();
    map.put(new Object(), immediateFuture(null));
    Map map2 = new HashMap();
    map2.put(new Object(), immediateFuture(null));
  }

  static void preconditions()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Preconditions.checkNotNull(getFuture());
    Preconditions.checkNotNull(new Object());
    FutureReturnValueIgnoredNegativeCases.class.getDeclaredMethod("preconditions").invoke(null);
  }

  static void checkIgnore() {
    getFutureIgnore();
  }

  void ignoreForkJoinTaskFork(ForkJoinTask<?> t) {
    t.fork();
  }

  void ignoreForkJoinTaskFork_subclass(RecursiveAction t) {
    t.fork();
  }

  void ignoreExecutorCompletionServiceSubmit(ExecutorCompletionService s) {
    s.submit(() -> null);
  }

  void ignoreChannelFutureAddListener(ChannelFuture cf) {
    cf.addListener((ChannelFuture f) -> {});
  }

  void ignoreChannelFutureAddListeners(ChannelFuture cf) {
    cf.addListeners((ChannelFuture f) -> {}, (ChannelFuture f) -> {});
  }
}
{% endhighlight %}

