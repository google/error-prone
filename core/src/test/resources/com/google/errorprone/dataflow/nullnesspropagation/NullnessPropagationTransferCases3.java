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
import static com.google.errorprone.dataflow.nullnesspropagation.NullnessPropagationTest.triggerNullnessCheckerOnBoxed;
import static com.google.errorprone.dataflow.nullnesspropagation.NullnessPropagationTest.triggerNullnessCheckerOnPrimitive;
import static com.google.errorprone.dataflow.nullnesspropagation.NullnessPropagationTransferCases3.MyEnum.ENUM_INSTANCE;

/**
 * Dataflow analysis cases for testing transfer functions in nullness propagation
 */
public class NullnessPropagationTransferCases3 {
  public void casts() {
    Object o = null;
    // BUG: Diagnostic contains: (Null)
    triggerNullnessChecker((String) o);

    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnBoxed((int) o);

    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive((int) o);

    o = "str";
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker((String) o);
  }

  public void literals() {
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive((byte) 1);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive((short) 1000);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(2);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(33L);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(0.444f);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(0.5555d);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(true);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive('z');
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker("a string literal");
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(String.class);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(MyEnum.ENUM_INSTANCE);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(ENUM_INSTANCE);
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(MyEnum.NOT_AN_ENUM_CONSTANT);
    // BUG: Diagnostic contains: (Null)
    triggerNullnessChecker(null);
  }

  public void autoboxed() {
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnBoxed((byte) 1);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnBoxed((short) 1000);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnBoxed(2);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnBoxed(33L);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnBoxed(0.444f);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnBoxed(0.5555d);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnBoxed(true);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnBoxed('z');
  }

  public void autounbox() {
    Integer i = null;
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(i);
  }

  enum MyEnum {
    ENUM_INSTANCE;

    static MyEnum valueOf(char c) {
      return null;
    }

    public static final MyEnum NOT_AN_ENUM_CONSTANT = ENUM_INSTANCE;
  }
}
