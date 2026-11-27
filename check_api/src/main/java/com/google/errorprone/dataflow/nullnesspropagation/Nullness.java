/*
 * Copyright 2014 The Error Prone Authors.
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

package com.google.errorprone.dataflow.nullnesspropagation;

import org.checkerframework.errorprone.dataflow.analysis.AbstractValue;

/// Represents one of the possible nullness values in our nullness analysis.
///
/// @author deminguyen@google.com (Demi Nguyen)
public enum Nullness implements AbstractValue<Nullness> {

  /// The lattice for nullness looks like:
  ///
  /// ```
  ///        Nullable
  ///       /        \
  ///   Null          Non-null
  ///        \      /
  ///         Bottom
  /// ```
  NULLABLE("Nullable"), // TODO(eaftan): Rename to POSSIBLY_NULL?
  NULL("Null"),
  NONNULL("Non-null"),
  BOTTOM("Bottom");

  private final String displayName;

  Nullness(String displayName) {
    this.displayName = displayName;
  }

  // The following leastUpperBound and greatestLowerBound methods were created by handwriting a
  // truth table and then encoding the values into these functions. A better approach would be to
  // represent the lattice directly and compute these functions from the lattice.

  @Override
  public Nullness leastUpperBound(Nullness other) {
    if (this == other) {
      return this;
    }
    // Bottom loses.
    if (this == BOTTOM) {
      return other;
    }
    if (other == BOTTOM) {
      return this;
    }
    // They disagree, and neither is bottom.
    return NULLABLE;
  }

  public Nullness greatestLowerBound(Nullness other) {
    if (this == other) {
      return this;
    }
    // Nullable loses.
    if (this == NULLABLE) {
      return other;
    }
    if (other == NULLABLE) {
      return this;
    }
    // They disagree, and neither is nullable.
    return BOTTOM;
  }

  /// Returns the `Nullness` that corresponds to what you can deduce by knowing that some expression
  /// is not equal to another expression with this `Nullness`.
  ///
  /// A `Nullness` represents a set of possible values for a expression. Suppose you have two
  /// variables `var1` and `var2`. If `var1 != var2`, then `var1` must be an element of the
  /// complement of the singleton set containing the value of `var2`. If you union these complement
  /// sets over all possible values of `var2`, the set that results is what this method returns,
  /// assuming that `this` is the `Nullness` of `var2`.
  ///
  /// Example 1: Suppose `nv2 == NULL`. Then `var2` can have exactly one value, `null`, and `var1`
  /// must have a value in the set of all values except `null`. That set is exactly `NONNULL`.
  ///
  /// Example 2: Suppose `nv2 == NONNULL`. Then `var2` can have any value except `null`. Suppose
  /// `var2` has value `"foo"`. Then `var1` must have a value in the set of all values except
  /// `"foo"`. Now suppose `var2` has value `"bar"` . Then `var1` must have a value in set of all
  /// values except `"bar"`. Since we don't know which value in the set `NONNULL var2` has, we union
  /// all possible complement sets to get the set of all values, or `NULLABLE`.
  public Nullness deducedValueWhenNotEqual() {
    return switch (this) {
      case NULLABLE, NONNULL -> NULLABLE;
      case NULL -> NONNULL;
      case BOTTOM -> BOTTOM;
    };
  }

  @Override
  public String toString() {
    return displayName;
  }
}
