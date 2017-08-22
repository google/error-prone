/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

/** @author mariasam@google.com (Maria Sam) */
public class IncrementInForLoopAndHeaderPositiveCases {

  public void basicTest() {
    // BUG: Diagnostic contains: increment
    for (int i = 0; i < 10; i++) {
      i++;
    }
  }

  public void decrement() {
    // BUG: Diagnostic contains: increment
    for (int i = 0; i < 10; i++) {
      i--;
    }
  }

  public void preInc() {
    // BUG: Diagnostic contains: increment
    for (int i = 0; i < 10; i++) {
      --i;
    }
  }

  public void multipleStatements() {
    // BUG: Diagnostic contains: increment
    for (int i = 0; i < 10; i++) {
      --i;
      int a = 0;
    }
  }

  public void multipleUpdates() {
    // BUG: Diagnostic contains: increment
    for (int i = 0, a = 1; i < 10; i++, a++) {
      a++;
    }
  }

  public void multipleUpdatesOtherVar() {
    // BUG: Diagnostic contains: increment
    for (int i = 0, a = 1; i < 10; i++, a++) {
      i++;
    }
  }

  public void multipleUpdatesBothVars() {
    // BUG: Diagnostic contains: increment
    for (int i = 0, a = 1; i < 10; i++, a++) {
      a++;
      i++;
    }
  }

  public void nestedFor() {
    for (int i = 0; i < 10; i++) {
      // BUG: Diagnostic contains: increment
      for (int a = 0; a < 10; a++) {
        a--;
      }
    }
  }

  public void nestedForBoth() {
    // BUG: Diagnostic contains: increment
    for (int i = 0; i < 10; i++) {
      i++;
      // BUG: Diagnostic contains: increment
      for (int a = 0; a < 10; a++) {
        a--;
      }
    }
  }

  public void expressionStatement() {
    // BUG: Diagnostic contains: increment
    for (int i = 0; i < 10; i++) i++;
  }
}
