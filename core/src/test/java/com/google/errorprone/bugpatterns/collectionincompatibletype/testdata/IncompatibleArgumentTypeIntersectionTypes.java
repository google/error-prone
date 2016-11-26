/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

package com.google.errorprone.bugpatterns.collectionincompatibletype.testdata;

import com.google.errorprone.annotations.CompatibleWith;

/** Test intersection types. */
public class IncompatibleArgumentTypeIntersectionTypes {

  interface Nothing {}

  interface Something {}

  interface Everything extends Nothing, Something {}

  class Test<X extends Nothing & Something> {
    void doSomething(@CompatibleWith("X") Object whatever) {}
  }

  class ArrayTest<X> {
    void doSomething(@CompatibleWith("X") Object whatever) {}
  }

  void testStuff(Test<Everything> someTest, Everything[] everythings, Nothing nothing) {
    // Final classes (Integer) can't be cast to an interface they don't implement
    // BUG: Diagnostic contains: int is not compatible with the required type: Everything
    someTest.doSomething(123);

    // Non-final classes can.
    someTest.doSomething((Object) 123);

    // Arrays can't, since they can only be cast to Serializable
    // BUG: Diagnostic contains: Everything[] is not compatible with the required type: Everything
    someTest.doSomething(everythings);

    // BUG: Diagnostic contains: Everything[][] is not compatible with the required type: Everything
    someTest.doSomething(new Everything[][] {everythings});

    // OK (since some other implementer of Nothing could implement Everything)
    someTest.doSomething(nothing);
  }

  void testArraySpecialization(
      ArrayTest<Number[]> arrayTest, Integer[] ints, Object[] objz, String[] strings) {
    arrayTest.doSomething(ints);

    arrayTest.doSomething(objz);

    // BUG: Diagnostic contains: String[] is not compatible with the required type: Number[]
    arrayTest.doSomething(strings);
  }
}
