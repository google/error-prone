/*
 * Copyright 2012 Google Inc. All Rights Reserved.
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

import com.google.common.base.Objects;

/** @author eaftan@google.com (Eddie Aftandilian) */
public class ArrayEqualsNegativeCases {
  public void neitherArray() {
    Object a = new Object();
    Object b = new Object();

    if (a.equals(b)) {
      System.out.println("Objects are equal!");
    } else {
      System.out.println("Objects are not equal!");
    }

    if (Objects.equal(a, b)) {
      System.out.println("Objects are equal!");
    } else {
      System.out.println("Objects are not equal!");
    }
  }

  public void firstArray() {
    Object[] a = new Object[3];
    Object b = new Object();

    if (a.equals(b)) {
      System.out.println("arrays are equal!");
    } else {
      System.out.println("arrays are not equal!");
    }

    if (Objects.equal(a, b)) {
      System.out.println("Objects are equal!");
    } else {
      System.out.println("Objects are not equal!");
    }
  }

  public void secondArray() {
    Object a = new Object();
    Object[] b = new Object[3];

    if (a.equals(b)) {
      System.out.println("arrays are equal!");
    } else {
      System.out.println("arrays are not equal!");
    }

    if (Objects.equal(a, b)) {
      System.out.println("Objects are equal!");
    } else {
      System.out.println("Objects are not equal!");
    }
  }
}
