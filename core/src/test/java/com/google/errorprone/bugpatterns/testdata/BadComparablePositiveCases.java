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
