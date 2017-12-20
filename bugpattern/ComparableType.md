---
title: ComparableType
summary: ' Implementing ''Comparable<T>'' where T is not compatible with the implementing
  class.'
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
When a class implements 'Comparable', it is possible to declare a type 'T' for
Comparable that is incompatible with the implementing class. This violates the
javadoc specs of 'compareTo'.

## But I'm doing something funky with my compareTo method!

The javadoc of [`Object.compareTo(Object)`][objcomp] defines object
comparability very precisely:

> 1) The implementor must ensure sgn(x.compareTo(y)) == -sgn(y.compareTo(x)) for
> all x and y. (This implies that x.compareTo(y) must throw an exception iff
> y.compareTo(x) throws an exception.)
>
> 2) The implementor must also ensure that the relation is transitive:
> (x.compareTo(y)>0 && y.compareTo(z)>0) implies x.compareTo(z)>0.
>
> 3) Finally, the implementor must ensure that x.compareTo(y)==0 implies that
> sgn(x.compareTo(z)) == sgn(y.compareTo(z)), for all z.

Consider the following example:

```java
public static class Foo implements Comparable<Integer> { // FAIL: Integer not compatible with Foo
  @Override
  public int compareTo(Integer o) {
    ...
  }
}
```

Here the type of 'Comparable' is Integer, which is not compatible with the type
of the class `Foo`. This implies that there is no way to satisfy the first
condition: `sgn(Foo.compareTo(Integer)) == -sgn(Integer.compareTo(Foo))` since
Integer's `compareTo` accepts `Integer`, not `Foo`.

[objcomp]: https://docs.oracle.com/javase/8/docs/api/java/lang/Comparable.html#compareTo-T-

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

  // BUG: Diagnostic contains: [ComparableType]
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

  public abstract static class One<T> implements Comparable<T> {}

  // BUG: Diagnostic contains: [ComparableType]
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
{% endhighlight %}

