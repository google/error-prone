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
import static com.google.errorprone.dataflow.nullnesspropagation.NullnessPropagationTransferCases2.HasStaticFields.staticIntField;
import static com.google.errorprone.dataflow.nullnesspropagation.NullnessPropagationTransferCases2.HasStaticFields.staticStringField;
import static com.google.errorprone.dataflow.nullnesspropagation.NullnessPropagationTransferCases2.MyEnum.ENUM_INSTANCE;
import static java.lang.String.format;
import static java.lang.String.valueOf;

import java.math.BigInteger;

/**
 * Dataflow analysis cases for testing transfer functions in nullness propagation
 */
public class NullnessPropagationTransferCases2 {
  private static class MyClass {
    public int field;

    static String staticReturnNullable() {
      return null;
    }
  }

  private static class MyContainerClass {
    private MyClass field;
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
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(str);

    otherStr = str;
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(str);
  }

  public void assignmentExpressionValue() {
    String str = "foo";
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(str);

    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(str = null);
    // BUG: Diagnostic contains: (Nullable)
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

    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(str = str2 = null);
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(str);
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(str2);
  }
  
  public void localVariable() {
    short s = 1000; // performs narrowing conversion from int literal to short
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(s);
    int i = 2;
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(i);
    String str = "a string literal";
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(str);
    Object obj = null;
    // BUG: Diagnostic contains: (Nullable)
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

  int i;
  String str;
  Object obj;

  public void field() {
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(i);

    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnBoxed(i);

    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(str);

    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(obj);
  }
  
  public void fieldQualifiedByThis() {
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(this.i);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnBoxed(this.i);
  }

  public void fieldQualifiedByOtherVar() {
    NullnessPropagationTransferCases2 self = this;

    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(self.i);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnBoxed(self.i);
  }

  public void fieldAccessIsDereference(MyClass nullableParam) {
    MyClass mc = nullableParam;
    int i = mc.field;
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(mc);
  }
  
  public void staticFieldAccessIsNotDereferenceNullableReturn(HasStaticFields nullableParam) {
    String s = nullableParam.staticStringField;
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(nullableParam);
  }
  
  public void staticFieldAccessIsNotDereferenceNonNullReturn(MyEnum nullableParam) {
    MyEnum x = nullableParam.ENUM_INSTANCE;
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(nullableParam);
  }

  public void fieldAssignmentIsDereference(MyClass nullableParam) {
    MyClass mc = nullableParam;
    mc.field = 0;
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(mc);
  }

  public void chainedFieldAssignmentIsDereference(MyClass nullableParam) {
    MyClass mc = nullableParam;
    MyContainerClass container = new MyContainerClass();
    container.field.field = 0;
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(container);
  }

  public void staticFieldAssignmentIsNotDereferenceNullableReturn(HasStaticFields nullableParam) {
    nullableParam.staticStringField = "foo";
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(nullableParam);
  }

  public void staticFieldAssignmentIsNotDereferenceNonNullReturn(HasStaticFields nullableParam) {
    nullableParam.staticIntField = 0;
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(nullableParam);
  }

  
  public void staticFieldAccessIsNotDereferenceButPreservesExistingInformation() {
    HasStaticFields container = new HasStaticFields();
    String s = container.staticStringField;
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(container);
  }
  
  public void fieldValuesMayChange() {
    MyContainerClass container = new MyContainerClass();
    container.field = new MyClass();
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(container.field);

    container.field.field = 10;
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(container.field);
  }

  public void assignmentToFieldExpressionValue() {
    MyContainerClass container = new MyContainerClass();
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(container.field = new MyClass());
  }

  public void assignmentToPrimitiveFieldExpressionValue() {
    MyClass mc = new MyClass();
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(mc.field = 10);
  }

  public void assignmentToStaticImportedFieldExpressionValue() {
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(staticStringField = null);
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessChecker(staticStringField = "foo");
  }

  public void assignmentToStaticImportedPrimitiveFieldExpressionValue() {
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(staticIntField = boxedIntReturningMethod());
  }

  public void nullableAssignmentToPrimitiveFieldExpressionValue() {
    MyClass mc = new MyClass();
    // BUG: Diagnostic contains: (Non-null)
    triggerNullnessCheckerOnPrimitive(mc.field = boxedIntReturningMethod());
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

  Integer boxedIntReturningMethod() {
    return null;
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

  public void vanillaVisitNode() {
    String[] a = new String[1];
    // BUG: Diagnostic contains: (Nullable)
    triggerNullnessChecker(a[0]);
  }

  public static class HasStaticFields {
    static String staticStringField;
    static int staticIntField;
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
         * constants.
         */
        // BUG: Diagnostic contains: (Nullable)
        triggerNullnessChecker(nonnull);
      }
    }
  }
}
