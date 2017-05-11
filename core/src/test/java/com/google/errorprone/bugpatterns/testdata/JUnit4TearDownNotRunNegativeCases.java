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
import org.junit.After;
import org.junit.internal.runners.JUnit38ClassRunner;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Not a JUnit 4 class (no @RunWith annotation on the class). */
public class JUnit4TearDownNotRunNegativeCases {
  public void tearDown() {}
}

@RunWith(JUnit38ClassRunner.class)
class J4TearDownDifferentRunner {
  public void tearDown() {}
}

@RunWith(JUnit4.class)
class J4TearDownHasAfter {
  @After
  public void tearDown() {}
}

@RunWith(JUnit4.class)
class J4TearDownExtendsTestCase extends TestCase {
  public void tearDown() {}
}

@RunWith(JUnit4.class)
class J4TearDownPrivateTearDown {
  private void tearDown() {}
}

@RunWith(JUnit4.class)
class J4TearDownPackageLocal {
  void tearDown() {}
}

@RunWith(JUnit4.class)
class J4TearDownNonVoidReturnType {
  int tearDown() {
    return 42;
  }
}

@RunWith(JUnit4.class)
class J4TearDownTearDownHasParameters {
  public void tearDown(int ignored) {}

  public void tearDown(boolean ignored) {}

  public void tearDown(String ignored) {}
}

@RunWith(JUnit4.class)
class J4TearDownStaticTearDown {
  public static void tearDown() {}
}

abstract class TearDownAnnotatedBaseClass {
  @After
  public void tearDown() {}
}

@RunWith(JUnit4.class)
class J4TearDownInheritsFromAnnotatedMethod extends TearDownAnnotatedBaseClass {
  public void tearDown() {}
}

@RunWith(JUnit4.class)
class J4TearDownInheritsFromAnnotatedMethod2 extends TearDownAnnotatedBaseClass {
  @After
  public void tearDown() {}
}
