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

package com.google.errorprone.bugpatterns.javadoc;

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link EscapedEntity} bug pattern. */
@RunWith(JUnit4.class)
public final class EscapedEntityTest {
  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(EscapedEntity.class, getClass());

  @Test
  public void positive_decimalEscape() {
    helper
        .addSourceLines(
            "Test.java", //
            "// BUG: Diagnostic contains:",
            "/** {@code &#064;Override} */",
            "interface Test {}")
        .doTest();
  }

  @Test
  public void positive_hexEscape() {
    helper
        .addSourceLines(
            "Test.java", //
            "// BUG: Diagnostic contains:",
            "/** {@code &#x52;Override} */",
            "interface Test {}")
        .doTest();
  }

  @Test
  public void positive_characterReferemce() {
    helper
        .addSourceLines(
            "Test.java", //
            "// BUG: Diagnostic contains:",
            "/** {@code A &amp; B} */",
            "interface Test {}")
        .doTest();
  }

  @Test
  public void negative() {
    helper
        .addSourceLines(
            "Test.java", //
            "/** <pre> &#064;Override </pre> */",
            "interface Test {}")
        .doTest();
  }
}
