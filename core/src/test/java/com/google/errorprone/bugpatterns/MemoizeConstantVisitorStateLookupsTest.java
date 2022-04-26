/*
 * Copyright 2020 The Error Prone Authors.
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

/** Tests for {@link com.google.errorprone.bugpatterns.MemoizeConstantVisitorStateLookups}. */
@RunWith(JUnit4.class)
public class MemoizeConstantVisitorStateLookupsTest {
  private final CompilationTestHelper compilationTestHelper =
      CompilationTestHelper.newInstance(MemoizeConstantVisitorStateLookups.class, getClass())
          .addModules(
              "jdk.compiler/com.sun.tools.javac.util", "jdk.compiler/com.sun.tools.javac.code");
  private final BugCheckerRefactoringTestHelper refactoringTestHelper =
      BugCheckerRefactoringTestHelper.newInstance(
              MemoizeConstantVisitorStateLookups.class, getClass())
          .addModules(
              "jdk.compiler/com.sun.tools.javac.util", "jdk.compiler/com.sun.tools.javac.code");

  @Test
  public void replaceSingleUsage() {
    refactoringTestHelper
        .addInputLines(
            "Test.java",
            "import com.google.errorprone.VisitorState;",
            "import com.sun.tools.javac.util.Name;",
            "class Test {",
            "  public Test(VisitorState state) {",
            "    Name me = state.getName(\"Test\");",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import com.google.errorprone.VisitorState;",
            "import com.google.errorprone.suppliers.Supplier;",
            "import com.sun.tools.javac.util.Name;",
            "class Test {",
            "  public Test(VisitorState state) {",
            "    Name me = TEST.get(state);",
            "  }",
            "  private static final Supplier<Name> TEST = ",
            "    VisitorState.memoize(state -> state.getName(\"Test\"));",
            "}")
        .doTest();
  }

  @Test
  public void prefersExistingStringConstant() {
    refactoringTestHelper
        .addInputLines(
            "Test.java",
            "import com.google.errorprone.VisitorState;",
            "import com.sun.tools.javac.code.Type;",
            "class Test {",
            "  private static final String MAP = \"java.util.Map\";",
            "  public Test(VisitorState state) {",
            "    Type map = state.getTypeFromString(MAP);",
            "    Type map2 = state.getTypeFromString(\"java.util.Map\");",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import com.google.errorprone.VisitorState;",
            "import com.google.errorprone.suppliers.Supplier;",
            "import com.sun.tools.javac.code.Type;",
            "class Test {",
            "  private static final String MAP = \"java.util.Map\";",
            "  public Test(VisitorState state) {",
            "    Type map = JAVA_UTIL_MAP.get(state);",
            "    Type map2 = JAVA_UTIL_MAP.get(state);",
            "  }",
            "  private static final Supplier<Type> JAVA_UTIL_MAP = ",
            "    VisitorState.memoize(state -> state.getTypeFromString(MAP));",
            "}")
        .doTest();
  }

  @Test
  public void replaceConflictingValues() {
    refactoringTestHelper
        .addInputLines(
            "Test.java",
            "import com.google.errorprone.VisitorState;",
            "import com.sun.tools.javac.code.Type;",
            "import com.sun.tools.javac.util.Name;",
            "class Test {",
            "  public Test(VisitorState state) {",
            "    Name className = state.getName(\"java.lang.Class\");",
            "    Type classType = state.getTypeFromString(\"java.lang.Class\");",
            "    Name lookupAgain = state.getName(\"java.lang.Class\");",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import com.google.errorprone.VisitorState;",
            "import com.google.errorprone.suppliers.Supplier;",
            "import com.sun.tools.javac.code.Type;",
            "import com.sun.tools.javac.util.Name;",
            "class Test {",
            "  public Test(VisitorState state) {",
            "    Name className = JAVA_LANG_CLASS_NAME.get(state);",
            "    Type classType = JAVA_LANG_CLASS_TYPE.get(state);",
            "    Name lookupAgain = JAVA_LANG_CLASS_NAME.get(state);",
            "  }",
            "  private static final Supplier<Name> JAVA_LANG_CLASS_NAME = ",
            "    VisitorState.memoize(state -> state.getName(\"java.lang.Class\"));",
            "  private static final Supplier<Type> JAVA_LANG_CLASS_TYPE = ",
            "    VisitorState.memoize(state -> state.getTypeFromString(\"java.lang.Class\"));",
            "}")
        .doTest();
  }

  @Test
  public void findingOnLookup() {
    compilationTestHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.VisitorState;",
            "import com.sun.tools.javac.code.Type;",
            "import com.sun.tools.javac.util.Name;",
            "class Test {",
            "  public Test(VisitorState state) {",
            "    // BUG: Diagnostic contains:",
            "    Name className = state.getName(\"java.lang.Class\");",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void negative_doesntMemoizeTwice() {
    refactoringTestHelper
        .addInputLines(
            "Test.java",
            "import com.google.common.collect.ImmutableSet;\n",
            "import com.google.errorprone.VisitorState;",
            "import com.google.errorprone.suppliers.Supplier;",
            "import com.sun.tools.javac.util.Name;",
            "class Test {",
            "  private static final Supplier<ImmutableSet<Name>> ALLOWED_NAMES ",
            "    = VisitorState.memoize(state -> ",
            "        ImmutableSet.of(state.getName(\"foo\"), state.getName(\"bar\")));",
            "}")
        .expectUnchanged()
        .doTest();
  }
}
