/*
 * Copyright 2016 The Error Prone Authors.
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
package com.google.errorprone.bugpatterns.inject.dagger;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Sets;
import com.google.errorprone.BugCheckerRefactoringTestHelper;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.TreeSet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/** Tests {@link EmptySetMultibindingContributions}. */
@RunWith(Parameterized.class)
public final class EmptySetMultibindingContributionsTest {
  @Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return Arrays.asList(
        new Object[][] {
          {Collections.class.getCanonicalName() + ".emptySet()"},
          {ImmutableSet.class.getCanonicalName() + ".of()"},
          {ImmutableSortedSet.class.getCanonicalName() + ".of()"},
          {"new " + HashSet.class.getCanonicalName() + "<>()"},
          {"new " + LinkedHashSet.class.getCanonicalName() + "<>()"},
          {"new " + TreeSet.class.getCanonicalName() + "<>()"},
          {Sets.class.getCanonicalName() + ".newHashSet()"},
          {Sets.class.getCanonicalName() + ".newLinkedHashSet()"},
          {Sets.class.getCanonicalName() + ".newConcurrentHashSet()"},
          {EnumSet.class.getCanonicalName() + ".noneOf(java.util.concurrent.TimeUnit.class)"},
        });
  }

  private final String emptySetSnippet;
  private BugCheckerRefactoringTestHelper testHelper;

  public EmptySetMultibindingContributionsTest(String emptySetSnippet) {
    this.emptySetSnippet = emptySetSnippet;
  }

  @Before
  public void setUp() {
    testHelper =
        BugCheckerRefactoringTestHelper.newInstance(
            new EmptySetMultibindingContributions(), getClass());
  }

  @Test
  public void elementsIntoSetMethod_emptySet() {
    testHelper
        .addInputLines(
            "in/Test.java",
            "import dagger.Module;",
            "import dagger.Provides;",
            "import dagger.multibindings.ElementsIntoSet;",
            "import java.util.Set;",
            "@Module",
            "class Test {",
            "  @Provides @ElementsIntoSet Set<?> provideEmpty() {",
            String.format("    return %s;", emptySetSnippet),
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "import dagger.Module;",
            "import dagger.Provides;",
            "import dagger.multibindings.ElementsIntoSet;",
            "import dagger.multibindings.Multibinds;",
            "import java.util.Set;",
            "@Module",
            "abstract class Test {",
            "  @Multibinds abstract Set<?> provideEmpty();",
            "}")
        .doTest();
  }
}
