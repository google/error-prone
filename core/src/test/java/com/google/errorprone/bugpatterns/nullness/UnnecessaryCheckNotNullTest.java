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

package com.google.errorprone.bugpatterns.nullness;

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Unit tests for {@link UnnecessaryCheckNotNull} check.
 *
 * @author bhagwani@google.com (Sumit Bhagwani)
 */
@RunWith(JUnit4.class)
public class UnnecessaryCheckNotNullTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(UnnecessaryCheckNotNull.class, getClass());

  @Test
  public void positive_newClass() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.base.Preconditions;",
            "import com.google.common.base.Verify;",
            "import java.util.Objects;",
            "class Test {",
            " void positive_checkNotNull() {",
            "// BUG: Diagnostic contains: UnnecessaryCheckNotNull",
            "Preconditions.checkNotNull(new String(\"\"));",
            "// BUG: Diagnostic contains: UnnecessaryCheckNotNull",
            "Preconditions.checkNotNull(new String(\"\"), new Object());",
            "// BUG: Diagnostic contains: UnnecessaryCheckNotNull",
            "Preconditions.checkNotNull(new String(\"\"), \"Message %s\", \"template\");",
            "// BUG: Diagnostic contains: UnnecessaryCheckNotNull",
            "String pa = Preconditions.checkNotNull(new String(\"\"));",
            "// BUG: Diagnostic contains: UnnecessaryCheckNotNull",
            "String pb = Preconditions.checkNotNull(new String(\"\"), new Object());",
            "// BUG: Diagnostic contains: UnnecessaryCheckNotNull",
            "String pc = Preconditions.checkNotNull(new String(\"\"), \"Message %s\","
                + " \"template\");",
            "}",
            " void positive_verifyNotNull() {",
            "// BUG: Diagnostic contains: UnnecessaryCheckNotNull",
            "Verify.verifyNotNull(new String(\"\"));",
            "// BUG: Diagnostic contains: UnnecessaryCheckNotNull",
            "Verify.verifyNotNull(new String(\"\"), \"Message\");",
            "// BUG: Diagnostic contains: UnnecessaryCheckNotNull",
            "Verify.verifyNotNull(new String(\"\"), \"Message %s\", \"template\");",
            "// BUG: Diagnostic contains: UnnecessaryCheckNotNull",
            "String va = Verify.verifyNotNull(new String(\"\"));",
            "// BUG: Diagnostic contains: UnnecessaryCheckNotNull",
            "String vb = Verify.verifyNotNull(new String(\"\"), \"Message\");",
            "// BUG: Diagnostic contains: UnnecessaryCheckNotNull",
            "String vc = Verify.verifyNotNull(new String(\"\"), \"Message %s\", \"template\");",
            "}",
            " void positive_requireNonNull() {",
            "// BUG: Diagnostic contains: UnnecessaryCheckNotNull",
            "Objects.requireNonNull(new String(\"\"));",
            "// BUG: Diagnostic contains: UnnecessaryCheckNotNull",
            "Objects.requireNonNull(new String(\"\"), \"Message\");",
            "// BUG: Diagnostic contains: UnnecessaryCheckNotNull",
            "String va = Objects.requireNonNull(new String(\"\"));",
            "// BUG: Diagnostic contains: UnnecessaryCheckNotNull",
            "String vb = Objects.requireNonNull(new String(\"\"), \"Message\");",
            "}",
            "}")
        .doTest();
  }

  @Test
  public void positive_newArray() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.base.Preconditions;",
            "import com.google.common.base.Verify;",
            "import java.util.Objects;",
            "class Test {",
            " void positive_checkNotNull() {",
            "// BUG: Diagnostic contains: UnnecessaryCheckNotNull",
            "Preconditions.checkNotNull(new int[3]);",
            "// BUG: Diagnostic contains: UnnecessaryCheckNotNull",
            "Preconditions.checkNotNull(new int[]{1, 2, 3});",
            "// BUG: Diagnostic contains: UnnecessaryCheckNotNull",
            "Preconditions.checkNotNull(new int[5][2]);",
            "}",
            " void positive_verifyNotNull() {",
            "// BUG: Diagnostic contains: UnnecessaryCheckNotNull",
            "Verify.verifyNotNull(new int[3]);",
            "// BUG: Diagnostic contains: UnnecessaryCheckNotNull",
            "Verify.verifyNotNull(new int[]{1, 2, 3});",
            "// BUG: Diagnostic contains: UnnecessaryCheckNotNull",
            "Verify.verifyNotNull(new int[5][2]);",
            "}",
            " void positive_requireNonNull() {",
            "// BUG: Diagnostic contains: UnnecessaryCheckNotNull",
            "Objects.requireNonNull(new int[3]);",
            "// BUG: Diagnostic contains: UnnecessaryCheckNotNull",
            "Objects.requireNonNull(new int[]{1, 2, 3});",
            "// BUG: Diagnostic contains: UnnecessaryCheckNotNull",
            "Objects.requireNonNull(new int[5][2]);",
            "}",
            "}")
        .doTest();
  }

  @Test
  public void negative() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.base.Preconditions;",
            "import com.google.common.base.Verify;",
            "import java.util.Objects;",
            "class Test {",
            "void negative() {",
            "Preconditions.checkNotNull(new String(\"\").substring(0, 0));",
            "Verify.verifyNotNull(new String(\"\").substring(0, 0));",
            "Objects.requireNonNull(new String(\"\").substring(0, 0));",
            "}",
            "}")
        .doTest();
  }
}
