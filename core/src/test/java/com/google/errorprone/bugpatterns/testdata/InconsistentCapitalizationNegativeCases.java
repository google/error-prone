/*
 * Copyright 2018 The Error Prone Authors.
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

/** Negative cases for {@link com.google.errorprone.bugpatterns.InconsistentCapitalizationTest}. */
public class InconsistentCapitalizationNegativeCases {

  public void doesntConflictWithOtherVariables() {
    int aa;
    int aA;
  }

  public void doesntConflictWithVariableOutOfScope() {
    if (true) {
      int a;
    }
    if (true) {
      int a;
    }
  }

  public void doesntConflictBetweenForVariables() {
    for (int i = 0; i < 1; i++) {}

    for (int i = 0; i < 1; i++) {}
  }

  private class DoesntConflictBetweenMethods {
    int a;

    void a() {}

    void b(int baba) {
      int c = baba;
      if (c == baba) {}
    }

    void c() {
      int c;
    }
  }

  private static class DoesntConflictWithClass {

    static int B;

    static class A {

      static int A;
    }

    class B {}
  }

  private static class DoesAllowUpperCaseStaticVariable {

    static int A;

    void method() {
      int a;
    }
  }

  private enum DoesntConflictWithUpperCaseEnum {
    TEST;

    private Object test;
  }

  public void doesntConflictWithMethodParameter(long aa) {
    int aA;
  }

  private class DoesntConflictWithConstructorParameter {

    DoesntConflictWithConstructorParameter(Object aa) {
      Object aA;
    }
  }

  private class DoesntConflictOutOfScope {

    class A {
      private Object aaa;
      private Object aab;
    }

    class B {
      private Object aaA;

      void method(String aaB) {
        char aAb;
      }
    }
  }

  private static class DoesntReplaceMember {

    class A {
      Object aa;
      Object ab;

      void method() {
        B b = new B();
        aa = b.aA;
        ab = b.aB.aA;
        new B().aA();
        aa.equals(ab);
        aa.equals(b.aB.aA);
        aa.equals(b.aB);
      }
    }

    class B {
      Object aA;
      C aB = new C();

      void aA() {}
    }

    class C {
      Object aA;
    }
  }

  class DoesntConflictWithNested {
    Object aa;
    Object ab;

    class Nested {
      Object aB;

      Nested(Object aa) {
        DoesntConflictWithNested.this.aa = aa;
      }

      class Nested2 {
        Object aB;

        Nested2(Object aa) {
          DoesntConflictWithNested.this.aa = aa;
        }
      }
    }
  }

  static class DoesntFixExternalParentClassFieldMatch {

    static class Parent {
      Object aa;
    }

    static class Child extends Parent {

      Child(Object aA) {
        aa = aA;
      }
    }
  }
}
