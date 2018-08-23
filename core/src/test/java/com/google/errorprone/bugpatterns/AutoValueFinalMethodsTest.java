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

package com.google.errorprone.bugpatterns;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Unit tests for {@link AutoValueFinalMethods} bug pattern.
 *
 * @author bhagwani@google.com (Sumit Bhagwani)
 */
@RunWith(JUnit4.class)
public class AutoValueFinalMethodsTest {
  private final BugCheckerRefactoringTestHelper testHelper =
      BugCheckerRefactoringTestHelper.newInstance(new AutoValueFinalMethods(), getClass());
  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(AutoValueFinalMethods.class, getClass());

  @Test
  public void testFinalAdditionToEqHcTs() {
    testHelper
        .addInputLines(
            "in/Test.java",
            "import com.google.auto.value.AutoValue;",
            "@AutoValue",
            "abstract class Test {",
            "  abstract String valueOne();",
            "  abstract String valueTwo();",
            "  static Test create(String valueOne, String valueTwo) {",
            "    return null;",
            "  }",
            "  @Override",
            "  public int hashCode() {",
            "    return 1;",
            "  }",
            "  @Override",
            "  public String toString() {",
            "    return \"Hakuna Matata\";",
            "  }",
            "  @Override",
            "  public boolean equals(Object obj) {",
            "    return true;",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import com.google.auto.value.AutoValue;",
            "@AutoValue",
            "abstract class Test {",
            "  abstract String valueOne();",
            "  abstract String valueTwo();",
            "  static Test create(String valueOne, String valueTwo) {",
            "    return null;",
            "  }",
            "  @Override",
            "  public final int hashCode() {",
            "    return 1;",
            "  }",
            "  @Override",
            "  public final String toString() {",
            "    return \"Hakuna Matata\";",
            "  }",
            "  @Override",
            "  public final boolean equals(Object obj) {",
            "    return true;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases() {
    compilationHelper
        .addSourceLines(
            "out/Test.java",
            "import com.google.auto.value.AutoValue;",
            "import com.google.auto.value.extension.memoized.Memoized;",
            "@AutoValue",
            "abstract class Test {",
            "  abstract String valueOne();",
            "  abstract String valueTwo();",
            "  static Test create(String valueOne, String valueTwo) {",
            "    return null;",
            "  }",
            "  @Override",
            "  public abstract int hashCode(); ",
            "  @Override",
            "  @Memoized",
            "  public String toString() {",
            "    return \"Hakuna Matata\";",
            "  }",
            "  @Override",
            "  public final boolean equals(Object obj) {",
            "    return true;",
            "  }",
            "  private int privateNonEqTsHcMethod() {",
            "    return 2;",
            "  }",
            "  public final String publicFinalNonEqTsHcMethod() {",
            "    return \"Hakuna Matata\";",
            "  }",
            "  public boolean publicNonEqTsHcMethod(Object obj) {",
            "    return true;",
            "  }",
            "}")
        .doTest();
  }
}
