/*
 * Copyright 2012 The Error Prone Authors.
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

/** @author alexeagle@google.com (Alex Eagle) */
@RunWith(JUnit4.class)
public class DeadExceptionTest {

  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper = CompilationTestHelper.newInstance(DeadException.class, getClass());
  }

  @Test
  public void testPositiveCase() {
    compilationHelper.addSourceFile("DeadExceptionPositiveCases.java").doTest();
  }

  @Test
  public void testNegativeCase() {
    compilationHelper.addSourceFile("DeadExceptionNegativeCases.java").doTest();
  }

  /**
   * It's somewhat common to test the side-effects of Exception constructors by creating one, and
   * asserting that an exception is thrown in the constructor.
   */
  @Test
  public void testNegativeCaseWhenExceptionsUnthrownInTests() {
    compilationHelper.addSourceFile("DeadExceptionTestingNegativeCases.java").doTest();
  }
}
