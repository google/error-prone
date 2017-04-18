/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

package com.google.errorprone.bugpatterns.argumentselectiondefects;

import com.google.errorprone.VisitorState;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol.MethodSymbol;

/**
 * Heuristic to keep suggestions which score sufficiently different to the original. The idea is
 * that we should leave the arguments alone if we are not sure we can swap them so if the overall
 * score of the alternative is not significantly better than the original we want to leave the
 * original in place.
 *
 * @author andrewrice@google.com (Andrew Rice)
 */
class PenaltyThresholdHeuristic implements Heuristic {

  private final double threshold;

  private static final double DEFAULT_THRESHOLD = 0.6;

  PenaltyThresholdHeuristic(double threshold) {
    this.threshold = threshold;
  }

  /** Constructs an instance using the default threshold value. */
  PenaltyThresholdHeuristic() {
    this(DEFAULT_THRESHOLD);
  }

  /** Return true if the change is sufficiently different. */
  @Override
  public boolean isAcceptableChange(
      Changes changes, Tree node, MethodSymbol symbol, VisitorState state) {

    int numberOfChanges = changes.changedPairs().size();

    return changes.totalOriginalCost() - changes.totalAssignmentCost()
        >= threshold * numberOfChanges;
  }
}
