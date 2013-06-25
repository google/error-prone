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

import com.google.inject.Singleton;

/**
 * @author sgoldfeder@google.com (Steven Goldfeder)
 */
public class InjectScopeAnnotationOnInterfaceOrAbstractClassNegativeCases {
  /**
   * concrete class has no scoping annotation
   */
  public class TestClass1 {
  }

  /**
   * abstract class has no scoping annotation
   */
  public abstract class TestClass2 {
  }
  /**
   * interface has no scoping annotation
   */
  public interface TestClass3 {
  }
  /**
   * concrete class has scoping annotation
   */
  @Singleton
  public class TestClass4 {
  }
}
