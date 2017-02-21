/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

/** {@link MutableConstantField}Test */
@RunWith(JUnit4.class)
public class MutableConstantFieldTest {
  private final CompilationTestHelper testHelper =
      CompilationTestHelper.newInstance(MutableConstantField.class, getClass());

  @Test
  public void staticFinalSetInitializedInDeclarationWithImmutableSetOf_suggestsFix() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableSet;",
            "import java.util.Set;",
            "class Test {",
            "  // BUG: Diagnostic contains: static final ImmutableSet<String> COLORS =",
            "  static final Set<String> COLORS = ImmutableSet.of(\"Red\", \"Green\", \"Blue\");",
            "}")
        .doTest();
  }

  @Test
  public void staticFinalRawSetInitializedInDeclarationWithImmutableSetOf_suggestsFix() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableSet;",
            "import java.util.Set;",
            "class Test {",
            "  // BUG: Diagnostic contains: static final ImmutableSet COLORS =",
            "  static final Set COLORS = ImmutableSet.of(\"Red\", \"Green\", \"Blue\");",
            "}")
        .doTest();
  }

  @Test
  public void staticFinalSetInitializedInDeclarationWithImmutableSetBuilder_suggestsFix() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableSet;",
            "import java.util.Set;",
            "class Test {",
            "  // BUG: Diagnostic contains: static final ImmutableSet<String> COLORS =",
            "  static final Set<String> COLORS =",
            "      ImmutableSet.<String>builder()",
            "         .add(\"Red\")",
            "         .add(\"Green\")",
            "         .add(\"Blue\")",
            "         .build();",
            "}")
        .doTest();
  }

  @Test
  public void staticFinalListInitializedInDeclarationWithImmutableListOf_suggestsFix() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableList;",
            "import java.util.List;",
            "class Test {",
            "  // BUG: Diagnostic contains: static final ImmutableList<String> COLORS =",
            "  static final List<String> COLORS = ImmutableList.of(\"Red\", \"Green\", \"Blue\");",
            "}")
        .doTest();
  }

  @Test
  public void staticFinalMapInitializedInDeclarationWithImmutableMapOf_suggestsFix() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableMap;",
            "import java.util.Map;",
            "class Test {",
            "  // BUG: Diagnostic contains: static final ImmutableMap<Integer, String> NUMBERS =",
            "  static final Map<Integer, String> NUMBERS =",
            "      ImmutableMap.of(1, \"One\", 2, \"Two\", 3, \"Three\");",
            "}")
        .doTest();
  }

  @Test
  public void staticFinalSetInitializedInStaticBlock_doesNotSuggestFix() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableSet;",
            "import java.util.Set;",
            "class Test {",
            "  static final Set<String> COLORS;",
            "  static {",
            "    COLORS = ImmutableSet.of(\"Red\", \"Green\", \"Blue\");",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void nonStatic_doesNotSuggestFix() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableSet;",
            "import java.util.Set;",
            "class Test {",
            "  final Set<String> colors = ImmutableSet.of(\"Red\", \"Green\", \"Blue\");",
            "}")
        .doTest();
  }

  @Test
  public void nonFinal_doesNotSuggestFix() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableSet;",
            "import java.util.Set;",
            "class Test {",
            "  static Set<String> colors = ImmutableSet.of(\"Red\", \"Green\", \"Blue\");",
            "}")
        .doTest();
  }

  @Test
  public void nonCapitalCase_doesNotSuggestFix() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableSet;",
            "import java.util.Set;",
            "class Test {",
            "  static final Set<String> colors = ImmutableSet.of(\"Red\", \"Green\", \"Blue\");",
            "}")
        .doTest();
  }

  @Test
  public void mutable_doesNotSuggestFix() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableSet;",
            "class Test {",
            "  static final ImmutableSet.Builder<String> MUTABLE = ImmutableSet.builder();",
            "}")
        .doTest();
  }
}
