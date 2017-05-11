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

package com.google.errorprone.bugpatterns.testdata;

/** @author avenet@google.com (Arnaud J. Venet) */
public class EqualsIncompatibleTypeNegativeCases {
  class A {
    public boolean equals(Object o) {
      if (o instanceof A) {
        return true;
      }
      return false;
    }
  }

  class B1 extends A {}

  class B2 extends A {}

  class B3 extends B2 {}

  void checkEqualsAB1B2B3(A a, B1 b1, B2 b2, B3 b3) {
    a.equals(a);
    a.equals(b1);
    a.equals(b2);
    a.equals(b3);
    a.equals(null);

    b1.equals(a);
    b1.equals(b1);
    b1.equals(b2);
    b1.equals(b3);
    b1.equals(null);

    b2.equals(a);
    b2.equals(b1);
    b2.equals(b2);
    b2.equals(b3);
    b2.equals(null);

    b3.equals(a);
    b3.equals(b1);
    b3.equals(b2);
    b3.equals(b3);
    b3.equals(null);
  }

  void checks(Object o, boolean[] bools, boolean bool) {
    o.equals(bool);
    o.equals(bools[0]);
  }

  void checkJUnit(B1 b1, B2 b2) {
    org.junit.Assert.assertFalse(b1.equals(b2));
  }

  void checkStaticEquals(A a, B1 b1, B2 b2, B3 b3) {
    java.util.Objects.equals(a, a);
    java.util.Objects.equals(a, b1);
    java.util.Objects.equals(a, b2);
    java.util.Objects.equals(a, b3);
    java.util.Objects.equals(a, null);

    java.util.Objects.equals(b1, b3);
    java.util.Objects.equals(b2, b3);
    java.util.Objects.equals(b3, b3);
    java.util.Objects.equals(null, b3);
  }

  void checkGuavaStaticEquals(A a, B1 b1, B2 b2, B3 b3) {
    com.google.common.base.Objects.equal(a, a);
    com.google.common.base.Objects.equal(a, b1);
    com.google.common.base.Objects.equal(a, b2);
    com.google.common.base.Objects.equal(a, b3);
    com.google.common.base.Objects.equal(a, null);

    com.google.common.base.Objects.equal(b1, b3);
    com.google.common.base.Objects.equal(b2, b3);
    com.google.common.base.Objects.equal(b3, b3);
    com.google.common.base.Objects.equal(null, b3);
  }

  class C {}

  abstract class C1 extends C {
    public abstract boolean equals(Object o);
  }

  abstract class C2 extends C1 {}

  abstract class C3 extends C1 {}

  void checkEqualsC1C2C3(C1 c1, C2 c2, C3 c3) {
    c1.equals(c1);
    c1.equals(c2);
    c1.equals(c3);
    c1.equals(null);

    c2.equals(c1);
    c2.equals(c2);
    c2.equals(c3);
    c2.equals(null);

    c3.equals(c1);
    c3.equals(c2);
    c3.equals(c3);
    c3.equals(null);
  }

  interface I {
    public boolean equals(Object o);
  }

  class E1 implements I {}

  class E2 implements I {}

  class E3 extends E2 {}

  void checkEqualsIE1E2E3(I e, E1 e1, E2 e2, E3 e3) {
    e.equals(e);
    e.equals(e1);
    e.equals(e2);
    e.equals(e3);
    e.equals(null);

    e1.equals(e);
    e1.equals(e1);
    e1.equals(e2);
    e1.equals(e3);
    e1.equals(null);

    e2.equals(e);
    e2.equals(e1);
    e2.equals(e2);
    e2.equals(e3);
    e2.equals(null);

    e3.equals(e);
    e3.equals(e1);
    e3.equals(e2);
    e3.equals(e3);
    e3.equals(null);
  }

  interface J {}

  class F1 implements J {}

  abstract class F2 {
    public abstract boolean equals(J o);
  }

  void checkOtherEquals(F1 f1, F2 f2) {
    f2.equals(f1);
  }
}
