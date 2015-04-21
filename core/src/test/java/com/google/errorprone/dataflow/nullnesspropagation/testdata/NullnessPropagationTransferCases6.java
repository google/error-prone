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
import static com.google.errorprone.dataflow.nullnesspropagation.NullnessPropagationTransferCases6.MyEnum.ENUM_INSTANCE;

/**
 * Tests for:
 *
 * <ul>
 * <li>bitwise operations
 * <li>numerical operations and comparisons
 * <li>plain {@code visitNode}
 * <li>name shadowing
 * </ul>
 */
public class NullnessPropagationTransferCases6 {
  enum MyEnum {
    ENUM_INSTANCE;
  }

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

  public void vanillaVisitNode() {
    String[] a = new String[1];
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(a[0]);
  }

  public void sameNameImmediatelyShadowed() {
    final String s = "foo";

    class Bar {
      void method(String s) {
        // BUG: Diagnostic contains: (Nullable)
        triggerNullnessChecker(s);
      }
    }
  }

  public void sameNameLaterShadowed() {
    final String s = "foo";

    class Bar {
      void method() {
        // BUG: Diagnostic contains: (Non-null)
        triggerNullnessChecker(s);

        String s = HasStaticFields.staticStringField;
        // BUG: Diagnostic contains: (Nullable)
        triggerNullnessChecker(s);
      }
    }
  }

  public void sameNameShadowedThenUnshadowed() {
    final String s = HasStaticFields.staticStringField;

    class Bar {
      void method() {
        {
          String s = "foo";
          // BUG: Diagnostic contains: (Non-null)
          triggerNullnessChecker(s);
        }

        // BUG: Diagnostic contains: (Nullable)
        triggerNullnessChecker(s);
      }
    }
  }

  public void nonCompileTimeConstantCapturedVariable() {
    final Object nonnull = ENUM_INSTANCE;

    class Bar {
      void method() {
        /*
         * We'd prefer for this to be non-null, but we don't run the analysis over the enclosing
         * class's enclosing method, so our captured-variable handling is limited to compile-time
         * constants, which include only primitives and strings:
         * http://docs.oracle.com/javase/specs/jls/se7/html/jls-15.html#jls-15.28
         */
        // BUG: Diagnostic contains: (Nullable)
        triggerNullnessChecker(nonnull);
      }
    }
  }

  static class HasStaticFields {
    static String staticStringField;
  }
}
