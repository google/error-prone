---
title: NoAllocation
layout: bugpattern
category: JDK
severity: ERROR
maturity: EXPERIMENTAL
---

# Bug pattern: NoAllocation
__@NoAllocation was specified on this method, but something was found that would trigger an allocation__

## The problem
Like many other languages, Java provides automatic memory management. In Java, this feature incurs an runtime cost, and can also lead to unpredictable execution pauses. In most cases, this is a reasonable tradeoff, but sometimes the loss of performance or predictability is unacceptable. Examples include pause-sensitive user interface handlers, high query rate server response handlers, or other soft-realtime applications.

In these situations, you can annotate a few carefully written methods with @NoAllocation. Methods with this annotation will avoid allocations in most cases, reducing pressure on the garbage collector. Note that allocations may still occur in methods with @NoAllocation if the compiler or runtime system inserts them.

To ease the use of exceptions, allocations are allowed if they occur within a throw statement. But if the throw statement contains a nested class with methods annotated with @NoAllocation, those methods will be disallowed from allocating.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("NoAllocation")` annotation to the enclosing element.

----------

# Examples
__NoAllocationCheckerNegativeCases.java__
{% highlight java %}
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

package com.google.errorprone.bugpatterns;

import com.google.errorprone.annotations.NoAllocation;

import java.util.Arrays;

/**
 * @author agoode@google.com (Adam Goode)
 */
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
    return new int[]{a, b};
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

{% endhighlight %}
__NoAllocationCheckerPositiveCases.java__
{% highlight java %}
/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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

package com.google.errorprone.bugpatterns;

import com.google.errorprone.annotations.NoAllocation;

/**
 * @author agoode@google.com (Adam Goode)
 */
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
    return new int[]{a, b};
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

  // Throwing doesn't whitelist through method declarations.
  @NoAllocation
  public String throwForeach(final Iterable<Object> a) {
    throw new RuntimeException() {
      @NoAllocation
      private void f() {
        // BUG: Diagnostic contains: @NoAllocation
        // Iterating
        for (Object o : a) {
          a.toString();
        }
      }
    };
  }
}

{% endhighlight %}
