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

package com.google.errorprone.bugpatterns.testdata;

import com.google.errorprone.annotations.NoAllocation;
import java.util.Arrays;

/** @author agoode@google.com (Adam Goode) */
public class NoAllocationCheckerNegativeCases {
  // Calling safe methods is fine.
  @NoAllocation
  public boolean comparison(int n) {
    return n > 1;
  }

  @NoAllocation
  public void callNoAllocationMethod() {
    comparison(5);
  }

  @NoAllocation
  @SuppressWarnings({"foo, bar"})
  public void annotatedWithArray() {}

  @NoAllocation
  public boolean arrayComparison(int[] a) {
    return a.length > 0 && a[0] > 1;
  }

  // Non string operations are fine.
  @NoAllocation
  public int sumInts(int a, int b) {
    return a + b;
  }

  @NoAllocation
  public int addOne(int a) {
    a += 1;
    return a;
  }

  // Foreach is allowed on arrays.
  @NoAllocation
  public int forEachArray(int[] a) {
    int last = -1;
    for (int i : a) {
      last = i;
    }
    return last;
  }

  // Varargs is ok if no autoboxing occurs.
  @NoAllocation
  public int varArgsMethod2(int a, int... b) {
    return a + b[0];
  }

  @NoAllocation
  public void callVarArgsNoAllocation(int[] b) {
    varArgsMethod2(1, b);
  }

  @NoAllocation
  public Object varArgsMethodObject2(Object a, Object... b) {
    return b[0];
  }

  @NoAllocation
  public void callVarArgsObject2(Object a, Object[] b) {
    varArgsMethodObject2(a, b);
  }

  // Unboxing is fine.
  @NoAllocation
  public void unboxByCalling(Integer i) {
    comparison(i);
  }

  @NoAllocation
  public int binaryUnbox(Integer a, int b) {
    return a + b;
  }

  // We can call a non-annotated method if we suppress warnings.
  @NoAllocation
  @SuppressWarnings("NoAllocation")
  public void trustMe() {
    String s = new String();
  }

  @NoAllocation
  public void trusting() {
    trustMe();
  }

  // Allocations are allowed in a throw statement.
  @NoAllocation
  public void throwNew() {
    throw new RuntimeException();
  }

  @NoAllocation
  public void throwNewArray() {
    throw new RuntimeException(Arrays.toString(new int[10]));
  }

  @NoAllocation
  public void throwMethod() {
    throw new RuntimeException(Integer.toString(5));
  }

  @NoAllocation
  public void throwStringConcatenation() {
    throw new RuntimeException("a" + 5);
  }

  @NoAllocation
  public void throwStringConcatenation2() {
    throw new RuntimeException("a" + Integer.toString(5));
  }

  @NoAllocation
  public void throwStringConcatenation3() {
    throw new RuntimeException("a" + getInt());
  }

  @NoAllocation
  public String throwStringConvCompoundAssign(int i) {
    String s = "";
    throw new RuntimeException(s += i);
  }

  class IntegerException extends RuntimeException {
    public IntegerException(Integer i) {
      super(i.toString());
    }
  }

  @NoAllocation
  public String throwBoxingCompoundAssign(Integer in, int i) {
    throw new IntegerException(in += i);
  }

  @NoAllocation
  public String throwBoxingAssign(Integer in, int i) {
    throw new IntegerException(in = i);
  }

  @NoAllocation
  public String throwBoxingInitialization(final int i) {
    throw new RuntimeException() {
      Integer in = i;
    };
  }

  @NoAllocation
  public String throwBoxingCast(int i) {
    throw new IntegerException((Integer) i);
  }

  @NoAllocation
  public String throwBoxingInvocation(int i) {
    throw new IntegerException(i);
  }

  class VarArgsException extends RuntimeException {
    public VarArgsException(int... ints) {
      super(Arrays.toString(ints));
    }
  }

  @NoAllocation
  public String throwBoxingVarArgs(int i) {
    throw new VarArgsException(i, i, i, 4);
  }

  @NoAllocation
  public String throwBoxingUnary(Integer i) {
    throw new IntegerException(i++);
  }

  // All of the positive cases with @NoAllocation removed are below.
  public int[] newArray(int size) {
    return new int[size];
  }

  public int[] arrayInitializer(int a, int b) {
    int[] array = {a, b};
    return array;
  }

  public int[] returnArrayInitializer(int a, int b) {
    return new int[] {a, b};
  }

  public String newString(String s) {
    return new String(s);
  }

  public String allocateString() {
    return new String();
  }

  public String getString() {
    return allocateString();
  }

  public int getInt() {
    return 1;
  }

  public String stringConvReturn(int i) {
    return "" + i;
  }

  public String stringConvAssign(int i) {
    String s = "" + i;
    return s;
  }

  public String stringConvAssign2(int i) {
    String s = "";
    s = s + i;
    return s;
  }

