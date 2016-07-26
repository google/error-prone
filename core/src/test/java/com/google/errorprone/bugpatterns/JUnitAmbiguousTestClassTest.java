/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

/** {@link JUnitAmbiguousTestClassTest}Test */
@RunWith(JUnit4.class)
public class JUnitAmbiguousTestClassTest {

  private CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(JUnitAmbiguousTestClass.class, getClass());

  @Test
  public void positive() {
    compilationHelper
        .addSourceLines(
            "Positive.java",
            "import org.junit.Test;",
            "import junit.framework.TestCase;",
            "// BUG: Diagnostic contains:",
            "public class Positive extends TestCase {",
            "  @Test",
            "  public void testCase() {}",
            "}")
        .doTest();
  }

  @Test
  public void negativeNoExtends() {
    compilationHelper
        .addSourceLines(
            "Positive.java",
            "import org.junit.Test;",
            "public class Positive {",
            "  @Test",
            "  public void testCase() {}",
            "}")
        .doTest();
  }

  @Test
  public void negativeNoAnnotations() {
    compilationHelper
        .addSourceLines(
            "Positive.java",
            "import junit.framework.TestCase;",
            "public class Positive extends TestCase {",
            "  public void testCase() {}",
            "}")
        .doTest();
  }
}
