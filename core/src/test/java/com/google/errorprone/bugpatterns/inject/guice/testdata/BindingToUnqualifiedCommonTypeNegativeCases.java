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

import com.google.common.collect.ImmutableList;
import com.google.inject.AbstractModule;
import com.google.inject.Binder;
import com.google.inject.BindingAnnotation;
import com.google.inject.Module;
import com.google.inject.Provides;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@code BindingToUnqualifiedCommonType} */
public class BindingToUnqualifiedCommonTypeNegativeCases {

  // All of the tagged instances would normally be flagged, but aren't because it's in a JUnit4
  // class
  @RunWith(JUnit4.class)
  static class MyTestClass {
    /** Regular module */
    class Module1 extends AbstractModule {
      @Override
      protected void configure() {
        bind(Integer.class).toInstance(2);
        bind(String.class).toInstance("Hello");
        bind(Double.class).toProvider(() -> 42.0);
        binder().bind(Long.class).toInstance(42L);
      }

      @Provides
      int providesFoo() {
        return 42;
      }

      @Provides
      Integer provideBoxedFoo() {
        return 42;
      }

      @Provides
      String providesGreeting() {
        return "hi";
      }
    }
  }

  /** Regular module */
  class Module1 extends AbstractModule {
    @Override
    protected void configure() {
      // Bindings to unannotated complex instances
      bind(A.class).toInstance(new A());

      // Binding to literals, but with a binding annotation
      bind(Integer.class).annotatedWith(MyBindingAnnotation.class).toInstance(42);
    }

    @Provides
    List<Integer> providesFoo() {
      return ImmutableList.of(42);
    }
  }

  /** implements the Module interface directly */
  class Module2 implements Module {
    @Override
    public void configure(Binder binder) {}

    @Provides
    @MyBindingAnnotation
    int providesFoo() {
      return 42;
    }
  }

  class A {}

  @BindingAnnotation
  @Retention(RetentionPolicy.RUNTIME)
  @interface MyBindingAnnotation {}
}
