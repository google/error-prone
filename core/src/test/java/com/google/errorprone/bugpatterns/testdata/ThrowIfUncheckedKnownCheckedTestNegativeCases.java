/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

import static com.google.common.base.Throwables.propagateIfPossible;
import static com.google.common.base.Throwables.throwIfUnchecked;

import java.io.IOException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

/** @author cpovirk@google.com (Chris Povirk) */
public class ThrowIfUncheckedKnownCheckedTestNegativeCases {
  void exception(Exception e) {
    throwIfUnchecked(e);
  }

  void throwable(Throwable e) {
    throwIfUnchecked(e);
  }

  void runtime(RuntimeException e) {
    // Better written as "throw e," but comes up too rarely to justify a compile error.
    throwIfUnchecked(e);
  }

  void error(Error e) {
    // Better written as "throw e," but comes up too rarely to justify a compile error.
    throwIfUnchecked(e);
  }

  void multiarg(IOException e) throws IOException {
    propagateIfPossible(e, IOException.class);
  }

  void union() {
    try {
      foo();
    } catch (IOException | ExecutionException | CancellationException e) {
      throwIfUnchecked(e);
    }
  }

  <E extends RuntimeException> void genericUnchecked(E e) {
    throwIfUnchecked(e);
  }

  <E extends Exception> void genericMaybeUnchecked(E e) {
    throwIfUnchecked(e);
  }

  <E extends T, T extends Exception> void genericUpperBoundDifferentFromErasure(E e) {
    throwIfUnchecked(e);
  }

  void foo() throws IOException, ExecutionException {}

  /*
   * I don't care whether these are flagged or not, since it won't come up in practice. I just want
   * to make sure that we don't blow up when running against the tests of Throwables.
   */
  void nullException() {
    throwIfUnchecked(null); // throws NPE
    propagateIfPossible(null); // no-op
  }
}
