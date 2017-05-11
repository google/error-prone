/*
 * Copyright 2011 Google Inc. All Rights Reserved.
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

import com.google.auto.value.AutoValue;
import com.google.common.collect.Range;

/** A replaced section of a source file. */
@AutoValue
public abstract class Replacement {

  /**
   * Creates a {@link Replacement}. Start and end positions are represented as code unit indices in
   * a Unicode 16-bit string.
   *
   * @param startPosition the beginning of the replacement
   * @param endPosition the end of the replacement, exclusive
   * @param replaceWith the replacement text
   */
  public static Replacement create(int startPosition, int endPosition, String replaceWith) {
    checkArgument(startPosition >= 0, "invalid startPosition: %s", startPosition);
    return new AutoValue_Replacement(Range.closedOpen(startPosition, endPosition), replaceWith);
  }

  /** The beginning of the replacement range. */
  public int startPosition() {
    return range().lowerEndpoint();
  }

  /** The length of the input text to be replaced. */
  public int length() {
    return endPosition() - startPosition();
  }

  /** The end of the replacement range, exclusive. */
  public int endPosition() {
    return range().upperEndpoint();
  }

  /** The {@link Range} to be replaced. */
  public abstract Range<Integer> range();

  /** The source text to appear in the output. */
  public abstract String replaceWith();
}
