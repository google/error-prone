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

/**
 * Dataflow analysis cases for testing transfer functions in nullness propagation around various
 * kinds of expressions, method parameter and call handling, and loops.
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
    // BUG: Diagnostic contains: (Null)
    triggerNullnessChecker(i);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(i);
  }

  public void parameter(String str, int i) {
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(str);

    // A call to plain triggerNullnessChecker() would implicitly call Integer.valueOf(i).
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(i);
  }

  public void assignment(String nullableParam) {
    String str = nullableParam;
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(str);

    str = "a string";
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(str);

    String otherStr = str;
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(str);

    str = null;
    // BUG: Diagnostic contains: (Null)
    triggerNullnessChecker(str);

    otherStr = str;
    // BUG: Diagnostic contains: (Null)
    triggerNullnessChecker(str);
  }

  public void assignmentExpressionValue() {
    String str = "foo";
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(str);
    // BUG: Diagnostic contains: (Null)
    triggerNullnessChecker(str = null);
    // BUG: Diagnostic contains: (Null)
    triggerNullnessChecker(str);

    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(str = "bar");
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(str);

    str = null;
    String str2 = null;
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(str = str2 = "bar");
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(str);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(str2);

    // BUG: Diagnostic contains: (Null)
    triggerNullnessChecker(str = str2 = null);
    // BUG: Diagnostic contains: (Null)
    triggerNullnessChecker(str);
    // BUG: Diagnostic contains: (Null)
    triggerNullnessChecker(str2);
  }

  public void localVariable() {
    short s;
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(s = 1000); // narrowing conversion
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(s);
    int i = 2;
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(i);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(i = s); // widening conversion
    String str = "a string literal";
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(str);
    Object obj = null;
    // BUG: Diagnostic contains: (Null)
    triggerNullnessChecker(obj);

    ++i;
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(i);
  }

  public void boxedPrimitives() {
    Short s = 1000;
    // BUG: Diagnostic contains: (Non-null)
    NullnessPropagationTest.triggerNullnessChecker(s);

    Integer i = 2;
    // BUG: Diagnostic contains: (Non-null)
    NullnessPropagationTest.triggerNullnessChecker(i);
  }

  public void nullableAssignmentToPrimitiveVariableExpressionValue() {
    int i;
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(i = boxedIntReturningMethod());
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(boxedIntReturningMethod());
  }

  public void methodInvocation() {
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(intReturningMethod());

    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(stringReturningMethod());
  }

  private Integer boxedIntReturningMethod() {
    return null;
  }

  private int intReturningMethod() {
    return 0;
  }

  private String stringReturningMethod() {
    return null;
  }

  public void objectCreation(Object nullableParam) {
    Object obj = nullableParam;
    obj = new Object();
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(obj);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(new Object[0]);
  }

  public void inc() {
    int i = 0;
    short s = 0;

    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(i++);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(s++);

    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(++i);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(++s);

    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(i += 5);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(s += 5);
  }

  public void loop1() {
    Object o = null;
    while (true) {
      // BUG: Diagnostic contains: (Nullable)
      triggerNullnessChecker(o);
      o.hashCode();
    }
  }

  public void loop2() {
    Object o = null;
    Object comingValue = null;
    while (true) {
      // BUG: Diagnostic contains: (Nullable)
      triggerNullnessChecker(o);

      o = comingValue;
      comingValue = new Object();
    }
  }
}
