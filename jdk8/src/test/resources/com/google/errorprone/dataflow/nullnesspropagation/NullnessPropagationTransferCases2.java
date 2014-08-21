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

/**
 * Dataflow analysis cases for testing transfer functions in nullness propagation
 */
public class NullnessPropagationTransferCases2 {
  private class MyClass {
    public int field;
  }

  private class MyContainerClass {
    private MyClass field;
  }

  public void literals() {
    // BUG: Diagnostic contains: triggerNullnessChecker(Non-null)
    triggerNullnessChecker((short) 1000);
    // BUG: Diagnostic contains: triggerNullnessChecker(Non-null)
    triggerNullnessChecker(2);
    // BUG: Diagnostic contains: triggerNullnessChecker(Non-null)
    triggerNullnessChecker(33L);
    // BUG: Diagnostic contains: triggerNullnessChecker(Non-null)
    triggerNullnessChecker(0.444f);
    // BUG: Diagnostic contains: triggerNullnessChecker(Non-null)
    triggerNullnessChecker(0.5555d);
    // BUG: Diagnostic contains: triggerNullnessChecker(Non-null)
    triggerNullnessChecker(true);
    // BUG: Diagnostic contains: triggerNullnessChecker(Non-null)
    triggerNullnessChecker('z');
    // BUG: Diagnostic contains: triggerNullnessChecker(Non-null)
    triggerNullnessChecker("a string literal");
    // BUG: Diagnostic contains: triggerNullnessChecker(Nullable)
    triggerNullnessChecker(null);
  }

  public void parameter(String str, int i) {
    // BUG: Diagnostic contains: triggerNullnessChecker(Nullable)
    triggerNullnessChecker(str);
    // BUG: Diagnostic contains: triggerNullnessChecker(Non-null)
    triggerNullnessChecker(i);
  }

  public void assignment(String nullableParam) {
    String str = nullableParam;
    // BUG: Diagnostic contains: triggerNullnessChecker(Nullable)
    triggerNullnessChecker(str);
    
    str = "a string";
    // BUG: Diagnostic contains: triggerNullnessChecker(Non-null)
    triggerNullnessChecker(str);
    
    String otherStr = str;
    // BUG: Diagnostic contains: triggerNullnessChecker(Non-null)
    triggerNullnessChecker(str);
    
    str = null;
    // BUG: Diagnostic contains: triggerNullnessChecker(Nullable)
    triggerNullnessChecker(str);

    otherStr = str;
    // BUG: Diagnostic contains: triggerNullnessChecker(Nullable)
    triggerNullnessChecker(str);
  }
  
  public void localVariable() {
    short s = 1000; // performs narrowing conversion from int literal to short
    // BUG: Diagnostic contains: triggerNullnessChecker(Non-null)
    triggerNullnessChecker(s);
    int i = 2;
    // BUG: Diagnostic contains: triggerNullnessChecker(Non-null)
    triggerNullnessChecker(i);
    String str = "a string literal";
    // BUG: Diagnostic contains: triggerNullnessChecker(Non-null)
    triggerNullnessChecker(str);
    Object obj = null;
    // BUG: Diagnostic contains: triggerNullnessChecker(Nullable)
    triggerNullnessChecker(obj);

    ++i;
    // BUG: Diagnostic contains: triggerNullnessChecker(Non-null)
    triggerNullnessChecker(i);
  }
  
  int i;
  String str;
  Object obj;

  public void field() {
    // BUG: Diagnostic contains: triggerNullnessChecker(Non-null)
    triggerNullnessChecker(i);

    // TODO(cpovirk): fix!
    // BUG: Diagnostic contains: triggerNullnessChecker(Non-null)
    triggerNullnessChecker(str);

    // TODO(cpovirk): fix!
    // BUG: Diagnostic contains: triggerNullnessChecker(Non-null)
    triggerNullnessChecker(obj);
  }
  
  public void fieldQualifiedByThis() {
    // BUG: Diagnostic contains: triggerNullnessChecker(Non-null)
    triggerNullnessChecker(this.i);
  }

  public void fieldQualifiedByOtherVar() {
    NullnessPropagationTransferCases2 self = this;

    // BUG: Diagnostic contains: triggerNullnessChecker(Non-null)
    triggerNullnessChecker(self.i);
  }

  public void fieldAccessIsDereference(MyClass nullableParam) {
    MyClass mc = nullableParam;
    int i = mc.field;
    // BUG: Diagnostic contains: triggerNullnessChecker(Non-null)
    triggerNullnessChecker(mc);
  }

  public void fieldValuesMayChange() {
    MyContainerClass container = new MyContainerClass();
    container.field = new MyClass();
    /*
     * TODO(cpovirk): fix! (IIUC, this is part of the general "all fields are non-null" problem, not
     * a special problem relating to the assignment to container.field.)
     */
    // BUG: Diagnostic contains: triggerNullnessChecker(Non-null)
    triggerNullnessChecker(container.field);

    container.field.field = 10;
    /*
     * TODO(cpovirk): fix! (IIUC, this is part of the general "all fields are non-null" problem, not
     * a special problem relating to the dereferencing to container.field.)
     */
    // BUG: Diagnostic contains: triggerNullnessChecker(Non-null)
    triggerNullnessChecker(container.field);
  }
  
  public void methodInvocation() {
    // BUG: Diagnostic contains: triggerNullnessChecker(Non-null)
    triggerNullnessChecker(intReturningMethod());

    // TODO(cpovirk): fix!
    // BUG: Diagnostic contains: triggerNullnessChecker(Non-null)
    triggerNullnessChecker(stringReturningMethod());
  }

  int intReturningMethod() {
    return 0;
  }

  String stringReturningMethod() {
    return null;
  }

  public void methodInvocationIsDereference(String nullableParam) {
    String str = nullableParam;
    str.toString();
    // BUG: Diagnostic contains: triggerNullnessChecker(Non-null)
    triggerNullnessChecker(str);
  }
  
  public void objectCreation(Object nullableParam) {
    Object obj = nullableParam;
    obj = new Object();
    // BUG: Diagnostic contains: triggerNullnessChecker(Non-null)
    triggerNullnessChecker(obj);
  }

  public void inc() {
    int i = 0;
    short s = 0;

    // BUG: Diagnostic contains: triggerNullnessChecker(Non-null)
    triggerNullnessChecker(i++);
    // BUG: Diagnostic contains: triggerNullnessChecker(Non-null)
    triggerNullnessChecker(s++);

    // BUG: Diagnostic contains: triggerNullnessChecker(Non-null)
    triggerNullnessChecker(++i);
    // BUG: Diagnostic contains: triggerNullnessChecker(Non-null)
    triggerNullnessChecker(++s);

    // BUG: Diagnostic contains: triggerNullnessChecker(Non-null)
    triggerNullnessChecker(i += 5);
    // BUG: Diagnostic contains: triggerNullnessChecker(Non-null)
    triggerNullnessChecker(s += 5);
  }
}
