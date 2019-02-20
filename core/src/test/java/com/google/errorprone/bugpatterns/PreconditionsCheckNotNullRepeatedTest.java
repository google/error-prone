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
 * Unit tests for {@link PreconditionsCheckNotNullRepeated} check.
 *
 * @author bhagwani@google.com (Sumit Bhagwani)
 */
@RunWith(JUnit4.class)
public class PreconditionsCheckNotNullRepeatedTest {

  private final BugCheckerRefactoringTestHelper testHelper =
      BugCheckerRefactoringTestHelper.newInstance(
          new PreconditionsCheckNotNullRepeated(), getClass());
  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(PreconditionsCheckNotNullRepeated.class, getClass());

  @Test
  public void testPositiveMatchesWithReplacement() {
    testHelper
        .addInputLines(
            "in/Test.java",
            "import static com.google.common.base.Preconditions.checkNotNull;",
            "import com.google.common.base.Preconditions;",
            "public class Test {",
            "  public void error() {",
            "    Object someObject = new Object();",
            "    Preconditions.checkNotNull(someObject, someObject);",
            "    checkNotNull(someObject, someObject);",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import static com.google.common.base.Preconditions.checkNotNull;",
            "import com.google.common.base.Preconditions;",
            "public class Test {",
            "  public void error() {",
            "    Object someObject = new Object();",
            "    Preconditions.checkNotNull(someObject, \"someObject must not be null\");",
            "    checkNotNull(someObject, \"someObject must not be null\");",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void flagArgInVarargs() {
    compilationHelper
        .addSourceLines(
            "out/Test.java",
            "import static com.google.common.base.Preconditions.checkNotNull;",
            "import com.google.common.base.Preconditions;",
            "public class Test {",
            "  public void notError() {",
            "    Object obj = new Object();",
            "    Preconditions.checkNotNull(",
            "        obj, \"%s must not be null\",",
            "        // BUG: Diagnostic contains: Including `obj` in the failure message",
            "        obj);",
            "    String s = \"test string\";",
            "    // BUG: Diagnostic contains: PreconditionsCheckNotNullRepeated",
            "    Preconditions.checkNotNull(s, s, s);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testNegativeCases() {
    compilationHelper
        .addSourceLines(
            "out/Test.java",
            "import static com.google.common.base.Preconditions.checkNotNull;",
            "import com.google.common.base.Preconditions;",
            "public class Test {",
            "  public void notError() {",
            "    Object obj = new Object();",
            "    Preconditions.checkNotNull(obj);",
            "    Preconditions.checkNotNull(obj, \"obj\");",
            "    Preconditions.checkNotNull(obj, \"check with message\");",
            "    Preconditions.checkNotNull(obj, \"check with msg and an arg %s\", new Object());",
            "  }",
            "}")
        .doTest();
  }
}
