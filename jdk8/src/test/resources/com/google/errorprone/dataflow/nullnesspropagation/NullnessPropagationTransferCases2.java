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
import static com.google.errorprone.dataflow.nullnesspropagation.NullnessPropagationTest.triggerNullnessCheckerOnInt;
import static com.google.errorprone.dataflow.nullnesspropagation.NullnessPropagationTransferCases2.MyEnum.ENUM_INSTANCE;
import static java.lang.String.valueOf;

import java.math.BigInteger;

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
    // BUG: Diagnostic contains: triggerNullnessChecker(Non-null)
    triggerNullnessChecker(String.class);
    // BUG: Diagnostic contains: triggerNullnessChecker(Non-null)
    triggerNullnessChecker(MyEnum.ENUM_INSTANCE);
    // BUG: Diagnostic contains: triggerNullnessChecker(Non-null)
    triggerNullnessChecker(ENUM_INSTANCE);
    // TODO(cpovirk): fix!
    // BUG: Diagnostic contains: triggerNullnessChecker(Non-null)
    triggerNullnessChecker(MyEnum.NOT_AN_ENUM_CONSTANT);
    // BUG: Diagnostic contains: triggerNullnessChecker(Nullable)
    triggerNullnessChecker(null);
  }

  static class MyBigInteger extends BigInteger {
    MyBigInteger(String val) {
      super(val);
    }

    // Has the same signature as a BigInteger method. In other words, our method hides that one.
    public static BigInteger valueOf(long val) {
      return null;
    }
  }

  enum MyEnum {
    ENUM_INSTANCE;

    static MyEnum valueOf(char c) {
      return null;
    }

    public static final MyEnum NOT_AN_ENUM_CONSTANT = ENUM_INSTANCE;
  }

  public void explicitValueOf() {
    // BUG: Diagnostic contains: triggerNullnessChecker(Non-null)
    triggerNullnessChecker(String.valueOf(3));
    // BUG: Diagnostic contains: triggerNullnessChecker(Non-null)
    triggerNullnessChecker(valueOf(3));
    // BUG: Diagnostic contains: triggerNullnessChecker(Non-null)
    triggerNullnessChecker(Integer.valueOf(null));
    // BUG: Diagnostic contains: triggerNullnessChecker(Non-null)
    triggerNullnessChecker(BigInteger.valueOf(3));
    // BUG: Diagnostic contains: triggerNullnessChecker(Non-null)
    triggerNullnessChecker(Enum.valueOf(MyEnum.class, "INSTANCE"));

    // BUG: Diagnostic contains: triggerNullnessChecker(Non-null)
    triggerNullnessChecker(MyEnum.valueOf("INSTANCE"));

    // TODO(cpovirk): fix!
    // BUG: Diagnostic contains: triggerNullnessChecker(Non-null)
    triggerNullnessChecker(MyBigInteger.valueOf(3));
    // TODO(cpovirk): fix!
    // BUG: Diagnostic contains: triggerNullnessChecker(Non-null)
    triggerNullnessChecker(MyEnum.valueOf('a'));
  }

  public void parameter(String str, int i) {
    // BUG: Diagnostic contains: triggerNullnessChecker(Nullable)
    triggerNullnessChecker(str);

    // A call to plain triggerNullnessChecker() would implicitly call Integer.valueOf(i).
    // BUG: Diagnostic contains: triggerNullnessCheckerOnInt(Non-null)
    triggerNullnessCheckerOnInt(i);
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

  public void assignmentExpressionValue() {
    String str = "foo";
    // BUG: Diagnostic contains: triggerNullnessChecker(Non-null)
    triggerNullnessChecker(str);
    // BUG: Diagnostic contains: triggerNullnessChecker(Nullable)
    triggerNullnessChecker(str = null);
    // BUG: Diagnostic contains: triggerNullnessChecker(Non-null)
    triggerNullnessChecker(str = "bar");
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
    // TODO(cpovirk): fix!
    // BUG: Diagnostic contains: triggerNullnessChecker(Non-null)
    triggerNullnessChecker(container.field);

    container.field.field = 10;
    // TODO(cpovirk): fix!
    // BUG: Diagnostic contains: triggerNullnessChecker(Non-null)
    triggerNullnessChecker(container.field);
  }

  public void assignmentToFieldExpressionValue() {
    MyContainerClass container = new MyContainerClass();
    // BUG: Diagnostic contains: triggerNullnessChecker(Nullable)
    triggerNullnessChecker(container.field = new MyClass());
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

  public void staticMethodInvocationIsNotDereference(String nullableParam) {
    nullableParam.format("%s", "foo");
    // TODO(cpovirk): fix!
    // BUG: Diagnostic contains: triggerNullnessChecker(Non-null)
    triggerNullnessChecker(nullableParam);
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

  public void vanillaVisitNode() {
    String[] a = new String[1];
    // BUG: Diagnostic contains: triggerNullnessChecker(Nullable)
    triggerNullnessChecker(a[0]);
  }
}
