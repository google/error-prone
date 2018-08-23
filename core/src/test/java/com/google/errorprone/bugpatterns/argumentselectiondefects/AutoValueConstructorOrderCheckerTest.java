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

package com.google.errorprone.bugpatterns.argumentselectiondefects;

import com.google.errorprone.CompilationTestHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for AutoValueConstructorOrderChecker */
@RunWith(JUnit4.class)
public class AutoValueConstructorOrderCheckerTest {

  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper =
        CompilationTestHelper.newInstance(AutoValueConstructorOrderChecker.class, getClass());
  }

  @Test
  public void autoValueChecker_detectsSwap_withExactNames() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.auto.value.AutoValue;",
            "@AutoValue",
            "abstract class Test {",
            "  abstract String valueOne();",
            "  abstract String valueTwo();",
            "  static Test create(String valueOne, String valueTwo) {",
            "    // BUG: Diagnostic contains: new AutoValue_Test(valueOne, valueTwo)",
            "    return new AutoValue_Test(valueTwo, valueOne);",
            "  }",
            "}",
            "class AutoValue_Test extends Test {",
            "  String valueOne() { return null; }",
            "  String valueTwo() { return null; }",
            "  AutoValue_Test(String valueOne, String valueTwo) {}",
            "}")
        .doTest();
  }

  @Test
  public void autoValueChecker_ignoresSwap_withInexactNames() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.auto.value.AutoValue;",
            "@AutoValue",
            "abstract class Test {",
            "  abstract String valueOne();",
            "  abstract String valueTwo();",
            "  static Test create(String valueOneArg, String valueTwoArg) {",
            "    return new AutoValue_Test(valueTwoArg, valueOneArg);",
            "  }",
            "}",
            "class AutoValue_Test extends Test {",
            "  String valueOne() { return null; }",
            "  String valueTwo() { return null; }",
            "  AutoValue_Test(String valueOne, String valueTwo) {}",
            "}")
        .doTest();
  }

  @Test
  public void autoValueChecker_makesNoSuggestion_withCorrectOrder() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.auto.value.AutoValue;",
            "@AutoValue",
            "abstract class Test {",
            "  abstract String valueOne();",
            "  abstract String valueTwo();",
            "  static Test create(String valueOne, String valueTwo) {",
            "    return new AutoValue_Test(valueOne, valueTwo);",
            "  }",
            "}",
            "class AutoValue_Test extends Test {",
            "  String valueOne() { return null; }",
            "  String valueTwo() { return null; }",
            "  AutoValue_Test(String valueOne, String valueTwo) {}",
            "}")
        .doTest();
  }
}
