/*
 * Copyright 2015 The Error Prone Authors.
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

import com.google.errorprone.annotations.NoAllocation;

/** @author agoode@google.com (Adam Goode) */
public class NoAllocationCheckerPositiveCases {
  // Trigger on new array.
  @NoAllocation
  public int[] newArray(int size) {
    // BUG: Diagnostic contains: @NoAllocation
    // Allocating a new array
    return new int[size];
  }

  @NoAllocation
  public int[] arrayInitializer(int a, int b) {
    // BUG: Diagnostic contains: @NoAllocation
    // Allocating a new array
    int[] array = {a, b};
    return array;
  }

  @NoAllocation
  public int[] returnArrayInitializer(int a, int b) {
    // BUG: Diagnostic contains: @NoAllocation
    // Allocating a new array
    return new int[] {a, b};
  }

  // Trigger on new.
  @NoAllocation
  public String newString(String s) {
    // BUG: Diagnostic contains: @NoAllocation
    // Constructing
    return new String(s);
  }

  // Trigger calling a method that does allocation.
  public String allocateString() {
    return new String();
  }

  @NoAllocation
  public String getString() {
    // BUG: Diagnostic contains: @NoAllocation
    // Calling a method
    return allocateString();
  }

  // Trigger on string conversion.
  @NoAllocation
  public int getInt() {
    return 1;
  }

  @NoAllocation
  public String stringConvReturn(int i) {
    // BUG: Diagnostic contains: @NoAllocation
    // String concatenation
    return "" + i;
  }

  @NoAllocation
  public String stringConvAssign(int i) {
    // BUG: Diagnostic contains: @NoAllocation
    // String concatenation
    String s = "" + i;
    return s;
  }

  @NoAllocation
  public String stringConvAssign2(int i) {
    String s = "";
    // BUG: Diagnostic contains: @NoAllocation
    // String concatenation
    s = s + i;
    return s;
  }

  @NoAllocation
  public String stringConvAssign3(int i) {
    String s = "";
    // BUG: Diagnostic contains: @NoAllocation
    // String concatenation
    s = i + s;
    return s;
  }

  @NoAllocation
  public String stringConvReturnMethod() {
    // BUG: Diagnostic contains: @NoAllocation
    // String concatenation
    String s = "" + getInt();
    return s;
  }

  @NoAllocation
  public String stringConvCompoundAssign(int i) {
    String s = "";
    // BUG: Diagnostic contains: @NoAllocation
    // Compound assignment
    s += i;
    return s;
  }

  @NoAllocation
  public String stringConvCompoundReturnMethod() {
    String s = "";
    // BUG: Diagnostic contains: @NoAllocation
    // Compound assignment
    s += getInt();
    return s;
  }

  // Trigger on string concatenation.
  @NoAllocation
  public String doubleString(String s) {
    // BUG: Diagnostic contains: @NoAllocation
    // String concatenation
    return s + s;
  }

  @NoAllocation
  public String doubleStringCompound(String s) {
    // BUG: Diagnostic contains: @NoAllocation
    // Compound assignment
    s += s;
    return s;
  }

  // Trigger on foreach with non-array.
  @NoAllocation
  public int iteration(Iterable<Object> a) {
    int result = 0;
    // BUG: Diagnostic contains: @NoAllocation
    // Iterating
    for (Object o : a) {
      result++;
    }
    return result;
  }

  // Trigger on autoboxing.
  @NoAllocation
  public Integer assignBox(int i) {
    Integer in;
    // BUG: Diagnostic contains: @NoAllocation
    // Assigning a primitive value
    in = i;
    return in;
  }

  @NoAllocation
  public Integer initializeBox(int i) {
    // BUG: Diagnostic contains: @NoAllocation
    // Initializing a non-primitive
    Integer in = i;
    return in;
  }

  @NoAllocation
  public Integer initializeBoxLiteral() {
    // BUG: Diagnostic contains: @NoAllocation
    // Initializing a non-primitive
    Integer in = 0;
    return in;
  }

  @NoAllocation
  public int castBox(int i) {
    // BUG: Diagnostic contains: @NoAllocation
    // Casting a primitive
    int in = (Integer) i;
    return in;
  }

  @NoAllocation
  public Integer returnBox(int i) {
    // BUG: Diagnostic contains: @NoAllocation
    // Returning a primitive
    return i;
  }

