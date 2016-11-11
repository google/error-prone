/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

package com.google.errorprone.bugpatterns.collectionincompatibletype.testdata;

import com.google.errorprone.annotations.CompatibleWith;
import java.util.Set;

/** Test case for enclosing type */
public class IncompatibleArgumentTypeEnclosingTypes {
  static class Foo<Y> {
    class Bar {
      void doSomething(@CompatibleWith("Y") Object x) {}
    }

    class Sub<X> {
      class SubSub<X> {
        void doSomething(@CompatibleWith("X") Object nestedResolution) {}

        <X> X methodVarIsReturn(@CompatibleWith("X") Object nestedResolution) {
          return null;
        }

        <X> void methodVarIsFree(@CompatibleWith("X") Object nestedResolution) {}

        void compatibleWithBase(@CompatibleWith("Y") Object nestedResolution) {}
      }
    }

    static class Baz {
      // Shouldn't resolve to anything, would be a compile error due to CompatibleWithMisuse
      static void doSomething(@CompatibleWith("X") Object x) {}
    }
  }

  void testSubs() {
    new Foo<String>().new Bar().doSomething("a");
    // BUG: Diagnostic contains: int is not compatible with the required type: String
    new Foo<String>().new Bar().doSomething(123);
    new Foo<Integer>().new Bar().doSomething(123);

    Foo.Bar rawtype = new Foo<String>().new Bar();
    rawtype.doSomething(123); // Weakness, rawtype isn't specialized in Foo

    Foo.Baz.doSomething(123); // No resolution of X
  }

  void testMegasub() {
    new Foo<String>().new Sub<Integer>().new SubSub<Boolean>().doSomething(true);
    // BUG: Diagnostic contains: int is not compatible with the required type: Boolean
    new Foo<String>().new Sub<Integer>().new SubSub<Boolean>().doSomething(123);

    // X in method is unbound
    new Foo<String>().new Sub<Integer>().new SubSub<Boolean>().methodVarIsReturn(123);

    // BUG: Diagnostic contains: int is not compatible with the required type: Set<?>
    new Foo<String>().new Sub<Integer>().new SubSub<Boolean>().<Set<?>>methodVarIsReturn(123);

    // BUG: Diagnostic contains: int is not compatible with the required type: String
    new Foo<String>().new Sub<Integer>().new SubSub<Boolean>().<Set<?>>compatibleWithBase(123);
  }

  void extraStuff() {
    // Javac throws away the type of <X> since it's not used in params/return type, so we can't
    // enforce it here.
    new Foo<String>().new Sub<Integer>().new SubSub<Boolean>().<Set<?>>methodVarIsFree(123);
  }
}
