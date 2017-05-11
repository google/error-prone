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

import junit.framework.TestCase;

/** Examples of an inner test case. */
public class MissingFailPositiveCases3 {

  /** Sample inner class. */
  public static class Inner extends TestCase {

    public void expectedException_emptyCatch() {
      try {
        // BUG: Diagnostic contains: fail()
        dummyMethod();
      } catch (Exception expected) {
      }
    }

    public void catchAssert() {
      try {
        // BUG: Diagnostic contains: fail()
        dummyMethod();
      } catch (Exception e) {
        assertDummy();
      }
    }
  }

  private static void dummyMethod() {}

  private static void assertDummy() {}
}
