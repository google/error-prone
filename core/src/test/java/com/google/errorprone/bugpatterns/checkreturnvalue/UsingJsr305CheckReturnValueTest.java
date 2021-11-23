/*
 * Copyright 2021 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.checkreturnvalue;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for the {@link UsingJsr305CheckReturnValue}. */
@RunWith(JUnit4.class)
public class UsingJsr305CheckReturnValueTest {

  @Test
  public void testJsr305Imported() {
    BugCheckerRefactoringTestHelper.newInstance(UsingJsr305CheckReturnValue.class, getClass())
        .addInputLines(
            "Client.java",
            "package com.google.frobber;",
            "import javax.annotation.CheckReturnValue;",
            "public final class Client {",
            "  @CheckReturnValue",
            "  public int getValue() {",
            "    return 42;",
            "  }",
            "}")
        .addOutputLines(
            "Client.java",
            "package com.google.frobber;",
            "import com.google.errorprone.annotations.CheckReturnValue;",
            "public final class Client {",
            "  @CheckReturnValue",
            "  public int getValue() {",
            "    return 42;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testJsr305FullyQualified() {
    CompilationTestHelper.newInstance(UsingJsr305CheckReturnValue.class, getClass())
        .addSourceLines(
            "Client.java",
            "package com.google.frobber;",
            "public final class Client {",
            // NOTE: fully-qualified annotations are not currently re-written
            "  @javax.annotation.CheckReturnValue",
            "  public int getValue() {",
            "    return 42;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testJsr305ImportStar() {
    CompilationTestHelper.newInstance(UsingJsr305CheckReturnValue.class, getClass())
        .addSourceLines(
            "Client.java",
            "package com.google.frobber;",
            "import javax.annotation.*;",
            "public final class Client {",
            // NOTE: wildcard-imported annotations are not currently re-written
            "  @CheckReturnValue",
            "  public int getValue() {",
            "    return 42;",
            "  }",
            "}")
        .doTest();
  }
}
