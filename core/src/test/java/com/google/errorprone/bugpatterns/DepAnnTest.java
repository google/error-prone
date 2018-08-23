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

package com.google.errorprone.bugpatterns;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class DepAnnTest {

  private CompilationTestHelper compilationHelper;

  public static final ImmutableList<String> JAVACOPTS = ImmutableList.of("-Xlint:-dep-ann");

  @Before
  public void setUp() {
    compilationHelper = CompilationTestHelper.newInstance(DepAnn.class, getClass());
  }

  @Test
  public void testPositiveCase() {
    compilationHelper.setArgs(JAVACOPTS).addSourceFile("DepAnnPositiveCases.java").doTest();
  }

  @Test
  public void testNegativeCase1() {
    compilationHelper.setArgs(JAVACOPTS).addSourceFile("DepAnnNegativeCase1.java").doTest();
  }

  @Test
  public void testNegativeCase2() {
    compilationHelper.setArgs(JAVACOPTS).addSourceFile("DepAnnNegativeCase2.java").doTest();
  }

  @Test
  public void testDisableable() {
    compilationHelper
        .setArgs(ImmutableList.of("-Xlint:-dep-ann", "-Xep:DepAnn:OFF"))
        .expectNoDiagnostics()
        .addSourceFile("DepAnnPositiveCases.java")
        .doTest();
  }
}
