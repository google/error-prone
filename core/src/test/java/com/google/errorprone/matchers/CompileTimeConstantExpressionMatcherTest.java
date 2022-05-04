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

import static com.google.common.truth.Truth.assertThat;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.util.ASTHelpers.getSymbol;

import com.google.errorprone.BugPattern;
import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.VisitorState;
import com.google.errorprone.annotations.CompileTimeConstant;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.sun.source.tree.VariableTree;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import javax.lang.model.element.ElementKind;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link CompileTimeConstantExpressionMatcher}Test */
@RunWith(JUnit4.class)
public class CompileTimeConstantExpressionMatcherTest {
  private final CompilationTestHelper testHelper =
      CompilationTestHelper.newInstance(
          CompileTimeConstantExpressionMatcherTester.class, getClass());

  @Test
  public void matchesLiteralsAndStaticFinals() {
    testHelper
        .addSourceLines(
            "Test.java",
            "public class Test {",
            "  private final String final_string = \"bap\";",
            "  private final int final_int = 29;",
            "  private static final int static_final_int = 29;",
            "  public void m() { ",
            "    // BUG: Diagnostic contains: true",
            "    String s1 = \"boop\";",
            "    // BUG: Diagnostic contains: true",
            "    String s2 = \"boop\" + final_string;",
            "    // BUG: Diagnostic contains: true",
            "    int int2 = 42;",
            "    // BUG: Diagnostic contains: true",
            "    Integer int3 = 42 * final_int;",
            "    // BUG: Diagnostic contains: true",
            "    Integer int4 = 12 - static_final_int;",
            "    // BUG: Diagnostic contains: true",
            "    boolean bool4 = false;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void nullLiteral() {
    testHelper
        .addSourceLines(
            "Test.java",
            "public class Test {",
            "  private static final String static_final_string = null;",
            "  public void m() { ",
            "    // BUG: Diagnostic contains: true",
            "    String s1 = null;",
            "    // BUG: Diagnostic contains: false",
            "    String s2 = static_final_string;",
            "    // BUG: Diagnostic contains: true",
            "    String s3 = (String) null;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void doesNotMatchNonLiterals() {
    testHelper
        .addSourceLines(
            "Test.java",
            "package test;",
            "public class Test {",
            "  private final int nonfinal_int;",
            "  public Test(int i) { ",
            "    nonfinal_int = i;",
            "  }",
            "  public void m(String s) { ",
            "    // BUG: Diagnostic contains: false",
            "    String s1 = s;",
            "    // BUG: Diagnostic contains: false",
            "    int int2 = s.length();",
            "    // BUG: Diagnostic contains: false",
            "    Integer int3 = nonfinal_int;",
            "    // BUG: Diagnostic contains: false",
            "    Integer int4 = 14 * nonfinal_int;",
            "    // BUG: Diagnostic contains: true",
            "    boolean bool4 = false;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void finalCompileTimeConstantMethodParameters() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.CompileTimeConstant;",
            "public class Test {",
            "  public void m1(final @CompileTimeConstant String s) {",
            "    // BUG: Diagnostic contains: true",
            "    String s1 = s;",
            "  }",
            "  public void m2(@CompileTimeConstant String s) {",
            "    s = null;",
            "    // BUG: Diagnostic contains: false",
            "    String s2 = s;",
            "  }",
            "  public void m3(final String s) {",
            "    // BUG: Diagnostic contains: false",
            "    String s3 = s;",
            "  }",
            "  public void m4(@CompileTimeConstant String s) {",
            "    // BUG: Diagnostic contains: true",
            "    String s4 = s;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void finalCompileTimeConstantConstructorParameters() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.CompileTimeConstant;",
            "public class Test {",
            "  public Test(final @CompileTimeConstant String s) {",
            "    // BUG: Diagnostic contains: true",
            "    String s1 = s;",
            "  }",
            "  public Test(@CompileTimeConstant String s, int foo) {",
            "    s = null;",
            "    // BUG: Diagnostic contains: false",
            "    String s2 = s;",
            "  }",
            "  public Test(final String s, boolean foo) {",
            "    // BUG: Diagnostic contains: false",
            "    String s3 = s;",
            "  }",
            "  public Test(@CompileTimeConstant String s, long foo) {",
            "    // BUG: Diagnostic contains: true",
            "    String s4 = s;",
            "  }",
            "}")
        .doTest();
  }

  // TODO(xtof): We'd like to eventually support other cases, but I first need
  // to determine with confidence that the checker can ensure all initializations
  // and assignments to such variables are compile-time-constant.
  // For now, the annotation's target is restricted to ElementType.PARAMETER.
  @Test
  public void testCompileTimeConstantAnnotationOnlyAllowedOnParameterOrField() {
    assertThat(CompileTimeConstant.class.getAnnotation(Target.class).value())
        .isEqualTo(new ElementType[] {ElementType.PARAMETER, ElementType.FIELD});
  }

  @Test
  public void conditionalExpression() {
    testHelper
        .addSourceLines(
            "Test.java",
            "abstract class Test {",
            "  abstract boolean g();",
            "  public void m(boolean flag) {",
            "    // BUG: Diagnostic contains: false",
            "    boolean bool1 = flag ? true : g();",
            "    // BUG: Diagnostic contains: false",
            "    boolean bool2 = flag ? g() : false;",
            "    // BUG: Diagnostic contains: true",
            "    boolean bool3 = flag ? true : false;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void parentheses() {
    testHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.CompileTimeConstant;",
            "abstract class Test {",
            "  public void m(@CompileTimeConstant String ctc) {",
            "    // BUG: Diagnostic contains: true",
            "    String a = (ctc);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void concatenatedStrings() {
    testHelper
        .addSourceLines(
            "Test.java",
            "package test;",
            "import com.google.errorprone.annotations.CompileTimeConstant;",
            "abstract class Test {",
            "  public void m(@CompileTimeConstant String ctc, String nonCtc) {",
            "    // BUG: Diagnostic contains: true",
            "    String a = \"foo\" + ctc;",
            "    // BUG: Diagnostic contains: true",
            "    String b = ctc + \"foo\";",
            "    // BUG: Diagnostic contains: false",
            "    String c = nonCtc + \"foo\";",
            "    // BUG: Diagnostic contains: false",
            "    String d = nonCtc + ctc;",
            "    // BUG: Diagnostic contains: true",
            "    String e = \"foo\" + (ctc == null ? \"\" : \"\");",
            "    // BUG: Diagnostic contains: true",
            "    String f = \"foo\" + 1;",
            "    // BUG: Diagnostic contains: true",
            "    String g = \"foo\" + 3.14;",
            "  }",
            "}")
        .doTest();
  }

  /** A test-only bugpattern for testing {@link CompileTimeConstantExpressionMatcher}. */
  @BugPattern(severity = WARNING, summary = "")
  public static final class CompileTimeConstantExpressionMatcherTester extends BugChecker
      implements VariableTreeMatcher {
    @Override
    public Description matchVariable(VariableTree tree, VisitorState state) {
      if (!getSymbol(tree).getKind().equals(ElementKind.LOCAL_VARIABLE)) {
        return Description.NO_MATCH;
      }
      return buildDescription(tree)
          .setMessage(
              CompileTimeConstantExpressionMatcher.instance().matches(tree.getInitializer(), state)
                  ? "true"
                  : "false")
          .build();
    }
  }
}
