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

import static org.junit.Assume.assumeTrue;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.BugCheckerRefactoringTestHelper.FixChoosers;
import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.util.RuntimeVersion;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link MixedMutabilityReturnType} bugpattern. */
@RunWith(JUnit4.class)
public final class MixedMutabilityReturnTypeTest {
  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(MixedMutabilityReturnType.class, getClass());
  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(new MixedMutabilityReturnType(), getClass());

  @Test
  public void positive() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.Collections;",
            "import java.util.List;",
            "import java.util.ArrayList;",
            "class Test {",
            "  // BUG: Diagnostic contains: MixedMutabilityReturnType",
            "  List<Integer> foo() {",
            "    if (hashCode() > 0) {",
            "      return Collections.emptyList();",
            "    }",
            "    return new ArrayList<>();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void whenSuppressed_noWarning() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.Collections;",
            "import java.util.List;",
            "import java.util.ArrayList;",
            "class Test {",
            "  @SuppressWarnings(\"MixedMutabilityReturnType\")",
            "  List<Integer> foo() {",
            "    if (hashCode() > 0) {",
            "      return Collections.emptyList();",
            "    }",
            "    return new ArrayList<>();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void tracksActualVariableTypes() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.Collections;",
            "import java.util.List;",
            "import java.util.ArrayList;",
            "class Test {",
            "  // BUG: Diagnostic contains: MixedMutabilityReturnType",
            "  List<Integer> foo() {",
            "    if (hashCode() > 0) {",
            "      return Collections.emptyList();",
            "    }",
            "    List<Integer> ints = new ArrayList<>();",
            "    return ints;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void uninferrableTypes_noMatch() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.Collections;",
            "import java.util.List;",
            "import java.util.ArrayList;",
            "class Test {",
            "  List<Integer> foo() {",
            "    if (hashCode() > 0) {",
            "      return Collections.emptyList();",
            "    }",
            "    return bar();",
            "  }",
            "  List<Integer> bar() {",
            "    return new ArrayList<>();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void allImmutable_noMatch() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.Collections;",
            "import java.util.List;",
            "import java.util.ArrayList;",
            "class Test {",
            "  List<Integer> foo() {",
            "    if (hashCode() > 0) {",
            "      return Collections.emptyList();",
            "    }",
            "    return Collections.singletonList(1);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void nullType_noMatch() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import java.util.Collections;",
            "import java.util.List;",
            "import java.util.ArrayList;",
            "class Test {",
            "  List<Integer> foo() {",
            "    if (hashCode() > 0) {",
            "      return null;",
            "    }",
            "    return Collections.singletonList(1);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void immutableEnumSetNotMisclassified() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.collect.Sets;",
            "import java.util.Set;",
            "class Test {",
            "  enum E { A, B }",
            "  Set<E> test() {",
            "    return Sets.immutableEnumSet(E.A);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void simpleRefactoring() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import java.util.ArrayList;",
            "import java.util.Collections;",
            "import java.util.List;",
            "final class Test {",
            "  List<Integer> foo() {",
            "    if (hashCode() > 0) {",
            "      return Collections.emptyList();",
            "    }",
            "    List<Integer> ints = new ArrayList<>();",
            "    ints.add(1);",
            "    return ints;",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import com.google.common.collect.ImmutableList;",
            "import java.util.ArrayList;",
            "import java.util.Collections;",
            "import java.util.List;",
            "final class Test {",
            "  ImmutableList<Integer> foo() {",
            "    if (hashCode() > 0) {",
            "      return ImmutableList.of();",
            "    }",
            "    ImmutableList.Builder<Integer> ints = ImmutableList.builder();",
            "    ints.add(1);",
            "    return ints.build();",
            "  }",
            "}")
        .setFixChooser(FixChoosers.SECOND)
        .doTest();
  }

  @Test
  public void refactoringOverridable() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import java.util.ArrayList;",
            "import java.util.Collections;",
            "import java.util.List;",
            "class Test {",
            "  List<Integer> foo() {",
            "    if (hashCode() > 0) {",
            "      return Collections.emptyList();",
            "    }",
            "    List<Integer> ints = new ArrayList<>();",
            "    ints.add(1);",
            "    return ints;",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import com.google.common.collect.ImmutableList;",
            "import java.util.ArrayList;",
            "import java.util.Collections;",
            "import java.util.List;",
            "class Test {",
            "  List<Integer> foo() {",
            "    if (hashCode() > 0) {",
            "      return ImmutableList.of();",
            "    }",
            "    List<Integer> ints = new ArrayList<>();",
            "    ints.add(1);",
            "    return ImmutableList.copyOf(ints);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void refactoringCantReplaceWithBuilder() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import com.google.common.collect.ImmutableList;",
            "import java.util.ArrayList;",
            "import java.util.Collections;",
            "import java.util.List;",
            "final class Test {",
            "  List<Integer> foo() {",
            "    if (hashCode() > 0) {",
            "      return ImmutableList.of();",
            "    }",
            "    List<Integer> ints = new ArrayList<>();",
            "    ints.add(1);",
            "    ints.clear();",
            "    ints.add(2);",
            "    return ints;",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import com.google.common.collect.ImmutableList;",
            "import java.util.ArrayList;",
            "import java.util.Collections;",
            "import java.util.List;",
            "final class Test {",
            "  ImmutableList<Integer> foo() {",
            "    if (hashCode() > 0) {",
            "      return ImmutableList.of();",
            "    }",
            "    List<Integer> ints = new ArrayList<>();",
            "    ints.add(1);",
            "    ints.clear();",
            "    ints.add(2);",
            "    return ImmutableList.copyOf(ints);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void refactoringIgnoresAlreadyImmutableMap() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import com.google.common.collect.ImmutableMap;",
            "import java.util.HashMap;",
            "import java.util.Map;",
            "final class Test {",
            "  Map<Integer, Integer> foo() {",
            "    if (hashCode() > 0) {",
            "      return ImmutableMap.of(1, 1);",
            "    }",
            "    Map<Integer, Integer> ints = new HashMap<>();",
            "    ints.put(2, 2);",
            "    return ints;",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import com.google.common.collect.ImmutableMap;",
            "import java.util.HashMap;",
            "import java.util.Map;",
            "final class Test {",
            "  ImmutableMap<Integer, Integer> foo() {",
            "    if (hashCode() > 0) {",
            "      return ImmutableMap.of(1, 1);",
            "    }",
            "    ImmutableMap.Builder<Integer, Integer> ints = ImmutableMap.builder();",
            "    ints.put(2, 2);",
            "    return ints.build();",
            "  }",
            "}")
        .setFixChooser(FixChoosers.SECOND)
        .doTest();
  }

  @Test
  public void refactoringGuavaFactories() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import com.google.common.collect.ImmutableList;",
            "import com.google.common.collect.Lists;",
            "import java.util.List;",
            "final class Test {",
            "  List<Integer> foo() {",
            "    if (hashCode() > 0) {",
            "      return ImmutableList.of(1);",
            "    } else if (hashCode() < 0) {",
            "      List<Integer> ints = Lists.newArrayList();",
            "      ints.add(2);",
            "      return ints;",
            "    } else {",
            "      List<Integer> ints = Lists.newArrayList(1, 3);",
            "      ints.add(2);",
            "      return ints;",
            "    }",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import com.google.common.collect.ImmutableList;",
            "import com.google.common.collect.Lists;",
            "import java.util.List;",
            "final class Test {",
            "  ImmutableList<Integer> foo() {",
            "    if (hashCode() > 0) {",
            "      return ImmutableList.of(1);",
            "    } else if (hashCode() < 0) {",
            "      ImmutableList.Builder<Integer> ints = ImmutableList.builder();",
            "      ints.add(2);",
            "      return ints.build();",
            "    } else {",
            "      List<Integer> ints = Lists.newArrayList(1, 3);",
            "      ints.add(2);",
            "      return ImmutableList.copyOf(ints);",
            "    }",
            "  }",
            "}")
        .setFixChooser(FixChoosers.SECOND)
        .doTest();
  }

  @Test
  public void refactoringTreeMap() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import com.google.common.collect.ImmutableMap;",
            "import java.util.Map;",
            "import java.util.TreeMap;",
            "final class Test {",
            "  Map<Integer, Integer> foo() {",
            "    if (hashCode() > 0) {",
            "      return ImmutableMap.of(1, 1);",
            "    }",
            "    Map<Integer, Integer> ints = new TreeMap<>();",
            "    ints.put(2, 1);",
            "    return ints;",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import com.google.common.collect.ImmutableMap;",
            "import java.util.Map;",
            "import java.util.TreeMap;",
            "final class Test {",
            "  ImmutableMap<Integer, Integer> foo() {",
            "    if (hashCode() > 0) {",
            "      return ImmutableMap.of(1, 1);",
            "    }",
            "    Map<Integer, Integer> ints = new TreeMap<>();",
            "    ints.put(2, 1);",
            "    return ImmutableMap.copyOf(ints);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void refactoringNonLocalReturnedVariable() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import java.util.ArrayList;",
            "import java.util.Collections;",
            "import java.util.List;",
            "class Test {",
            "  List<Integer> ints = new ArrayList<>();",
            "  List<Integer> foo() {",
            "    if (hashCode() > 0) {",
            "      return Collections.emptyList();",
            "    }",
            "    ints.add(1);",
            "    return ints;",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import com.google.common.collect.ImmutableList;",
            "import java.util.ArrayList;",
            "import java.util.Collections;",
            "import java.util.List;",
            "class Test {",
            "  List<Integer> ints = new ArrayList<>();",
            "  List<Integer> foo() {",
            "    if (hashCode() > 0) {",
            "      return ImmutableList.of();",
            "    }",
            "    ints.add(1);",
            "    return ImmutableList.copyOf(ints);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void refactoringWithNestedCollectionsHelper() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import java.util.ArrayList;",
            "import java.util.Collections;",
            "import java.util.List;",
            "class Test {",
            "  <T> List<T> foo(T a) {",
            "    if (hashCode() > 0) {",
            "      return new ArrayList<>(Collections.singleton(a));",
            "    }",
            "    return Collections.singletonList(a);",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import com.google.common.collect.ImmutableList;",
            "import java.util.ArrayList;",
            "import java.util.Collections;",
            "import java.util.List;",
            "class Test {",
            "  <T> List<T> foo(T a) {",
            "    if (hashCode() > 0) {",
            "      return ImmutableList.copyOf(new ArrayList<>(Collections.singleton(a)));",
            "    }",
            "    return ImmutableList.of(a);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void refactoringWithVar() {
    assumeTrue(RuntimeVersion.isAtLeast15());
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import java.util.ArrayList;",
            "import java.util.Collections;",
            "import java.util.List;",
            "final class Test {",
            "  List<Object> foo() {",
            "    if (hashCode() > 0) {",
            "      return Collections.emptyList();",
            "    }",
            "    var ints = new ArrayList<>();",
            "    ints.add(1);",
            "    return ints;",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import com.google.common.collect.ImmutableList;",
            "import java.util.ArrayList;",
            "import java.util.Collections;",
            "import java.util.List;",
            "final class Test {",
            "  ImmutableList<Object> foo() {",
            "    if (hashCode() > 0) {",
            "      return ImmutableList.of();",
            "    }",
            "    var ints = ImmutableList.builder();",
            "    ints.add(1);",
            "    return ints.build();",
            "  }",
            "}")
        .setFixChooser(FixChoosers.SECOND)
        .doTest();
  }
}
