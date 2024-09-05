/*
 * Copyright 2017 The Error Prone Authors.
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

import static com.google.common.truth.TruthJUnit.assume;

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * @author mariasam@google.com (Maria Sam)
 * @author sulku@google.com (Marsela Sulku)
 */
@RunWith(JUnit4.class)
public class NestedInstanceOfConditionsTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(NestedInstanceOfConditions.class, getClass());

  @Test
  public void positiveCase() {
    compilationHelper.addSourceFile("NestedInstanceOfConditionsPositiveCases.java").doTest();
  }

  @Test
  public void negativeCase() {
    compilationHelper.addSourceFile("NestedInstanceOfConditionsNegativeCases.java").doTest();
  }

  @Test
  public void patternMatchingInstanceof() {
    assume().that(Runtime.version().feature()).isAtLeast(21);
    compilationHelper
        .addSourceLines(
            "Test.java",
            "public class Test {",
            "  record Struct(Object a) {}",
            "  public void test(Object x, Object y) {",
            "    if (x instanceof Struct(Integer a1)) {",
            "      if (y instanceof Struct(Integer a2)) {}",
            "    }",
            "  }",
            "}")
        .doTest();
  }
}
