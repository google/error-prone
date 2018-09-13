/*
 * Copyright 2017 The Error Prone Authors.
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

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.Category;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.sun.source.tree.ClassTree;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** @author epmjohnston@google.com (Emily Johnston) */
@RunWith(JUnit4.class)
public final class JUnitMatchersTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(JUnitVersionMatcher.class, getClass());

  @Test
  public void runWithAnnotationOnClass_shouldBeJUnit4() {
    compilationHelper
        .addSourceLines(
            "RunWithAnnotationOnClass.java",
            "import org.junit.runner.RunWith;",
            "import org.junit.runners.JUnit4;",
            "@RunWith(JUnit4.class)",
            "// BUG: Diagnostic contains: Version:JUnit4",
            "public class RunWithAnnotationOnClass {}")
        .doTest();
  }

  @Test
  public void testAnnotationOnMethod_shouldBeJUnit4() {
    compilationHelper
        .addSourceLines(
            "TestAnnotationOnMethod.java",
            "import org.junit.Test;",
            "// BUG: Diagnostic contains: Version:JUnit4",
            "public class TestAnnotationOnMethod {",
            "  @Test",
            "  public void someTest() {}",
            "}")
        .doTest();
  }

  @Test
  public void beforeAfterAnnotations_notRecognized() {
    compilationHelper
        .addSourceLines(
            "BeforeAnnotationOnMethod.java",
            "import org.junit.Before;",
            "public class BeforeAnnotationOnMethod {",
            "  @Before",
            "  public void someTest() {}",
            "}")
        .addSourceLines(
            "BeforeClassAnnotationOnMethod.java",
            "import org.junit.BeforeClass;",
            "public class BeforeClassAnnotationOnMethod {",
            "  @BeforeClass",
            "  public void someTest() {}",
            "}")
        .addSourceLines(
            "AfterAnnotationOnMethod.java",
            "import org.junit.After;",
            "public class AfterAnnotationOnMethod {",
            "  @After",
            "  public void someTest() {}",
            "}")
        .addSourceLines(
            "AfterClassAnnotationOnMethod.java",
            "import org.junit.AfterClass;",
            "public class AfterClassAnnotationOnMethod {",
            "  @AfterClass",
            "  public void someTest() {}",
            "}")
        .doTest();
  }

  @Test
  public void ignoreAnnotation_notRecognized() {
    compilationHelper
        .addSourceLines(
            "TestIgnoreAnnotation.java",
            "import org.junit.Ignore;",
            "public class TestIgnoreAnnotation {",
            "  @Ignore",
            "  public void someTest() {}",
            "}")
        .doTest();
  }

  @Test
  public void ignoreClassAnnotation_notRecognized() {
    compilationHelper
        .addSourceLines(
            "TestIgnoreAnnotation.java",
            "import org.junit.Ignore;",
            // Uncomment this line if we decide to recognize @Ignored classes as JUnit4
            // "// BUG: Diagnostic contains: Version:JUnit4",
            "@Ignore public class TestIgnoreAnnotation {}")
        .doTest();
  }

  @Test
  public void ruleAnnotation_notRecognized() {
    compilationHelper
        .addSourceLines(
            "MyRule.java",
            "import org.junit.rules.TestRule;",
            "import org.junit.runners.model.Statement;",
            "import org.junit.runner.Description;",
            "public class MyRule implements TestRule {",
            "  public Statement apply(Statement s, Description d) { return null; }",
            "}")
        .addSourceLines(
            "TestRuleAnnotation.java",
            "import org.junit.Rule;",
            "public class TestRuleAnnotation {",
            "  @Rule public final MyRule r = new MyRule();",
            "}")
        .doTest();
  }

  @Test
  public void testCaseDescendant_shouldBeJUnit3() {
    compilationHelper
        .addSourceLines(
            "TestCaseDescendant.java",
            "import junit.framework.TestCase;",
            "// BUG: Diagnostic contains: Version:JUnit3",
            "public class TestCaseDescendant extends TestCase {}")
        .doTest();
  }

  @Test
  public void ambiguous_noRecognizedVersion() {
    compilationHelper
        .addSourceLines(
            "AmbiguousRunWith.java",
            "import junit.framework.TestCase;",
            "import org.junit.runner.RunWith;",
            "import org.junit.runners.JUnit4;",
            "import org.junit.Test;",
            "@RunWith(JUnit4.class)",
            "// BUG: Diagnostic contains: Version:Both",
            "public class AmbiguousRunWith extends TestCase {",
            "  public void someTest() {}",
            "}")
        .addSourceLines(
            "AmbiguousTest.java",
            "import junit.framework.TestCase;",
            "import org.junit.runner.RunWith;",
            "import org.junit.runners.JUnit4;",
            "import org.junit.Test;",
            "// BUG: Diagnostic contains: Version:Both",
            "public class AmbiguousTest extends TestCase {",
            "  @Test",
            "  public void someTest() {}",
            "}")
        .doTest();
  }

  /** Helper class to surface which version of JUnit a class looks like to Error Prone. */
  @BugPattern(
      name = "JUnitVersionMatcher",
      summary = "Matches on JUnit test classes, emits description with its JUnit version.",
      category = Category.ONE_OFF,
      severity = SeverityLevel.WARNING)
  public static class JUnitVersionMatcher extends BugChecker implements ClassTreeMatcher {

    @Override
    public Description matchClass(ClassTree tree, VisitorState state) {
      // As currently implemented, isJUnit3TestClass, isJUnit4TestClass, and isAmbiguousJUnitVersion
      // matches should be disjoint. This may change in the future, in which case the assertion
      // here should be changed to allow for multiple matches.
      List<String> versions = new ArrayList<>();
      if (JUnitMatchers.isJUnit3TestClass.matches(tree, state)) {
        versions.add("JUnit3");
      }
      if (JUnitMatchers.isJUnit4TestClass.matches(tree, state)) {
        versions.add("JUnit4");
      }
      if (JUnitMatchers.isAmbiguousJUnitVersion.matches(tree, state)) {
        versions.add("Both");
      }
      if (versions.isEmpty()) {
        return Description.NO_MATCH;
      }
      assertThat(versions).hasSize(1);
      return this.buildDescription(tree).setMessage("Version:" + versions.get(0)).build();
    }
  }
}
