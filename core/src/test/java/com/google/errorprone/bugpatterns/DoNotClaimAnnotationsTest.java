/*
 * Copyright 2020 The Error Prone Authors.
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

/** {@link DoNotClaimAnnotations}Test */
@RunWith(JUnit4.class)
public class DoNotClaimAnnotationsTest {

  private final BugCheckerRefactoringTestHelper testHelper =
      BugCheckerRefactoringTestHelper.newInstance(new DoNotClaimAnnotations(), getClass());

  @Test
  public void testPositive() {
    testHelper
        .addInputLines(
            "Test.java",
            "import java.util.Set;",
            "import javax.annotation.processing.Processor;",
            "import javax.annotation.processing.RoundEnvironment;",
            "import javax.annotation.processing.RoundEnvironment;",
            "import javax.lang.model.element.TypeElement;",
            "abstract class Test implements Processor {",
            "  @Override",
            "  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment"
                + " roundEnv) {",
            "    return true;",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import java.util.Set;",
            "import javax.annotation.processing.Processor;",
            "import javax.annotation.processing.RoundEnvironment;",
            "import javax.annotation.processing.RoundEnvironment;",
            "import javax.lang.model.element.TypeElement;",
            "abstract class Test implements Processor {",
            "  @Override",
            "  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment"
                + " roundEnv) {",
            "    return false;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegative() {
    testHelper
        .addInputLines(
            "Test.java",
            "import java.util.Set;",
            "import javax.annotation.processing.Processor;",
            "import javax.annotation.processing.RoundEnvironment;",
            "import javax.annotation.processing.RoundEnvironment;",
            "import javax.lang.model.element.TypeElement;",
            "abstract class Test implements Processor {",
            "  @Override",
            "  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment"
                + " roundEnv) {",
            "    return false;",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void testNegative_notAProcessor() {
    testHelper
        .addInputLines(
            "Test.java",
            "import java.util.Set;",
            "import javax.annotation.processing.RoundEnvironment;",
            "import javax.annotation.processing.RoundEnvironment;",
            "import javax.lang.model.element.TypeElement;",
            "abstract class Test {",
            "  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment"
                + " roundEnv) {",
            "    return true;",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void unfixable() {
    CompilationTestHelper.newInstance(DoNotClaimAnnotations.class, getClass())
        .addSourceLines(
            "Test.java",
            "import java.util.Set;",
            "import javax.annotation.processing.Processor;",
            "import javax.annotation.processing.RoundEnvironment;",
            "import javax.annotation.processing.RoundEnvironment;",
            "import javax.lang.model.element.TypeElement;",
            "abstract class Test implements Processor {",
            "  abstract boolean helper();",
            "  @Override",
            "  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment"
                + " roundEnv) {",
            "    try {",
            "      // BUG: Diagnostic contains:",
            "      return helper();",
            "    } catch (Throwable t) {}",
            "    return false;",
            "  }",
            "}")
        .doTest();
  }
}
