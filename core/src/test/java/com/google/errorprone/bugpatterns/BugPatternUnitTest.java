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
import com.google.errorprone.Scanner;

import java.io.File;
import java.net.URL;

/**
 * @author Bill Pugh (bill.pugh@gmail.com)
 *
 * TODO(eaftan): Migrate other test cases to this pattern.
 */
public abstract class BugPatternUnitTest {

  protected CompilationTestHelper compilationHelper;
  protected String testname;

  protected void setScanner(Scanner scanner) {
    Class<?> c = scanner.getClass().getEnclosingClass();
    if (c == null) {
      throw new IllegalArgumentException("Scanner must be an inner class");
    }
    if (!BugChecker.class.isAssignableFrom(c)) {
      throw new IllegalArgumentException("Bad outer class for scanner: " + c);
    }
    testname = c.getSimpleName();
    compilationHelper = new CompilationTestHelper(scanner);
  }

  public void testPositiveCase() throws Exception {
    String name = testname + "PositiveCases.java";
    URL resource = this.getClass().getResource(name);
    if (resource == null)
      throw new RuntimeException("Could not find resource " + name);
    compilationHelper.assertCompileFailsWithMessages(new File(resource
        .toURI()));
  }

  public void testNegativeCase() throws Exception {
    String name = testname + "NegativeCases.java";
    URL resource = this.getClass().getResource(name);
    if (resource == null)
      throw new RuntimeException("Could not find resource " + name);
    compilationHelper.assertCompileSucceeds(new File(resource.toURI()));

  }

}
