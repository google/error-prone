/*
 * Copyright 2019 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.testdata;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.stream.Stream;
import javax.annotation.Nullable;

/** @author awturner@google.com (Andy Turner) */
class UnnecessaryBoxedVariableCases {
  void positive_local() {
    int i = 0;
  }

  int positive_local_return() {
    int i = 0;
    return i;
  }

  Integer positive_local_addition() {
    int i = 0;
    return i + 1;
  }

  void positive_local_compoundAddition(Integer addend) {
    int i = 0;
    i += addend;

    int j = 0;
    j += i;
  }

  void positive_methodInvocation() {
    int i = 0;
    methodPrimitiveArg(i);
  }

  void negative_methodInvocation() {
    Integer i = 0;
    methodBoxedArg(i);
  }

  void positive_assignedValueOf() {
    int i = Integer.valueOf(0);
  }

  int positive_assignedValueOf_return() {
    int i = Integer.valueOf(0);
    return i;
  }

  int positive_noInitializer() {
    int i;
    i = 0;
    return i;
  }

  void negative_enhancedForLoopOverCollection(List<Integer> list) {
    for (Integer i : list) {}
  }

  void negative_enhancedForLoopOverWrappedArray(Integer[] array) {
    for (Integer i : array) {}
  }

  void positive_enhancedForLoopOverPrimitiveArray(int[] array) {
    for (int i : array) {}
  }

  final void negative_invokeMethod(Integer i) throws InterruptedException {
    i.wait(0);
  }

  final Object[] negative_objectArray(Long l) {
    return new Object[] {"", l};
  }

  void negative_null() {
    Integer i = null;
  }

  void negative_null_noInitializer() {
    Integer i;
    i = null;
    i = 0;
  }

  void negative_null_reassignNull() {
    Integer i = 0;
    i = null;
  }

  void negative_enhancedForLoopOverPrimitiveArray_assignInLoop(int[] array) {
    for (Integer i : array) {
      i = null;
    }
  }

  void negative_boxedVoid() {
    Void v;
  }

  int negative_assignmentInReturn() {
    Integer myVariable;
    return myVariable = methodBoxedArg(42);
  }

  int positive_assignmentInReturn() {
    int myVariable;
    return myVariable = Integer.valueOf(42);
  }

  int positive_assignmentInReturn2() {
    int myVariable;
    return myVariable = Integer.valueOf(42);
  }

  int positive_hashCode() {
    int myVariable = 0;
    return Integer.hashCode(myVariable);
  }

  short positive_castMethod() {
    int myVariable = 0;
    return (short) myVariable;
  }

  int positive_castMethod_sameType() {
    int myVariable = 0;
    return myVariable;
  }

  void positive_castMethod_statementExpression() {
    int myVariable = 0;
  }

  void negative_methodReference() {
    Integer myVariable = 0;
    Stream<Integer> stream = Stream.of(1).filter(myVariable::equals);
  }

  static void positive_parameter_staticMethod(boolean b) {
    boolean a = b;
  }

  static void negative_parameter_staticMethod(Boolean b) {
    System.out.println("a " + b);
  }

  static boolean positive_parameter_returnType(boolean b) {
    return b;
  }

  void negative_parameter_instanceMethod_nonFinal(Boolean b) {
    boolean a = b;
  }

  final void negative_parameter_instanceMethod_final(boolean b) {
    boolean a = b;
  }

  static void negative_parameter_unused(Integer i) {}

  static void positive_removeNullable_parameter(int i) {
    int j = i;
  }

  static void positive_removeNullable_localVariable() {
    int i = 0;
    int j = 0;
    int k = i + j;
  }

  static int positive_nullChecked_expression(int i) {
    return i;
  }

  static int positive_nullChecked_expression_message(int i) {
    return i;
  }

  static int positive_nullChecked_statement(int i) {
    return i;
  }

  static int positive_nullChecked_statement_message(int i) {
    return i;
  }

  private void methodPrimitiveArg(int i) {}

  private Integer methodBoxedArg(Integer i) {
    return i;
  }
}
