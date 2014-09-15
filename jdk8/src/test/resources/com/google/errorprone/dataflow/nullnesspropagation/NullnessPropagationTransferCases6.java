/*
 * Copyright 2014 Google Inc. All Rights Reserved.
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

import static com.google.errorprone.dataflow.nullnesspropagation.NullnessPropagationTest.triggerNullnessChecker;
import static com.google.errorprone.dataflow.nullnesspropagation.NullnessPropagationTest.triggerNullnessCheckerOnPrimitive;

/**
 * Tests for bitwise and numerical operations and comparisons.
 */
public class NullnessPropagationTransferCases6 {
  public void bitwiseOperations() {
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(1 | 2);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(1 & 2);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(1 ^ 2);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(~1);
  }

  public void bitwiseOperationsAreDereferences(Integer i) {
    /*
     * This next part has nothing to do with bitwise operations per se. The reason that it works is
     * that we recognize the implicit intValue() call as a dereference.
     */
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(i);
    int unused = ~i;
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(i);
  }

  public void numercialOperations() {
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(1 + 2);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(1 - 2);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(1 * 2);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(1 / 2);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(1 % 2);

    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(1.0 / 2);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(1.0 % 2);

    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(1 << 2);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(1 >> 2);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(1 >>> 2);

    int i = 1;
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(+i);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(-i);
  }

  public void numericalOperationsAreDereferences(Integer i) {
    // See bitwiseOperationsAreDereferences for some background.

    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(i);
    int unused = i + i;
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(i);
  }

  public void numercialComparisons() {
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(1 < 2);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(1 > 2);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(1 <= 2);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(1 >= 2);
  }

  public void numericalComparisonsAreDereferences(Integer a, Integer b) {
    // See bitwiseOperationsAreDereferences for some background.

    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(a);
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(b);
    int unused = a + b;
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(a);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(b);
  }
}
