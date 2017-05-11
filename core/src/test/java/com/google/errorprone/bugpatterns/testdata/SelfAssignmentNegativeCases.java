/*
 * Copyright 2011 Google Inc. All Rights Reserved.
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

/**
 * Tests for self assignment
 *
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public class SelfAssignmentNegativeCases {
  private int a;

  private static int b = StaticClass.b;
  private static final int C = SelfAssignmentNegativeCases.b;
  private static final int D = checkNotNull(SelfAssignmentNegativeCases.C);
  private static final int E = StaticClass.getIntArr().length;

  public void test1(int a) {
    int b = SelfAssignmentNegativeCases.b;
    this.a = a;
    this.a = checkNotNull(a);
  }

  public void test2() {
    int a = 0;
    int b = a;
    a = b;
  }

  public void test3() {
    int a = 10;
  }

  public void test4() {
    int i = 1;
    i += i;
  }

  public void test5(SelfAssignmentNegativeCases n) {
    a = n.a;
  }

  public void test6() {
    Foo foo = new Foo();
    Bar bar = new Bar();
    foo.a = bar.a;
    foo.a = checkNotNull(bar.a);
  }

  public void test7() {
    Foobar f1 = new Foobar();
    f1.foo = new Foo();
    f1.bar = new Bar();
    f1.foo.a = f1.bar.a;
    f1.foo.a = checkNotNull(f1.bar.a);
  }

  public void test8(SelfAssignmentNegativeCases that) {
    this.a = that.a;
    this.a = checkNotNull(that.a);
  }

  private static class Foo {
    int a;
  }

  private static class Bar {
    int a;
  }

  private static class Foobar {
    Foo foo;
    Bar bar;
  }

  private static class StaticClass {
    static int b;

    public static int[] getIntArr() {
      return new int[10];
    }
  }
}
