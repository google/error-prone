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

package com.google.errorprone.bugpatterns;

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link PrimitiveAtomicReference}. */
@RunWith(JUnit4.class)
public final class PrimitiveAtomicReferenceTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(PrimitiveAtomicReference.class, getClass());

  @Test
  public void positive() {
    helper
        .addSourceLines(
            "Test.java",
            "import java.util.concurrent.atomic.AtomicReference;",
            "class Test {",
            "  private AtomicReference<Integer> ref = new AtomicReference<>();",
            "  public boolean cas(int i) {",
            "    // BUG: Diagnostic contains:",
            "    return ref.compareAndSet(i, 10);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeNull() {
    helper
        .addSourceLines(
            "Test.java",
            "import java.util.concurrent.atomic.AtomicReference;",
            "class Test {",
            "  private AtomicReference<Integer> ref = new AtomicReference<>();",
            "  public boolean cas() {",
            "    return ref.compareAndSet(null, 10);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeNotBoxedType() {
    helper
        .addSourceLines(
            "Test.java",
            "import java.util.concurrent.atomic.AtomicReference;",
            "class Test {",
            "  private AtomicReference<String> ref = new AtomicReference<>();",
            "  public boolean cas(String s) {",
            "    return ref.compareAndSet(s, \"foo\");",
            "  }",
            "}")
        .doTest();
  }
}
