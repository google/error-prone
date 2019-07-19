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

/** @author amalloy@google.com (Alan Malloy) */
@RunWith(JUnit4.class)
public class UnnecessaryMethodInvocationMatcherTest {
  private final BugCheckerRefactoringTestHelper refactoringTestHelper =
      BugCheckerRefactoringTestHelper.newInstance(
          new UnnecessaryMethodInvocationMatcher(), getClass());

  @Test
  public void replace() {
    refactoringTestHelper
        .addInputLines(
            "Test.java",
            "import static com.google.errorprone.matchers.Matchers.*;",
            "import com.google.errorprone.matchers.Matcher;",
            "import com.sun.source.tree.ExpressionTree;",
            "public class Test {",
            "  private static final Matcher<ExpressionTree> TO_STRING = ",
            "    methodInvocation(",
            "      instanceMethod()",
            "        .anyClass()",
            "        .named(\"toString\"));",
            "}")
        .addOutputLines(
            "Test.java",
            "import static com.google.errorprone.matchers.Matchers.*;",
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
  public void descendIntoCombinators() {
    refactoringTestHelper
        .addInputLines(
            "Test.java",
            "import static com.google.errorprone.matchers.Matchers.*;",
            "import com.google.errorprone.matchers.Matcher;",
            "import com.sun.source.tree.ExpressionTree;",
            "public class Test {",
            "  private static final Matcher<ExpressionTree> STRINGIFY = ",
            "    methodInvocation(",
            "      anyOf(",
            "        instanceMethod()",
            "          .anyClass()",
            "          .named(\"toString\"),",
            "        allOf(",
            "          staticMethod())));",
            "}")
        .addOutputLines(
            "Test.java",
            "import static com.google.errorprone.matchers.Matchers.*;",
            "import com.google.errorprone.matchers.Matcher;",
            "import com.sun.source.tree.ExpressionTree;",
            "public class Test {",
            "  private static final Matcher<ExpressionTree> STRINGIFY = ",
            "    anyOf(",
            "      instanceMethod()",
            "        .anyClass()",
            "        .named(\"toString\"),",
            "      allOf(",
            "        staticMethod()));",
            "}")
        .doTest();
  }

  @Test
  public void onlyChangeMethodMatchers() {
    refactoringTestHelper
        .addInputLines(
            "Test.java",
            "import static com.google.errorprone.matchers.Matchers.*;",
            "import com.google.errorprone.matchers.Matcher;",
            "import com.sun.source.tree.ExpressionTree;",
            "public class Test {",
            "  private static final Matcher<ExpressionTree> STRINGIFY = ",
            "    methodInvocation(",
            "      anyOf(",
            "        instanceMethod()",
            "          .anyClass()",
            "          .named(\"toString\"),",
            "        allOf(",
            "          hasAnnotation(\"java.lang.SuppressWarnings\"))));",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void permitWithArguments() {
    refactoringTestHelper
        .addInputLines(
            "Test.java",
            "import static com.google.errorprone.matchers.ChildMultiMatcher.MatchType.ALL;",
            "import static com.google.errorprone.matchers.Matchers.*;",
            "import com.google.errorprone.matchers.Matcher;",
            "import com.sun.source.tree.ExpressionTree;",
            "public class Test {",
            "  private static final Matcher<ExpressionTree> TO_STRING = ",
            "    methodInvocation(",
            "      instanceMethod()",
            "        .anyClass()",
            "        .named(\"toString\"),",
            "      ALL,",
            "      isVariable());",
            "}")
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void expressionStatement() {
    refactoringTestHelper
        .addInputLines(
            "Test.java",
            "import static com.google.errorprone.matchers.Matchers.*;",
            "import com.google.errorprone.matchers.Matcher;",
            "import com.sun.source.tree.StatementTree;",
            "public class Test {",
            "  private static final Matcher<StatementTree> TARGETED =",
            "      expressionStatement(",
            "          methodInvocation(",
            "              instanceMethod()",
            "                  .onDescendantOfAny(",
            "                      \"java.lang.Class\",",
            "                      \"java.lang.String\")));",
            "}")
        .addOutputLines(
            "Test.java",
            "import static com.google.errorprone.matchers.Matchers.*;",
            "import com.google.errorprone.matchers.Matcher;",
            "import com.sun.source.tree.StatementTree;",
            "public class Test {",
            "  private static final Matcher<StatementTree> TARGETED =",
            "      expressionStatement(",
            "          instanceMethod()",
            "              .onDescendantOfAny(",
            "                  \"java.lang.Class\",",
            "                  \"java.lang.String\"));",
            "}")
        .doTest();
  }
}
