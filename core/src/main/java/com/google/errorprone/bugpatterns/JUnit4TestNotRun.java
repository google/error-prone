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
import static com.google.errorprone.matchers.JUnitMatchers.isJUnit4TestClass;
import static com.google.errorprone.matchers.JUnitMatchers.isJunit3TestCase;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.enclosingClass;
import static com.google.errorprone.matchers.Matchers.hasModifier;
import static com.google.errorprone.matchers.Matchers.methodHasParameters;
import static com.google.errorprone.matchers.Matchers.methodReturns;
import static com.google.errorprone.matchers.Matchers.not;
import static com.google.errorprone.suppliers.Suppliers.VOID_TYPE;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import java.util.List;
import java.util.Optional;
import javax.lang.model.element.Modifier;

/** @author eaftan@google.com (Eddie Aftandilian) */
@BugPattern(
  name = "JUnit4TestNotRun",
  summary =
      "This looks like a test method but is not run; please add @Test or @Ignore, or, if this is a "
          + "helper method, reduce its visibility.",
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

  private static final String TEST_CLASS = "org.junit.Test";
  private static final String IGNORE_CLASS = "org.junit.Ignore";
  private static final String TEST_ANNOTATION = "@Test ";
  private static final String IGNORE_ANNOTATION = "@Ignore ";


  /**
   * Looks for methods that are structured like tests but aren't run. Matches public, void, no-param
   * methods in JUnit4 test classes that aren't annotated with any JUnit4 annotations.
   */
  private final Matcher<MethodTree> possibleTestMethod;


  public JUnit4TestNotRun() {
    possibleTestMethod =
        allOf(
            hasModifier(PUBLIC),
            methodReturns(VOID_TYPE),
            methodHasParameters(),
            not(hasJUnitAnnotation),
            enclosingClass(isJUnit4TestClass));
  }

  /**
   * Matches if:
   *
   * <ol>
   *   <li>The method is public, void, and has no parameters;
   *   <li>the method is not already annotated with {@code @Test}, {@code @Before}, {@code @After},
   *       {@code @BeforeClass}, or {@code @AfterClass};
   *   <li>the enclosing class appears to be intended to run in JUnit4, that is:
   *       <ol type="a">
   *         <li>it is non-abstract,
   *         <li>it does not extend JUnit3 {@code TestCase},
   *         <li>it has an {@code @RunWith} annotation or at least one other method annotated
   *             {@code @Test};
   *       </ol>
   *   <li>and, the method appears to be a test method, that is:
   *       <ol type="a">
   *         <li>or, the method body contains a method call with a name that contains "assert",
   *             "verify", "check", "fail", or "expect".
   *       </ol>
   * </ol>
   */
  @Override
  public Description matchMethod(MethodTree methodTree, VisitorState state) {
    if (!possibleTestMethod.matches(methodTree, state)) {
      return NO_MATCH;
    }

    // Method appears to be a JUnit 3 test case (name prefixed with "test"), probably a test.
    if (isJunit3TestCase.matches(methodTree, state)) {
      return describeFixes(methodTree, state);
    }


    // Method is annotated, probably not a test.
    List<? extends AnnotationTree> annotations = methodTree.getModifiers().getAnnotations();
    if (annotations != null && !annotations.isEmpty()) {
      return NO_MATCH;
    }

    // Method non-static and contains call(s) to testing method, probably a test,
    // unless it is called elsewhere in the class, in which case it is a helper method.
    if (not(hasModifier(STATIC)).matches(methodTree, state)
        && containsTestMethod(methodTree)
        && !calledElsewhere(methodTree, state)) {
      return describeFixes(methodTree, state);
    }
    return NO_MATCH;
  }

  /** Whether the given method is called elsewhere in the enclosing class. */
  private static boolean calledElsewhere(MethodTree methodTree, VisitorState state) {
    MethodSymbol methodSymbol = getSymbol(methodTree);
    if (methodSymbol == null) {
      return false;
    }
    return state
        .findEnclosing(ClassTree.class)
        .accept(
            new TreeScanner<Boolean, Void>() {
              @Override
              public Boolean visitMethodInvocation(MethodInvocationTree callTree, Void unused) {
                if (methodSymbol.equals(getSymbol(callTree.getMethodSelect()))) {
                  return true;
                }
                return super.visitMethodInvocation(callTree, unused);
              }

              @Override
              public Boolean reduce(Boolean r1, Boolean r2) {
                r1 = (r1 == null) ? false : r1;
                r2 = (r2 == null) ? false : r2;
                return r1 || r2;
              }
            },
            null);
  }

  /**
   * Returns a finding for the given method tree containing fixes:
   *
   * <ol>
   *   <li>Add @Test, remove static modifier if present.
   *   <li>Add @Test and @Ignore, remove static modifier if present.
   *   <li>Change visibility to private (for local helper methods).
   * </ol>
   */
  private Description describeFixes(MethodTree methodTree, VisitorState state) {
    Optional<SuggestedFix> removeStatic =
        SuggestedFixes.removeModifiers(methodTree, state, Modifier.STATIC);
    SuggestedFix testFix =
        SuggestedFix.builder()
            .merge(removeStatic.orElse(null))
            .addImport(TEST_CLASS)
            .prefixWith(methodTree, TEST_ANNOTATION)
            .build();
    SuggestedFix ignoreFix =
        SuggestedFix.builder()
            .merge(testFix)
            .addImport(IGNORE_CLASS)
            .prefixWith(methodTree, IGNORE_ANNOTATION)
            .build();

    SuggestedFix visibilityFix =
        SuggestedFix.builder()
            .merge(SuggestedFixes.removeModifiers(methodTree, state, Modifier.PUBLIC).orElse(null))
            .merge(SuggestedFixes.addModifiers(methodTree, state, Modifier.PRIVATE).orElse(null))
            .build();

    // Suggest @Ignore first if test method is named like a purposely disabled test.
    String methodName = methodTree.getName().toString();
    if (methodName.startsWith("disabl") || methodName.startsWith("ignor")) {
      return buildDescription(methodTree)
          .addFix(ignoreFix)
          .addFix(testFix)
          .addFix(visibilityFix)
          .build();
    }
    return buildDescription(methodTree)
        .addFix(testFix)
        .addFix(ignoreFix)
        .addFix(visibilityFix)
        .build();
  }
}
