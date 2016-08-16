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

/**
 * @author sgoldfeder@gooogle.com (Steven Goldfeder)
 */
public class OverridesJavaxInjectableMethodPositiveCases {

  /** Class with foo() */
  public class TestClass0 {
    public void foo() {}
  }

  /**
   * Class with a method foo() that is annotated with {@code javax.inject.Inject}. Other test
   * classes will extend this class.
   */
  public class TestClass1 extends TestClass0 {
    @javax.inject.Inject
    public void foo() {}
  }

  /**
   * Class with a method foo() that is not annotated, but overrides a method annotated with
   * @javax.inject.Inject.
   */
  public class TestClass2 extends TestClass1 {
    // BUG: Diagnostic contains: @Inject
    public void foo() {}
  }
  
  /**
   * Class with a method foo() that is not annotated, but overrides a method that in turn overrides
   * a method that is annotated with @javax.inject.Inject.
   */
  public class TestClass3 extends TestClass2 {
    // BUG: Diagnostic contains: @Inject
    public void foo() {}
  }
}
