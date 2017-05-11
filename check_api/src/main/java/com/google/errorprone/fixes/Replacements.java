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

package com.google.errorprone.fixes;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Joiner;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeMap;

/** A collection of {@link Replacement}s to be made to a source file. */
public class Replacements {

  /**
   * We apply replacements in reverse order of start position, so that replacements that change the
   * length of the input don't affect the position of earlier replacements.
   */
  private static final Comparator<Range<Integer>> DESCENDING =
      new Comparator<Range<Integer>>() {
        @Override
        public int compare(Range<Integer> o1, Range<Integer> o2) {
          return ComparisonChain.start()
              .compare(o1.lowerEndpoint(), o2.lowerEndpoint(), Ordering.natural().reverse())
              .compare(o1.upperEndpoint(), o2.upperEndpoint(), Ordering.natural().reverse())
              .result();
        }
      };

  private final TreeMap<Range<Integer>, Replacement> replacements = new TreeMap<>(DESCENDING);
  private final RangeMap<Integer, Replacement> overlaps = TreeRangeMap.create();

  public Replacements add(Replacement replacement) {
    if (replacements.containsKey(replacement.range())) {
      Replacement existing = replacements.get(replacement.range());
      if (!existing.equals(replacement)) {
        if (replacement.range().isEmpty()) {
          // The replacement is an insertion, and there's an existing insertion at the same point.
          // In that case, we coalesce the additional insertion with the existing one.
          replacement =
              Replacement.create(
                  existing.startPosition(),
                  existing.endPosition(),
                  existing.replaceWith() + replacement.replaceWith());
        } else {
          throw new IllegalArgumentException(
              String.format("%s conflicts with existing replacement %s", replacement, existing));
        }
      }
    } else {
      checkOverlaps(replacement);
    }
    replacements.put(replacement.range(), replacement);
    return this;
  }

  private void checkOverlaps(Replacement replacement) {
    Collection<Replacement> overlap =
        overlaps.subRangeMap(replacement.range()).asMapOfRanges().values();
    checkArgument(
        overlap.isEmpty(),
        "%s overlaps with existing replacements: %s",
        replacement,
        Joiner.on(", ").join(overlap));
    overlaps.put(replacement.range(), replacement);
  }

  /** Non-overlapping replacements, sorted in descending order by position. */
  public Set<Replacement> descending() {
    // TODO(cushon): refactor SuggestedFix#getReplacements and just return a Collection,
    return new LinkedHashSet<>(replacements.values());
  }

  public boolean isEmpty() {
    return replacements.isEmpty();
  }
}
