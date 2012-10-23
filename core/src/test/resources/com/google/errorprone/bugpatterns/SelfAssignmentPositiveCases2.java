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

package com.google.errorprone.bugpatterns;

/**
 * Tests for self assignment
 * 
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public class SelfAssignmentPositiveCases2 {
  // TODO(eaftan): what happens with a static field that has the same name 
  // as a local field? 
  
  private int a;
  private Foo foo;
    
  public void test6() {
    Foo foo = new Foo();
    foo.a = 2;
    //BUG: Suggestion includes "remove this line"
    foo.a = foo.a;
  }
  
  public void test7() {
    Foobar f = new Foobar();
    f.foo = new Foo();
    f.foo.a = 10;
    //BUG: Suggestion includes "remove this line"
    f.foo.a = f.foo.a;
  }
  
  public void test8() {
    foo = new Foo();
    //BUG: Suggestion includes "remove this line"
    this.foo.a = foo.a;
  }
  
  public void test9(Foo fao, Foo bar) {
    //BUG: Suggestion includes "this.foo = fao"
    this.foo = foo;
  }
  
  public void test10(Foo foo) {
    //BUG: Suggestion includes "this.foo = foo"
    foo = foo;
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
