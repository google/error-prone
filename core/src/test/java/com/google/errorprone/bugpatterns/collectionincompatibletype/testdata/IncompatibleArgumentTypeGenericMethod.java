/*
 * Copyright 2016 The Error Prone Authors.
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

/** Test IncompatibleArgumentType with a generic method */
public class IncompatibleArgumentTypeGenericMethod {
  class A<B> {
    <C> C remove(@CompatibleWith("B") Object b, @CompatibleWith("C") Object c) {
      return null;
    }

    <C> C varargs(@CompatibleWith("B") Object b, @CompatibleWith("C") Object... cs) {
      return (C) cs[0];
    }
  }

  class C extends A<String> {}

  void testfoo(C c, A<?> unbound, A<? extends Number> boundToNumber) {
    c.remove("a", null); // OK, match null to Double
    c.remove("a", 123.0); // OK, match Double to Double
    c.remove("a", 123); // OK, 2nd arg is unbound

    unbound.remove(null, 123); // OK, variables unbound

    // BUG: Diagnostic contains: String is not compatible with the required type: Number
    boundToNumber.remove("123", null);

    // BUG: Diagnostic contains: int is not compatible with the required type: Double
    Double d = c.remove("a", 123);
    // BUG: Diagnostic contains: int is not compatible with the required type: Double
    c.<Double>remove("a", 123);

    // BUG: Diagnostic contains: float is not compatible with the required type: Double
    c.<Double>remove(123, 123.0f);
  }

  void testVarargs(A<String> stringA) {
    // OK, all varargs elements compatible with Integer
    Integer first = stringA.varargs("hi", 2, 3, 4);

    // BUG: Diagnostic contains: long is not compatible with the required type: Integer
    first = stringA.varargs("foo", 2, 3L);

    // OK, everything compatible w/ Object
    Object o = stringA.varargs("foo", 2L, 1.0d, "a");
  }
}
