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

package com.google.errorprone.bugpatterns.collectionincompatibletype;

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link TruthIncompatibleType}Test */
@RunWith(JUnit4.class)
public class TruthIncompatibleTypeTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(TruthIncompatibleType.class, getClass());

  @Test
  public void positive() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "public class Test {",
            "  static final class A {}",
            "  static final class B {}",
            "  public void f(A a, B b) {",
            "    // BUG: Diagnostic contains:",
            "    assertThat(a).isEqualTo(b);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "public class Test {",
            "  static final class A {}",
            "  static final class B {}",
            "  public void f(A a, B b) {",
            "    assertThat(a).isEqualTo(a);",
            "    assertThat(b).isEqualTo(b);",
            "    assertThat(\"a\").isEqualTo(\"b\");",
            "  }",
            "}")
        .doTest();
  }
}
