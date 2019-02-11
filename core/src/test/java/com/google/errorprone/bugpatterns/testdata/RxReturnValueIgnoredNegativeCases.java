/*
 * Copyright 2019 The Error Prone Authors.
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

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;

/** @author friedj@google.com (Jake Fried) */
public class RxReturnValueIgnoredNegativeCases {
  interface CanIgnoreMethod {
    @CanIgnoreReturnValue
    Observable<Object> getObservable();

    @CanIgnoreReturnValue
    Single<Object> getSingle();

    @CanIgnoreReturnValue
    Flowable<Object> getFlowable();

    @CanIgnoreReturnValue
    Maybe<Object> getMaybe();
  }

  public static class CanIgnoreImpl implements CanIgnoreMethod {
    @Override
    public Observable<Object> getObservable() {
      return null;
    }

    @Override
    public Single<Object> getSingle() {
      return null;
    }

    @Override
    public Flowable<Object> getFlowable() {
      return null;
    }

    @Override
    public Maybe<Object> getMaybe() {
      return null;
    }
  }

  static void callIgnoredInterfaceMethod() {
    new CanIgnoreImpl().getObservable();
    new CanIgnoreImpl().getSingle();
    new CanIgnoreImpl().getFlowable();
    new CanIgnoreImpl().getMaybe();
  }

  @CanIgnoreReturnValue
  Observable<Object> getObservable() {
    return null;
  }

  @CanIgnoreReturnValue
  Single<Object> getSingle() {
    return null;
  }

  @CanIgnoreReturnValue
  Flowable<Object> getFlowable() {
    return null;
  }

  @CanIgnoreReturnValue
  Maybe<Object> getMaybe() {
    return null;
  }

  void checkIgnore() {
    getObservable();
    getSingle();
    getFlowable();
    getMaybe();
  }
}
