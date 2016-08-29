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

/** Tests for {@code ProvidesMethodOutsideOfModule} */
public class ProvidesMethodOutsideOfModulePositiveCases {

  /** Random class contains a provides method. */
  public class TestClass1 {
    // BUG: Diagnostic contains: remove
    @Provides
    void providesBlah() {}
  }

  /** Module contains an anonymous inner with a Provides method. */
  public class TestModule extends AbstractModule {
    @Override
    protected void configure() {
      Object x =
          new Object() {
            // BUG: Diagnostic contains: remove
            @Provides
            void providesBlah() {}
          };
    }
  }

  /** Class has inner module class */
  public class TestClass2 {
    class NestedModule extends AbstractModule {
      @Override
      protected void configure() {}

      @Provides
      int thisIsOk() {
        return 42;
      }
    }

    // BUG: Diagnostic contains: remove
    @Provides
    int thisIsNotOk() {
      return 42;
    }
  }
}
