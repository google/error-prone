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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Verify.verifyNotNull;
import static com.google.errorprone.dataflow.nullnesspropagation.NullnessPropagationTest.triggerNullnessChecker;
import static com.google.errorprone.dataflow.nullnesspropagation.NullnessPropagationTest.triggerNullnessCheckerOnPrimitive;
import static com.google.errorprone.dataflow.nullnesspropagation.NullnessPropagationTransferCases2.MyEnum.ENUM_INSTANCE;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.base.Strings;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;

/**
 * Dataflow analysis cases for testing transfer functions in nullness propagation
 */
@SuppressWarnings("deprecation")  // test cases include deprecated JUnit methods
public class NullnessPropagationTransferCases2 {
  private static class MyClass {
    static String staticReturnNullable() {
      return null;
    }
  }

  static final int CONSTANT_INT = 1;
  static final Integer CONSTANT_BOXED_INTEGER = 1;
  static final String CONSTANT_STRING = "foo";
  static final String CONSTANT_NULL_STRING = null;
  static final MyClass CONSTANT_OTHER_CLASS = new MyClass();

  public void constants() {
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(CONSTANT_INT);
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(CONSTANT_BOXED_INTEGER);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(CONSTANT_STRING);
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(CONSTANT_NULL_STRING);
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(CONSTANT_OTHER_CLASS);
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
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(String.valueOf(3));
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(valueOf(3));
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(Integer.valueOf(null));
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(BigInteger.valueOf(3));
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(Enum.valueOf(MyEnum.class, "INSTANCE"));

    // We'd prefer this to be Non-null. See the TODO on CLASSES_WITH_NON_NULLABLE_VALUE_OF_METHODS.
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(MyEnum.valueOf("INSTANCE"));

    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(MyBigInteger.valueOf(3));
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(MyEnum.valueOf('a'));
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

  public void methodInvocationIsDereference(String nullableParam) {
    String str = nullableParam;
    str.toString();
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(str);
  }

  public void staticMethodInvocationIsNotDereferenceNullableReturn(MyClass nullableParam) {
    nullableParam.staticReturnNullable();
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(nullableParam);
  }

  public void staticMethodInvocationIsNotDereferenceNonNullReturn(String nullableParam) {
    nullableParam.valueOf(true);
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(nullableParam);
  }

  public void staticMethodInvocationIsNotDereferenceButPreservesExistingInformation() {
    String s = "foo";
    s.format("%s", "foo");
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(s);
  }

  public void staticMethodInvocationIsNotDereferenceButDefersToOtherNewInformation(String s) {
    s = s.valueOf(true);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(s);
  }

  public void objectCreation(Object nullableParam) {
    Object obj = nullableParam;
    obj = new Object();
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(obj);
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

  public void filesToStringReturnNonNull() throws IOException {
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(Files.toString(new File("/dev/null"), UTF_8));
  }

  public void stringStaticMethodsReturnNonNull() {
    String s = null;
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(String.format("%s", "foo"));
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(format("%s", "foo"));
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(s.format("%s", "foo"));
  }

  public void stringInstanceMethodsReturnNonNull() {
    String s = null;
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(s.substring(0));
  }

  public void checkNotNullReturnsNonNull(String s) {
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(checkNotNull(s));
  }

  public void checkNotNullUpdatesVariableNullness(String s) {
    checkNotNull(s);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(s);
  }

  public void verifyNotNullReturnsNonNull(String s) {
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(verifyNotNull(s));
  }

  public void verifyNotNullUpdatesVariableNullness(String s) {
    verifyNotNull(s);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(s);
  }

  public void junit3AssertNotNullOneArgUpdatesVariableNullness(Object o) {
    junit.framework.Assert.assertNotNull(o);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(o);
  }

  public void junit3AssertNotNullTwoArgUpdatesVariableNullness(String message, Object o) {
    junit.framework.Assert.assertNotNull(message, o);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(o);
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(message);
  }

  public void junit4AssertNotNullOneArgUpdatesVariableNullness(Object o) {
    org.junit.Assert.assertNotNull(o);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(o);
  }

  public void junit4AssertNotNullTwoArgUpdatesVariableNullness(String message, Object o) {
    org.junit.Assert.assertNotNull(message, o);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(o);
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(message);
  }

  public void stringsIsNullOrEmptyIsNullCheck(String s) {
    if (Strings.isNullOrEmpty(s)) {
      // BUG: Diagnostic contains: (Nullable)
      triggerNullnessChecker(s);
    } else {
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(s);
    }
  }
}
