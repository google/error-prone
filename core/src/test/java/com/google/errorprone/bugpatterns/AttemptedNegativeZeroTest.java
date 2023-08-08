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

/** {@link AttemptedNegativeZero}Test. */
@RunWith(JUnit4.class)
public final class AttemptedNegativeZeroTest {
  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(AttemptedNegativeZero.class, getClass());

  @Test
  public void positive() {
    compilationHelper
        .addSourceLines(
            "Foo.java",
            "class Foo {",
            "  // BUG: Diagnostic contains: ",
            "  double x = -0;",
            "  // BUG: Diagnostic contains: ",
            "  double y = -0L;", // I didn't see the `long` case in the wild.
            "}")
        .doTest();
  }

  @Test
  public void negative() {
    compilationHelper
        .addSourceLines(
            "Foo.java",
            "class Foo {",
            "  int w = -0;", // weird but not likely to be hiding any bugs
            "  double x = -0.0;",
            "  double y = 0;",
            "  double z = +0;",
            "}")
        .doTest();
  }
}
