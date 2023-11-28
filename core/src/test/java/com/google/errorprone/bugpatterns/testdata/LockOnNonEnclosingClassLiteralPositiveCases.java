/*
 * Copyright 2023 The Error Prone Authors.
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

public class LockOnNonEnclosingClassLiteralPositiveCases {

  static {
    // BUG: Diagnostic contains: Lock on the class other than the enclosing class of the code block
    // can unintentionally prevent the locked class being used properly.
    synchronized (String.class) {
    }
  }

  private void methodContainsSynchronizedBlock() {
    // BUG: Diagnostic contains: Lock on the class other than the enclosing class of the code block
    // can unintentionally prevent the locked class being used properly.
    synchronized (String.class) {
    }
  }

  class SubClass {

    public void methodContainsSynchronizedBlock() {
      // BUG: Diagnostic contains: Lock on the class other than the enclosing class of the code
      // block can unintentionally prevent the locked class being used properly.
      synchronized (LockOnNonEnclosingClassLiteralPositiveCases.class) {
      }
    }
  }
}
