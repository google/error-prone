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
import static com.google.errorprone.dataflow.nullnesspropagation.NullnessPropagationTransferCases7.HasStaticFields.staticIntField;
import static com.google.errorprone.dataflow.nullnesspropagation.NullnessPropagationTransferCases7.HasStaticFields.staticStringField;

/**
 * Tests for field accesses and assignments.
 */
public class NullnessPropagationTransferCases7 {
  private static class MyClass {
    int field;
  }

  private static class MyContainerClass {
    MyClass field;
  }

  enum MyEnum {
    ENUM_INSTANCE;
  }

  static class HasStaticFields {
    static String staticStringField;
    static int staticIntField;
  }

  private int i;
  private String str;
  private Object obj;

  private Integer boxedIntReturningMethod() {
    return null;
  }

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
    NullnessPropagationTransferCases7 self = this;

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
    // BUG: Diagnostic contains: (Null)
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
}
