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

import javax.inject.Inject;

/** @author sgoldfeder@google.com (Steven Goldfeder) */
public class JavaxInjectOnAbstractMethodNegativeCases {

  /** Concrete class has no methods or annotations. */
  public class TestClass1 {}

  /** Abstract class has a single abstract method with no annotation. */
  public abstract class TestClass2 {
    abstract void abstractMethod();
  }

  /** Concrete class has an injectable method. */
  public class TestClass3 {
    @Inject
    public void foo() {}
  }

  /** Abstract class has an injectable concrete method. */
  public abstract class TestClass4 {
    abstract void abstractMethod();

    @Inject
    public void concreteMethod() {}
  }

  /**
   * Abstract class has an com.google.inject.Inject abstract method (This is allowed; Injecting
   * abstract methods is only forbidden with javax.inject.Inject).
   */
  public abstract class TestClass5 {
    @com.google.inject.Inject
    abstract void abstractMethod();
  }

  /** Abstract class has an injectable(javax.inject.Inject) abstract method. Error is suppressed. */
  public abstract class TestClass6 {
    @SuppressWarnings("JavaxInjectOnAbstractMethod")
    @javax.inject.Inject
    abstract void abstractMethod();
  }
}
