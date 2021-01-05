/*
 * Copyright 2020 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.testdata;

import java.util.Objects;

/** @author vlk@google.com (Volodymyr Kachurovskyi) */
public class ObjectEqualsForPrimitivesPositiveCases {

  public void booleans() {
    boolean a = true;
    boolean b = false;

    // BUG: Diagnostic contains: (a == b)
    if (Objects.equals(a, b)) {
      System.out.println("values are equal");
    } else {
      System.out.println("values are not equal");
    }
  }

  public void ints() {
    int a = 1;
    int b = 2;

    // BUG: Diagnostic contains: (a == b)
    if (Objects.equals(a, b)) {
      System.out.println("values are equal");
    } else {
      System.out.println("values are not equal");
    }
  }

  public void longs() {
    long a = 1;
    long b = 2;

    // BUG: Diagnostic contains: (a == b)
    if (Objects.equals(a, b)) {
      System.out.println("values are equal");
    } else {
      System.out.println("values are not equal");
    }
  }

  public void doubles() {
    double a = 1.0;
    double b = 2.0;

    // BUG: Diagnostic contains: (a == b)
    if (Objects.equals(a, b)) {
      System.out.println("values are equal");
    } else {
      System.out.println("values are not equal");
    }
  }

  public void mixed() {
    int a = 1;
    long b = 2;

    // BUG: Diagnostic contains: (a == b)
    if (Objects.equals(a, b)) {
      System.out.println("values are equal");
    } else {
      System.out.println("values are not equal");
    }
  }

  public void withNegation() {
    boolean a = true;
    boolean b = false;

    // BUG: Diagnostic contains: !(a == b)
    if (!Objects.equals(a, b)) {
      System.out.println("values are not equal");
    } else {
      System.out.println("values are equal");
    }
  }
}
