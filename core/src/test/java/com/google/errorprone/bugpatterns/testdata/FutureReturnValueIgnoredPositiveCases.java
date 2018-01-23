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
