/*
 * Copyright 2012 The Error Prone Authors.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.common.base.Function;
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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** @author eaftan@google.com (Eddie Aftandilian) */
@RunWith(JUnit4.class)
public class PreconditionsCheckNotNullPrimitiveTest extends CompilerBasedAbstractTest {
  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper =
        CompilationTestHelper.newInstance(PreconditionsCheckNotNullPrimitive.class, getClass());
  }

  @Test
  public void testPositiveCases() {
    compilationHelper
        .addSourceFile("PreconditionsCheckNotNullPrimitivePositiveCases.java")
        .doTest();
  }

  @Test
  public void testNegativeCase1() {
    compilationHelper
        .addSourceFile("PreconditionsCheckNotNullPrimitiveNegativeCases.java")
        .doTest();
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
        return new TestScanner(builder.build());
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

    private void assertMatch(ExpressionTree node, List<String> expected) {
      List<IdentifierTree> uses = PreconditionsCheckNotNullPrimitive.getVariableUses(node);
      assertEquals(
          "variables used in " + node,
          expected,
          Lists.transform(
              uses,
              new Function<IdentifierTree, String>() {
                @Override
                public String apply(IdentifierTree ident) {
                  return ident.toString();
                }
              }));
    }

    public void assertFoundAll() {
      for (Map.Entry<String, Match> entry : matches.entrySet()) {
        assertTrue("found " + entry.getKey(), entry.getValue().found);
      }
    }
  }
}
