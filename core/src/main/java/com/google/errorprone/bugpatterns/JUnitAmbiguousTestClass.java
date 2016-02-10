/*
 * Copyright 2014 Google Inc. All Rights Reserved.
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
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.JUnitMatchers.hasJUnit4TestCases;
import static com.google.errorprone.matchers.JUnitMatchers.isTestCaseDescendant;
import static com.google.errorprone.matchers.Matchers.allOf;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;

import com.sun.source.tree.ClassTree;

/**
 * @author mwacker@google.com (Mike Wacker)
 */
@BugPattern(
    name = "JUnitAmbiguousTestClass",
    summary = "Test class inherits from JUnit 3's TestCase but has JUnit 4 @Test annotations.",
    explanation = "For JUnit3-style tests, behavior is defined in junit.framework.TestCase and "
        + "tests add behavior by overriding methods. For JUnit4-style tests, special "
        + "behavior happens with fields and methods annotated with JUnit 4 annotations. Having "
        + "JUnit4-style tests extend from junit.framework.TestCase (directly or indirectly) "
        + "historically has been a source of test bugs and "
        + "unexpected behavior (e.g.: teardown logic and/or verification does not run because "
        + "JUnit doesn't call the inherited code). "
        + "Error Prone also cannot infer whether the test class runs with JUnit 3 or JUnit 4. "
        + "Thus, even if the test class runs with JUnit 4, Error Prone will not run "
        + "additional checks which can catch common errors with JUnit 4 test classes. "
        + "Either use only JUnit4 classes and annotations and remove the inheritance from "
        + "TestCase, or use only JUnit 3 and remove the @Test annotations. When looking for "
        + "replacements for base test classes, consider using Rules (see the @Rule annotation and "
        + "implementations of TestRule and MethodRule).",
    category = JUNIT, maturity = MATURE, severity = WARNING)
public class JUnitAmbiguousTestClass extends BugChecker implements ClassTreeMatcher {

  private static final Matcher<ClassTree> matcher = allOf(
      isTestCaseDescendant,
      hasJUnit4TestCases);

  @Override
  public Description matchClass(ClassTree classTree, VisitorState state) {
    return matcher.matches(classTree, state) ? describeMatch(classTree) : NO_MATCH;
  }
}
