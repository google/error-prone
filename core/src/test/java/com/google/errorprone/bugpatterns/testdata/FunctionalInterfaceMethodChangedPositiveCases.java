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

package com.google.errorprone.bugpatterns.testdata;

public class FunctionalInterfaceMethodChangedPositiveCases {
  @FunctionalInterface
  interface SuperFI {
    void superSam();
  }

  @FunctionalInterface
  interface OtherSuperFI {
    void otherSuperSam();
  }

  @FunctionalInterface
  interface SubFI extends SuperFI {
    void subSam();

    @Override
    // BUG: Diagnostic contains:
    default void superSam() {
      subSam();
      System.out.println("do something else");
    }
  }

  @FunctionalInterface
  interface MultipleInheritanceSubFIOneBad extends SuperFI, OtherSuperFI {
    void subSam();

    @Override
    default void superSam() {
      subSam();
    }

    @Override
    // BUG: Diagnostic contains:
    default void otherSuperSam() {
      subSam();
      System.out.println("do something else");
    }
  }

  @FunctionalInterface
  interface MultipleInheritanceSubFIBothBad extends SuperFI, OtherSuperFI {
    void subSam();

    @Override
    // BUG: Diagnostic contains:
    default void superSam() {
      superSam();
      System.out.println("do something else");
    }

    @Override
    // BUG: Diagnostic contains:
    default void otherSuperSam() {
      subSam();
      System.out.println("do something else");
    }
  }

  @FunctionalInterface
  interface ValueReturningSuperFI {
    String superSam();
  }

  @FunctionalInterface
  interface ValueReturningSubFI extends ValueReturningSuperFI {
    String subSam();

    @Override
    // BUG: Diagnostic contains:
    default String superSam() {
      System.out.println("do something else");
      return subSam();
    }
  }
}
