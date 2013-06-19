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

package com.google.errorprone.bugpatterns;

import com.google.inject.Provides;
import com.google.inject.Singleton;

/**
 * @author sgoldfeder@google.com (Steven Goldfeder)
 */
public class InjectMisuseOfScopingAnnotationNegativeCases {

  /**
   * Class has no scoping annotation.
   */
  public class TestClass1 {
    public TestClass1(int n) {}
  }

  /**
   * has a scoping annotation on the class
   */
  @Singleton
  public class TestClass2 {
    public TestClass2(int n) {}
  }

  /**
   * Class has scoping annotation on a @Provides method
   */
  public class TestClass3 {
    public TestClass3(int n) {}

    @Provides
    @Singleton
    String provideString() {
      return "";
    }
  }
}
