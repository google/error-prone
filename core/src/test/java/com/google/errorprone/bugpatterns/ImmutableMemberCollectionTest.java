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
  public void listInitInline_notMutated_replacesTypeWithImmutableList() {
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
  public void setInitConstructor_notMutatedButSuppressed_doesNothing() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import java.util.List;",
            "class Test {",
            "  @SuppressWarnings(\"ImmutableMemberCollection\")",
            "  private final List<String> myList;",
            "  Test(List<String> myList) {",
            "    this.myList = myList;",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void listInitConstructor_notMutated_replacesTypeWithImmutableList() {
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

  @Test
  public void setInit_mutableTypeInConstructor_mutated_doesNothing() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import java.util.Set;",
            "import java.util.HashSet;",
            "class Test {",
            "  private final Set<String> mySet;",
            "  Test() {",
            "    mySet = new HashSet<>();",
            "  }",
            "  private void myFunc() {",
            "    mySet.add(\"myString\");",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void setInit_mutableTypeInConstructor_returnedAsIs_doesNothing() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import java.util.Set;",
            "import java.util.HashSet;",
            "class Test {",
            "  private final Set<String> mySet = new HashSet<String>();",
            "  private Set<String> myFunc() {",
            "    return mySet;",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void setInit_mutableTypeInConstructor_returnedInConditional_doesNothing() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import java.util.Set;",
            "import java.util.HashSet;",
            "class Test {",
            "  private final Set<String> mySet = new HashSet<String>();",
            "  private Set<String> myFunc() {",
            "    return 1 > 2 ? new HashSet<String>() : mySet;",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void setInit_mutableTypeInStaticBlock_mutated_doesNothing() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import java.util.Set;",
            "import java.util.HashSet;",
            "class Test {",
            "  private static final Set<String> mySet;",
            "  static {",
            "    mySet = new HashSet<>();",
            "  }",
            "  private static void myFunc() {",
            "    mySet.add(\"myString\");",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void setInit_mutableTypeInStaticBlock_passedToAnotherFunction_doesNothing() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import java.util.Set;",
            "import java.util.HashSet;",
            "class Test {",
            "  private static final Set<String> mySet;",
            "  static {",
            "    mySet = new HashSet<>();",
            "  }",
            "  private static void myFunc() {",
            "    System.out.println(mySet);",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void listInitWithMutableType_notMutated_replacesTypeAndMakesDefensiveCopy() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import com.google.common.collect.ImmutableList;",
            "import java.util.List;",
            "import java.util.ArrayList;",
            "class Test {",
            "  private final List<String> myList1 = new ArrayList<>();",
            "  private final List<String> myList2;",
            "  Test() {",
            "    myList2 = new ArrayList<>();",
            "  }",
            "  Test(String x) {",
            "    myList2 = ImmutableList.of(x);",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import com.google.common.collect.ImmutableList;",
            "import java.util.List;",
            "import java.util.ArrayList;",
            "class Test {",
            "  private final ImmutableList<String> myList1 = ",
            "      ImmutableList.copyOf(new ArrayList<>());",
            "  private final ImmutableList<String> myList2;",
            "  Test() {",
            "    myList2 = ImmutableList.copyOf(new ArrayList<>());",
            "  }",
            "  Test(String x) {",
            "    myList2 = ImmutableList.of(x);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void listInitWithMutableType_invokesReadOnlyMethods_replacesTypeAndMakesDefensiveCopy() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import java.util.ArrayList;",
            "import java.util.List;",
            "class Test {",
            "  private final List<String> myList = new ArrayList<>();",
            "  private String myFunc() {",
            "    return myList.get(0);",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import com.google.common.collect.ImmutableList;",
            "import java.util.ArrayList;",
            "import java.util.List;",
            "class Test {",
            "  private final ImmutableList<String> myList = ",
            "      ImmutableList.copyOf(new ArrayList<>());",
            "  private String myFunc() {",
            "    return myList.get(0);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void setMutation_thisReference_doesNothing() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import com.google.common.collect.ImmutableSet;",
            "import java.util.Set;",
            "import java.util.HashSet;",
            "class Test {",
            "  private final ImmutableSet<String> mySet;",
            "  Test() {",
            "    mySet = ImmutableSet.of();",
            "  }",
            "  private static final class Builder {",
            "    private final Set<String> mySet = new HashSet<>();",
            "    public void addString(String x) {",
            "      this.mySet.add(x);",
            "    }",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void setInNestedClassMutationInParent_doesNothing() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            "import com.google.common.collect.ImmutableSet;",
            "import java.util.Set;",
            "import java.util.HashSet;",
            "class Test {",
            "  public void addString(String x) {",
            "    NestedTest nested = new NestedTest();",
            "    nested.mySet.add(x);",
            "  }",
            "  private static final class NestedTest {",
            "    private final Set<String> mySet = new HashSet<>();",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }
}
