/*
 * Copyright 2023 The Error Prone Authors.
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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class MutableGuiceModuleTest {
  private final CompilationTestHelper testHelper =
      CompilationTestHelper.newInstance(MutableGuiceModule.class, getClass());

  @Test
  public void positive() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.inject.AbstractModule;",
            "class Test extends AbstractModule {",
            "  // BUG: Diagnostic contains:",
            "  String x = new String();",
            "}")
        .doTest();
  }

  @Test
  public void positiveType() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.inject.AbstractModule;",
            "class Test extends AbstractModule {",
            "  // BUG: Diagnostic contains: Object is mutable",
            "  final Object x = new Object();",
            "}")
        .doTest();
  }

  @Test
  public void negativeFinal() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.inject.AbstractModule;",
            "class Test extends AbstractModule {",
            "  final String x = new String();",
            "}")
        .doTest();
  }

  @Test
  public void negativeNotAModule() {
    testHelper
        .addSourceLines(
            "Test.java", //
            "class Test {",
            "  String x = new String();",
            "}")
        .doTest();
  }
}
