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

package com.google.errorprone.bugpatterns.nullness;

import static com.google.common.truth.Truth.assertWithMessage;

import com.google.common.base.Functions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.CompilerBasedAbstractTest;
import com.google.errorprone.scanner.Scanner;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Unit tests for {@link UnnecessaryCheckNotNull} check.
 *
 * @author bhagwani@google.com (Sumit Bhagwani)
 */
@RunWith(JUnit4.class)
public class UnnecessaryCheckNotNullTest extends CompilerBasedAbstractTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(UnnecessaryCheckNotNull.class, getClass());

  @Test
  public void positive_newClass() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.base.Preconditions;",
            "import com.google.common.base.Verify;",
            "import java.util.Objects;",
            "class Test {",
            " void positive_checkNotNull() {",
            "// BUG: Diagnostic contains: UnnecessaryCheckNotNull",
            "Preconditions.checkNotNull(new String(\"\"));",
            "// BUG: Diagnostic contains: UnnecessaryCheckNotNull",
            "Preconditions.checkNotNull(new String(\"\"), new Object());",
            "// BUG: Diagnostic contains: UnnecessaryCheckNotNull",
            "Preconditions.checkNotNull(new String(\"\"), \"Message %s\", \"template\");",
            "// BUG: Diagnostic contains: UnnecessaryCheckNotNull",
            "String pa = Preconditions.checkNotNull(new String(\"\"));",
            "// BUG: Diagnostic contains: UnnecessaryCheckNotNull",
            "String pb = Preconditions.checkNotNull(new String(\"\"), new Object());",
            "// BUG: Diagnostic contains: UnnecessaryCheckNotNull",
            "String pc = Preconditions.checkNotNull(new String(\"\"), \"Message %s\","
                + " \"template\");",
            "}",
            " void positive_verifyNotNull() {",
            "// BUG: Diagnostic contains: UnnecessaryCheckNotNull",
            "Verify.verifyNotNull(new String(\"\"));",
            "// BUG: Diagnostic contains: UnnecessaryCheckNotNull",
            "Verify.verifyNotNull(new String(\"\"), \"Message\");",
            "// BUG: Diagnostic contains: UnnecessaryCheckNotNull",
            "Verify.verifyNotNull(new String(\"\"), \"Message %s\", \"template\");",
            "// BUG: Diagnostic contains: UnnecessaryCheckNotNull",
            "String va = Verify.verifyNotNull(new String(\"\"));",
            "// BUG: Diagnostic contains: UnnecessaryCheckNotNull",
            "String vb = Verify.verifyNotNull(new String(\"\"), \"Message\");",
            "// BUG: Diagnostic contains: UnnecessaryCheckNotNull",
            "String vc = Verify.verifyNotNull(new String(\"\"), \"Message %s\", \"template\");",
            "}",
            " void positive_requireNonNull() {",
            "// BUG: Diagnostic contains: UnnecessaryCheckNotNull",
            "Objects.requireNonNull(new String(\"\"));",
            "// BUG: Diagnostic contains: UnnecessaryCheckNotNull",
            "Objects.requireNonNull(new String(\"\"), \"Message\");",
            "// BUG: Diagnostic contains: UnnecessaryCheckNotNull",
            "String va = Objects.requireNonNull(new String(\"\"));",
            "// BUG: Diagnostic contains: UnnecessaryCheckNotNull",
            "String vb = Objects.requireNonNull(new String(\"\"), \"Message\");",
            "}",
            "}")
        .doTest();
  }

  @Test
  public void positive_newArray() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.base.Preconditions;",
            "import com.google.common.base.Verify;",
            "import java.util.Objects;",
            "class Test {",
            " void positive_checkNotNull() {",
            "// BUG: Diagnostic contains: UnnecessaryCheckNotNull",
            "Preconditions.checkNotNull(new int[3]);",
            "// BUG: Diagnostic contains: UnnecessaryCheckNotNull",
            "Preconditions.checkNotNull(new int[]{1, 2, 3});",
            "// BUG: Diagnostic contains: UnnecessaryCheckNotNull",
            "Preconditions.checkNotNull(new int[5][2]);",
            "}",
            " void positive_verifyNotNull() {",
            "// BUG: Diagnostic contains: UnnecessaryCheckNotNull",
            "Verify.verifyNotNull(new int[3]);",
            "// BUG: Diagnostic contains: UnnecessaryCheckNotNull",
            "Verify.verifyNotNull(new int[]{1, 2, 3});",
            "// BUG: Diagnostic contains: UnnecessaryCheckNotNull",
            "Verify.verifyNotNull(new int[5][2]);",
            "}",
            " void positive_requireNonNull() {",
            "// BUG: Diagnostic contains: UnnecessaryCheckNotNull",
            "Objects.requireNonNull(new int[3]);",
            "// BUG: Diagnostic contains: UnnecessaryCheckNotNull",
            "Objects.requireNonNull(new int[]{1, 2, 3});",
            "// BUG: Diagnostic contains: UnnecessaryCheckNotNull",
            "Objects.requireNonNull(new int[5][2]);",
            "}",
            "}")
        .doTest();
  }

  @Test
  public void negative() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.common.base.Preconditions;",
            "import com.google.common.base.Verify;",
            "import java.util.Objects;",
            "class Test {",
            "void negative() {",
            "Preconditions.checkNotNull(new String(\"\").substring(0, 0));",
            "Verify.verifyNotNull(new String(\"\").substring(0, 0));",
            "Objects.requireNonNull(new String(\"\").substring(0, 0));",
            "}",
            "}")
        .doTest();
  }

  @Test
  public void testPositiveCase() {
    compilationHelper.addSourceFile("UnnecessaryCheckNotNullPositiveCase.java").doTest();
  }

  @Test
  public void testNegativeCase() {
    compilationHelper.addSourceFile("UnnecessaryCheckNotNullNegativeCase.java").doTest();
  }

  @Test
  public void testPrimitivePositiveCases() {
    compilationHelper.addSourceFile("UnnecessaryCheckNotNullPrimitivePositiveCases.java").doTest();
  }

  @Test
  public void testPrimitiveNegativeCases() {
    compilationHelper.addSourceFile("UnnecessaryCheckNotNullPrimitiveNegativeCases.java").doTest();
  }

  @Test
  public void testGetVariableUses() {
    writeFile("A.java", "public class A {", "  public String b;", "  void foo() {}", "}");
    writeFile(
        "B.java",
        "public class B {",
        "  A my;",
        "  B bar() { return null; }",
        "  void foo(String x, A a) {",
        "    x.trim().intern();",
        "    a.b.trim().intern();",
        "    this.my.foo();",
        "    my.foo();",
        "    this.bar();",
        "    String.valueOf(0);",
        "    java.lang.String.valueOf(1);",
        "    bar().bar();",
        "    System.out.println();",
        "    a.b.indexOf(x.substring(1));",
        "  }",
        "}");

    TestScanner scanner =
        new TestScanner.Builder()
            .add("x.trim().intern()", "x")
            .add("a.b.trim().intern()", "a")
            .add("this.my.foo()", "this")
            .add("my.foo()", "my")
            .add("this.bar()", "this")
            .add("String.valueOf(0)")
            .add("java.lang.String.valueOf(1)")
            .add("bar().bar()")
            .add("System.out.println()")
            .add("a.b.indexOf(x.substring(1))", "a", "x")
            .build();
    assertCompiles(scanner);
    scanner.assertFoundAll();
  }

  // TODO(mdempsky): Make this more reusable.
  private static class TestScanner extends Scanner {
    private static class Match {
      private final ImmutableList<String> expected;
      private boolean found = false;

      private Match(String... expected) {
        this.expected = ImmutableList.copyOf(expected);
      }
    }

    private static class Builder {
      private final ImmutableMap.Builder<String, Match> builder = ImmutableMap.builder();

      public Builder add(String expression, String... expected) {
        builder.put(expression, new Match(expected));
        return this;
      }

      public TestScanner build() {
        return new TestScanner(builder.buildOrThrow());
      }
    }

    private final ImmutableMap<String, Match> matches;

    private TestScanner(ImmutableMap<String, Match> matches) {
      this.matches = matches;
    }

    @Override
    public Void visitExpressionStatement(ExpressionStatementTree node, VisitorState state) {
      ExpressionTree expression = node.getExpression();
      Match match = matches.get(expression.toString());
      if (match != null) {
        assertMatch(expression, match.expected);
        match.found = true;
      }
      return super.visitExpressionStatement(node, state);
    }

    private static void assertMatch(ExpressionTree node, List<String> expected) {
      List<IdentifierTree> uses = UnnecessaryCheckNotNull.getVariableUses(node);
      assertWithMessage("variables used in " + node)
          .that(Lists.transform(uses, Functions.toStringFunction()))
          .isEqualTo(expected);
    }

    public void assertFoundAll() {
      for (Map.Entry<String, Match> entry : matches.entrySet()) {
        assertWithMessage("found " + entry.getKey()).that(entry.getValue().found).isTrue();
      }
    }
  }
}
