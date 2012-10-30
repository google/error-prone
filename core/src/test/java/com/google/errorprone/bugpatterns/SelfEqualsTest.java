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

import java.io.File;

import org.junit.Test;

import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.Scanner;

/**
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public class SelfEqualsTest {

  File positiveCase1;
  File positiveCase2;
  File negativeCases;

  public SelfEqualsTest() throws Exception {
    positiveCase1 = new File(this.getClass().getResource("SelfEqualsPositiveCase1.java").toURI());
    positiveCase2 = new File(this.getClass().getResource("SelfEqualsPositiveCase2.java").toURI());
    negativeCases = new File(this.getClass().getResource("SelfEqualsNegativeCases.java").toURI());
  }

  @Test
  public void testPositiveCase1() throws Exception {
    CompilationTestHelper compilationHelper = new CompilationTestHelper(new SelfEquals.Scanner());
    compilationHelper.assertCompileFailsWithMessages(positiveCase1);
  }

  @Test
  public void testPositiveCase2() throws Exception {
    CompilationTestHelper compilationHelper = new CompilationTestHelper(new SelfEquals.Scanner());
    compilationHelper.assertCompileFailsWithMessages(positiveCase2);
  }

  @Test
  public void testNegativeCase() throws Exception {
    CompilationTestHelper compilationHelper = new CompilationTestHelper(new SelfEquals.Scanner());
    compilationHelper.assertCompileSucceeds(negativeCases);
  }

  @Test
  public void testFlags() throws Exception {
    // Both checks off.
    Scanner scanner = new SelfEquals.Scanner(false, false);
    CompilationTestHelper compilationHelper = new CompilationTestHelper(scanner);
    compilationHelper.assertCompileSucceeds(positiveCase1);
    compilationHelper = new CompilationTestHelper(scanner);
    compilationHelper.assertCompileSucceeds(positiveCase2);

    // Both checks on.
    scanner = new SelfEquals.Scanner(true, true);
    compilationHelper = new CompilationTestHelper(scanner);
    compilationHelper.assertCompileFailsWithMessages(positiveCase1);
    compilationHelper = new CompilationTestHelper(scanner);
    compilationHelper.assertCompileFailsWithMessages(positiveCase2);

    // Guava on, Eauals off.
    scanner = new SelfEquals.Scanner(true, false);
    compilationHelper = new CompilationTestHelper(scanner);
    compilationHelper.assertCompileFailsWithMessages(positiveCase1);
    compilationHelper = new CompilationTestHelper(scanner);
    compilationHelper.assertCompileSucceeds(positiveCase2);

    // Equals on, Guava off.
    scanner = new SelfEquals.Scanner(false, true);
    compilationHelper = new CompilationTestHelper(scanner);
    compilationHelper.assertCompileSucceeds(positiveCase1);
    compilationHelper = new CompilationTestHelper(scanner);
    compilationHelper.assertCompileFailsWithMessages(positiveCase2);
  }

}
