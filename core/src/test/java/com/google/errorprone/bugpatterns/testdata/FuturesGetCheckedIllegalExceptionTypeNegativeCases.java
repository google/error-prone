/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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
