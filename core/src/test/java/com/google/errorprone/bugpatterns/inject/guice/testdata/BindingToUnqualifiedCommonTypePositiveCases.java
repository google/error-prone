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

package com.google.errorprone.bugpatterns.inject.guice.testdata;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

/** Tests for {@code BindingToUnqualifiedCommonType} */
public class BindingToUnqualifiedCommonTypePositiveCases {

  /** Regular module */
  class Module1 extends AbstractModule {
    @Override
    protected void configure() {
      // BUG: Diagnostic contains: BindingToUnqualifiedCommonType
      bind(Integer.class).toInstance(2);

      // BUG: Diagnostic contains: BindingToUnqualifiedCommonType
      bind(String.class).toInstance("Hello");

      // BUG: Diagnostic contains: BindingToUnqualifiedCommonType
      bind(Double.class).toProvider(() -> 42.0);

      // BUG: Diagnostic contains: BindingToUnqualifiedCommonType
      binder().bind(Long.class).toInstance(42L);
    }

    @Provides
    // BUG: Diagnostic contains: BindingToUnqualifiedCommonType
    int providesFoo() {
      return 42;
    }

    @Provides
    // BUG: Diagnostic contains: BindingToUnqualifiedCommonType
    Integer provideBoxedFoo() {
      return 42;
    }

    @Provides
    // BUG: Diagnostic contains: BindingToUnqualifiedCommonType
    String providesGreeting() {
      return "hi";
    }
  }
}
