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
  //BUG: Suggestion includes "testName"
  public void tesName() {}

  //BUG: Suggestion includes "testNameStatic"
  public static void tesNameStatic() {}

  //BUG: Suggestion includes "testName"
  public void ttestName() {}

  //BUG: Suggestion includes "testName"
  public void teestName() {}

  //BUG: Suggestion includes "testName"
  public void tstName() {}

  //BUG: Suggestion includes "testName"
  public void tetName() {}

  //BUG: Suggestion includes "testName"
  public void etstName() {}

  //BUG: Suggestion includes "testName"
  public void tsetName() {}

  //BUG: Suggestion includes "testName"
  public void teatName() {}

  //BUG: Suggestion includes "testName"
  public void TestName() {}

  //BUG: Suggestion includes "test_NAME"
  public void TEST_NAME() {}

  // These names are trickier to correct, but we should still indicate the bug
  //BUG: Suggestion includes "test"
  public void tetsName() {}

  //BUG: Suggestion includes "test"
  public void tesstName() {}

  //BUG: Suggestion includes "test"
  public void tesetName() {}

  //BUG: Suggestion includes "test"
  public void tesgName() {}

  // tentative - can cause false positives
  //BUG: Suggestion includes "testName"
  public void textName() {}

  // test with @Test annotation not run by JUnit3
  //BUG: Suggestion includes "testName"
  @Test public void name() {}

  // non-standard formatting - just to check that replacements are correctly located
  //BUG: Suggestion includes "testMoreSpaces"
  public    void    tesMoreSpaces()    {}

  @Test public void
  //BUG: Suggestion includes "testMultiline"
      tesMultiline() {}
}
