/*
 * Copyright 2014 The Error Prone Authors.
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

/** @author glorioso@google.com (Nick Glorioso) */
@RunWith(JUnit4.class)
public class JUnit4TearDownNotRunTest {

  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper = CompilationTestHelper.newInstance(JUnit4TearDownNotRun.class, getClass());
  }

  @Test
  public void testPositiveCases() {
    compilationHelper.addSourceFile("JUnit4TearDownNotRunPositiveCases.java").doTest();
  }

  @Test
  public void testPositiveCase_customAnnotation() {
    compilationHelper.addSourceFile("JUnit4TearDownNotRunPositiveCaseCustomAfter.java").doTest();
  }

  @Test
  public void testPositiveCase_customAnnotationDifferentName() {
    compilationHelper.addSourceFile("JUnit4TearDownNotRunPositiveCaseCustomAfter2.java").doTest();
  }

  @Test
  public void testNegativeCases() {
    compilationHelper.addSourceFile("JUnit4TearDownNotRunNegativeCases.java").doTest();
  }
}
