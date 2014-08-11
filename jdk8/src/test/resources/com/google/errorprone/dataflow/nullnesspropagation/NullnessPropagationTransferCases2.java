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

/**
 * Dataflow analysis cases for testing transfer functions in nullness propagation
 */
public class NullnessPropagationTransferCases2 {
  private class MyClass {
    public int field;
  }
  
  public void literals() {
    short s = 1000; // performs narrowing conversion from int literal to short
    // BUG: Diagnostic contains: triggerNullnessChecker(Non-null)
    NullnessPropagationTest.triggerNullnessChecker(s);
    int i = 2;
    // BUG: Diagnostic contains: triggerNullnessChecker(Non-null)
    NullnessPropagationTest.triggerNullnessChecker(i);
    long l = 33L;
    // BUG: Diagnostic contains: triggerNullnessChecker(Non-null)
    NullnessPropagationTest.triggerNullnessChecker(l);
    float f = 0.444f;
    // BUG: Diagnostic contains: triggerNullnessChecker(Non-null)
    NullnessPropagationTest.triggerNullnessChecker(f);
    double d = 0.5555d;
    // BUG: Diagnostic contains: triggerNullnessChecker(Non-null)
    NullnessPropagationTest.triggerNullnessChecker(d);
    boolean b = true;
    // BUG: Diagnostic contains: triggerNullnessChecker(Non-null)
    NullnessPropagationTest.triggerNullnessChecker(b);
    char c = 'z';
    // BUG: Diagnostic contains: triggerNullnessChecker(Non-null)
    NullnessPropagationTest.triggerNullnessChecker(c);
    String str = "a string literal";
    // BUG: Diagnostic contains: triggerNullnessChecker(Non-null)
    NullnessPropagationTest.triggerNullnessChecker(str);
    Object obj = null;
    // BUG: Diagnostic contains: triggerNullnessChecker(Nullable)
    NullnessPropagationTest.triggerNullnessChecker(obj);
  }
  
  public void assignment(String nullableParam) {
    String str = nullableParam;
    // BUG: Diagnostic contains: triggerNullnessChecker(Nullable)
    NullnessPropagationTest.triggerNullnessChecker(str);
    
    str = "a string";
    // BUG: Diagnostic contains: triggerNullnessChecker(Non-null)
    NullnessPropagationTest.triggerNullnessChecker(str);
    
    str = null;
    // BUG: Diagnostic contains: triggerNullnessChecker(Nullable)
    NullnessPropagationTest.triggerNullnessChecker(str);
  }
  
  public void localVariableOfPrimitiveType() {
    int i = 2;
    ++i;
    // BUG: Diagnostic contains: triggerNullnessChecker(Non-null)
    NullnessPropagationTest.triggerNullnessChecker(i);
  }
  
  public void fieldAccess(MyClass nullableParam) {
    MyClass mc = nullableParam;
    int i = mc.field;
    // BUG: Diagnostic contains: triggerNullnessChecker(Non-null)
    NullnessPropagationTest.triggerNullnessChecker(mc);
  }
  
  public void methodInvocation(String nullableParam) {
    String str = nullableParam;
    str.toString();
    // BUG: Diagnostic contains: triggerNullnessChecker(Non-null)
    NullnessPropagationTest.triggerNullnessChecker(str);
  }
  
  public void objectCreation(Object nullableParam) {
    Object obj = nullableParam;
    obj = new Object();
    // BUG: Diagnostic contains: triggerNullnessChecker(Non-null)
    NullnessPropagationTest.triggerNullnessChecker(obj);
  }
}