/*
 * Copyright 2018 The Error Prone Authors.
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
package com.google.errorprone.bugpatterns.time;

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link JodaTimeConverterManager}. */
@RunWith(JUnit4.class)
public final class JodaTimeConverterManagerTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(JodaTimeConverterManager.class, getClass());

  @Test
  public void converterManager() {
    helper
        .addSourceLines(
            "Test.java",
            "import org.joda.time.convert.ConverterManager;",
            "class Test {",
            "  // BUG: Diagnostic contains: JodaTimeConverterManager",
            "  ConverterManager cm = ConverterManager.getInstance();",
            "}")
        .doTest();
  }

  @Test
  public void withinJoda() {
    helper
        .addSourceLines(
            "Test.java",
            "package org.joda.time;",
            "import org.joda.time.convert.ConverterManager;",
            "class Test {",
            "  ConverterManager cm = ConverterManager.getInstance();",
            "}")
        .doTest();
  }
}
