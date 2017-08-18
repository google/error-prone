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

/** {@link RandomCast}Test */
@RunWith(JUnit4.class)
public class RandomCastTest {

  private final CompilationTestHelper testHelper =
      CompilationTestHelper.newInstance(RandomCast.class, getClass());

  @Test
  public void positive() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import java.util.Random;",
            "class Test {",
            "  {",
            "    // BUG: Diagnostic contains:",
            "    int x = (int) new Random().nextFloat();",
            "    // BUG: Diagnostic contains:",
            "    x = (int) new Random().nextDouble();",
            "    // BUG: Diagnostic contains:",
            "    long y = (long) new Random().nextFloat();",
            "    // BUG: Diagnostic contains:",
            "    y = (long) new Random().nextDouble();",
            "    // BUG: Diagnostic contains:",
            "    int z = (int) Math.random();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import java.util.Random;",
            "class Test {",
            "  {",
            "    float x = new Random().nextFloat();",
            "    x = (float) new Random().nextDouble();",
            "    double y = new Random().nextFloat();",
            "    y = new Random().nextDouble();",
            "    double z = Math.random();",
            "    int i = (int) new Random().nextInt();",
            "    i = (int) new Random().nextLong();",
            "    long l = (long) new Random().nextInt();",
            "    l = (long) new Random().nextLong();",
            "  }",
            "}")
        .doTest();
  }
}
