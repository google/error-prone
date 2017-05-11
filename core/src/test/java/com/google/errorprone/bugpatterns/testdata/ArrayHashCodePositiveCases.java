/*
 * Copyright 2014 Google Inc. All Rights Reserved.
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
public class ArrayHashCodePositiveCases {
  private Object[] objArray = {1, 2, 3};
  private String[] stringArray = {"1", "2", "3"};
  private int[] intArray = {1, 2, 3};
  private byte[] byteArray = {1, 2, 3};
  private int[][] multidimensionalIntArray = {{1, 2, 3}, {4, 5, 6}};
  private String[][] multidimensionalStringArray = {{"1", "2", "3"}, {"4", "5", "6"}};

  public void objectHashCode() {
    int hashCode;
    // BUG: Diagnostic contains: Arrays.hashCode(objArray)
    hashCode = objArray.hashCode();
    // BUG: Diagnostic contains: Arrays.hashCode(stringArray)
    hashCode = stringArray.hashCode();
    // BUG: Diagnostic contains: Arrays.hashCode(intArray)
    hashCode = intArray.hashCode();

    // BUG: Diagnostic contains: Arrays.deepHashCode(multidimensionalIntArray)
    hashCode = multidimensionalIntArray.hashCode();
    // BUG: Diagnostic contains: Arrays.deepHashCode(multidimensionalStringArray)
    hashCode = multidimensionalStringArray.hashCode();
  }

  public void guavaObjectsHashCode() {
    int hashCode;
    // BUG: Diagnostic contains: Arrays.hashCode(intArray)
    hashCode = Objects.hashCode(intArray);
    // BUG: Diagnostic contains: Arrays.hashCode(byteArray)
    hashCode = Objects.hashCode(byteArray);

    // BUG: Diagnostic contains: Arrays.deepHashCode(multidimensionalIntArray)
    hashCode = Objects.hashCode(multidimensionalIntArray);
    // BUG: Diagnostic contains: Arrays.deepHashCode(multidimensionalStringArray)
    hashCode = Objects.hashCode(multidimensionalStringArray);
  }

  public void varargsHashCodeOnMoreThanOneArg() {
    int hashCode;
    // BUG: Diagnostic contains: Objects.hashCode(Arrays.hashCode(objArray),
    // Arrays.hashCode(intArray))
    hashCode = Objects.hashCode(objArray, intArray);
    // BUG: Diagnostic contains: Objects.hashCode(Arrays.hashCode(stringArray),
    // Arrays.hashCode(byteArray))
    hashCode = Objects.hashCode(stringArray, byteArray);

    Object obj1 = new Object();
    Object obj2 = new Object();
    // BUG: Diagnostic contains: Objects.hashCode(obj1, obj2, Arrays.hashCode(intArray))
    hashCode = Objects.hashCode(obj1, obj2, intArray);
    // BUG: Diagnostic contains: Objects.hashCode(obj1, Arrays.hashCode(intArray), obj2)
    hashCode = Objects.hashCode(obj1, intArray, obj2);
    // BUG: Diagnostic contains: Objects.hashCode(Arrays.hashCode(intArray), obj1, obj2)
    hashCode = Objects.hashCode(intArray, obj1, obj2);

    // BUG: Diagnostic contains: Objects.hashCode(obj1, obj2,
    // Arrays.deepHashCode(multidimensionalIntArray))
    hashCode = Objects.hashCode(obj1, obj2, multidimensionalIntArray);
    // BUG: Diagnostic contains: Objects.hashCode(obj1, obj2,
    // Arrays.deepHashCode(multidimensionalStringArray))
    hashCode = Objects.hashCode(obj1, obj2, multidimensionalStringArray);
  }
}
