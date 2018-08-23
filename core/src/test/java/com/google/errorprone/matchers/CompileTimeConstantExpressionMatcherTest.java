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

package com.google.errorprone.matchers;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.common.truth.Truth;
import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.VisitorState;
import com.google.errorprone.annotations.CompileTimeConstant;
import com.google.errorprone.scanner.Scanner;
import com.google.errorprone.scanner.ScannerSupplier;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.tools.javac.main.Main.Result;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link CompileTimeConstantExpressionMatcher}Test */
@RunWith(JUnit4.class)
public class CompileTimeConstantExpressionMatcherTest {

  @Test
  public void testMatches_matchesLiteralsAndStaticFinals() {
    String[] lines = {
      "package test;",
      "import com.google.errorprone.annotations.CompileTimeConstant;",
      "public class CompileTimeConstantExpressionMatcherTestCase {",
      "  private final String final_string = \"bap\";",
      "  private final int final_int = 29;",
      "  private static final int static_final_int = 29;",
      "  public void m() { ",
      "    String s1; s1 = \"boop\"; s1 = \"boop\" + final_string;",
      "    int int2; int2 = 42;",
      "    Integer int3; int3 = 42 * final_int; int3 = 12 - static_final_int;",
      "    boolean bool4; bool4 = false;",
      "  }",
      "}"
    };

    Map<String, Boolean> expectedMatches = new HashMap<String, Boolean>();
    expectedMatches.put("s1", true);
    expectedMatches.put("int2", true);
    expectedMatches.put("int3", true);
    expectedMatches.put("bool4", true);
    assertCompilerMatchesOnAssignment(expectedMatches, lines);
  }

  @Test
  public void testMatches_nullLiteral() {
    String[] lines = {
      "package test;",
      "import com.google.errorprone.annotations.CompileTimeConstant;",
      "public class CompileTimeConstantExpressionMatcherTestCase {",
      "  private static final String static_final_string = null;",
      "  public void m() { ",
      "    String s1; s1 = null;",
      "    String s2; s2 = static_final_string;",
      "  }",
      "}"
    };
    Map<String, Boolean> expectedMatches = new HashMap<String, Boolean>();
    expectedMatches.put("s1", true);
    // Even though s2 has the compile-time constant value "null", it's not
    // a literal.  I don't know how to distinguish this, but I doubt this is
    // an important use case.
    expectedMatches.put("s2", false);
    assertCompilerMatchesOnAssignment(expectedMatches, lines);
  }

  @Test
  public void testMatches_doesNotMatchNonLiterals() {
    String[] lines = {
      "package test;",
      "import com.google.errorprone.annotations.CompileTimeConstant;",
      "public class CompileTimeConstantExpressionMatcherTestCase {",
      "  private final int nonfinal_int;",
      "  public CompileTimeConstantExpressionMatcherTestCase(int i) { ",
      "    nonfinal_int = i;",
      "  }",
      "  public void m(String s) { ",
      "    String s1; s1 = s;",
      "    int int2; int2 = s.length();",
      "    Integer int3; int3 = nonfinal_int; int3 = 14 * nonfinal_int;",
      "    boolean bool4; bool4 = false;",
      "  }",
      "}"
    };
    Map<String, Boolean> expectedMatches = new HashMap<String, Boolean>();
    expectedMatches.put("s1", false);
    expectedMatches.put("int2", false);
    expectedMatches.put("int3", false);
    assertCompilerMatchesOnAssignment(expectedMatches, lines);
  }

  @Test
  public void testMatches_finalCompileTimeConstantMethodParameters() {
    String[] lines = {
      "package test;",
      "import com.google.errorprone.annotations.CompileTimeConstant;",
      "public class CompileTimeConstantExpressionMatcherTestCase {",
      "  public void m1(final @CompileTimeConstant String s) { ",
      "    String s1; s1 = s;",
      "  }",
      "  public void m2(@CompileTimeConstant String s) { ",
      "    s = null;",
      "    String s2; s2 = s;",
      "  }",
      "  public void m3(final String s) { ",
      "    String s3; s3 = s;",
      "  }",
      "  public void m4(@CompileTimeConstant String s) { ",
      "    String s4; s4 = s;",
      "  }",
      "}"
    };
    Map<String, Boolean> expectedMatches = new HashMap<String, Boolean>();
    expectedMatches.put("s1", true);
    expectedMatches.put("s2", false);
    expectedMatches.put("s3", false);
    expectedMatches.put("s4", true);
    assertCompilerMatchesOnAssignment(expectedMatches, lines);
  }

