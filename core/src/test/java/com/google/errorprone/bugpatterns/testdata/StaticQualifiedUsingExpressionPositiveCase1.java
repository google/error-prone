/*
 * Copyright 2012 Google Inc. All Rights Reserved.
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

import java.math.BigDecimal;

/** @author eaftan@google.com (Eddie Aftandilian) */
class MyClass {

  static int STATIC_FIELD = 42;

  static int staticMethod() {
    return 42;
  }

  int FIELD = 42;

  int method() {
    return 42;
  }

  static class StaticInnerClass {
    static final MyClass myClass = new MyClass();
  }
}

class MyStaticClass {
  static MyClass myClass = new MyClass();
}

public class StaticQualifiedUsingExpressionPositiveCase1 {

  public static int staticVar1 = 1;
  private StaticQualifiedUsingExpressionPositiveCase1 next;

  public static int staticTestMethod() {
    return 1;
  }

  public static Object staticTestMethod2() {
    return new Object();
  }

  public static Object staticTestMethod3(Object x) {
    return null;
  }

  public void test1() {
    StaticQualifiedUsingExpressionPositiveCase1 testObj =
        new StaticQualifiedUsingExpressionPositiveCase1();
    int i;

    // BUG: Diagnostic contains: variable staticVar1
    // i = staticVar1
    i = this.staticVar1;
    // BUG: Diagnostic contains: variable staticVar1
    // i = staticVar1
    i = testObj.staticVar1;
    // BUG: Diagnostic contains: variable staticVar1
    // i = staticVar1
    i = testObj.next.next.next.staticVar1;
  }

  public void test2() {
    int i;
    Integer integer = new Integer(1);
    // BUG: Diagnostic contains: variable MAX_VALUE
    // i = Integer.MAX_VALUE
    i = integer.MAX_VALUE;
  }

  public void test3() {
    String s1 = new String();
    // BUG: Diagnostic contains: method valueOf
    // String s2 = String.valueOf(10)
    String s2 = s1.valueOf(10);
    // BUG: Diagnostic contains: method valueOf
    // s2 = String.valueOf(10)
    s2 = new String().valueOf(10);
    // BUG: Diagnostic contains: method staticTestMethod
    // int i = staticTestMethod()
    int i = this.staticTestMethod();
    // BUG: Diagnostic contains: method staticTestMethod2
    // String s3 = staticTestMethod2().toString
    String s3 = this.staticTestMethod2().toString();
    // BUG: Diagnostic contains: method staticTestMethod
    // i = staticTestMethod()
    i = this.next.next.next.staticTestMethod();
  }

  public void test4() {
    BigDecimal decimal = new BigDecimal(1);
    // BUG: Diagnostic contains: method valueOf
    // BigDecimal decimal2 = BigDecimal.valueOf(1)
    BigDecimal decimal2 = decimal.valueOf(1);
  }

  public static MyClass hiding;

  public void test5(MyClass hiding) {
    // BUG: Diagnostic contains: method staticTestMethod3
    // Object o = staticTestMethod3(this.toString())
    Object o = this.staticTestMethod3(this.toString());
    // BUG: Diagnostic contains: variable myClass
    // x = MyClass.StaticInnerClass.myClass.FIELD;
    int x = new MyClass.StaticInnerClass().myClass.FIELD;
    // BUG: Diagnostic contains: variable STATIC_FIELD
    // x = MyClass.STATIC_FIELD;
    x = new MyClass.StaticInnerClass().myClass.STATIC_FIELD;
    // BUG: Diagnostic contains: variable hiding
    // StaticQualifiedUsingExpressionPositiveCase1.hiding = hiding;
    this.hiding = hiding;
    // BUG: Diagnostic contains: variable STATIC_FIELD
    // x = MyClass.STATIC_FIELD;
    x = MyStaticClass.myClass.STATIC_FIELD;
    // BUG: Diagnostic contains: method staticMethod
    // x = MyClass.staticMethod();
    x = MyStaticClass.myClass.staticMethod();

    x = MyStaticClass.myClass.FIELD;
    x = MyStaticClass.myClass.method();
  }

  static class Bar {
    static int baz = 0;

    static int baz() {
      return 42;
    }
  }

  static class Foo {
    static Bar bar;
  }

  static void test6() {
    Foo foo = new Foo();
    // BUG: Diagnostic contains: method baz
    // x = Bar.baz();
    int x = Foo.bar.baz();
    Bar bar = Foo.bar;
    // BUG: Diagnostic contains: variable bar
    // bar = Foo.bar;
    bar = foo.bar;
    // BUG: Diagnostic contains: variable baz
    // x = Bar.baz;
    x = Foo.bar.baz;
  }

  static class C<T extends String> {
    static int foo() {
      return 42;
    }
  }

  public void test7() {
    // BUG: Diagnostic contains: method foo
    // x = C.foo();
    int x = new C<String>().foo();
  }
}
