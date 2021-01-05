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
public class ObjectEqualsForPrimitivesNegativeCases {

  public void boxedInts() {
    Integer a = Integer.valueOf(1);
    Integer b = Integer.valueOf(2);

    if (Objects.equals(a, b)) {
      System.out.println("values are equal");
    } else {
      System.out.println("values are not equal");
    }
  }

  public void primitiveAndBoxed() {
    int a = 1;
    Integer b = Integer.valueOf(2);

    if (Objects.equals(a, b)) {
      System.out.println("values are equal");
    } else {
      System.out.println("values are not equal");
    }

    if (Objects.equals(b, a)) {
      System.out.println("values are equal");
    } else {
      System.out.println("values are not equal");
    }
  }

  public void objects() {
    Object a = new Object();
    Object b = new Object();

    if (Objects.equals(a, b)) {
      System.out.println("values are equal");
    } else {
      System.out.println("values are not equal");
    }
  }
}
