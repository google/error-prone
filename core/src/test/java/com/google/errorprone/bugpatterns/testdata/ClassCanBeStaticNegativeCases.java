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

/** @author alexloh@google.com (Alex Loh) */
public class ClassCanBeStaticNegativeCases {
  int outerVar;

  public int outerMethod() {
    return 0;
  }

  public static class Inner1 { // inner class already static
    int innerVar;
  }

  public class Inner2 { // inner class references an outer variable
    int innerVar = outerVar;
  }

  public class Inner3 { // inner class references an outer variable in a method
    int localMethod() {
      return outerVar;
    }
  }

  public class Inner4 { // inner class references an outer method in a method
    int localMethod() {
      return outerMethod();
    }
  }

  // outer class is a nested but non-static, and thus cannot have a static class
  class NonStaticOuter {
    int nonStaticVar = outerVar;

    class Inner5 {}
  }

  // inner class is local and thus cannot be static
  void foo() {
    class Inner6 {}
  }

  // inner class is anonymous and thus cannot be static
  Object bar() {
    return new Object() {};
  }

  // enums are already static
  enum Inner7 {
    RED,
    BLUE,
    VIOLET,
  }

  // outer class is a nested but non-static, and thus cannot have a static class
  void baz() {
    class NonStaticOuter2 {
      int nonStaticVar = outerVar;

      class Inner8 {}
    }
  }

  // inner class references a method from inheritance
  public static interface OuterInter {
    int outerInterMethod();
  }

  abstract static class AbstractOuter implements OuterInter {
    class Inner8 {
      int localMethod() {
        return outerInterMethod();
      }
    }
  }
}
