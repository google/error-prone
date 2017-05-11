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

import com.google.inject.Inject;
import java.beans.ConstructorProperties;

/** @author sgoldfeder@google.com(Steven Goldfeder) */
public class MoreThanOneInjectableConstructorPositiveCases {

  /** Class has 2 constructors, both are injectable */
  // BUG: Diagnostic contains: MoreThanOneInjectableConstructor
  public class TestClass1 {
    @Inject
    public TestClass1() {}

    @Inject
    public TestClass1(int n) {}
  }

  /** Class has 3 constructors, two of which are injectable. */
  // BUG: Diagnostic contains: MoreThanOneInjectableConstructor
  public class TestClass2 {
    @Inject
    public TestClass2() {}

    public TestClass2(int n) {}

    @Inject
    public TestClass2(String s) {}
  }

  /**
   * testing that the error appears on the @Inject annotation even in the presence of other
   * annotations
   */
  // BUG: Diagnostic contains: MoreThanOneInjectableConstructor
  public class TestClass3 {
    @Inject
    public TestClass3() {}

    @Inject
    @ConstructorProperties({"m", "n"})
    public TestClass3(int m, int n) {}
  }

  /** Fails on 3 constructors w/ @Inject */
  // BUG: Diagnostic contains: MoreThanOneInjectableConstructor
  public class TestClass4 {
    @Inject
    public TestClass4() {}

    @Inject
    public TestClass4(int m, int n) {}

    @Inject
    public TestClass4(int m, int n, boolean x) {}
  }
}
