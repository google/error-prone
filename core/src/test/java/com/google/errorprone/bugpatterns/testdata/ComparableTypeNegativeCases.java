/* Copyright 2017 The Error Prone Authors.
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

import java.io.Serializable;
import java.util.Comparator;

public class ComparableTypeNegativeCases {

  /** Class that implements comparable, with castable type */
  public static class ComparableTypeNegative implements Comparable<ComparableTypeNegative> {

    @Override
    public int compareTo(ComparableTypeNegative o) {
      return 0;
    }
  }

  /** abstract class that implements comparable */
  public abstract static class OnlyComparable implements Comparable<OnlyComparable> {}

  /** class that implements comparable and something else like Serializable */
  public static class SerializableComparable
      implements Serializable, Comparable<SerializableComparable> {

    @Override
    public int compareTo(SerializableComparable o) {
      return 0;
    }
  }

  /** class that implements comparable and something else with a type */
  public static class SomeClass implements Comparable<SomeClass>, Comparator<SomeClass> {
    @Override
    public int compareTo(SomeClass comparableNode) {
      return 0;
    }

    @Override
    public int compare(SomeClass a, SomeClass b) {
      return 0;
    }
  }

  // Example interfaces
  interface Door {}

  public static class HalfOpen implements Door {}

  static final class Open extends HalfOpen implements Comparable<Door> {
    @Override
    public int compareTo(Door o) {
      return 0;
    }
  }

  public static class A {}

  public static class B extends A implements Comparable<A> {

    @Override
    public int compareTo(A o) {
      return 0;
    }
  }

  // ignore enums
  enum Location {
    TEST_TARGET
  }

  public abstract static class AClass implements Comparable<AClass> {}

  public static class BClass extends AClass {
    @Override
    public int compareTo(AClass o) {
      return 0;
    }
  }

  abstract class Foo<T> implements Comparable<Foo<T>> {}

  class T extends Foo<String> {
    public int compareTo(Foo<String> o) {
      return 0;
    }
  }

  static final class XGram implements Comparable {

    @Override
    public int compareTo(Object o) {
      return 0;
    }
  }
}
