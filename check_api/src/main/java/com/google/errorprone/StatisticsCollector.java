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

package com.google.errorprone;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Multiset;

/** A collector of counters keyed by strings. */
public interface StatisticsCollector {

  /** Adds 1 to the counter for {@code key}. */
  default void incrementCounter(String key) {
    incrementCounter(key, 1);
  }

  /** Adds {@code count} to the counter for {@code key}. */
  void incrementCounter(String key, int count);

  /** Returns a copy of the counters in this statistics collector. */
  ImmutableMultiset<String> counters();

  /** Returns a new statistics collector that will successfully count keys added to it. */
  static StatisticsCollector createCollector() {
    return new StatisticsCollector() {
      private final Multiset<String> strings = HashMultiset.create();

      @Override
      public void incrementCounter(String key, int count) {
        strings.add(key, count);
      }

      @Override
      public ImmutableMultiset<String> counters() {
        return ImmutableMultiset.copyOf(strings);
      }
    };
  }

  /**
   * Returns a statistics collector that will ignore any statistics added to it, always returning an
   * empty result for {@link #counters}.
   */
  static StatisticsCollector createNoOpCollector() {
    return new StatisticsCollector() {
      @Override
      public void incrementCounter(String key, int count) {}

      @Override
      public ImmutableMultiset<String> counters() {
        return ImmutableMultiset.of();
      }
    };
  }
}
