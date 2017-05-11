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
