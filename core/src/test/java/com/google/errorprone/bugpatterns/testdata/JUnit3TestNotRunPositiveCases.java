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

package com.google.errorprone.bugpatterns;

import junit.framework.TestCase;
import org.junit.Test;

/**
 * @author rburny@google.com (Radoslaw Burny)
 */
public class JUnit3TestNotRunPositiveCases extends TestCase {
  // misspelled names
  // BUG: Diagnostic contains: testName
  public void tesName() {}

  // BUG: Diagnostic contains: testNameStatic
  public static void tesNameStatic() {}

  // BUG: Diagnostic contains: testName
  public void ttestName() {}

  // BUG: Diagnostic contains: testName
  public void teestName() {}

  // BUG: Diagnostic contains: testName
  public void tstName() {}

  // BUG: Diagnostic contains: testName
  public void tetName() {}

  // BUG: Diagnostic contains: testName
  public void etstName() {}

  // BUG: Diagnostic contains: testName
  public void tsetName() {}

  // BUG: Diagnostic contains: testName
  public void teatName() {}

  // BUG: Diagnostic contains: testName
  public void TestName() {}

  // BUG: Diagnostic contains: test_NAME
  public void TEST_NAME() {}

  // These names are trickier to correct, but we should still indicate the bug
  // BUG: Diagnostic contains: test
  public void tetsName() {}

  // BUG: Diagnostic contains: test
  public void tesstName() {}

  // BUG: Diagnostic contains: test
  public void tesetName() {}

  // BUG: Diagnostic contains: test
  public void tesgName() {}

  // tentative - can cause false positives
  // BUG: Diagnostic contains: testName
  public void textName() {}

  // test with @Test annotation not run by JUnit3
  // BUG: Diagnostic contains: testName
  @Test public void name() {}

  // a few checks to verify the substitution is well-formed
  // BUG: Diagnostic contains: void testBasic() {
  public void tesBasic() {}

  // BUG: Diagnostic contains: void testMoreSpaces() {
  public    void    tesMoreSpaces(  )    {}

  @Test public void
  // BUG: Diagnostic contains: void testMultiline() {
      tesMultiline() {}
}
