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

package com.google.errorprone.bugpatterns;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link ImmutableMemberCollection}. */
@RunWith(JUnit4.class)
public final class ImmutableMemberCollectionTest {
  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(ImmutableMemberCollection.class, getClass());

  @Test
  public void listInitInline_replacesTypeWithImmutableList() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import com.google.common.collect.ImmutableList;",
            "import java.util.List;",
            "class Test {",
            "  private final List<String> myList = ImmutableList.of(\"a\");",
            "}")
        .addOutputLines(
            "Test.java",
            "import com.google.common.collect.ImmutableList;",
            "import java.util.List;",
            "class Test {",
            "  private final ImmutableList<String> myList = ImmutableList.of(\"a\");",
            "}")
        .doTest();
  }

  @Test
  public void listInitConstructor_replacesTypeWithImmutableList() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import com.google.common.collect.ImmutableList;",
            "import java.util.List;",
            "class Test {",
            "  private final List<String> myList;",
            "  private List<String> doNotTouchThisList;",
            "  Test() {",
            "    myList = ImmutableList.of(\"a\");",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import com.google.common.collect.ImmutableList;",
            "import java.util.List;",
            "class Test {",
            "  private final ImmutableList<String> myList;",
            "  private List<String> doNotTouchThisList;",
            "  Test() {",
            "    myList = ImmutableList.of(\"a\");",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void listInitInline_bindAnnotation_doesNothing() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import com.google.common.collect.ImmutableList;",
            "import com.google.inject.testing.fieldbinder.Bind;",
            "import java.util.List;",
            "class Test {",
            "  @Bind private final List<String> myList = ImmutableList.of(\"a\");",
            "}")
        .expectUnchanged()
        .doTest();
  }
}
