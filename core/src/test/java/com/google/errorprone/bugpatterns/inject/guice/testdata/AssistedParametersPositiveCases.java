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

import com.google.inject.assistedinject.Assisted;
import java.util.List;
import javax.inject.Inject;

/**
 * @author sgoldfeder@google.com(Steven Goldfeder)
 */
public class AssistedParametersPositiveCases {

  /**
   * Class has constructor with two @Assisted parameters of the same type.
   */
  public class TestClass1 {
    @Inject
    // BUG: Diagnostic contains: java.lang.String: x, y
    public TestClass1(int n, @Assisted String x, @Assisted String y, int z) {}

    @Inject
    // BUG: Diagnostic contains: java.lang.String, @Assisted("baz"): x, z
    public TestClass1(
        @Assisted("foo") int a,
        @Assisted("foo") int b,
        @Assisted("baz") String x,
        @Assisted("baz") String z) {}
  }

  /**
   * Class has constructor with two @Assisted parameters of the same type and same value.
   */
  public class TestClass2 {
    @Inject
    // BUG: Diagnostic contains: int, @Assisted("foo"): x, y
    public TestClass2(int n, @Assisted("foo") int x, @Assisted("foo") int y, String z) {}
  }

  /**
   * Class has constructor with two @Assisted parameters of the same parameterized type.
   */
  public class TestClass3 {
    private static final String FOO = "foo";

    @Inject
    // BUG: Diagnostic contains: java.util.List<java.lang.String>, @Assisted("foo"): x, y
    public TestClass3(
        int n, @Assisted("foo") List<String> x, @Assisted(FOO) List<String> y, String z) {}

    @Inject
    // BUG: Diagnostic contains: int, @Assisted("bar"): x, y
    public TestClass3(
        @Assisted() int n, @Assisted("bar") int x, @Assisted("bar") int y, String z) {}
  }

  class GenericClass<T> {
    @Inject
    // BUG: Diagnostic contains: T: a, b
    GenericClass(@Assisted T a, @Assisted T b) {}

    @Inject
    // BUG: Diagnostic contains: int: a, b
    GenericClass(@Assisted Integer a, @Assisted int b) {}
  }
}
