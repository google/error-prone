/*
 * Copyright 2013 The Error Prone Authors.
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

import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** @author eaftan@google.com (Eddie Aftandilian) */
@RunWith(JUnit4.class)
public class JUnit4TestNotRunNegativeCase3 {
  // Doesn't begin with "test", and doesn't contain any assertion-like method invocations.
  public void thisIsATest() {}

  // Isn't public.
  void testTest1() {}

  // Have checked annotation.
  @Test
  public void testTest2() {}

  @Before
  public void testBefore() {}

  @After
  public void testAfter() {}

  @BeforeClass
  public void testBeforeClass() {}

  @AfterClass
  public void testAfterClass() {}

  // Has parameters.
  public void testTest3(int foo) {}

  // Doesn't return void
  public int testSomething() {
    return 42;
  }
}
