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

/** @author sgoldfeder@google.com(Steven Goldfeder) */
public class MoreThanOneInjectableConstructorNegativeCases {

  /** Class has a single non-injectable constructor. */
  public class TestClass1 {
    public TestClass1() {}
  }

  /** Class has a single injectable constructor. */
  public class TestClass2 {
    @Inject
    public TestClass2() {}
  }

  /** Class has two constructors, one of which is injectable. */
  public class TestClass3 {
    @Inject
    public TestClass3() {}

    public TestClass3(int n) {}
  }

  /** Class has two constructors, one of which is injectable. Class also has an injectable field. */
  public class TestClass4 {

    @Inject String x;

    @Inject
    public TestClass4() {}

    public TestClass4(int n) {}
  }

  /** Class has 2 constructors, both are injectable. Error is suppressed. */
  @SuppressWarnings("MoreThanOneInjectableConstructor")
  public class TestClass5 {
    @Inject
    public TestClass5() {}

    @Inject
    public TestClass5(int n) {}
  }

  /** Suppressed class */
  @SuppressWarnings("inject-constructors")
  public class TestClass6 {
    @Inject
    public TestClass6() {}

    @Inject
    public TestClass6(int n) {}
  }

  /** Suppressed class */
  @SuppressWarnings("InjectMultipleAtInjectConstructors")
  public class TestClass7 {
    @Inject
    public TestClass7() {}

    @Inject
    public TestClass7(int n) {}
  }
}
