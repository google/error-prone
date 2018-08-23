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

import com.google.common.collect.ImmutableList;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Test cases for {@link LongLiteralLowerCaseSuffix}.
 *
 * @author Simon Nickerson (sjnickerson@google.com)
 */
@RunWith(JUnit4.class)
public class LongLiteralLowerCaseSuffixTest {

  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper =
        CompilationTestHelper.newInstance(LongLiteralLowerCaseSuffix.class, getClass());
  }

  @Test
  public void testPositiveCase() {
    compilationHelper.addSourceFile("LongLiteralLowerCaseSuffixPositiveCase1.java").doTest();
  }

  /** Test for Java 7 integer literals that include underscores. */
  @Test
  public void testJava7PositiveCase() {
    compilationHelper.addSourceFile("LongLiteralLowerCaseSuffixPositiveCase2.java").doTest();
  }

  @Test
  public void testNegativeCase() {
    compilationHelper.addSourceFile("LongLiteralLowerCaseSuffixNegativeCases.java").doTest();
  }

  @Test
  public void testDisableable() {
    compilationHelper
        .setArgs(ImmutableList.of("-Xep:LongLiteralLowerCaseSuffix:OFF"))
        .expectNoDiagnostics()
        .addSourceFile("LongLiteralLowerCaseSuffixPositiveCase1.java")
        .doTest();
  }
}
