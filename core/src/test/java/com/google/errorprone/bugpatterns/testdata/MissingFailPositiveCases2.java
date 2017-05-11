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

// unused import to make sure we don't introduce an import conflict.

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Test cases for missing fail */
@RunWith(JUnit4.class)
public class MissingFailPositiveCases2 {

  @Test
  public void expectedException() {
    try {
      // BUG: Diagnostic contains: fail()
      dummyMethod();
    } catch (Exception expected) {
    }
  }

  public void expectedException_helperMethod() {
    try {
      // BUG: Diagnostic contains: fail()
      dummyMethod();
    } catch (Exception expected) {
    }
  }

  private static void dummyMethod() {}
}
