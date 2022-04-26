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

/**
 * @author amalloy@google.com (Alan Malloy)
 */
@RunWith(JUnit4.class)
public class WithSignatureDiscouragedTest {
  private final BugCheckerRefactoringTestHelper refactoringTestHelper =
      BugCheckerRefactoringTestHelper.newInstance(WithSignatureDiscouraged.class, getClass());

  @Test
  public void named() {
    refactoringTestHelper
        .addInputLines(
            "Test.java",
            "import static com.google.errorprone.matchers.Matchers.instanceMethod;",
            "import com.google.errorprone.matchers.Matcher;",
            "import com.sun.source.tree.ExpressionTree;",
            "public class Test {",
            "  private static final Matcher<ExpressionTree> TO_STRING = ",
            "    instanceMethod()",
            "      .anyClass()",
            "      .withSignature(\"toString\");",
            "}")
        .addOutputLines(
            "Test.java",
            "import static com.google.errorprone.matchers.Matchers.instanceMethod;",
            "import com.google.errorprone.matchers.Matcher;",
            "import com.sun.source.tree.ExpressionTree;",
            "public class Test {",
            "  private static final Matcher<ExpressionTree> TO_STRING = ",
            "    instanceMethod()",
            "      .anyClass()",
            "      .named(\"toString\");",
            "}")
        .doTest();
  }

  @Test
  public void withEmptyParameterList() {
    refactoringTestHelper
        .addInputLines(
            "Test.java",
            "import static com.google.errorprone.matchers.Matchers.instanceMethod;",
            "import com.google.errorprone.matchers.Matcher;",
            "import com.sun.source.tree.ExpressionTree;",
            "public class Test {",
            "  private static final Matcher<ExpressionTree> TO_STRING = ",
            "    instanceMethod()",
            "      .anyClass()",
            "      .withSignature(\"toString()\");",
            "}")
        .addOutputLines(
            "Test.java",
            "import static com.google.errorprone.matchers.Matchers.instanceMethod;",
            "import com.google.errorprone.matchers.Matcher;",
            "import com.sun.source.tree.ExpressionTree;",
            "public class Test {",
            "  private static final Matcher<ExpressionTree> TO_STRING = ",
            "    instanceMethod()",
            "      .anyClass()",
            "      .named(\"toString\")",
            "      .withNoParameters();",
            "}")
        .doTest();
  }

  @Test
  public void withParameters() {
    refactoringTestHelper
        .addInputLines(
            "Test.java",
            "import static com.google.errorprone.matchers.Matchers.instanceMethod;",
            "import com.google.errorprone.matchers.Matcher;",
            "import com.sun.source.tree.ExpressionTree;",
            "public class Test {",
            "  private static final Matcher<ExpressionTree> VALUE_OF = ",
            "    instanceMethod()",
            "      .anyClass()",
            "      .withSignature(\"valueOf(double)\");",
            "}")
        .addOutputLines(
            "Test.java",
            "import static com.google.errorprone.matchers.Matchers.instanceMethod;",
            "import com.google.errorprone.matchers.Matcher;",
            "import com.sun.source.tree.ExpressionTree;",
            "public class Test {",
            "  private static final Matcher<ExpressionTree> VALUE_OF = ",
            "    instanceMethod()",
            "      .anyClass()",
            "      .named(\"valueOf\")",
            "      .withParameters(\"double\");",
            "}")
        .doTest();
  }

  @Test
  public void leaveVarargs() {
    refactoringTestHelper
        .addInputLines(
            "Test.java",
            "import static com.google.errorprone.matchers.Matchers.staticMethod;",
            "import com.google.errorprone.matchers.Matcher;",
            "import com.sun.source.tree.ExpressionTree;",
            "public class Test {",
            "  private static final Matcher<ExpressionTree> STRING_FORMAT = ",
            "    staticMethod()",
            "      .onClass(\"java.lang.String\")",
            "      .withSignature(\"format(java.lang.String,java.lang.Object...)\");",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void leaveGenerics() {
    refactoringTestHelper
        .addInputLines(
            "Test.java",
            "import static com.google.errorprone.matchers.Matchers.staticMethod;",
            "import com.google.errorprone.matchers.Matcher;",
            "import com.sun.source.tree.ExpressionTree;",
            "public class Test {",
            "  private static final Matcher<ExpressionTree> TO_STRING = ",
            "    staticMethod()",
            "      .onClass(\"com.google.common.collect.ImmutableList\")",
            "      .withSignature(\"<E>builder()\");",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void dontMigrateArrays() {
    refactoringTestHelper
        .addInputLines(
            "Test.java",
            "import static com.google.errorprone.matchers.Matchers.staticMethod;",
            "import com.google.errorprone.matchers.Matcher;",
            "import com.sun.source.tree.ExpressionTree;",
            "public class Test {",
            "  private static final Matcher<ExpressionTree> TO_STRING = ",
            "    staticMethod()",
            "      .onClass(\"java.lang.String\")",
            "      .withSignature(\"copyValueOf(char[])\");",
            "}")
        .expectUnchanged()
        .doTest();
  }
}
