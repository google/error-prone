/*
 * Copyright 2012 Google Inc. All Rights Reserved.
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
