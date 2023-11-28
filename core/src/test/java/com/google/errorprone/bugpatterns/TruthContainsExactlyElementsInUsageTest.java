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

package com.google.errorprone.bugpatterns;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class TruthContainsExactlyElementsInUsageTest {

  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(
          TruthContainsExactlyElementsInUsage.class, getClass());

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(TruthContainsExactlyElementsInUsage.class, getClass());

  @Test
  public void negativeDirectContainsExactlyUsage() {
    compilationHelper
        .addSourceLines(
            "ExampleClassTest.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "import com.google.common.collect.ImmutableList;",
            "public class ExampleClassTest {",
            "  void test() {",
            "    assertThat(ImmutableList.of(1, 2, 3)).containsExactly(1,2,3);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeVariableContainsExactlyUsage() {
    compilationHelper
        .addSourceLines(
            "ExampleClassTest.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "import com.google.common.collect.ImmutableList;",
            "import java.util.List;",
            "public class ExampleClassTest {",
            "  void test() {",
            "    List<Integer> list = ImmutableList.of(1, 2, 3);",
            "    assertThat(list).containsExactly(1,2,3);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeVariableTruthContainsExactlyElementsInUsage() {
    compilationHelper
        .addSourceLines(
            "ExampleClassTest.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "import com.google.common.collect.ImmutableList;",
            "import java.util.List;",
            "public class ExampleClassTest {",
            "  void test() {",
            "    List<Integer> list = ImmutableList.of(1, 2, 3);",
            "    assertThat(list).containsExactlyElementsIn(list);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeVariableTruthContainsExactlyElementsInUsageWithCopy() {
    compilationHelper
        .addSourceLines(
            "ExampleClassTest.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "import com.google.common.collect.ImmutableList;",
            "import java.util.List;",
            "public class ExampleClassTest {",
            "  void test() {",
            "    List<Integer> list = ImmutableList.of(1, 2, 3);",
            "    assertThat(list).containsExactlyElementsIn(ImmutableList.copyOf(list));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeTruthContainsExactlyElementsInUsageWithHashSet() {
    compilationHelper
        .addSourceLines(
            "ExampleClassTest.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "import com.google.common.collect.ImmutableList;",
            "import com.google.common.collect.Sets;",
            "public class ExampleClassTest {",
            "  void test() {",
            "    assertThat(ImmutableList.of(1, 2, 3))",
            "    .containsExactlyElementsIn(Sets.newHashSet(1, 2, 3));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negativeTruthContainsExactlyElementsInUsageWithImmutableSet() {
    compilationHelper
        .addSourceLines(
            "ExampleClassTest.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "import com.google.common.collect.ImmutableList;",
            "import com.google.common.collect.ImmutableSet;",
            "public class ExampleClassTest {",
            "  void test() {",
            "    assertThat(ImmutableList.of(1, 2, 3))",
            "    .containsExactlyElementsIn(ImmutableSet.of(1, 2, 3));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void refactoringTruthContainsExactlyElementsInUsageWithArrayList() {
    refactoringHelper
        .addInputLines(
            "ExampleClassTest.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "import com.google.common.collect.ImmutableList;",
            "import java.util.Arrays;",
            "public class ExampleClassTest {",
            "  void test() {",
            "    assertThat(ImmutableList.of(1, 2, 3))",
            "    .containsExactlyElementsIn(Arrays.asList(1, 2, 3));",
            "  }",
            "}")
        .addOutputLines(
            "ExampleClassTest.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "import com.google.common.collect.ImmutableList;",
            "import java.util.Arrays;",
            "public class ExampleClassTest {",
            "  void test() {",
            "    assertThat(ImmutableList.of(1, 2, 3)).containsExactly(1, 2, 3);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void refactoringTruthContainsExactlyElementsInUsageWithListOf() {
    refactoringHelper
        .addInputLines(
            "ExampleClassTest.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "import com.google.common.collect.ImmutableList;",
            "import java.util.List;",
            "public class ExampleClassTest {",
            "  void test() {",
            "    assertThat(ImmutableList.of(1, 2, 3))",
            "    .containsExactlyElementsIn(List.of(1, 2, 3));",
            "  }",
            "}")
        .addOutputLines(
            "ExampleClassTest.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "import com.google.common.collect.ImmutableList;",
            "import java.util.List;",
            "public class ExampleClassTest {",
            "  void test() {",
            "    assertThat(ImmutableList.of(1, 2, 3)).containsExactly(1, 2, 3);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void refactoringTruthContainsExactlyElementsInUsageWithInOrderList() {
    refactoringHelper
        .addInputLines(
            "ExampleClassTest.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "import com.google.common.collect.ImmutableList;",
            "import java.util.Arrays;",
            "public class ExampleClassTest {",
            "  void test() {",
            "    assertThat(ImmutableList.of(1, 2, 3))",
            "    .containsExactlyElementsIn(Arrays.asList(1, 2, 3)).inOrder();",
            "  }",
            "}")
        .addOutputLines(
            "ExampleClassTest.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "import com.google.common.collect.ImmutableList;",
            "import java.util.Arrays;",
            "public class ExampleClassTest {",
            "  void test() {",
            "    assertThat(ImmutableList.of(1, 2, 3)).containsExactly(1, 2, 3).inOrder();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void refactoringTruthContainsExactlyElementsInUsageWithStaticallyImportedAsList() {
    refactoringHelper
        .addInputLines(
            "ExampleClassTest.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "import com.google.common.collect.ImmutableList;",
            "import static java.util.Arrays.asList;",
            "public class ExampleClassTest {",
            "  void test() {",
            "    assertThat(ImmutableList.of(1, 2, 3)).containsExactlyElementsIn(asList(1, 2, 3));",
            "  }",
            "}")
        .addOutputLines(
            "ExampleClassTest.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "import com.google.common.collect.ImmutableList;",
            "import static java.util.Arrays.asList;",
            "public class ExampleClassTest {",
            "  void test() {",
            "    assertThat(ImmutableList.of(1, 2, 3)).containsExactly(1, 2, 3);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void refactoringTruthContainsExactlyElementsInUsageWithNewArrayList() {
    refactoringHelper
        .addInputLines(
            "ExampleClassTest.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "import com.google.common.collect.ImmutableList;",
            "import com.google.common.collect.Lists;",
            "public class ExampleClassTest {",
            "  void test() {",
            "    assertThat(ImmutableList.of(1, 2, 3))",
            "    .containsExactlyElementsIn(Lists.newArrayList(1, 2, 3));",
            "  }",
            "}")
        .addOutputLines(
            "ExampleClassTest.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "import com.google.common.collect.ImmutableList;",
            "import com.google.common.collect.Lists;",
            "public class ExampleClassTest {",
            "  void test() {",
            "    assertThat(ImmutableList.of(1, 2, 3)).containsExactly(1, 2, 3);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void refactoringTruthContainsExactlyElementsInUsageWithSingletonList() {
    refactoringHelper
        .addInputLines(
            "ExampleClassTest.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "import com.google.common.collect.ImmutableList;",
            "import java.util.Collections;",
            "public class ExampleClassTest {",
            "  void test() {",
            "    assertThat(ImmutableList.of(1))",
            "    .containsExactlyElementsIn(Collections.singletonList(1));",
            "  }",
            "}")
        .addOutputLines(
            "ExampleClassTest.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "import com.google.common.collect.ImmutableList;",
            "import java.util.Collections;",
            "public class ExampleClassTest {",
            "  void test() {",
            "    assertThat(ImmutableList.of(1)).containsExactly(1);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void refactoringTruthContainsExactlyElementsInUsageWithEmptyList() {
    refactoringHelper
        .addInputLines(
            "ExampleClassTest.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "import com.google.common.collect.ImmutableList;",
            "import java.util.Arrays;",
            "public class ExampleClassTest {",
            "  void test() {",
            "    assertThat(ImmutableList.of()).containsExactlyElementsIn(Arrays.asList());",
            "  }",
            "}")
        .addOutputLines(
            "ExampleClassTest.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "import com.google.common.collect.ImmutableList;",
            "import java.util.Arrays;",
            "public class ExampleClassTest {",
            "  void test() {",
            "    assertThat(ImmutableList.of()).containsExactly();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void refactoringTruthContainsExactlyElementsInUsageWithImmutableList() {
    refactoringHelper
        .addInputLines(
            "ExampleClassTest.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "import com.google.common.collect.ImmutableList;",
            "public class ExampleClassTest {",
            "  void test() {",
            "    assertThat(ImmutableList.of(1, 2, 3))",
            "    .containsExactlyElementsIn(ImmutableList.of(1, 2, 3));",
            "  }",
            "}")
        .addOutputLines(
            "ExampleClassTest.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "import com.google.common.collect.ImmutableList;",
            "public class ExampleClassTest {",
            "  void test() {",
            "    assertThat(ImmutableList.of(1, 2, 3)).containsExactly(1, 2, 3);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void refactoringTruthContainsExactlyElementsInUsageWithArray() {
    refactoringHelper
        .addInputLines(
            "ExampleClassTest.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "import com.google.common.collect.ImmutableList;",
            "public class ExampleClassTest {",
            "  void test() {",
            "    assertThat(ImmutableList.of(1, 2, 3))",
            "    .containsExactlyElementsIn(new Integer[] {1, 2, 3});",
            "  }",
            "}")
        .addOutputLines(
            "ExampleClassTest.java",
            "import static com.google.common.truth.Truth.assertThat;",
            "import com.google.common.collect.ImmutableList;",
            "public class ExampleClassTest {",
            "  void test() {",
            "    assertThat(ImmutableList.of(1, 2, 3)).containsExactly(1,2,3);",
            "  }",
            "}")
        .doTest();
  }
}
