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

package com.google.errorprone.bugpatterns;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link StronglyTypeByteString}. */
@RunWith(JUnit4.class)
public final class StronglyTypeByteStringTest {
  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(StronglyTypeByteString.class, getClass());

  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(StronglyTypeByteString.class, getClass());

  @Test
  public void findingLocatedOnField() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.protobuf.ByteString;",
            "class Test {",
            "  // BUG: Diagnostic contains: ByteString instances",
            "  private static final byte[] FOO_BYTES = new byte[10];",
            "  public ByteString get() {",
            "    return ByteString.copyFrom(FOO_BYTES);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void byteStringFactory() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import com.google.protobuf.ByteString;",
            "class Test {",
            "  private static final byte[] FOO_BYTES = new byte[10];",
            "  public ByteString get() {",
            "    return ByteString.copyFrom(FOO_BYTES);",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import com.google.protobuf.ByteString;",
            "class Test {",
            "  private static final ByteString FOO_BYTES = ByteString.copyFrom(new byte[10]);",
            "  public ByteString get() {",
            "    return FOO_BYTES;",
            "  }",
            "}")
        .doTest();
  }
}
