/*
 * Copyright 2013 Google Inc. All Rights Reserved.
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

import static com.google.errorprone.BugPattern.Category.JUNIT;
import static com.google.errorprone.BugPattern.MaturityLevel.MATURE;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.JUnitMatchers.hasJUnitAnnotation;
import static com.google.errorprone.matchers.JUnitMatchers.isJunit3TestCase;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.enclosingClass;
import static com.google.errorprone.matchers.Matchers.not;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.JUnitMatchers.JUnit4TestClassMatcher;
import com.google.errorprone.matchers.Matchers;

import com.sun.source.tree.MethodTree;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;

import javax.lang.model.element.Modifier;

/**
 * @author eaftan@google.com (Eddie Aftandilian)
 */
@BugPattern(name = "JUnit4TestNotRun",
    summary = "Test method will not be run; please add @Test annotation",
    explanation = "JUnit 3 required that test methods be named in a special way to be run as " +
        "part of a test case. JUnit 4 requires that test methods be annotated with @Test. " +
        "The test method that triggered this error is named like a JUnit 3 test, but is in a " +
        "JUnit 4 test class.  Thus, it will not be run unless you annotate it with @Test.\n\n" +
        "If you intend for this test method not to run, please add both an @Test and an " +
        "@Ignore annotation to make it clear that you are purposely disabling it.",
    category = JUNIT, maturity = MATURE, severity = ERROR)
public class JUnit4TestNotRun extends BugChecker implements MethodTreeMatcher {

  private static final String JUNIT4_TEST_ANNOTATION = "org.junit.Test";
  private static final JUnit4TestClassMatcher isJUnit4TestClass = new JUnit4TestClassMatcher();

  /**
   * Matches if:
   * 1) The method appears to be a JUnit 3 test case.
   * 2) The method is not annotated with @Test, @Before, @After, @BeforeClass, or @AfterClass.
   * 3) The enclosing class has an @RunWith annotation and does not extend TestCase. This marks
   *    that the test is intended to run with JUnit 4.
   */
  @Override
  public Description matchMethod(MethodTree methodTree, VisitorState state) {
    boolean matches = allOf(
        isJunit3TestCase,
        not(hasJUnitAnnotation),
        enclosingClass(isJUnit4TestClass))
        .matches(methodTree, state);
    if (!matches) {
      return Description.NO_MATCH;
    }

    /*
     * Add the @Test annotation.  If the method is static, also make the method non-static.
     *
     * TODO(user): The static case here relies on having tree end positions available.  Come up
     * with a better way of doing this that doesn't require tree end positions.  Maybe we should
     * just not provide suggested fixes for these few cases when the javac infrastructure gets in
     * the way.
     */
    if (Matchers.<MethodTree>hasModifier(Modifier.STATIC).matches(methodTree, state)) {
      CharSequence methodSource = state.getSourceForNode((JCMethodDecl) methodTree);
      if (methodSource != null) {
        String methodString = "@Test\n" + methodSource.toString().replaceFirst(" static ", " ");
        Fix fix = SuggestedFix.builder()
            .addImport(JUNIT4_TEST_ANNOTATION)
            .replace(methodTree, methodString)
            .build();
        return describeMatch(methodTree, fix);
      }
    }
    Fix fix = SuggestedFix.builder()
        .addImport(JUNIT4_TEST_ANNOTATION)
        .prefixWith(methodTree, "@Test\n")
        .build();
    return describeMatch(methodTree, fix);
  }
}
