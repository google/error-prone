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

import com.google.inject.assistedinject.AssistedInject;

/** @author sgoldfeder@google.com (Steven Goldfeder) */
public class AssistedInjectAndInjectOnSameConstructorNegativeCases {
  /** Class has a single constructor with no annotation. */
  public class TestClass1 {
    TestClass1() {}
  }

  /** Class has a constructor with a @javax.inject.Inject annotation. */
  public class TestClass2 {
    @javax.inject.Inject
    public TestClass2() {}
  }

  /** Class has a constructor with a @com.google.injectInject annotation. */
  public class TestClass3 {
    @com.google.inject.Inject
    public TestClass3() {}
  }

  /** Class has a constructor annotated with @AssistedInject */
  public class TestClass4 {
    @AssistedInject
    public TestClass4() {}
  }

  /** Class has one constructor with a @AssistedInject and one with @javax.inject.inject . */
  public class TestClass5 {
    @javax.inject.Inject
    public TestClass5(int n) {}

    @AssistedInject
    public TestClass5() {}
  }

  /** Class has one constructor with a @AssistedInject and one with @javax.inject.inject . */
  public class TestClass6 {
    @com.google.inject.Inject
    public TestClass6(int n) {}

    @AssistedInject
    public TestClass6() {}
  }

  /**
   * Class has a constructor annotated with @javax.inject.Inject and @AssistedInject. Error is
   * suppressed.
   */
  @SuppressWarnings("AssistedInjectAndInjectOnSameConstructor")
  public class TestClass7 {
    @javax.inject.Inject
    @AssistedInject
    public TestClass7() {}
  }

  /**
   * Class has a constructor annotated with @com.google.inject.Inject and @AssistedInject. Error is
   * suppressed.
   */
  @SuppressWarnings("AssistedInjectAndInjectOnSameConstructor")
  public class TestClass8 {
    @com.google.inject.Inject
    @AssistedInject
    public TestClass8() {}
  }
}
