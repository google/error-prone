/*
 * Copyright 2014 The Error Prone Authors.
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

package com.google.errorprone.dataflow.nullnesspropagation.testdata;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Verify.verifyNotNull;
import static com.google.errorprone.dataflow.nullnesspropagation.NullnessPropagationTest.triggerNullnessChecker;
import static com.google.errorprone.dataflow.nullnesspropagation.NullnessPropagationTest.triggerNullnessCheckerOnPrimitive;
import static com.google.errorprone.dataflow.nullnesspropagation.testdata.NullnessPropagationTransferCases2.MyEnum.ENUM_INSTANCE;
import static java.lang.String.format;
import static java.lang.String.valueOf;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import java.math.BigInteger;
import java.util.Objects;

/**
 * Dataflow analysis cases for testing transfer functions in nullness propagation around constants
 * and built-in knowledge.
 */
@SuppressWarnings("deprecation") // test cases include deprecated JUnit methods
public class NullnessPropagationTransferCases2 {
  private static class MyClass {
    static String staticReturnNullable() {
      return null;
    }
  }

  static final int CONSTANT_INT = 1;
  static final Integer CONSTANT_BOXED_INTEGER = 1;
  static final Integer CONSTANT_DERIVED_INTEGER = (Integer) (CONSTANT_INT);
  static final Boolean CONSTANT_DERIVED_BOOLEAN = CONSTANT_INT == 1;
  static final String CONSTANT_STRING = "foo";
  static final String CONSTANT_NULL_STRING = null;
  static final String CONSTANT_DERIVED_STRING = CONSTANT_DERIVED_BOOLEAN ? CONSTANT_STRING : "";
  static final MyClass CONSTANT_OTHER_CLASS = new MyClass();
  static final Integer[] CONSTANT_OBJECT_ARRAY = new Integer[7];
  static final Integer[] CONSTANT_ARRAY_INITIALIZER = {Integer.valueOf(5)};
  static final Object CONSTANT_NO_INITIALIZER;

  static {
    CONSTANT_NO_INITIALIZER = new Object();
  }

  public void constants() {
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(CONSTANT_INT);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(CONSTANT_BOXED_INTEGER);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(CONSTANT_DERIVED_INTEGER);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(CONSTANT_DERIVED_BOOLEAN);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(CONSTANT_STRING);
    // BUG: Diagnostic contains: (Null)
    triggerNullnessChecker(CONSTANT_NULL_STRING);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(CONSTANT_DERIVED_STRING);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(CONSTANT_OTHER_CLASS);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(CONSTANT_OBJECT_ARRAY);
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(CONSTANT_OBJECT_ARRAY[0]);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(CONSTANT_ARRAY_INITIALIZER);
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(CONSTANT_NO_INITIALIZER);
  }

  public static final MyBigInteger CIRCULAR = MyBigInteger.CIRCLE;

  public void circularInitialization() {
    // BUG: Diagnostic contains: (Null)
    triggerNullnessChecker(MyBigInteger.CIRCLE);
  }

  static class MyBigInteger extends BigInteger {
    // Shadows BigInteger.ONE.
    public static final MyBigInteger ONE = null;
    // Creates circular initializer dependency.
    public static final MyBigInteger CIRCLE = NullnessPropagationTransferCases2.CIRCULAR;

    MyBigInteger(String val) {
      super(val);
    }

    // Has the same signature as a BigInteger method. In other words, our method hides that one.
    public static BigInteger valueOf(long val) {
      return null;
    }
  }

  public void builtInConstants() {
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(BigInteger.ZERO);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(MyBigInteger.ZERO);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(BigInteger.ONE);
    // BUG: Diagnostic contains: (Null)
    triggerNullnessChecker(MyBigInteger.ONE);
  }

  enum MyEnum {
    ENUM_INSTANCE;

    static MyEnum valueOf(char c) {
      return null;
    }

    public static final MyEnum NOT_COMPILE_TIME_CONSTANT = ENUM_INSTANCE;
    public static final MyEnum UNKNOWN_VALUE_CONSTANT = instance();

    public static MyEnum instance() {
      return ENUM_INSTANCE;
    }
  }

  public void enumConstants() {
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(MyEnum.ENUM_INSTANCE);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(ENUM_INSTANCE);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(MyEnum.NOT_COMPILE_TIME_CONSTANT);
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(MyEnum.UNKNOWN_VALUE_CONSTANT);
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

  public void methodInvocationIsDereference(String nullableParam) {
    String str = nullableParam;
    str.toString();
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(str);
  }

  public void booleanMethodInvocationIsDereference(String nullableParam) {
    String str = nullableParam;
    str.isEmpty();
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
  
  public void classgetNamesMethods() {
    Class<?> klass = this.getClass();
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(klass.getName());
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(klass.getSimpleName());
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(klass.getCanonicalName());
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

  public void requireNonNullReturnsNonNull(String s) {
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(Objects.requireNonNull(s));
  }

  public void requireNonNullUpdatesVariableNullness(String s) {
    Objects.requireNonNull(s);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(s);
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

  public void objectsIsNullIsNullCheck(String s) {
    if (Objects.isNull(s)) {
      // BUG: Diagnostic contains: (Null)
      triggerNullnessChecker(s);
    } else {
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(s);
    }
  }

  public void objectsNonNullIsNullCheck(String s) {
    if (Objects.nonNull(s)) {
      // BUG: Diagnostic contains: (Non-null)
      triggerNullnessChecker(s);
    } else {
      // BUG: Diagnostic contains: (Null)
      triggerNullnessChecker(s);
    }
  }

  public void optionalMethodsReturnNonNullUnlessAnnotated() {
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(Optional.absent());
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(Optional.of(""));
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(Optional.fromNullable(null));
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(Optional.of("Test"));
    Optional<String> myOptional = Optional.absent();
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(myOptional.get());
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(myOptional.or(""));
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(myOptional.asSet());

    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(myOptional.orNull());
  }
}