  public String stringConvAssign3(int i) {
    String s = "";
    s = i + s;
    return s;
  }

  public String stringConvReturnMethod() {
    String s = "" + getInt();
    return s;
  }

  public String stringConvCompoundAssign(int i) {
    String s = "";
    s += i;
    return s;
  }

  public String stringConvCompoundReturnMethod() {
    String s = "";
    s += getInt();
    return s;
  }

  public String doubleString(String s) {
    return s + s;
  }

  public String doubleStringCompound(String s) {
    s += s;
    return s;
  }

  public int iteration(Iterable<Object> a) {
    int result = 0;
    for (Object o : a) {
      result++;
    }
    return result;
  }

  public Integer assignBox(int i) {
    Integer in;
    in = i;
    return in;
  }

  public Integer initializeBox(int i) {
    Integer in = i;
    return in;
  }

  public Integer initializeBoxLiteral() {
    Integer in = 0;
    return in;
  }

  public int castBox(int i) {
    int in = (Integer) i;
    return in;
  }

  public Integer returnBox(int i) {
    return i;
  }

  public int unBox(Integer i) {
    return i;
  }

  public void callBox(int i) {
    unBox(i);
  }

  public int unBox2(int i1, Integer i2) {
    return i2;
  }

  public void callBox2(int i1, int i2) {
    unBox2(i1, i2);
  }

  public int unBox3(Integer i1, int i2) {
    return i1;
  }

  public void callBox3(int i1, int i2) {
    unBox3(i1, i2);
  }

  public int varArgsMethod(int a, int... b) {
    return a + b[0];
  }

  public void callVarArgs0() {
    varArgsMethod(0);
  }

  public void callVarArgs() {
    varArgsMethod(1, 2);
  }

  public void callVarArgs2() {
    varArgsMethod(1, 2, 3);
  }

  public Object varArgsMethodObject(Object a, Object... b) {
    return b[0];
  }

  public void callVarArgsObject(Object a, Object[] b) {
    varArgsMethodObject(a, b[0]);
  }

  public void callVarArgsObjectWithPrimitiveArray(Object a, int[] b) {
    varArgsMethodObject(a, b);
  }

  public int forBox(int[] a) {
    int count = 0;
    for (Integer i = 0; i < a.length; i++) {
      count++;
    }
    return count;
  }

  public void arrayBox(Integer[] a, int i) {
    a[0] = i;
  }

  public int preIncrementBox(Integer i) {
    ++i;
    return i;
  }

  public int postIncrementBox(Integer i) {
    i++;
    return i;
  }

  public int preDecrementBox(Integer i) {
    --i;
    return i;
  }

  public int postDecrementBox(Integer i) {
    i--;
    return i;
  }

  public int forEachBox(int[] a) {
    int last = -1;
    for (Integer i : a) {
      last = i;
    }
    return last;
  }

  public void arrayPreIncrementBox(Integer[] a) {
    ++a[0];
  }

  public void arrayPostIncrementBox(Integer[] a) {
    a[0]++;
  }

  public void arrayPreDecrementBox(Integer[] a) {
    --a[0];
  }

  public void arrayPostDecrementBox(Integer[] a) {
    a[0]--;
  }

  public int compoundBox(Integer a, int b) {
    a += b;
    return a;
  }

  public void arrayCompoundBox(Integer[] a, int b) {
    a[0] += b;
  }

  public void andAssignmentBox(Integer i1, Integer i2) {
    i1 &= i2;
  }

  public void divideAssignmentBox(Integer i1, Integer i2) {
    i1 /= i2;
  }

  public void leftShiftAssignmentBox(Integer i1, Integer i2) {
    i1 <<= i2;
  }

  public void minusAssignmentBox(Integer i1, Integer i2) {
    i1 -= i2;
  }

  public void multiplyAssignmentBox(Integer i1, Integer i2) {
    i1 *= i2;
  }

  public void orAssignmentBox(Integer i1, Integer i2) {
    i1 |= i2;
  }

  public void plusAssignmentBox(Integer i1, Integer i2) {
    i1 += i2;
  }

  public void remainderAssignmentBox(Integer i1, Integer i2) {
    i1 %= i2;
  }

  public void rightShiftAssignmentBox(Integer i1, Integer i2) {
    i1 >>= i2;
  }

  public void unsignedRightShiftAssignmentBox(Integer i1, Integer i2) {
    i1 >>>= i2;
  }

  public void xorAssignmentBox(Integer i1, Integer i2) {
    i1 ^= i2;
  }

  public Object doClone() throws CloneNotSupportedException {
    return clone();
  }

  @NoAllocation
  public String throwForeach(final Iterable<Object> a) {
    throw new RuntimeException() {
      private void f() {
        for (Object o : a) {
          a.toString();
        }
      }
    };
  }
}