  @NoAllocation
  public int unBox(Integer i) {
    return i;
  }

  @NoAllocation
  public void callBox(int i) {
    // BUG: Diagnostic contains: @NoAllocation
    // Calling a method
    unBox(i);
  }

  @NoAllocation
  public int unBox2(int i1, Integer i2) {
    return i2;
  }

  @NoAllocation
  public void callBox2(int i1, int i2) {
    // BUG: Diagnostic contains: @NoAllocation
    // Calling a method
    unBox2(i1, i2);
  }

  @NoAllocation
  public int unBox3(Integer i1, int i2) {
    return i1;
  }

  @NoAllocation
  public void callBox3(int i1, int i2) {
    // BUG: Diagnostic contains: @NoAllocation
    // Calling a method
    unBox3(i1, i2);
  }

  @NoAllocation
  public int varArgsMethod(int a, int... b) {
    return a + b[0];
  }

  @NoAllocation
  public void callVarArgs0() {
    // BUG: Diagnostic contains: @NoAllocation
    // Calling a method
    varArgsMethod(0);
  }

  @NoAllocation
  public void callVarArgs() {
    // BUG: Diagnostic contains: @NoAllocation
    // Calling a method
    varArgsMethod(1, 2);
  }

  @NoAllocation
  public void callVarArgs2() {
    // BUG: Diagnostic contains: @NoAllocation
    // Calling a method
    varArgsMethod(1, 2, 3);
  }

  @NoAllocation
  public Object varArgsMethodObject(Object a, Object... b) {
    return b[0];
  }

  @NoAllocation
  public void callVarArgsObject(Object a, Object[] b) {
    // BUG: Diagnostic contains: @NoAllocation
    // Calling a method
    varArgsMethodObject(a, b[0]);
  }

  @NoAllocation
  public void callVarArgsObjectWithPrimitiveArray(Object a, int[] b) {
    // BUG: Diagnostic contains: @NoAllocation
    // Calling a method
    varArgsMethodObject(a, b);
  }

  @NoAllocation
  public int forBox(int[] a) {
    int count = 0;
    // BUG: Diagnostic contains: @NoAllocation
    // Pre- and post- increment/decrement
    for (Integer i = 0; i < a.length; i++) {
      count++;
    }
    return count;
  }

  @NoAllocation
  public void arrayBox(Integer[] a, int i) {
    // BUG: Diagnostic contains: @NoAllocation
    // Assigning a primitive value
    a[0] = i;
  }

  @NoAllocation
  public int preIncrementBox(Integer i) {
    // BUG: Diagnostic contains: @NoAllocation
    // Pre- and post- increment/decrement
    ++i;
    return i;
  }

  @NoAllocation
  public int postIncrementBox(Integer i) {
    // BUG: Diagnostic contains: @NoAllocation
    // Pre- and post- increment/decrement
    i++;
    return i;
  }

  @NoAllocation
  public int preDecrementBox(Integer i) {
    // BUG: Diagnostic contains: @NoAllocation
    // Pre- and post- increment/decrement
    --i;
    return i;
  }

  @NoAllocation
  public int postDecrementBox(Integer i) {
    // BUG: Diagnostic contains: @NoAllocation
    // Pre- and post- increment/decrement
    i--;
    return i;
  }

  @NoAllocation
  public int forEachBox(int[] a) {
    int last = -1;
    // BUG: Diagnostic contains: @NoAllocation
    // Iterating
    for (Integer i : a) {
      last = i;
    }
    return last;
  }

  @NoAllocation
  public void arrayPreIncrementBox(Integer[] a) {
    // BUG: Diagnostic contains: @NoAllocation
    // Pre- and post- increment/decrement
    ++a[0];
  }

  @NoAllocation
  public void arrayPostIncrementBox(Integer[] a) {
    // BUG: Diagnostic contains: @NoAllocation
    // Pre- and post- increment/decrement
    a[0]++;
  }

  @NoAllocation
  public void arrayPreDecrementBox(Integer[] a) {
    // BUG: Diagnostic contains: @NoAllocation
    // Pre- and post- increment/decrement
    --a[0];
  }

