---
title: ComparableAndComparator
summary: Class should not implement both `Comparable` and `Comparator`
layout: bugpattern
tags: ''
severity: WARNING
providesFix: NO_FIX
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
A `Comparator` is an object that knows how to compare other objects, whereas an
objectimplementing `Comparable` knows how to compare itself to other objects of
the same type.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("ComparableAndComparator")` to the enclosing element.

----------

### Positive examples
__ComparableAndComparatorPositiveCases.java__

{% highlight java %}
/* Copyright 2016 The Error Prone Authors.
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

import java.util.Comparator;

/**
 * @author sulku@google.com (Marsela Sulku)
 * @author mariasam@google.com (Maria Sam)
 */
public class ComparableAndComparatorPositiveCases {

  /** implements both interfaces */
  // BUG: Diagnostic contains: Class should not implement both
  public static class BadClass implements Comparable<BadClass>, Comparator<BadClass> {
    @Override
    public int compareTo(BadClass comparableNode) {
      return 0;
    }

    @Override
    public int compare(BadClass a, BadClass b) {
      return 0;
    }
  }

  /** Superclass test class */
  public static class SuperClass implements Comparator<SuperClass> {
    @Override
    public int compare(SuperClass o1, SuperClass o2) {
      return 0;
    }
  }

  /** SubClass test class */
  // BUG: Diagnostic contains: Class should not implement both
  public static class SubClass extends SuperClass implements Comparable<SubClass> {
    @Override
    public int compareTo(SubClass o) {
      return 0;
    }
  }
}
{% endhighlight %}

### Negative examples
__ComparableAndComparatorNegativeCases.java__

{% highlight java %}
/* Copyright 2016 The Error Prone Authors.
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

import java.util.Comparator;

/** Created by mariasam on 6/5/17. */
public class ComparableAndComparatorNegativeCases {

  /** Class that implements comparable, but also defines a comparator */
  public static class ComparableAndComparatorNested
      implements Comparable<ComparableAndComparatorNested> {

    /** Comparator */
    private static final Comparator<ComparableAndComparatorNested> myComparator =
        new Comparator<ComparableAndComparatorNested>() {

          @Override
          public int compare(ComparableAndComparatorNested o1, ComparableAndComparatorNested o2) {
            return 0;
          }
        };

    @Override
    public int compareTo(ComparableAndComparatorNested o) {
      return 0;
    }
  }

  /** class that only implements comparable */
  public static class OnlyComparable implements Comparable<OnlyComparable> {

    @Override
    public int compareTo(OnlyComparable o) {
      return 0;
    }
  }

  /** class that only implements comparator */
  public static class OnlyComparator implements Comparator<OnlyComparator> {
    @Override
    public int compare(OnlyComparator o1, OnlyComparator o2) {
      return 0;
    }
  }

  /** This test case is here to increase readability */
  // BUG: Diagnostic contains: Class should not implement both
  public static class BadClass implements Comparable<BadClass>, Comparator<BadClass> {
    @Override
    public int compareTo(BadClass comparableNode) {
      return 0;
    }

    @Override
    public int compare(BadClass a, BadClass b) {
      return 0;
    }
  }

  /** Subclass should not cause error */
  public static class BadClassSubclass extends BadClass {
    public int sampleMethod() {
      return 0;
    }
  }

  /** Enums implementing comparator are ok */
  enum TestEnum implements Comparator<Integer> {
    MONDAY,
    TUESDAY,
    WEDNESDAY;

    @Override
    public int compare(Integer one, Integer two) {
      return 0;
    }
  }
}
{% endhighlight %}

