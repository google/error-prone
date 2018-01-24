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

 Nested futures can also result in missed cancellation signals or suppressed exceptions - see [Avoiding Nested Futures](https://github.com/google/guava/wiki/ListenableFutureExplained#avoid-nested-futures) for details.

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
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

/** @author eaftan@google.com (Eddie Aftandilian) */
public class FutureReturnValueIgnoredPositiveCases {

  IntValue intValue = new IntValue(0);

  private static Future<Integer> increment(int bar) {
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

  static <I, N extends Q, Q> ListenableFuture<Q> transform(
      ListenableFuture<I> input, Function<? super I, ? extends N> function, Executor executor) {
    return null;
  }

  static ListenableFuture<Integer> futureReturningMethod() {
    return null;
  }

  static ListenableFuture<Integer> futureReturningMethod(Object unused) {
    return null;
  }

  static void consumesFuture(Future<Object> future) {}

  static void testIgnoredFuture() throws Exception {
    ListenableFuture<String> input = null;
    // BUG: Diagnostic contains: nested type
    Future<?> output = transform(input, foo -> futureReturningMethod(), runnable -> runnable.run());

    Future<?> otherOutput =
        // BUG: Diagnostic contains: nested type
        transform(
            input,
            new Function<String, ListenableFuture<Integer>>() {
              @Override
              public ListenableFuture<Integer> apply(String string) {
                return futureReturningMethod();
              }
            },
            runnable -> runnable.run());

    // BUG: Diagnostic contains: nested type
    transform(
            input,
            new Function<String, ListenableFuture<Integer>>() {
              @Override
              public ListenableFuture<Integer> apply(String string) {
                return futureReturningMethod();
              }
            },
            runnable -> runnable.run())
        .get();

    consumesFuture(
        // BUG: Diagnostic contains: nested type
        transform(
            input,
            new Function<String, ListenableFuture<Integer>>() {
              @Override
              public ListenableFuture<Integer> apply(String string) {
                System.out.println("First generics");
                return futureReturningMethod();
              }
            },
            runnable -> runnable.run()));

    consumesFuture(
        transform(
            input,
            new Function<String, Object>() {
              @Override
              public Object apply(String string) {
                // BUG: Diagnostic contains: returned future may be ignored
                return futureReturningMethod();
              }
            },
            runnable -> runnable.run()));
    consumesFuture(
        transform(
            input,
            new Function<String, Object>() {
              @Override
              public Object apply(String string) {
                Future<?> result = futureReturningMethod();
                // BUG: Diagnostic contains: returned future may be ignored
                return result;
              }
            },
            runnable -> runnable.run()));

    consumesFuture(
        // BUG: Diagnostic contains: nested type
        transform(input, foo -> futureReturningMethod(), runnable -> runnable.run()));

    consumesFuture(
        // BUG: Diagnostic contains: nested type
        transform(
            input,
            foo -> {
              return futureReturningMethod();
            },
            runnable -> runnable.run()));

    consumesFuture(
        // BUG: Diagnostic contains: nested type
        transform(
            input,
            FutureReturnValueIgnoredPositiveCases::futureReturningMethod,
            runnable -> runnable.run()));

    ListenableFuture<Object> done =
        // BUG: Diagnostic contains: nested type
        transform(
            // BUG: Diagnostic contains: nested type
            transform(
                input,
                new Function<String, ListenableFuture<Integer>>() {
                  @Override
                  public ListenableFuture<Integer> apply(String string) {
                    return futureReturningMethod();
                  }
                },
                runnable -> runnable.run()),
            new Function<Object, Object>() {
              @Override
              public Object apply(Object string) {
                return new Object();
              }
            },
            runnable -> runnable.run());
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
import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.doAnswer;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.base.Ticker;
import com.google.common.reflect.AbstractInvocationHandler;
import com.google.common.util.concurrent.AbstractFuture;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.netty.channel.ChannelFuture;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.ScheduledExecutorService;
import org.junit.function.ThrowingRunnable;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

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

  <V> ListenableFuture<V> ignoreVarArgs(
      Callable<V> combiner, Executor executor, ListenableFuture<?>... futures) {
    return combine(combiner, executor, Arrays.asList(futures));
  }

  public static <V> ListenableFuture<V> combine(
      final Callable<V> combiner,
      Executor executor,
      Iterable<? extends ListenableFuture<?>> futures) {
    return null;
  }

  private static final class TypedClass<T> {
    ListenableFuture<Void> ignoreReturnTypeSetByInputFuture(T input) {
      return returnsInputType(logAsyncInternal(input), 0);
    }

    protected ListenableFuture<Void> logAsyncInternal(T record) {
      return null;
    }

    <V> ListenableFuture<V> returnsInputType(ListenableFuture<V> future, final int n) {
      return null;
    }
  }

  public static <T, E extends Exception> CheckedFuture<List<T>, E> allAsList(
      CheckedFuture<? extends T, E>... futures) {
    return asListHelper(Futures.allAsList(null, null), futures);
  }

  private static <T, E extends Exception> CheckedFuture<List<T>, E> asListHelper(
      ListenableFuture<List<T>> future, CheckedFuture<? extends T, E>... originalFutures) {
    return asListHelper(future, Arrays.asList(originalFutures));
  }

  /**
   * Helper method for Iterable allAsList and successfulAsList calls which special-cases empty
   * iterables.
   *
   * @param future the future to make checked
   * @param originalFutures the original futures
   * @return a checked version of the future argument
   */
  private static <T, E extends Exception> CheckedFuture<List<T>, E> asListHelper(
      ListenableFuture<List<T>> future,
      Iterable<? extends CheckedFuture<? extends T, E>> originalFutures) {
    Iterator<? extends CheckedFuture<? extends T, E>> iterator = originalFutures.iterator();
    if (!iterator.hasNext()) {
      return null;
    }
    return null;
  }

  public static class RetryingFuture<T> extends AbstractFuture<T> {

    /**
     * Enables the user to receive notifications about state changes of a retrying future, and use
     * them e.g. for logging.
     */
    public interface Interceptor<T> {}

    /** Creates a builder for {@link RetryingFuture} instances. */
    public static Builder<Object> builder() {
      return new Builder<>();
    }

    /** A builder for {@link RetryingFuture} instances. */
    public static final class Builder<T> {

      private Builder() {}

      /** Sets the {@link Executor} in which all tries and retries are performed. */
      @CanIgnoreReturnValue
      public Builder<T> setExecutor(Executor executor) {
        return this;
      }

      /**
       * Sets the {@link ScheduledExecutorService} used for scheduling retries after delay. It will
       * also be used for tries and retries if {@link #setExecutor(Executor)} is not called.
       */
      @CanIgnoreReturnValue
      public Builder<T> setScheduledExecutorService(
          ScheduledExecutorService scheduledExecutorService) {
        return this;
      }

      public <U extends T> Builder<U> setInterceptor(Interceptor<U> interceptor) {
        // Safely limiting the kinds of RetryingFutures this builder can produce,
        // based on the type of the interceptor.
        @SuppressWarnings("unchecked")
        Builder<U> me = (Builder<U>) this;
        return me;
      }

      public Builder<T> setTicker(Ticker ticker) {
        return this;
      }

      public <U extends T> RetryingFuture<U> build(
          Supplier<? extends ListenableFuture<U>> futureSupplier,
          Predicate<? super Exception> shouldContinue) {
        return new RetryingFuture<U>(
            futureSupplier,
            null,
            shouldContinue,
            null,
            // We need to maintain Java 7 compatibility
            null,
            null,
            null);
      }

      public <U extends T> RetryingFuture<U> build(
          Supplier<? extends ListenableFuture<U>> futureSupplier,
          Object strategy,
          Predicate<? super Exception> shouldContinue) {
        return new RetryingFuture<U>(
            futureSupplier,
            strategy,
            shouldContinue,
            null,
            // We need to maintain Java 7 compatibility
            null,
            null,
            null);
      }
    }

    RetryingFuture(
        Supplier<? extends ListenableFuture<T>> futureSupplier,
        Object strategy,
        Predicate<? super Exception> shouldContinue,
        Executor executor,
        ScheduledExecutorService scheduledExecutorService,
        Ticker ticker,
        final Interceptor<? super T> interceptor) {}

    public static <T> RetryingFuture<T> retryingFuture(
        Supplier<? extends ListenableFuture<T>> futureSupplier,
        Object strategy,
        Predicate<? super Exception> shouldContinue,
        Executor executor,
        Interceptor<? super T> interceptor) {
      return builder()
          .setInterceptor(interceptor)
          .setExecutor(executor)
          .build(futureSupplier, strategy, shouldContinue);
    }
  }

  private static class TypedObject<T> {
    public <O extends Object> ListenableFuture<O> transformAndClose(
        Function<? super T, O> function, Executor executor) {
      return null;
    }

    public ListenableFuture<T> close() {
      return transformAndClose(Functions.identity(), directExecutor());
    }
  }

  private static void mocking() {
    doAnswer(invocation -> immediateFuture(null)).when(null);
    doAnswer(
            invocation -> {
              return immediateFuture(null);
            })
        .when(null);
    doAnswer(
            new Answer() {
              @Override
              public Object answer(InvocationOnMock mock) {
                return immediateFuture(null);
              }
            })
        .when(null);
  }

  private static void throwing() {
    assertThrows(RuntimeException.class, () -> immediateFuture(null));
    assertThrows(
        RuntimeException.class,
        () -> {
          immediateFuture(null);
        });
    assertThrows(
        RuntimeException.class,
        new ThrowingRunnable() {
          @Override
          public void run() throws Throwable {
            immediateFuture(null);
          }
        });
  }

  private static AsyncFunction<String, String> provideAsyncFunction() {
    return Futures::immediateFuture;
  }

  private static Runnable provideNonFutureInterface() {
    return new FutureTask(null);
  }

  private static void invocation() {
    new AbstractInvocationHandler() {
      @Override
      protected Object handleInvocation(Object o, Method method, Object[] params) {
        return immediateFuture(null);
      }
    };
  }
}
{% endhighlight %}

