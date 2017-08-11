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

import java.util.function.Supplier;
import javax.annotation.CheckReturnValue;

/** @author eaftan@google.com (Eddie Aftandilian) */
public class CheckReturnValueNegativeCases {

  public void test1() {
    test2();
    Object obj = new String();
    obj.toString();
  }

  @SuppressWarnings("foo") // wrong annotation
  public void test2() {}

  @CheckReturnValue
  private int mustCheck() {
    return 5;
  }

  private void callRunnable(Runnable runnable) {
    runnable.run();
  }

  private void testNonCheckedCallsWithMethodReferences() {
    Object obj = new String();
    callRunnable(String::new);
    callRunnable(this::test2);
    callRunnable(obj::toString);
  }

  private void callSupplier(Supplier<Integer> supplier) {
    supplier.get();
  }

  public void testResolvedToVoidLambda() {
    callSupplier(() -> mustCheck());
  }

  public void testMethodReference() {
    callSupplier(this::mustCheck);
  }
}
