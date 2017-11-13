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

import javax.annotation.CheckReturnValue;
import org.junit.rules.ExpectedException;

/** @author eaftan@google.com (Eddie Aftandilian) */
public class CheckReturnValuePositiveCases {

  IntValue intValue = new IntValue(0);

  @javax.annotation.CheckReturnValue
  private int increment(int bar) {
    return bar + 1;
  }

  public void foo() {
    int i = 1;
    // BUG: Diagnostic contains: remove this line
    increment(i);
    System.out.println(i);
  }

  public void bar() {
    // BUG: Diagnostic contains: this.intValue = this.intValue.increment()
    this.intValue.increment();
  }

  public void testIntValue() {
    IntValue value = new IntValue(10);
    // BUG: Diagnostic contains: value = value.increment()
    value.increment();
  }

  private void callRunnable(Runnable runnable) {
    runnable.run();
  }

  public void testResolvedToVoidLambda() {
    // BUG: Diagnostic contains: Ignored return value
    callRunnable(() -> this.intValue.increment());
  }

  public void testResolvedToVoidMethodReference() {
    // BUG: Diagnostic contains: Ignored return value
    callRunnable(this.intValue::increment);
  }

  public void testRegularLambda() {
    callRunnable(
        () -> {
          // BUG: Diagnostic contains: Ignored return value
          this.intValue.increment();
        });
  }

  public void testBeforeAndAfterRule() {
    // BUG: Diagnostic contains: remove this line
    new IntValue(1).increment();
    ExpectedException.none().expect(IllegalStateException.class);
    new IntValue(1).increment(); // No error here, last statement in block
  }

  public void constructor() {
    /*
     * We may or may not want to treat this as a bug. On the one hand, the
     * subclass might be "using" the superclass, so it might not be being
     * "ignored." (Plus, it would be a pain to produce a valid suggested fix
     * that incorporates any subclass constructor body, which might even contain
     * calls to methods in the class.) On the other hand, the more likely
     * scenario may be a class like IteratorTester, which requires (a) that the
     * user subclass it to implement a method and (b) that the user call test()
     * on the constructed object. There, it would be nice if IteratorTester
     * could be annotated with @CheckReturnValue to mean "anyone who creates an
     * anonymous subclasses of this should still do something with that
     * subclass." But perhaps that's an abuse of @CheckForNull.
     *
     * Anyway, these tests are here to ensure that subclasses don't don't crash
     * the compiler.
     */
    new MyObject() {};

    class MySubObject1 extends MyObject {}

    class MySubObject2 extends MyObject {
      MySubObject2() {}
    }

    class MySubObject3 extends MyObject {
      MySubObject3() {
        super();
      }
    }

    // TODO(cpovirk): This one probably ought to be treated as a bug:
    new MyObject();
  }

  private class IntValue {
    final int i;

    public IntValue(int i) {
      this.i = i;
    }

    @javax.annotation.CheckReturnValue
    public IntValue increment() {
      return new IntValue(i + 1);
    }

    public void increment2() {
      // BUG: Diagnostic contains: remove this line
      this.increment();
    }

    public void increment3() {
      // BUG: Diagnostic contains: remove this line
      increment();
    }
  }

  private static class MyObject {
    @javax.annotation.CheckReturnValue
    MyObject() {}
  }

  private abstract static class LB1<A> {}

  private static class LB2<A> extends LB1<A> {

    @javax.annotation.CheckReturnValue
    public static <T> LB2<T> lb1() {
      return new LB2<T>();
    }

    public static <T> LB2<T> lb2() {
      // BUG: Diagnostic contains: remove this line
      lb1();
      return lb1();
    }
  }

  private static class ErrorProneAnnotation {
    @com.google.errorprone.annotations.CheckReturnValue
    public static int check() {
      return 1;
    }

    public static void ignoresCheck() {
      // BUG: Diagnostic contains: remove this line
      check();
    }
  }
}
