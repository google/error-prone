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
 * Dataflow analysis cases for testing transfer functions in nullness propagation
 */
public class NullnessPropagationTransferCases1 {

  public void conditionalNot(String foo) {
    if (!(foo == null)) {
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(foo);
      return;
    }

    // BUG: Diagnostic contains: (Null)
    triggerNullnessChecker(foo);
  }

  public void conditionalOr1(String foo, String bar) {
    if (foo == null || bar == null) {
      // BUG: Diagnostic contains: (Nullable)
      triggerNullnessChecker(foo);
      // BUG: Diagnostic contains: (Nullable)
      triggerNullnessChecker(bar);
      return;
    }

    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(foo);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(bar);
  }

  public void conditionalOr2(String foo, String bar) {
    if (foo != null || bar != null) {
      // BUG: Diagnostic contains: (Nullable)
      triggerNullnessChecker(foo);
    }

    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(foo);
  }

  public void conditionalOr3(String foo) {
    if (foo != null || foo != null) {
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(foo);
    }

    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(foo);
  }

  public void conditionalAnd1(String foo, String bar) {
    if (foo != null && bar != null) {
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(foo);
    }

    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(foo);
  }

  public void conditionalAnd2(String foo) {
    if (foo == null && foo != null) {
      // BUG: Diagnostic contains: (Bottom)
      triggerNullnessChecker(foo);
      return;
    }

    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(foo);
  }

  public void valueOfComparisonItself() {
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(1 == 1);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(1 != 1);
    boolean b;
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(b = (1 == 1));
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(b = (1 != 1));

    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(!b);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(b || b);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(b && b);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(b = !b);
  }

  public void leastUpperBoundOfNonNullAndUnknown(String param, boolean b) {
    if (b) {
      param = "foo";
    }
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(param);
  }

  public void stringConcatenation(String a, String b) {
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(a + b);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(null + b);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(a + 5);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(null + (String) null);
  }
}