  @NoAllocation
  public void arrayPostDecrementBox(Integer[] a) {
    // BUG: Diagnostic contains: @NoAllocation
    // Pre- and post- increment/decrement
    a[0]--;
  }

  @NoAllocation
  public int compoundBox(Integer a, int b) {
    // BUG: Diagnostic contains: @NoAllocation
    // Compound assignment
    a += b;
    return a;
  }

  @NoAllocation
  public void arrayCompoundBox(Integer[] a, int b) {
    // BUG: Diagnostic contains: @NoAllocation
    // Compound assignment
    a[0] += b;
  }

  @NoAllocation
  public void andAssignmentBox(Integer i1, Integer i2) {
    // BUG: Diagnostic contains: @NoAllocation
    // Compound assignment
    i1 &= i2;
  }

  @NoAllocation
  public void divideAssignmentBox(Integer i1, Integer i2) {
    // BUG: Diagnostic contains: @NoAllocation
    // Compound assignment
    i1 /= i2;
  }

  @NoAllocation
  public void leftShiftAssignmentBox(Integer i1, Integer i2) {
    // BUG: Diagnostic contains: @NoAllocation
    // Compound assignment
    i1 <<= i2;
  }

  @NoAllocation
  public void minusAssignmentBox(Integer i1, Integer i2) {
    // BUG: Diagnostic contains: @NoAllocation
    // Compound assignment
    i1 -= i2;
  }

  @NoAllocation
  public void multiplyAssignmentBox(Integer i1, Integer i2) {
    // BUG: Diagnostic contains: @NoAllocation
    // Compound assignment
    i1 *= i2;
  }

  @NoAllocation
  public void orAssignmentBox(Integer i1, Integer i2) {
    // BUG: Diagnostic contains: @NoAllocation
    // Compound assignment
    i1 |= i2;
  }

  @NoAllocation
  public void plusAssignmentBox(Integer i1, Integer i2) {
    // BUG: Diagnostic contains: @NoAllocation
    // Compound assignment
    i1 += i2;
  }

  @NoAllocation
  public void remainderAssignmentBox(Integer i1, Integer i2) {
    // BUG: Diagnostic contains: @NoAllocation
    // Compound assignment
    i1 %= i2;
  }

  @NoAllocation
  public void rightShiftAssignmentBox(Integer i1, Integer i2) {
    // BUG: Diagnostic contains: @NoAllocation
    // Compound assignment
    i1 >>= i2;
  }

  @NoAllocation
  public void unsignedRightShiftAssignmentBox(Integer i1, Integer i2) {
    // BUG: Diagnostic contains: @NoAllocation
    // Compound assignment
    i1 >>>= i2;
  }

  @NoAllocation
  public void xorAssignmentBox(Integer i1, Integer i2) {
    // BUG: Diagnostic contains: @NoAllocation
    // Compound assignment
    i1 ^= i2;
  }

  // Cloning is right out.
  @NoAllocation
  public Object doClone() throws CloneNotSupportedException {
    // BUG: Diagnostic contains: @NoAllocation
    // Calling a method
    return clone();
  }

  // Throwing doesn't exempt through method declarations.
  @NoAllocation
  public String throwForeach(final Iterable<Object> a) {
    throw new RuntimeException() {
      @NoAllocation
      private void f() {
        // BUG: Diagnostic contains: @NoAllocation
        // Iterating
        for (Object o : a) {
          // BUG: Diagnostic contains: @NoAllocation
          // Calling a method
          a.toString();
        }
      }
    };
  }

  public interface NoAllocationInterface {
    @NoAllocation
    void method();
  }

  public static class NoAllocationImplementingClass implements NoAllocationInterface {
    @Override
    // BUG: Diagnostic contains: @NoAllocation
    public void method() {}
  }

  public abstract static class NoAllocationAbstractClass {
    @NoAllocation
    abstract void method();
  }

  public static class NoAllocationConcreteClass extends NoAllocationAbstractClass {
    @Override
    // BUG: Diagnostic contains: @NoAllocation
    void method() {}
  }

  public static class NoAllocationParentClass implements NoAllocationInterface {
    @Override
    @NoAllocation
    public void method() {}
  }

  public static class NoAllocationSubclass extends NoAllocationParentClass {
    @Override
    // BUG: Diagnostic contains: @NoAllocation
    public void method() {}
  }
}
