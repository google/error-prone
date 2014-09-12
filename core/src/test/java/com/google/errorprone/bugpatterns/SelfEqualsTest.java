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

import static org.junit.Assert.fail;

import com.google.errorprone.CompilationTestHelper;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import javax.tools.JavaFileObject;

/**
 * @author eaftan@google.com (Eddie Aftandilian)
 */
@RunWith(JUnit4.class)
public class SelfEqualsTest {

  CompilationTestHelper compilationHelper = CompilationTestHelper.newInstance(
      new SelfEquals(true, true));
  final JavaFileObject positiveCase1;
  final JavaFileObject positiveCase2;
  final JavaFileObject negativeCases;

  public SelfEqualsTest() throws Exception {
    positiveCase1 =
        compilationHelper.fileManager().source(getClass(), "SelfEqualsPositiveCase1.java");
    positiveCase2 =
        compilationHelper.fileManager().source(getClass(), "SelfEqualsPositiveCase2.java");
    negativeCases =
        compilationHelper.fileManager().source(getClass(), "SelfEqualsNegativeCases.java");
  }

  @Test
  public void testPositiveCase1() throws Exception {
    compilationHelper.assertCompileFailsWithMessages(positiveCase1);
  }

  @Test
  public void testPositiveCase2() throws Exception {
    compilationHelper.assertCompileFailsWithMessages(positiveCase2);
  }

  @Test
  public void testNegativeCase() throws Exception {
    compilationHelper.assertCompileSucceeds(negativeCases);
  }

  @Test
  public void testFlags() throws Exception {
    SelfEquals checker;
    CompilationTestHelper compilationHelper;
    // Both checks off.
    try {
      new SelfEquals(false, false);
      fail();
    } catch (IllegalArgumentException e) {
      // Expected to get an exception.
    }

    // Both checks on.
    checker = new SelfEquals(true, true);
    compilationHelper = CompilationTestHelper.newInstance(checker);
    compilationHelper.assertCompileFailsWithMessages(positiveCase1);
    compilationHelper = CompilationTestHelper.newInstance(checker);
    compilationHelper.assertCompileFailsWithMessages(positiveCase2);

    // Guava on, Eauals off.
    checker = new SelfEquals(true, false);
    compilationHelper = CompilationTestHelper.newInstance(checker);
    compilationHelper.assertCompileFailsWithMessages(positiveCase1);
    compilationHelper = CompilationTestHelper.newInstance(checker);
    compilationHelper.assertCompileSucceeds(positiveCase2);

    // Equals on, Guava off.
    checker = new SelfEquals(false, true);
    compilationHelper = CompilationTestHelper.newInstance(checker);
    compilationHelper.assertCompileSucceeds(positiveCase1);
    compilationHelper = CompilationTestHelper.newInstance(checker);
    compilationHelper.assertCompileFailsWithMessages(positiveCase2);
  }

}
