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

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link DescribeMatch}Test */
@RunWith(JUnit4.class)
public class DescribeMatchTest {
  private final BugCheckerRefactoringTestHelper testHelper =
      BugCheckerRefactoringTestHelper.newInstance(new DescribeMatch(), getClass());

  @Test
  public void refactoring() {
    testHelper
        .addInputLines(
            "Test.java",
            "import com.google.errorprone.bugpatterns.BugChecker;",
            "import com.google.errorprone.fixes.Fix;",
            "import com.sun.source.tree.Tree;",
            "import com.google.errorprone.matchers.Description;",
            "class Test extends BugChecker {",
            "  Description fix(Tree tree, Fix fix) {",
            "    return buildDescription(tree).addFix(fix).build();",
            "  }",
            "}")
        .addOutputLines(
            "Test.java",
            "import com.google.errorprone.bugpatterns.BugChecker;",
            "import com.google.errorprone.fixes.Fix;",
            "import com.sun.source.tree.Tree;",
            "import com.google.errorprone.matchers.Description;",
            "class Test extends BugChecker {",
            "  Description fix(Tree tree, Fix fix) {",
            "    return describeMatch(tree, fix);",
            "  }",
            "}")
        .addModules(
            "jdk.compiler/com.sun.tools.javac.util", "jdk.compiler/com.sun.tools.javac.tree")
        .doTest();
  }

  @Test
  public void noMatchInBugChecker() {
    testHelper
        .addInputLines(
            "BugChecker.java",
            "package com.google.errorprone.bugpatterns;",
            "import com.google.errorprone.fixes.Fix;",
            "import com.sun.source.tree.Tree;",
            "import com.google.errorprone.matchers.Description;",
            "abstract class BugChecker {",
            "  Description.Builder buildDescription(Tree tree) {",
            "    return null;",
            "  }",
            "  Description fix(Tree tree, Fix fix) {",
            "    return buildDescription(tree).addFix(fix).build();",
            "  }",
            "}")
        .expectUnchanged()
        .doTest();
  }
}
