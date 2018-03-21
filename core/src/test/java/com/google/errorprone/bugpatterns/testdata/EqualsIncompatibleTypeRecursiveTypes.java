/*
 * Copyright 2018 The Error Prone Authors.
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

import java.time.DayOfWeek;
import java.time.Month;
import java.util.Objects;

/** Checks for objects with recursive type bounds. */
public class EqualsIncompatibleTypeRecursiveTypes {

  interface Bar<X, Y> {}

  final class ConcreteBar<X, Y> implements Bar<X, Y> {}

  static class Foo<T extends Enum<T>> implements Bar<String, T> {
    T field;

    void check(Foo<?> other) {
      // OK since Enum<?> and Enum<T> are not incompatible
      this.field.equals(other.field);
    }

    <X extends ConcreteBar<String, X>> void badCheck(Bar<String, X> other) {
      // BUG: Diagnostic contains: T and X are incompatible
      this.equals(other);
    }
  }

  interface RandomInterface {}

  interface Entity<
      E extends Entity<E, K, V, V2>,
      K extends EntityKey<K>,
      V extends Enum<V>,
      V2 extends Enum<V2>> {}

  interface EntityKey<K extends EntityKey<K>> extends Comparable<K> {}

  static final class EK1 implements EntityKey<EK1> {
    @Override
    public int compareTo(EK1 o) {
      return 0;
    }
  }

  static final class E1 implements Entity<E1, EK1, DayOfWeek, Month>, RandomInterface {}

  static final class E2 implements Entity<E2, EK1, Month, DayOfWeek>, RandomInterface {}

  void testMultilayer(Class<? extends Entity<?, ?, ?, ?>> eClazz, Class<? extends E2> e2Clazz) {
    if (Objects.equals(eClazz, E1.class)) {
      System.out.println("yay");
    }

    if (Objects.equals(eClazz, E2.class)) {
      System.out.println("yay");
    }

    if (Objects.equals(e2Clazz, E2.class)) {
      System.out.println("yay");
    }

    // BUG: Diagnostic contains: E2 and E1 are incompatible.
    if (Objects.equals(e2Clazz, E1.class)) {
      System.out.println("boo");
    }
  }

  interface First<A extends First<A>> {
    default A get() {
      return null;
    }
  }

  interface Second<B> extends First<Second<B>> {}

  interface Third extends Second<Third> {}

  interface Fourth extends Second<Fourth> {}

  void testing(Third third, Fourth fourth) {
    // BUG: Diagnostic contains: Third and Fourth
    boolean equals = third.equals(fourth);
  }

  interface RecOne extends Comparable<Comparable<RecOne>> {}

  interface RecTwo extends Comparable<Comparable<RecTwo>> {}

  void testMultiRecursion(RecOne a, RecTwo b) {
    // BUG: Diagnostic contains: RecOne and RecTwo
    boolean bad = a.equals(b);
  }

  interface Quux<A extends Quux<A>> {}

  interface Quuz<A extends Quux<A>> extends Quux<A> {}

  interface Quiz<A extends Quux<A>> extends Quux<A> {}

  interface Id1 extends Quuz<Id1> {}

  interface Id2 extends Quiz<Id2> {}

  abstract static class Id3 implements Quuz<Id3>, Quiz<Id3> {}

  void test(Id1 first, Id3 second) {
    // BUG: Diagnostic contains: Id1 and Id3
    boolean res = Objects.equals(first, second);
  }

  class I<T> {}

  class J<A extends I<B>, B extends I<C>, C extends I<A>> {}

  <
          A1 extends I<B1>,
          B1 extends I<C1>,
          C1 extends I<A1>,
          A2 extends I<B2>,
          B2 extends I<C2>,
          C2 extends I<A2>>
      void something(J<A1, B1, C1> j1, J<A2, B2, C2> j2) {
    // Technically this could work, since there's nothing stopping A1 == A2, etc.
    boolean equals = j1.equals(j2);
  }
}
