/*
 * Copyright 2013 Google Inc. All Rights Reserved.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import junit.framework.TestCase;
import org.junit.Assert;

/** @author adamwos@google.com (Adam Wos) */
public class TryFailThrowablePositiveCases {

  public static void emptyCatch_failNoMessage() {
    try {
      dummyMethod();
      Assert.fail();
      // BUG: Diagnostic contains: catch (Exception t)
    } catch (Throwable t) {
    }
  }

  public static void commentCatch_failNoMessage() {
    try {
      dummyMethod();
      Assert.fail();
      // BUG: Diagnostic contains: catch (Exception t123)
    } catch (Throwable t123) {
      // expected!
      ;
      /* that's an empty comment */
    }
  }

  public static void commentCatch_failWithMessage() {
    try {
      dummyMethod();
      Assert.fail("Faaail!");
      // BUG: Diagnostic contains: catch (Exception t)
    } catch (Throwable t) {
      // expected!
    }
  }

  public static void commentCatch_failNotLast() {
    try {
      dummyMethod();
      fail("Faaail!");
      dummyMethod();
      // BUG: Diagnostic contains: catch (Exception t)
    } catch (Throwable t) {
      // expected!
    }
  }

  public static void commentCatch_assert() {
    try {
      dummyMethod();
      assertEquals(1, 2);
      // BUG: Diagnostic contains: catch (Exception t)
    } catch (Throwable t) {
      // expected!
    }
  }

  public static void commentCatch_assertNotLast() {
    try {
      dummyMethod();
      assertTrue("foobar!", true);
      dummyRecover();
      // BUG: Diagnostic contains: catch (Exception t)
    } catch (Throwable t) {
      // expected!
    }
  }

  public static void customMoreAsserts() {
    try {
      dummyMethod();
      CustomMoreAsserts.assertFoobar();
      dummyMethod();
      // BUG: Diagnostic contains: catch (Exception t)
    } catch (Throwable t) {
      // expected!
    }
  }

  public static void customMoreAsserts_fail() {
    try {
      dummyMethod();
      CustomMoreAsserts.fail("param", 0x42);
      dummyMethod();
      // BUG: Diagnostic contains: catch (Exception t)
    } catch (Throwable t) {
      // expected!
    }
  }

  static final class SomeTest extends TestCase {
    public void testInTestCase() {
      try {
        dummyMethod();
        fail("message");
        // BUG: Diagnostic contains: catch (Exception codeCatch_oldAssertFailWithMessage)
      } catch (Throwable codeCatch_oldAssertFailWithMessage) {
        // comment
        /* another */
      }
    }
  }

  static final class CustomMoreAsserts {
    static void assertFoobar() {}

    static void fail(String param1, int param2) {}
  }

  private static void dummyRecover() {}

  private static void dummyMethod() {}

  public static void catchesAssertionError() {
    try {
      dummyMethod();
      Assert.fail();
      // BUG: Diagnostic contains: remove this line
    } catch (AssertionError e) {
    }
  }

  public static void hasMessage() {
    try {
      dummyMethod();
      Assert.fail("foo");
      // BUG: Diagnostic contains: remove this line
    } catch (AssertionError e) {
    }
  }

  public static void catchesError_lastStatement() {
    try {
      dummyMethod();
      Assert.fail();
      // BUG: Diagnostic contains: remove this line
    } catch (Error e) {
    }
  }

  public static void catchesError_notLastStatement() {
    try {
      dummyMethod();
      Assert.fail();
      // BUG: Diagnostic contains: boolean threw = false;
    } catch (Error e) {
    }

    assertTrue(true);
  }

  public static void catchesError_nested() {
    for (Object o : Arrays.asList()) {
      try {
        dummyMethod();
        Assert.fail();
        // BUG: Diagnostic contains: boolean threw = false;
      } catch (Error e) {
      }
    }
  }

  public static void catchesError_nestedNoBlock() {
    for (Object o : Arrays.asList())
      try {
        dummyMethod();
        Assert.fail();
        // BUG: Diagnostic contains: boolean threw = false;
      } catch (Error e) {
      }
  }
}
