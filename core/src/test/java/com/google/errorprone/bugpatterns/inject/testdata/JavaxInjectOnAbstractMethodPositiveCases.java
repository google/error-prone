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

/** @author sgoldfeder@google.com (Steven Goldfeder) */
public class JavaxInjectOnAbstractMethodPositiveCases {

  /** Abstract class has an injectable(javax.inject.Inject) abstract method. */
  public abstract class TestClass1 {
    // BUG: Diagnostic contains: remove
    @javax.inject.Inject
    abstract void abstractMethod();
  }

  /**
   * Abstract class has an injectable(javax.inject.Inject) abstract method and an unrelated concrete
   * method.
   */
  public abstract class TestClass2 {
    // BUG: Diagnostic contains: remove
    @javax.inject.Inject
    abstract void abstractMethod();

    public void foo() {}
  }

  /**
   * Abstract class has an injectable(javax.inject.Inject) abstract method and an unrelated abstract
   * method.
   */
  public abstract class TestClass3 {
    // BUG: Diagnostic contains: remove
    @javax.inject.Inject
    abstract void abstractMethod1();

    abstract void abstractMethod2();
  }

  /** Interface with Inject method, should also fail. */
  public interface TestInterface {
    // BUG: Diagnostic contains: remove
    @javax.inject.Inject
    void abstractMethod();
  }

  /** Concrete Implementer of interface. */
  public class Implementing implements TestInterface {
    // No error here
    public void abstractMethod() {}
  }

  /** Abstract implementer of interface. */
  public abstract class AbstractImplementing implements TestInterface {
    // BUG: Diagnostic contains: remove
    @javax.inject.Inject
    public abstract void abstractMethod();
  }

  interface HasDefault {
    // BUG: Diagnostic contains: remove
    @javax.inject.Inject
    default void foo() {}
  }
}
