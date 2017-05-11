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

package com.google.errorprone.bugpatterns.inject.testdata;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.servlet.SessionScoped;
import dagger.Component;
import dagger.Subcomponent;
import dagger.producers.ProductionComponent;
import dagger.producers.ProductionSubcomponent;

/** @author sgoldfeder@google.com(Steven Goldfeder) */
public class MoreThanOneScopeAnnotationOnClassNegativeCases {

  /** Class has no annotation. */
  public class TestClass1 {}
  /** Class has a single non scoping annotation. */
  @SuppressWarnings("foo")
  public class TestClass2 {}

  /** Class hasa single scoping annotation. */
  @Singleton
  public class TestClass3 {}

  /** Class has two annotations, one of which is a scoping annotation. */
  @Singleton
  @SuppressWarnings("foo")
  public class TestClass4 {}

  /**
   * Class has two annotations, one of which is a scoping annotation. Class also has a method with a
   * scoping annotation.
   */
  @SuppressWarnings("foo")
  public class TestClass5 {
    @Singleton
    @Provides
    public void foo() {}
  }

  /** Class has two scoped annotations, but is a Dagger component */
  @Singleton
  @SessionScoped
  @Component
  public class DaggerComponent {}

  /** Class has two scoped annotations, but is a Dagger subcomponent */
  @Singleton
  @SessionScoped
  @Subcomponent
  public class DaggerSubcomponent {}

  /** Class has two scoped annotations, but is a Dagger component */
  @Singleton
  @SessionScoped
  @ProductionComponent
  public class DaggerProductionComponent {}

  /** Class has two scoped annotations, but is a Dagger subcomponent */
  @Singleton
  @SessionScoped
  @ProductionSubcomponent
  public class DaggerProductionSubcomponent {}

  /** Suppression through secondary name */
  @SuppressWarnings("MoreThanOneScopeAnnotationOnClass")
  public class TestClass6 {}
}
