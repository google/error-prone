/*
 * Copyright 2016 The Error Prone Authors.
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
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Ordering;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

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
  private final TreeSet<Integer> zeroLengthRanges = new TreeSet<>();

  /** A policy for handling overlapping insertions. */
  public enum CoalescePolicy {
    /** Reject overlapping insertions and throw an {@link IllegalArgumentException}. */
    REJECT(DuplicateInsertPolicy.DROP) {
      @Override
      public String coalesce(String replacement, String existing) {
        throw new IllegalArgumentException(
            String.format("%s conflicts with existing replacement %s", replacement, existing));
      }
    },
    /**
     * Accept overlapping insertions, with the new insertion before the existing one. Duplicate
     * insertions (inserting the same text at the same position) will still be dropped.
     */
    REPLACEMENT_FIRST(DuplicateInsertPolicy.DROP) {
      @Override
      public String coalesce(String replacement, String existing) {
        return replacement + existing;
      }
    },
    /**
     * Accept overlapping insertions, with the existing insertion before the new one. Duplicate
     * insertions (inserting the same text at the same position) will still be dropped.
     */
    EXISTING_FIRST(DuplicateInsertPolicy.DROP) {
      @Override
      public String coalesce(String replacement, String existing) {
        return existing + replacement;
      }
    },
    /**
     * Reject overlapping inserts, but treat duplicate inserts (same text at same position)
     * specially. Instead of dropping duplicates, as the other coalesce policies do, this policy
     * keeps them.
     */
    KEEP_ONLY_IDENTICAL_INSERTS(DuplicateInsertPolicy.KEEP) {
      @Override
      public String coalesce(String replacement, String existing) {
        return REJECT.coalesce(replacement, existing);
      }
    };

    private final DuplicateInsertPolicy duplicateInsertPolicy;

    CoalescePolicy(DuplicateInsertPolicy duplicateInsertPolicy) {
      this.duplicateInsertPolicy = duplicateInsertPolicy;
    }

    /**
     * Handle two insertions at the same position.
     *
     * @param replacement the replacement being added
     * @param existing the existing insert at this position
     * @return the coalesced replacement
     */
    public abstract String coalesce(String replacement, String existing);

    /**
     * Duplicate inserts are handled specially: usually dropped (e.g. don't add the same import
     * twice), but can be kept. e.g., a BugChecker that adds {} blocks around some statements may
     * need to insert a close brace at the same place twice.
     */
    private Replacement handleDuplicateInsertion(Replacement replacement) {
      return duplicateInsertPolicy.combineDuplicateInserts(replacement);
    }

    private enum DuplicateInsertPolicy {
      KEEP {
        @Override
        Replacement combineDuplicateInserts(Replacement insertion) {
          return insertion.withDifferentText(insertion.replaceWith() + insertion.replaceWith());
        }
      },
      DROP {
        @Override
        Replacement combineDuplicateInserts(Replacement insertion) {
          return insertion;
        }
      };

      abstract Replacement combineDuplicateInserts(Replacement insertion);
    }
  }

  @CanIgnoreReturnValue
  public Replacements add(Replacement replacement) {
    return add(replacement, CoalescePolicy.REJECT);
  }

  @CanIgnoreReturnValue
  public Replacements add(Replacement replacement, CoalescePolicy coalescePolicy) {
    if (replacements.containsKey(replacement.range())) {
      Replacement existing = replacements.get(replacement.range());
      if (replacement.range().isEmpty()) {
        // The replacement is an insertion, and there's an existing insertion at the same point.
        // First check whether it's a duplicate insert.
        if (existing.equals(replacement)) {
          replacement = coalescePolicy.handleDuplicateInsertion(replacement);
        } else {
          // Coalesce overlapping non-duplicate insertions together.
          replacement =
              replacement.withDifferentText(
                  coalescePolicy.coalesce(replacement.replaceWith(), existing.replaceWith()));
        }
      } else if (existing.equals(replacement)) {
        // Two copies of a non-insertion edit. Just ignore the new one since it's already done.
      } else {
        throw new IllegalArgumentException(
            String.format("%s conflicts with existing replacement %s", replacement, existing));
      }
    } else {
      checkOverlaps(replacement);
    }
    replacements.put(replacement.range(), replacement);
    return this;
  }

  private void checkOverlaps(Replacement replacement) {
    Range<Integer> replacementRange = replacement.range();
    Collection<Replacement> overlap =
        overlaps.subRangeMap(replacementRange).asMapOfRanges().values();
    checkArgument(
        overlap.isEmpty(),
        "%s overlaps with existing replacements: %s",
        replacement,
        Joiner.on(", ").join(overlap));
    Set<Integer> containedZeroLengthRangeStarts =
        zeroLengthRanges.subSet(
            replacementRange.lowerEndpoint(),
            /* fromInclusive= */ false,
            replacementRange.upperEndpoint(),
            /* toInclusive= */ false);
    checkArgument(
        containedZeroLengthRangeStarts.isEmpty(),
        "%s overlaps with existing zero-length replacements: %s",
        replacement,
        Joiner.on(", ").join(containedZeroLengthRangeStarts));
    overlaps.put(replacementRange, replacement);
    if (replacementRange.isEmpty()) {
      zeroLengthRanges.add(replacementRange.lowerEndpoint());
    }
  }

  /**
   * Non-overlapping replacements, sorted in descending order by position. Prefer using {@link
   * #ascending} when applying changes, because applying changes in reverse tends to result in
   * quadratic-time copying of the underlying string.
   */
  @Deprecated
  public Set<Replacement> descending() {
    // TODO(cushon): refactor SuggestedFix#getReplacements and just return a Collection,
    return new LinkedHashSet<>(replacements.values());
  }

  /** Non-overlapping replacements, sorted in ascending order by position. */
  public ImmutableSet<Replacement> ascending() {
    return ImmutableSet.copyOf(replacements.descendingMap().values());
  }

  public boolean isEmpty() {
    return replacements.isEmpty();
  }
}
