/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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

/** {@link MultipleTopLevelClasses}Test */
@RunWith(JUnit4.class)
public class MultipleTopLevelClassesTest {

  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper =
        CompilationTestHelper.newInstance(MultipleTopLevelClasses.class, getClass());
  }

  @Test
  public void twoClasses() throws Exception {
    compilationHelper
        .addSourceLines(
            "a/A.java",
            "// BUG: Diagnostic contains: one top-level class declaration, instead found: One, Two",
            "package a;",
            "class One {}",
            "class Two {}")
        .doTest();
  }

  @Test
  public void packageInfo() throws Exception {
    compilationHelper
        .addSourceLines("a/package-info.java", "/** Documentation for our package */", "package a;")
        .doTest();
  }

  @Test
  public void defaultPackage() throws Exception {
    compilationHelper.addSourceLines("a/A.java", "class A {}", "class B {}").doTest();
  }
}
