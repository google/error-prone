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

import com.google.errorprone.CompilationTestHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * @author rburny@google.com (Radoslaw Burny)
 */
@RunWith(JUnit4.class)
public class JUnit3TestNotRunTest {

  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper = CompilationTestHelper.newInstance(JUnit3TestNotRun.class, getClass());
  }

  @Test
  public void testPositiveCases() throws Exception {
    compilationHelper.addSourceFile("JUnit3TestNotRunPositiveCases.java").doTest();
  }

  @Test
  public void testNegativeCase1() throws Exception {
    compilationHelper.addSourceFile("JUnit3TestNotRunNegativeCase1.java").doTest();
  }

  @Test
  public void testNegativeCase2() throws Exception {
    compilationHelper.addSourceFile("JUnit3TestNotRunNegativeCase2.java").doTest();
  }

  @Test
  public void testNegativeCase3() throws Exception {
    compilationHelper.addSourceFile("JUnit3TestNotRunNegativeCase3.java").doTest();
  }

  @Test
  public void testNegativeCase4() throws Exception {
    compilationHelper.addSourceFile("JUnit3TestNotRunNegativeCase4.java").doTest();
  }

  @Test
  public void testNegativeCase5() throws Exception {
    compilationHelper
        .addSourceFile("JUnit3TestNotRunNegativeCase3.java") // needed as a dependency
        .addSourceFile("JUnit3TestNotRunNegativeCase5.java")
        .doTest();
  }
}
