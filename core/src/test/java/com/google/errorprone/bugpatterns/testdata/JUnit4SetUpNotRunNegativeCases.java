/*
 * Copyright 2014 Google Inc. All Rights Reserved.
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
import org.junit.Before;
import org.junit.internal.runners.JUnit38ClassRunner;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Not a JUnit 4 test (no @RunWith annotation on the class). */
public class JUnit4SetUpNotRunNegativeCases {
  public void setUp() {}
}

@RunWith(JUnit38ClassRunner.class)
class J4SetUpWrongRunnerType {
  public void setUp() {}
}

@RunWith(JUnit4.class)
class J4SetUpCorrectlyDone {
  @Before
  public void setUp() {}
}

/** May be a JUnit 3 test -- has @RunWith annotation on the class but also extends TestCase. */
@RunWith(JUnit4.class)
class J4SetUpJUnit3Class extends TestCase {
  public void setUp() {}
}

/** setUp() method is private and wouldn't be run by JUnit3 */
@RunWith(JUnit4.class)
class J4PrivateSetUp {
  private void setUp() {}
}

/**
 * setUp() method is package-local. You couldn't have a JUnit3 test class with a package-private
 * setUp() method (narrowing scope from protected to package)
 */
@RunWith(JUnit4.class)
class J4PackageLocalSetUp {
  void setUp() {}
}

@RunWith(JUnit4.class)
class J4SetUpNonVoidReturnType {
  int setUp() {
    return 42;
  }
}

/** setUp() has parameters */
@RunWith(JUnit4.class)
class J4SetUpWithParameters {
  public void setUp(int ignored) {}

  public void setUp(boolean ignored) {}

  public void setUp(String ignored) {}
}

/** setUp() method is static and wouldn't be run by JUnit3 */
@RunWith(JUnit4.class)
class J4StaticSetUp {
  public static void setUp() {}
}

abstract class SetUpAnnotatedBaseClass {
  @Before
  public void setUp() {}
}

/** setUp() method overrides parent method with @Before. It will be run by JUnit4BlockRunner */
@RunWith(JUnit4.class)
class J4SetUpExtendsAnnotatedMethod extends SetUpAnnotatedBaseClass {
  public void setUp() {}
}
