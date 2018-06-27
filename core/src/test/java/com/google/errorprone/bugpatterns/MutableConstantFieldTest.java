/*
 * Copyright 2017 The Error Prone Authors.
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
  public void staticFinalIterableInitializedInDeclarationWithImmutableSetOf_suggestsFix() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableSet;",
            "class Test {",
            "  // BUG: Diagnostic contains: static final ImmutableSet<String> FOO =",
            "  static final Iterable<String> FOO = ImmutableSet.of();",
            "}")
        .doTest();
  }

  @Test
  public void bind() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableList;",
            "import com.google.inject.testing.fieldbinder.Bind;",
            "import java.util.List;",
            "class Test {",
            "   @Bind ",
            "   private static final List<String> LABELS = ImmutableList.of(\"MiniCluster\");",
            "}")
        .doTest();
  }

  @Test
  public void staticFinalMapInitializedInDeclarationWithImmutableBiMapOf_suggestsFix() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableBiMap;",
            "import java.util.Map;",
            "class Test {",
            "  // BUG: Diagnostic contains: static final ImmutableMap<String, String> FOO =",
            "  static final Map<String, String> FOO = ImmutableBiMap.of();",
            "}")
        .doTest();
  }

  @Test
  public void staticFinalSetInitializedInDeclarationWithImmutableSetOf_suggestsFix() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableSet;",
            "import java.util.Set;",
            "class Test {",
            "  // BUG: Diagnostic contains: static final ImmutableSet<String> FOO =",
            "  static final Set<String> FOO = ImmutableSet.of();",
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
            "  // BUG: Diagnostic contains: static final ImmutableSet FOO =",
            "  static final Set FOO = ImmutableSet.of();",
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
            "  // BUG: Diagnostic contains: static final ImmutableSet<String> FOO =",
            "  static final Set<String> FOO = ImmutableSet.<String>builder().build();",
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
            "  // BUG: Diagnostic contains: static final ImmutableList<String> FOO =",
            "  static final List<String> FOO = ImmutableList.of();",
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
            "  // BUG: Diagnostic contains: static final ImmutableMap<Integer, String> FOO =",
            "  static final Map<Integer, String> FOO = ImmutableMap.of();",
            "}")
        .doTest();
  }

  @Test
  public void
      staticFinalImmutableMultimapInitializedInDeclarationWithImmutableListMultimap_doesNotSuggestFix() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableListMultimap;",
            "import com.google.common.collect.ImmutableMultimap;",
            "class Test {",
            "  static final ImmutableMultimap<String, String> FOO = ImmutableListMultimap.of();",
            "}")
        .doTest();
  }

  @Test
  public void staticFinalImmutableSetInitializedInDeclarationWithImmutableSet_doesNotSuggestFix() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.ImmutableSet;",
            "class Test {",
            "  static final ImmutableSet<String> FOO = ImmutableSet.of();",
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
            "  static final Set<String> FOO;",
            "  static {",
            "    FOO = ImmutableSet.of();",
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
            "  final Set<String> NON_STATIC = ImmutableSet.of();",
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
            "  static Set<String> NON_FINAL = ImmutableSet.of();",
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
            "  static final Set<String> nonCapitalCase = ImmutableSet.of();",
            "}")
        .doTest();
  }

  @Test
  public void mutable_doesNotSuggestFix() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import java.util.ArrayList;",
            "import java.util.List;",
            "class Test {",
            "  static final List<String> MUTABLE = new ArrayList<>();",
            "}")
        .doTest();
  }
}
