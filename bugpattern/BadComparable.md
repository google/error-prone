---
title: BadComparable
summary: Possible sign flip from narrowing conversion
layout: bugpattern
tags: FragileCode
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
A narrowing integral conversion can cause a sign flip, since it simply discards all but the n lowest order bits, where n is the number of bits used to represent the target type (JLS 5.1.3). In a compare or compareTo method, this can cause incorrect and unstable sort orders.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("BadComparable")` annotation to the enclosing element.

----------

### Positive examples
__BadComparablePositiveCases.java__

{% highlight java %}
/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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

/** @author irogers@google.com (Ian Rogers) */
public class BadComparablePositiveCases {
  static class ComparableTest implements Comparable<ComparableTest> {
    private final long value = 0;

    public int compareTo(ComparableTest other) {
      // BUG: Diagnostic contains: return Long.compare(value, other.value);
      return (int) (value - other.value);
    }
  }

  static class BoxedComparableTest implements Comparable<BoxedComparableTest> {
    private final Long value = Long.valueOf(0);

    public int compareTo(BoxedComparableTest other) {
      // BUG: Diagnostic contains: return value.compareTo(other.value);
      return (int) (value - other.value);
    }
  }

  static final Comparator<Number> COMPARATOR_UNBOXED_INT_CAST =
      new Comparator<Number>() {
        public int compare(Number n1, Number n2) {
          // BUG: Diagnostic contains: return Long.compare(n1.longValue(), n2.longValue())
          return (int) (n1.longValue() - n2.longValue());
        }
      };

  static final Comparator<Long> COMPARATOR_BOXED_INT_CAST =
      new Comparator<Long>() {
        public int compare(Long n1, Long n2) {
          // BUG: Diagnostic contains: return n1.compareTo(n2)
          return (int) (n1 - n2);
        }
      };
}
{% endhighlight %}

### Negative examples
__BadComparableNegativeCases.java__

{% highlight java %}
/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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

/** @author irogers@google.com (Ian Rogers) */
public class BadComparableNegativeCases {
  // The corrected cases of the PositiveCases test.
  static class ComparableTest implements Comparable<ComparableTest> {
    private final long value = 0;

    public int compareTo(ComparableTest other) {
      return Long.compare(value, other.value);
    }
  }

  static class BoxedComparableTest implements Comparable<BoxedComparableTest> {
    private final Long value = Long.valueOf(0);

    public int compareTo(BoxedComparableTest other) {
      return value.compareTo(other.value);
    }
  }

  static final Comparator<Number> COMPARATOR_UNBOXED_INT_CAST =
      new Comparator<Number>() {
        public int compare(Number n1, Number n2) {
          return Long.compare(n1.longValue(), n2.longValue());
        }
      };

  static final Comparator<Long> COMPARATOR_BOXED_INT_CAST =
      new Comparator<Long>() {
        public int compare(Long n1, Long n2) {
          return n1.compareTo(n2);
        }
      };

  // Don't match non-Comparable or Comparator cases.
  static class NonComparableTest {
    private final long value = 0;

    public int compareTo(ComparableTest other) {
      return (int) (value - other.value);
    }
  }

  static final Object COMPARATOR_LIKE_INT_CAST =
      new Object() {
        public int compare(Long n1, Long n2) {
          return (int) (n1 - n2);
        }
      };

  // Narrowing conversions that don't follow the long -> int pattern.
  static final Comparator<Number> COMPARATOR_UNBOXED_NON_PATTERN_LONG_CAST =
      new Comparator<Number>() {
        // To match the Comparator API.
        @Override
        public int compare(Number n1, Number n2) {
          return (int) (n1.intValue() - n2.intValue());
        }

        public short compare(int n1, int n2) {
          return (short) (n1 - n2);
        }

        public byte compare(long n1, long n2) {
          return (byte) (n1 - n2);
        }
      };

  // Not narrowing conversions.
  static final Comparator<Number> COMPARATOR_UNBOXED_NON_NARROW_LONG_CAST =
      new Comparator<Number>() {
        // To match the Comparator API.
        @Override
        public int compare(Number n1, Number n2) {
          return (int) (n1.intValue() - n2.intValue());
        }

        public long compare(long n1, long n2) {
          return (long) (n1 - n2);
        }
      };

  static final Comparator<Number> COMPARATOR_UNBOXED_NON_NARROW_INT_CAST =
      new Comparator<Number>() {
        public int compare(Number n1, Number n2) {
          return (int) (n1.intValue() - n2.intValue());
        }
      };

  static final Comparator<Number> COMPARATOR_UNBOXED_NON_NARROW_SHORT_CAST =
      new Comparator<Number>() {
        // To match the Comparator API.
        @Override
        public int compare(Number n1, Number n2) {
          return (int) (n1.intValue() - n2.intValue());
        }

        public short compare(short n1, short n2) {
          return (short) (n1 - n2);
        }
      };

  static final Comparator<Number> COMPARATOR_UNBOXED_NON_NARROW_BYTE_CAST =
      new Comparator<Number>() {
        // To match the Comparator API.
        @Override
        public int compare(Number n1, Number n2) {
          return (int) (n1.intValue() - n2.intValue());
        }

        public byte compare(byte n1, byte n2) {
          return (byte) (n1 - n2);
        }
      };

  // Not signed conversions.
  static final Comparator<Number> COMPARATOR_UNBOXED_NON_NARROW_CHAR_CAST =
      new Comparator<Number>() {
        @Override
        public int compare(Number n1, Number n2) {
          return (char) (n1.shortValue() - n2.shortValue());
        }

        public char compare(char n1, char n2) {
          return (char) (n1 - n2);
        }

        public char compare(byte n1, byte n2) {
          return (char) (n1 - n2);
        }
      };
}
{% endhighlight %}

