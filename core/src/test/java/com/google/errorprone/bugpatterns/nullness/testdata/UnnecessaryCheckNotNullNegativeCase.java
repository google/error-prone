/*
 * Copyright 2011 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.nullness.testdata;

public class UnnecessaryCheckNotNullNegativeCase {
  public void go_checkNotNull() {
    Preconditions.checkNotNull("this is ok");
  }

  public void go_verifyNotNull() {
    Verify.verifyNotNull("this is ok");
  }

  public void go_requireNonNull() {
    Objects.requireNonNull("this is ok");
  }

  private static class Preconditions {
    static void checkNotNull(String string) {
      System.out.println(string);
    }
  }

  private static class Verify {
    static void verifyNotNull(String string) {
      System.out.println(string);
    }
  }

  private static class Objects {
    static void requireNonNull(String string) {
      System.out.println(string);
    }
  }

  public void go() {
    Object testObj = null;
    com.google.common.base.Preconditions.checkNotNull(testObj, "this is ok");
    com.google.common.base.Verify.verifyNotNull(testObj, "this is ok");
    java.util.Objects.requireNonNull(testObj, "this is ok");
  }
}
