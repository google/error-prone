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
import java.util.Arrays;

/** @author friedj@google.com (Jake Fried) */
public class RxReturnValueIgnoredPositiveCases {
  private static Observable getObservable() {
    return null;
  }

  private static Single getSingle() {
    return null;
  }

  private static Flowable getFlowable() {
    return null;
  }

  private static Maybe getMaybe() {
    return null;
  }

  {
    new Observable();
    new Single();
    new Flowable();
    new Maybe();

    // BUG: Diagnostic contains: Rx objects must be checked.
    getObservable();
    // BUG: Diagnostic contains: Rx objects must be checked.
    getSingle();
    // BUG: Diagnostic contains: Rx objects must be checked.
    getFlowable();
    // BUG: Diagnostic contains: Rx objects must be checked.
    getMaybe();

    // BUG: Diagnostic contains: Rx objects must be checked.
    Arrays.asList(1, 2, 3).forEach(n -> getObservable());
    // BUG: Diagnostic contains: Rx objects must be checked.
    Arrays.asList(1, 2, 3).forEach(n -> getSingle());
    // BUG: Diagnostic contains: Rx objects must be checked.
    Arrays.asList(1, 2, 3).forEach(n -> getFlowable());
    // BUG: Diagnostic contains: Rx objects must be checked.
    Arrays.asList(1, 2, 3).forEach(n -> getMaybe());
  }

  private abstract static class IgnoringParent<T> {
    @CanIgnoreReturnValue
    abstract T ignoringFunction();
  }

  private class NonIgnoringObservableChild extends IgnoringParent<Observable<Integer>> {
    @Override
    Observable<Integer> ignoringFunction() {
      return null;
    }
  }

  private class NonIgnoringSingleChild extends IgnoringParent<Single<Integer>> {
    @Override
    Single<Integer> ignoringFunction() {
      return null;
    }
  }

  private class NonIgnoringFlowableChild extends IgnoringParent<Flowable<Integer>> {
    @Override
    Flowable<Integer> ignoringFunction() {
      return null;
    }
  }

  private class NonIgnoringMaybeChild extends IgnoringParent<Maybe<Integer>> {
    @Override
    Maybe<Integer> ignoringFunction() {
      return null;
    }
  }

  public void inheritenceTest() {
    NonIgnoringObservableChild observableChild = new NonIgnoringObservableChild();
    NonIgnoringSingleChild singleChild = new NonIgnoringSingleChild();
    NonIgnoringFlowableChild flowableChild = new NonIgnoringFlowableChild();
    NonIgnoringMaybeChild maybeChild = new NonIgnoringMaybeChild();

    // BUG: Diagnostic contains: Rx objects must be checked.
    observableChild.ignoringFunction();
    // BUG: Diagnostic contains: Rx objects must be checked.
    singleChild.ignoringFunction();
    // BUG: Diagnostic contains: Rx objects must be checked.
    flowableChild.ignoringFunction();
    // BUG: Diagnostic contains: Rx objects must be checked.
    maybeChild.ignoringFunction();
  }

  public void conditional() {
    if (false) {
      // BUG: Diagnostic contains: Rx objects must be checked.
      getObservable();
      // BUG: Diagnostic contains: Rx objects must be checked.
      getSingle();
      // BUG: Diagnostic contains: Rx objects must be checked.
      getFlowable();
      // BUG: Diagnostic contains: Rx objects must be checked.
      getMaybe();
    }

    return;
  }
}
