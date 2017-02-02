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
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.JUnitMatchers.containsTestMethod;
import static com.google.errorprone.matchers.JUnitMatchers.hasJUnitAnnotation;
import static com.google.errorprone.matchers.JUnitMatchers.isJunit3TestCase;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.enclosingClass;
import static com.google.errorprone.matchers.Matchers.hasModifier;
import static com.google.errorprone.matchers.Matchers.methodHasParameters;
import static com.google.errorprone.matchers.Matchers.methodReturns;
import static com.google.errorprone.matchers.Matchers.not;
import static com.google.errorprone.suppliers.Suppliers.VOID_TYPE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.JUnitMatchers.JUnit4TestClassMatcher;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.MethodTree;
import com.sun.tools.javac.util.Options;
import java.util.List;
import javax.lang.model.element.Modifier;

/** @author eaftan@google.com (Eddie Aftandilian) */
@BugPattern(
  name = "JUnit4TestNotRun",
  summary = "Test method will not be run; please add @Test annotation",
  explanation =
      "Unlike in JUnit 3, JUnit 4 tests will not be run unless annotated with @Test. "
          + "The test method that triggered this error looks like it was meant to be a test, but "
          + "was not so annotated, so it will not be run. If you intend for this test method not "
          + "to run, please add both an @Test and an @Ignore annotation to make it clear that you "
          + "are purposely disabling it. If this is a helper method and not a test, consider "
          + "reducing its visibility to non-public, if possible.",
  category = JUNIT,
  severity = ERROR
)
public class JUnit4TestNotRun extends BugChecker implements MethodTreeMatcher {

  private static final String JUNIT4_TEST_ANNOTATION = "org.junit.Test";
  private static final JUnit4TestClassMatcher isJUnit4TestClass = new JUnit4TestClassMatcher();


  /**
   * Looks for methods that are structured like tests but aren't run. Matches public, void, no-param
   * methods in JUnit4 test classes that aren't annotated with any JUnit4 annotations *
   */
  private static final Matcher<MethodTree> POSSIBLE_TEST_METHOD =
      allOf(
          hasModifier(PUBLIC),
          methodReturns(VOID_TYPE),
          methodHasParameters(),
          not(hasJUnitAnnotation),
          enclosingClass(isJUnit4TestClass));

  /**
   * Matches if:
   *
   * <ol>
   *   <li>The method is public, void, and has no parameters,
   *   <li>The method is not annotated with {@code @Test}, {@code @Before}, {@code @After},
   *       {@code @BeforeClass}, or {@code @AfterClass},
   *   <li>The enclosing class has an {@code @RunWith} annotation and does not extend TestCase. This
   *       marks that the test is intended to run with JUnit 4, and
   *   <li> Either:
   *       <ol type="a">
   *         <li>The method body contains a method call with a name that contains "assert",
   *             "verify", "check", "fail", or "expect".
   *       </ol>
   *
   * </ol>
   */
  @Override
  public Description matchMethod(MethodTree methodTree, VisitorState state) {
    if (!POSSIBLE_TEST_METHOD.matches(methodTree, state)) {
      return NO_MATCH;
    }

    // Method appears to be a JUnit 3 test case (name prefixed with "test"), probably a test.
    if (isJunit3TestCase.matches(methodTree, state)) {
      return describeMatch(methodTree, prefixMethodWithTestAnnotation(methodTree, state));
    }

    // TODO(b/34062183): Remove check for flag once cleanup complete.
    if (Options.instance(state.context).getBoolean("expandedTestNotRunHeuristic")) {

      // Method is annotated, probably not a test.
      List<? extends AnnotationTree> annotations = methodTree.getModifiers().getAnnotations();
      if (annotations != null && !annotations.isEmpty()) {
        return NO_MATCH;
      }

      // Method non-static and contains call(s) to testing method, probably a test.
      if (not(hasModifier(STATIC)).matches(methodTree, state) && containsTestMethod(methodTree)) {
        return describeMatch(methodTree, prefixMethodWithTestAnnotation(methodTree, state));
      }
    }
    return NO_MATCH;
  }

  /*
   * Add the @Test annotation.  If the method is static, also make the method non-static.
   */
  private static Fix prefixMethodWithTestAnnotation(MethodTree methodTree, VisitorState state) {
    if (Matchers.<MethodTree>hasModifier(Modifier.STATIC).matches(methodTree, state)) {
      CharSequence methodSource = state.getSourceForNode(methodTree);
      if (methodSource != null) {
        String methodString = "@Test\n" + methodSource.toString().replaceFirst(" static ", " ");
        return SuggestedFix.builder()
            .addImport(JUNIT4_TEST_ANNOTATION)
            .replace(methodTree, methodString)
            .build();
      }
    }
    return SuggestedFix.builder()
        .addImport(JUNIT4_TEST_ANNOTATION)
        .prefixWith(methodTree, "@Test\n")
        .build();
  }
}