  @Test
  public void testMatches_finalCompileTimeConstantConstructorParameters() {
    String[] lines = {
      "package test;",
      "import com.google.errorprone.annotations.CompileTimeConstant;",
      "public class CompileTimeConstantExpressionMatcherTestCase {",
      "  public CompileTimeConstantExpressionMatcherTestCase(",
      "      final @CompileTimeConstant String s) { ",
      "    String s1; s1 = s;",
      "  }",
      "  public CompileTimeConstantExpressionMatcherTestCase(",
      "      @CompileTimeConstant String s, int foo) { ",
      "    s = null;",
      "    String s2; s2 = s;",
      "  }",
      "  public CompileTimeConstantExpressionMatcherTestCase(",
      "      final String s, boolean foo) { ",
      "    String s3; s3 = s;",
      "  }",
      "  public CompileTimeConstantExpressionMatcherTestCase(",
      "      @CompileTimeConstant String s, long foo) { ",
      "    String s4; s4 = s;",
      "  }",
      "}"
    };
    Map<String, Boolean> expectedMatches = new HashMap<String, Boolean>();
    expectedMatches.put("s1", true);
    expectedMatches.put("s2", false);
    expectedMatches.put("s3", false);
    expectedMatches.put("s4", true);
    assertCompilerMatchesOnAssignment(expectedMatches, lines);
  }

  // TODO(xtof): We'd like to eventually support other cases, but I first need
  // to determine with confidence that the checker can ensure all initializations
  // and assignments to such variables are compile-time-constant.
  // For now, the annotation's target is restricted to ElementType.PARAMETER.
  @Test
  public void testCompileTimeConstantAnnotationOnlyAllowedOnParameter() {
    Truth.assertThat(CompileTimeConstant.class.getAnnotation(Target.class).value())
        .isEqualTo(new ElementType[] {ElementType.PARAMETER});
  }

  @Test
  public void conditionalExpression() {
    String[] lines = {
      "package test;",
      "abstract class CompileTimeConstantExpressionMatcherTestCase {",
      "  abstract boolean g();",
      "  public void m(boolean flag) { ",
      "    boolean bool1; bool1 = flag ? true : g();",
      "    boolean bool2; bool2 = flag ? g() : false;",
      "    boolean bool3; bool3 = flag ? true : false;",
      "  }",
      "}"
    };

    Map<String, Boolean> expectedMatches = new HashMap<String, Boolean>();
    expectedMatches.put("bool1", false);
    expectedMatches.put("bool2", false);
    expectedMatches.put("bool3", true);
    assertCompilerMatchesOnAssignment(expectedMatches, lines);
  }

  // Helper methods.
  private void assertCompilerMatchesOnAssignment(
      final Map<String, Boolean> expectedMatches, String... lines) {
    final Matcher<ExpressionTree> matcher = new CompileTimeConstantExpressionMatcher();
    final Scanner scanner =
        new Scanner() {
          @Override
          public Void visitAssignment(AssignmentTree t, VisitorState state) {
            ExpressionTree lhs = t.getVariable();
            if (expectedMatches.containsKey(lhs.toString())) {
              boolean matches = matcher.matches(t.getExpression(), state);
              if (expectedMatches.get(lhs.toString())) {
                assertTrue("Matcher should match expression" + t.getExpression(), matches);
              } else {
                assertFalse("Matcher should not match expression" + t.getExpression(), matches);
              }
            }
            return super.visitAssignment(t, state);
          }
        };

    CompilationTestHelper.newInstance(ScannerSupplier.fromScanner(scanner), getClass())
        .expectResult(Result.OK)
        .addSourceLines("test/CompileTimeConstantExpressionMatcherTestCase.java", lines)
        .doTest();
  }
}
