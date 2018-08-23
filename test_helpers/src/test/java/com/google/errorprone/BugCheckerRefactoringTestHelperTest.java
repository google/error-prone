/*
 * Copyright 2015 The Error Prone Authors.
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

package com.google.errorprone;

import static com.google.common.truth.Truth.assertThat;
import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;
import static org.junit.Assert.fail;

import com.google.errorprone.BugCheckerRefactoringTestHelper.TestMode;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.AnnotationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.CompilationUnitTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.ReturnTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ReturnTree;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link BugCheckerRefactoringTestHelper}. */
@RunWith(JUnit4.class)
public class BugCheckerRefactoringTestHelperTest {

  private BugCheckerRefactoringTestHelper helper;

  @Before
  public void setUp() {
    helper = BugCheckerRefactoringTestHelper.newInstance(new ReturnNullRefactoring(), getClass());
  }

  @Test
  public void testNoMatch() {
    helper
        .addInputLines("in/Test.java", "public class Test {}")
        .addOutputLines("out/Test.java", "public class Test {}")
        .doTest();
  }

  @Test
  public void testReplace() {
    helper
        .addInputLines(
            "in/Test.java",
            "public class Test {",
            "  public Object foo() {",
            "    Integer i = 1 + 2;",
            "    return i;",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "public class Test {",
            "  public Object foo() {",
            "    Integer i = 1 + 2;",
            "  return null;",
            "  }",
            "}")
        .doTest();
  }

  @Test(expected = AssertionError.class)
  public void testReplaceFail() {
    helper
        .addInputLines(
            "in/Test.java",
            "public class Test {",
            "  public Object foo() {",
            "    Integer i = 1 + 2;",
            "    return i;",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "public class Test {",
            "  public Object foo() {",
            "    Integer i = 1 + 2;",
            "    return i;",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testReplaceTextMatch() {
    helper
        .addInputLines(
            "in/Test.java",
            "public class Test {",
            "  public Object foo() {",
            "    Integer i = 1 + 2;",
            "    return i;",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "public class Test {",
            "  public Object foo() {",
            "    Integer i = 1 + 2;",
            "    return null;",
            "  }",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void testIgnoreWhitespace() {
    helper
        .addInputLines(
            "in/Test.java",
            "public class Test {",
            "  public Object foo() { Integer i = 2 + 1; return i; }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "public class Test {",
            "  public Object foo() {",
            "    Integer i = 2 + 1;",
            "  return null;",
            "  }",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test(expected = AssertionError.class)
  public void testReplaceTextMatchFail() {
    helper
        .addInputLines(
            "in/Test.java",
            "public class Test {",
            "  public Object foo() {",
            "    Integer i = 1 + 2;",
            "    return i;",
            "  }",
            "}")
        .addOutputLines(
            "out/Test.java",
            "public class Test {",
            "  public Object foo() {",
            "    Integer i = 2 + 1;",
            "  return null;",
            "  }",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void compilationErrorFail() {
    try {
      helper
          .addInputLines("syntax_error.java", "public clazz Bar { ! this should fail }")
          .expectUnchanged()
          .doTest();
    } catch (AssertionError e) {
      assertThat(e.getMessage()).contains("compilation failed unexpectedly");
      return;
    }
    fail("compilation succeeded unexpectedly");
  }

  @Test
  public void testAnnotationFullName() {
    BugCheckerRefactoringTestHelper.newInstance(new RemoveAnnotationRefactoring(), getClass())
        .addInputLines("bar/Foo.java", "package bar;", "public @interface Foo {", "};")
        .expectUnchanged()
        .addInputLines("foo/Bar.java", "import bar.Foo;", "public @Foo class Bar {", "}")
        .addOutputLines("out/foo/Bar.java", "import bar.Foo;", "public  class Bar {", "}")
        .doTest(TestMode.TEXT_MATCH);
  }
  /** Mock {@link BugChecker} for testing only. */
  @BugPattern(
      name = "ReturnNullRefactoring",
      summary = "Mock refactoring that replaces all returns with 'return null;' statement.",
      explanation = "For test purposes only.",
      category = JDK,
      severity = SUGGESTION,
      providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION)
  public static class ReturnNullRefactoring extends BugChecker implements ReturnTreeMatcher {
    @Override
    public Description matchReturn(ReturnTree tree, VisitorState state) {
      return describeMatch(tree, SuggestedFix.replace(tree, "return null;"));
    }
  }
  /** Mock {@link BugChecker} for testing only. */
  @BugPattern(
      name = "RemoveAnnotationRefactoring",
      summary = "Mock refactoring that removes all annotations declared in package bar ",
      explanation = "For test purposes only.",
      category = JDK,
      severity = SUGGESTION,
      providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION)
  public static class RemoveAnnotationRefactoring extends BugChecker
      implements AnnotationTreeMatcher {

    @Override
    public Description matchAnnotation(AnnotationTree tree, VisitorState state) {
      if (ASTHelpers.getType(tree).asElement().toString().startsWith("bar.")) {
        return describeMatch(tree, SuggestedFix.replace(tree, ""));
      }
      return Description.NO_MATCH;
    }
  }

  @Test
  public void compilationError() {
    try {
      helper
          .addInputLines("Test.java", "public class Test extends NoSuch {}")
          .expectUnchanged()
          .doTest();
    } catch (AssertionError e) {
      assertThat(e.getMessage()).contains("error: cannot find symbol");
      return;
    }
    fail("compilation succeeded unexpectedly");
  }

  @Test
  public void staticLastImportOrder() {
    BugCheckerRefactoringTestHelper.newInstance(new ImportArrayList(), getClass())
        .setImportOrder("static-last")
        .addInputLines("pkg/A.java", "import static java.lang.Math.min;", "class A {", "}")
        .addOutputLines(
            "out/pkg/A.java",
            "import java.util.ArrayList;",
            "",
            "import static java.lang.Math.min;",
            "class A {",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  /** Mock {@link BugChecker} for testing only. */
  @BugPattern(
      name = "ImportArrayList",
      summary = "Mock refactoring that imports an ArrayList",
      explanation = "For test purposes only.",
      category = JDK,
      severity = SUGGESTION,
      providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION)
  public static class ImportArrayList extends BugChecker implements CompilationUnitTreeMatcher {

    @Override
    public Description matchCompilationUnit(CompilationUnitTree tree, VisitorState state) {
      SuggestedFix fix = SuggestedFix.builder().addImport("java.util.ArrayList").build();
      return buildDescription(tree).addFix(fix).build();
    }
  }
}
