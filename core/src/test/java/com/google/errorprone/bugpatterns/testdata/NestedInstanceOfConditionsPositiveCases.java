/* Copyright 2017 Google Inc. All Rights Reserved.
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

/**
 * @author mariasam@google.com (Maria Sam)
 * @author sulku@google.com (Marsela Sulku)
 */
public class NestedInstanceOfConditionsPositiveCases {

  public static void nestedInstanceOfPost() {
    Object foo = new ClassA();
    Object bar = new ClassB();

    // BUG: Diagnostic contains: Nested instanceOf conditions of disjoint types
    if (foo instanceof ClassA) {
      if (foo instanceof ClassB) {
        System.out.println("test");
      }
    }

    // BUG: Diagnostic contains: Nested instanceOf conditions of disjoint types
    if (foo instanceof ClassA) {
      System.out.println("test");
      if (foo instanceof ClassB) {
        System.out.println("test");
      }
      System.out.println("test");
    }

    // BUG: Diagnostic contains: Nested instanceOf conditions of disjoint types
    if (foo instanceof ClassA) {
      // BUG: Diagnostic contains: Nested instanceOf conditions of disjoint types
      if (foo instanceof ClassA) {
        if (foo instanceof ClassB) {
          System.out.println("test");
        }
      }
    }

    // BUG: Diagnostic contains: Nested instanceOf conditions of disjoint types
    if (foo instanceof ClassA) {
      // BUG: Diagnostic contains: Nested instanceOf conditions of disjoint types
      if (foo instanceof ClassB) {
        if (foo instanceof ClassC) {
          System.out.println("test");
        }
      }
    }

    // BUG: Diagnostic contains: Nested instanceOf conditions
    if (foo instanceof ClassA) {
      if (bar instanceof ClassB) {
        if (foo instanceof ClassC) {
          System.out.println("test");
        }
      }
    }

    if (foo instanceof ClassA) {
      System.out.println("yay");
      // BUG: Diagnostic contains: Nested instanceOf conditions
    } else if (foo instanceof ClassB) {
      if (foo instanceof ClassC) {
        System.out.println("uh oh");
      }
    }
  }

  static class ClassA {}

  static class ClassB {}

  static class ClassC {}
}
