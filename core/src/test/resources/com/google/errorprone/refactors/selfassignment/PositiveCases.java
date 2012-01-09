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

package com.google.errorprone.refactors.selfassignment;

/**
 * Tests for self assignment
 * 
 * TODO(eaftan): I think I'm hitting a limit on the number of errors in one file.
 * Refactor this into multiple files.
 *
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public class PositiveCases {
  // TODO(eaftan): what happens with a static field that has the same name 
  // as a local field? 
  
  private int a;
  
  public void test1(int b) {
    this.a = a;
  } 
  
  public void test2(int b) {
    a = this.a;
  }
  
  public void test3() {
    int a = 0;
    a = a;
  }
  
  public void test4() {
    this.a = this.a;
  }
  
  public void test5() {
    if ((a = a) != 10) {
      System.out.println("foo");
    }
  }
  
  public void test6() {
    Foo foo = new Foo();
    foo.a = 2;
    foo.a = foo.a;
  }
  
  public void test7() {
    Foobar f = new Foobar();
    f.foo = new Foo();
    f.foo.a = 10;
    f.foo.a = f.foo.a;
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
}
