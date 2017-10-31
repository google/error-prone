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
import static java.util.Objects.requireNonNull;

/**
 * Tests for self assignment
 *
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public class SelfAssignmentPositiveCases2 {
  private int a;
  private Foo foo;

  // BUG: Diagnostic contains: private static final Object obj
  private static final Object obj = SelfAssignmentPositiveCases2.obj;
  // BUG: Diagnostic contains: private static final Object obj2
  private static final Object obj2 = checkNotNull(SelfAssignmentPositiveCases2.obj2);

  public void test6() {
    Foo foo = new Foo();
    foo.a = 2;
    // BUG: Diagnostic contains: remove this line
    foo.a = foo.a;
    // BUG: Diagnostic contains: checkNotNull(foo.a)
    foo.a = checkNotNull(foo.a);
    // BUG: Diagnostic contains: requireNonNull(foo.a)
    foo.a = requireNonNull(foo.a);
  }

  public void test7() {
    Foobar f = new Foobar();
    f.foo = new Foo();
    f.foo.a = 10;
    // BUG: Diagnostic contains: remove this line
    f.foo.a = f.foo.a;
    // BUG: Diagnostic contains: checkNotNull(f.foo.a)
    f.foo.a = checkNotNull(f.foo.a);
    // BUG: Diagnostic contains: requireNonNull(f.foo.a)
    f.foo.a = requireNonNull(f.foo.a);
  }

  public void test8() {
    foo = new Foo();
    // BUG: Diagnostic contains: remove this line
    this.foo.a = foo.a;
    // BUG: Diagnostic contains: checkNotNull(foo.a)
    this.foo.a = checkNotNull(foo.a);
    // BUG: Diagnostic contains: requireNonNull(foo.a)
    this.foo.a = requireNonNull(foo.a);
  }

  public void test9(Foo fao, Foo bar) {
    // BUG: Diagnostic contains: this.foo = fao
    this.foo = foo;
    // BUG: Diagnostic contains: this.foo = checkNotNull(fao)
    this.foo = checkNotNull(foo);
    // BUG: Diagnostic contains: this.foo = requireNonNull(fao)
    this.foo = requireNonNull(foo);
  }

  public void test10(Foo foo) {
    // BUG: Diagnostic contains: this.foo = foo
    foo = foo;
    // BUG: Diagnostic contains: this.foo = checkNotNull(foo)
    foo = checkNotNull(foo);
    // BUG: Diagnostic contains: this.foo = requireNonNull(foo)
    foo = requireNonNull(foo);
  }

  class Test11 {
    final Foo foo;

    Foo fao;

    Test11(Foo foo) {
      if (true) {
        // BUG: Diagnostic contains: this.fao = foo
        foo = foo;
      }
      this.foo = foo;
    }

    public void test11a(Foo foo) {
      // BUG: Diagnostic contains: this.fao = foo
      foo = foo;
    }
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
