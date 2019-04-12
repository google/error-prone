---
title: ComparableType
summary: Implementing 'Comparable<T>' where T is not the same as the implementing class is incorrect, since it violates the symmetry contract of compareTo.
layout: bugpattern
tags: ''
severity: ERROR
providesFix: NO_FIX
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
The type argument of `Comparable` should always be the type of the current
class.

For example, do this:

```java {.good}
class Foo implements Comparable<Foo> {
  public int compareTo(Foo other) { ... }
}
```

not this:

```java {.bad}
class Foo implements Comparable<Bar> {
  public int compareTo(Foo other) { ... }
}
```

Implementing `Comparable` for a different type breaks the API contract, which
requires `x.compareTo(y) == -y.compareTo(x)` for all `x` and `y`. If `x` and `y`
are different types, this behaviour can't be guaranteed.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("ComparableType")` to the enclosing element.

----------

### Positive examples
__ComparableTypePositiveCases.java__

{% highlight java %}
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

public class ComparableTypePositiveCases {

  // BUG: Diagnostic contains: [ComparableType]
  public static class CompareClass implements Comparable<Integer> {

    @Override
    public int compareTo(Integer o) {
      return 0;
    }
  }

  // BUG: Diagnostic contains: [ComparableType]
  public static class SerializableComparable implements Serializable, Comparable<Long> {

    @Override
    public int compareTo(Long o) {
      return 0;
    }
  }

  // BUG: Diagnostic contains: [ComparableType]
  public static class ComparableSerializable implements Comparable<Long>, Serializable {

    @Override
    public int compareTo(Long o) {
      return 0;
    }
  }

  // BUG: Diagnostic contains: [ComparableType]
  public static class BadClass implements Comparable<Double>, Comparator<Double> {

    @Override
    public int compareTo(Double o) {
      return 0;
    }

    @Override
    public int compare(Double o1, Double o2) {
      return 0;
    }
  }

  // BUG: Diagnostic contains: [ComparableType]
  public static class AnotherBadClass implements Comparator<Double>, Comparable<Double> {

    @Override
    public int compareTo(Double o) {
      return 0;
    }

    @Override
    public int compare(Double o1, Double o2) {
      return 0;
    }
  }

  public static class A {}

  public static class B extends A {}

  // BUG: Diagnostic contains: [ComparableType]
  public static class C extends A implements Comparable<B> {

    @Override
    public int compareTo(B o) {
      return 0;
    }
  }

  interface Foo {}

  // BUG: Diagnostic contains: [ComparableType]
  static final class Open implements Comparable<Foo> {
    @Override
    public int compareTo(Foo o) {
      return 0;
    }
  }

  // BUG: Diagnostic contains: [ComparableType]
  public abstract static class AClass implements Comparable<Integer> {}

  public static class BClass extends AClass {
    @Override
    public int compareTo(Integer o) {
      return 0;
    }
  }

  // found via flume
  public static class SpendXGetYValues {
    public Comparable<SpendXGetYValues> yToXRatio() {
      // BUG: Diagnostic contains:  [ComparableType]
      return new Comparable<SpendXGetYValues>() {
        @Override
        public int compareTo(SpendXGetYValues other) {
          return 0;
        }
      };
    }
  }

  // BUG: Diagnostic contains: [ComparableType]
  public abstract static class One<T> implements Comparable<T> {}

  public static class Two extends One<Integer> {
    @Override
    public int compareTo(Integer o) {
      return 0;
    }
  }
}
{% endhighlight %}

### Negative examples
__ComparableTypeNegativeCases.java__

{% highlight java %}
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

  // BUG: Diagnostic contains: [ComparableType]
  static final class Open extends HalfOpen implements Comparable<Door> {
    @Override
    public int compareTo(Door o) {
      return 0;
    }
  }

  public static class A {}

  // BUG: Diagnostic contains: [ComparableType]
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

  // BUG: Diagnostic contains: [ComparableType]
  static final class XGram implements Comparable {

    @Override
    public int compareTo(Object o) {
      return 0;
    }
  }
}
{% endhighlight %}

