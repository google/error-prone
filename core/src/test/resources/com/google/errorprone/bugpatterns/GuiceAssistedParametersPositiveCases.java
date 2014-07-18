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

import com.google.inject.assistedinject.Assisted;

import java.util.List;

import javax.inject.Inject;

/**
 * @author sgoldfeder@google.com(Steven Goldfeder)
 */
public class GuiceAssistedParametersPositiveCases {

  /**
   * Class has constructor with two @Assisted parameters of the same type.
   */
  public class TestClass1 {
    @Inject
    public TestClass1(int n,
                      // BUG: Diagnostic contains: remove
                      @Assisted
                      String x,
                      // BUG: Diagnostic contains: remove
                      @Assisted 
                      String y, int z) {}
  }

  /**
   * Class has constructor with two @Assisted parameters of the same type and same value.
   */
  public class TestClass2 {
    @Inject
    public TestClass2(int n,
                      // BUG: Diagnostic contains: remove
                      @Assisted("foo") 
                      int x,
                      // BUG: Diagnostic contains: remove
                      @Assisted("foo") 
                      int y, String z) {}
  }

  /**
   * Class has constructor with two @Assisted parameters of the same parameterized type.
   */
  public class TestClass3 {
    @Inject
    public TestClass3(int n,
                      // BUG: Diagnostic contains: remove
                      @Assisted("foo")
                      List<String> x,
                      // BUG: Diagnostic contains: remove
                      @Assisted("foo")
                      List<String> y, String z) {}
  }
}
