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
import org.junit.Assert;
import org.mockito.Mockito;

/** Test cases for missing fail */
public class MissingFailPositiveCases extends TestCase {

  private boolean foo = true;

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

  public void catchVerify() {
    try {
      // BUG: Diagnostic contains: fail()
      dummyMethod();
    } catch (Exception e) {
      verifyDummy();
    }
  }

  public void expectedException_throwOutsideTryTree() throws Exception {
    try {
      // BUG: Diagnostic contains: fail()
      dummyMethod();
    } catch (Exception expected) {
    }
    throw new Exception();
  }

  public void expectedException_assertLastCall() throws Exception {
    try {
      dummyMethod();
      // BUG: Diagnostic contains: fail()
      assertDummy();
    } catch (Exception expected) {
    }
    throw new Exception();
  }

  public void expectedException_fieldAssignmentInCatch() {
    try {
      // BUG: Diagnostic contains: fail()
      dummyMethod();
    } catch (Exception expected) {
      foo = true;
    }
  }

  public void catchAssert_noopAssertLastCall() {
    try {
      dummyMethod();
      // BUG: Diagnostic contains: fail()
      Assert.assertTrue(true);
    } catch (Exception e) {
      assertDummy();
    }
  }

  public void assertInCatch_verifyNotLastStatement() {
    try {
      Mockito.verify(new Dummy()).dummy();
      // BUG: Diagnostic contains: fail()
      dummyMethod();
    } catch (Exception e) {
      assertDummy();
    }
  }

  public void assertInCatch_verifyInCatch() {
    try {
      // BUG: Diagnostic contains: fail()
      dummyMethod();
    } catch (Exception e) {
      assertDummy();
      Mockito.verify(new Dummy()).dummy();
    }
  }

  public void expectedException_logInTry() {
    try {
      new Logger().log();
      // BUG: Diagnostic contains: fail()
      dummyMethod();
    } catch (Exception expected) {
      foo = true;
    }
  }

  /** Sameple inner class. */
  public static class Inner {
    public void expectedException_emptyCatch() {
      try {
        // BUG: Diagnostic contains: fail()
        dummyMethod();
      } catch (Exception expected) {
      }
    }
  }

  private static class Dummy {

    String dummy() {
      return "";
    }
  }

  private static class Logger {

    void log() {};

    void info() {};
  }

  private static void dummyMethod() {}

  private static void assertDummy() {}

  private static void verifyDummy() {}
}
