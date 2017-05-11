/*
 * Copyright 2013 Google Inc. All Rights Reserved.
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

package com.google.errorprone.bugpatterns.inject.guice.testdata;

import com.google.inject.Inject;
import javax.annotation.Nullable;

/** @author sgoldfeder@google.com (Steven Goldfeder) */
public class InjectOnFinalFieldPositiveCases {
  /** Class has a final injectable(com.google.inject.Inject) field. */
  public class TestClass1 {
    // BUG: Diagnostic contains: @Inject int a
    @Inject final int a = 0;

    @Inject
    // BUG: Diagnostic contains: public int b
    public final int b = 0;

    @Inject @Nullable
    // BUG: Diagnostic contains: Object c
    final Object c = null;
  }
}
