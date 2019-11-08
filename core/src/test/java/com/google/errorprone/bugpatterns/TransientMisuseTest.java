/*
 * Copyright 2019 The Error Prone Authors.
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

/** Unit tests for {@link TransientMisuse}. */
@RunWith(JUnit4.class)
public class TransientMisuseTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(TransientMisuse.class, getClass());

  @Test
  public void positive() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class Test {",
            "  // BUG: Diagnostic contains: static String foo1",
            "  static transient String foo1;",
            "  // BUG: Diagnostic contains: static String foo2",
            "  transient static String foo2;",
            "  // BUG: Diagnostic contains: static final public String foo3",
            "  static final public transient String foo3 = \"\";",
            "  // BUG: Diagnostic contains: protected final static String foo4",
            "  protected final static transient String foo4 = \"\";",
            "}")
        .doTest();
  }

  @Test
  public void negative() {
    compilationHelper
        .addSourceLines(
            "Test.java", //
            "class Test {",
            "  static String foo;",
            "}")
        .doTest();
  }
}
