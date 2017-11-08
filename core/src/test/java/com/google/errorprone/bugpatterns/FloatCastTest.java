/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

/** {@link FloatCast}Test */
@RunWith(JUnit4.class)
public class FloatCastTest {
  @Test
  public void positive() {
    CompilationTestHelper.newInstance(FloatCast.class, getClass())
        .addSourceLines(
            "Test.java", //
            "class Test {",
            "  {",
            "    // BUG: Diagnostic contains:"
                + "'int x = ((int) 0.9f) * 42;' or 'int x = (int) (0.9f * 42);'",
            "    int x = (int) 0.9f * 42;",
            "    // BUG: Diagnostic contains:"
                + "'float y = ((int) 0.9f) * 0.9f;' or 'float y = (int) (0.9f * 0.9f);'",
            "    float y = (int) 0.9f * 0.9f;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative() {
    CompilationTestHelper.newInstance(FloatCast.class, getClass())
        .addSourceLines(
            "Test.java", //
            "class Test {",
            "  {",
            "    int x = (int) 0.9f + 42;",
            "    float y = (int) 0.9f - 0.9f;",
            "    x = ((int) 0.9f) * 42;",
            "    y = (int) (0.9f * 0.9f);",
            "    String s = (int) 0.9f + \"\";",
            "    boolean b = (int) 0.9f > 1;",
            "  }",
            "}")
        .doTest();
  }
}
