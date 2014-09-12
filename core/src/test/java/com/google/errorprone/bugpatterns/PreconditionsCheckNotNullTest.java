/*
 * Copyright 2012 Google Inc. All Rights Reserved.
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
 * @author eaftan@google.com (Eddie Aftandilian)
 */
@RunWith(JUnit4.class)
public class PreconditionsCheckNotNullTest {

  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper = CompilationTestHelper.newInstance(new PreconditionsCheckNotNull());
  }

  @Test
  public void testPositiveCase1() throws Exception {
    compilationHelper.assertCompileFailsWithMessages(compilationHelper.fileManager()
        .sources(getClass(), "PreconditionsCheckNotNullPositiveCase1.java"));
  }

  @Test
  public void testPositiveCase2() throws Exception {
    compilationHelper.assertCompileFailsWithMessages(compilationHelper.fileManager()
        .sources(getClass(), "PreconditionsCheckNotNullPositiveCase2.java"));
  }

  @Test
  public void testPositiveCase3() throws Exception {
    compilationHelper.assertCompileFailsWithMessages(compilationHelper.fileManager()
        .sources(getClass(), "PreconditionsCheckNotNullPositiveCase3.java"));
  }

  @Test
  public void testNegativeCase1() throws Exception {
    compilationHelper.assertCompileSucceeds(compilationHelper.fileManager()
        .sources(getClass(), "PreconditionsCheckNotNullNegativeCase1.java"));
  }

  @Test
  public void testNegativeCase2() throws Exception {
    compilationHelper.assertCompileSucceeds(compilationHelper.fileManager()
        .sources(getClass(), "PreconditionsCheckNotNullNegativeCase2.java"));
  }

}
