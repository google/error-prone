/*
 * Copyright 2015 The Error Prone Authors.
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

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** @author avenet@google.com (Arnaud J. Venet) */
public class EqualsIncompatibleTypePositiveCases {
  class A {}

  class B {}

  void checkEqualsAB(A a, B b) {
    // BUG: Diagnostic contains: incompatible types
    a.equals(b);
    // BUG: Diagnostic contains: incompatible types
    b.equals(a);
  }

  class C {}

  abstract class C1 extends C {
    public abstract boolean equals(Object o);
  }

  abstract class C2 extends C1 {}

  abstract class C3 extends C {}

  void checkEqualsCC1C2C3(C c, C1 c1, C2 c2, C3 c3) {
    // BUG: Diagnostic contains: incompatible types
    c3.equals(c1);
    // BUG: Diagnostic contains: incompatible types
    c3.equals(c2);
    // BUG: Diagnostic contains: incompatible types
    c1.equals(c3);
    // BUG: Diagnostic contains: incompatible types
    c2.equals(c3);
  }

  void checkStaticEqualsCC1C2C3(C c, C1 c1, C2 c2, C3 c3) {
    // BUG: Diagnostic contains: incompatible types
    java.util.Objects.equals(c3, c1);
    // BUG: Diagnostic contains: incompatible types
    java.util.Objects.equals(c3, c2);
    // BUG: Diagnostic contains: incompatible types
    java.util.Objects.equals(c1, c3);
    // BUG: Diagnostic contains: incompatible types
    java.util.Objects.equals(c2, c3);
  }

  void checkGuavaStaticEqualsCC1C2C3(C c, C1 c1, C2 c2, C3 c3) {
    // BUG: Diagnostic contains: incompatible types
    com.google.common.base.Objects.equal(c3, c1);
    // BUG: Diagnostic contains: incompatible types
    com.google.common.base.Objects.equal(c3, c2);
    // BUG: Diagnostic contains: incompatible types
    com.google.common.base.Objects.equal(c1, c3);
    // BUG: Diagnostic contains: incompatible types
    com.google.common.base.Objects.equal(c2, c3);
  }

  void checkPrimitiveEquals(int a, long b) {
    // BUG: Diagnostic contains: incompatible types
    java.util.Objects.equals(a, b);
    // BUG: Diagnostic contains: incompatible types
    java.util.Objects.equals(b, a);
    // BUG: Diagnostic contains: incompatible types
    com.google.common.base.Objects.equal(a, b);
    // BUG: Diagnostic contains: incompatible types
    com.google.common.base.Objects.equal(b, a);
  }

  interface I {
    public boolean equals(Object o);
  }

  class D {}

  class D1 extends D {}

  class D2 extends D implements I {}

  void checkEqualsDD1D2(D d, D1 d1, D2 d2) {
    // BUG: Diagnostic contains: incompatible types
    d1.equals(d2);
    // BUG: Diagnostic contains: incompatible types
    d2.equals(d1);
  }

  enum MyEnum {}

  enum MyOtherEnum {}

  void enumEquals(MyEnum m, MyOtherEnum mm) {
    // BUG: Diagnostic contains: incompatible types
    m.equals(mm);
    // BUG: Diagnostic contains: incompatible types
    mm.equals(m);

    // BUG: Diagnostic contains: incompatible types
    com.google.common.base.Objects.equal(m, mm);
    // BUG: Diagnostic contains: incompatible types
    com.google.common.base.Objects.equal(mm, m);
  }

  void collectionsWithGenericMismatches(
      List<String> stringList,
      List<Integer> intList,
      Set<String> stringSet,
      Set<Integer> intSet,
      ImmutableList<String> stringImmutableList) {

    // BUG: Diagnostic contains: incompatible types
    stringList.equals(intList);

    // BUG: Diagnostic contains: incompatible types
    stringSet.equals(intSet);

    // BUG: Diagnostic contains: incompatible types
    stringList.equals(stringSet);

    // BUG: Diagnostic contains: incompatible types
    intList.equals(stringImmutableList);
  }

  void mapKeyChecking(
      Map<String, Integer> stringIntegerMap,
      Map<Integer, String> integerStringMap,
      Map<List<String>, Set<String>> stringListSetMap,
      Map<List<String>, Set<Integer>> intListSetMap) {
    // BUG: Diagnostic contains: incompatible types
    stringIntegerMap.equals(integerStringMap);

    // BUG: Diagnostic contains: incompatible types
    stringListSetMap.equals(intListSetMap);
  }

  void nestedColls(Set<List<String>> setListString, Set<List<Integer>> setListInteger) {
    // BUG: Diagnostic contains: String and Integer are incompatible
    boolean equals = setListString.equals(setListInteger);
  }

  class MyGenericClazz<T> {}

  <T extends I> void testSomeGenerics(
      MyGenericClazz<String> strClazz, MyGenericClazz<Integer> intClazz, MyGenericClazz<T> iClazz) {
    // BUG: Diagnostic contains: String and Integer are incompatible
    strClazz.equals(intClazz);

    // BUG: Diagnostic contains: T and String are incompatible
    iClazz.equals(strClazz);
  }
}
