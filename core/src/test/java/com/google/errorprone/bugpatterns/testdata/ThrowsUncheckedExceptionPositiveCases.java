/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

import java.io.IOException;

/** @author yulissa@google.com (Yulissa Arroyo-Paredes) */
public class ThrowsUncheckedExceptionPositiveCases {
  // BUG: Diagnostic contains: 'public void doSomething() {'
  public void doSomething() throws IllegalArgumentException {
    throw new IllegalArgumentException("thrown");
  }

  // BUG: Diagnostic contains: 'public void doSomethingElse() {'
  public void doSomethingElse() throws RuntimeException, NullPointerException {
    throw new NullPointerException("thrown");
  }

  // BUG: Diagnostic contains: Unchecked exceptions do not need to be declared
  public void doMore() throws RuntimeException, IOException {
    throw new IllegalArgumentException("thrown");
  }

  // BUG: Diagnostic contains: Unchecked exceptions do not need to be declared
  public void doEverything() throws RuntimeException, IOException, IndexOutOfBoundsException {
    throw new IllegalArgumentException("thrown");
  }

  // BUG: Diagnostic contains: 'public void doBetter() {'
  public void doBetter() throws RuntimeException, AssertionError {
    throw new RuntimeException("thrown");
  }
}
