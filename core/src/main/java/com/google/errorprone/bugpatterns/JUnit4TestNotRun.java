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
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.enclosingClass;
import static com.google.errorprone.matchers.Matchers.hasAnnotation;
import static com.google.errorprone.matchers.Matchers.methodHasModifier;
import static com.google.errorprone.matchers.Matchers.methodHasParameters;
import static com.google.errorprone.matchers.Matchers.methodNameStartsWith;
import static com.google.errorprone.matchers.Matchers.not;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.DescribingMatcher;
import com.google.errorprone.matchers.Description;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;


import javax.lang.model.element.Modifier;

/**
 * TODO(eaftan): Similar checkers for setUp() and tearDown().
 *
 * @author eaftan@google.com (Eddie Aftandilian)
 */
@BugPattern(name = "JUnit4TestNotRun",
    summary = "Test method will not be run",
    explanation = "JUnit 4 requires that test methods be annotated with @Test to be run. " +
    		"This checker matches JUnit 4 test methods that would be run in JUnit 3 but are not " +
    		"annotated with @Test.",
    category = JUNIT, maturity = MATURE, severity = ERROR)
public class JUnit4TestNotRun extends DescribingMatcher<MethodTree> {

  private static final String JUNIT_TEST_ANNOTATION = "org.junit.Test";

  /**
   * Matches if:
   * 1) The method's name begins with "test".
   * 2) The method has no parameters.
   * 3) The method is public.
   * 4) The method is not annotated with @Test.
   * 5) The enclosing class has an @RunWith annotation.  This marks that the test is intended
   *    to run with JUnit 4.
   *
   * TODO(eaftan): Checking the class for @RunWith annotation is sufficient for Google, but not
   * externally.  Look at https://github.com/junit-team/junit/blob/master/src/main/java/org/junit/internal/builders/AllDefaultPossibilitiesBuilder.java
   * to see all the ways to determine if the test is a JUnit 4 test.
   */
  @SuppressWarnings("unchecked")
  @Override
  public boolean matches(MethodTree methodTree, VisitorState state) {
    return allOf(methodNameStartsWith("test"),
        methodHasParameters(),
        methodHasModifier(Modifier.PUBLIC),
        not(hasAnnotation(JUNIT_TEST_ANNOTATION)),
        enclosingClass(hasAnnotation("org.junit.runner.RunWith", ClassTree.class)))
        .matches(methodTree, state);
  }

  /**
   * Add the @Test annotation.  If the method is static, also make the method non-static.
   *
   * TODO(eaftan): The static case here relies on having tree end positions available.  Come up
   * with a better way of doing this that doesn't require tree end positions.  Maybe we should
   * just not provide suggested fixes for these few cases when the javac infrastructure gets in the
   * way.
   */
  @Override
  public Description describe(MethodTree methodTree, VisitorState state) {
    if (methodHasModifier(Modifier.STATIC).matches(methodTree, state)) {
      CharSequence methodSource = state.getSourceForNode((JCMethodDecl) methodTree);
      if (methodSource != null) {
        String methodString = "@Test\n" + methodSource.toString().replaceFirst(" static ", " ");
        SuggestedFix fix = new SuggestedFix()
            .addImport(JUNIT_TEST_ANNOTATION)
            .replace(methodTree, methodString);
        return new Description(methodTree, diagnosticMessage, fix);
      }
    }
    SuggestedFix fix = new SuggestedFix()
        .addImport(JUNIT_TEST_ANNOTATION)
        .prefixWith(methodTree, "@Test\n");
    return new Description(methodTree, diagnosticMessage, fix);
  }

  public static class Scanner extends com.google.errorprone.Scanner {
    private JUnit4TestNotRun matcher = new JUnit4TestNotRun();

    @Override
    public Void visitMethod(MethodTree node, VisitorState visitorState) {
      evaluateMatch(node, visitorState, matcher);
      return super.visitMethod(node, visitorState);
    }
  }
}
