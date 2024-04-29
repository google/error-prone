/*
 * Copyright 2023 The Error Prone Authors.
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

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class NullableWildcardTest {

  private final BugCheckerRefactoringTestHelper testHelper =
      BugCheckerRefactoringTestHelper.newInstance(NullableWildcard.class, getClass());

  @Test
  public void positive() {
    testHelper
        .addInputLines(
            "T.java",
            "import java.util.List;",
            "import org.checkerframework.checker.nullness.qual.NonNull;",
            "import org.checkerframework.checker.nullness.qual.Nullable;",
            "class T {",
            "  List<@Nullable ?> xs;",
            "  List<@NonNull ?> ys;",
            "}")
        .addOutputLines(
            "T.java",
            "import java.util.List;",
            "import org.checkerframework.checker.nullness.qual.NonNull;",
            "import org.checkerframework.checker.nullness.qual.Nullable;",
            "class T {",
            "  List<? extends @Nullable Object> xs;",
            "  List<? extends @NonNull Object> ys;",
            "}")
        .doTest();
  }

  @Test
  public void noFix() {
    testHelper
        .addInputLines(
            "T.java",
            "import java.util.List;",
            "import org.checkerframework.checker.nullness.qual.NonNull;",
            "import org.checkerframework.checker.nullness.qual.Nullable;",
            "class T {",
            "  List<@Nullable ? extends @Nullable Object> x;",
            "  List<@Nullable ? super Object> y;",
            "  List<@Nullable @NonNull ?> z;",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void diagnostics() {
    CompilationTestHelper.newInstance(NullableWildcard.class, getClass())
        .addSourceLines(
            "T.java",
            "import java.util.List;",
            "import org.checkerframework.checker.nullness.qual.NonNull;",
            "import org.checkerframework.checker.nullness.qual.Nullable;",
            "class T {",
            "  // BUG: Diagnostic contains:",
            "  List<@Nullable ? extends @Nullable Object> x;",
            "  // BUG: Diagnostic contains:",
            "  List<@Nullable ? super Object> y;",
            "  // BUG: Diagnostic contains:",
            "  List<@Nullable @NonNull ?> z;",
            "}")
        .doTest();
  }
}
