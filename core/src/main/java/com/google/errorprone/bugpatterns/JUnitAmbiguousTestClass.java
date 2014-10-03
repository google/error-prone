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
import static com.google.errorprone.BugPattern.MaturityLevel.EXPERIMENTAL;
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
    summary = "Test class mixes JUnit 3 and JUnit 4 idioms",
    explanation = "The test class could execute either as a JUnit 3 class or a JUnit 4 class, "
        + "and tests could behave differently depending on whether it runs in JUnit 3 or JUnit 4. ",
    category = JUNIT, maturity = EXPERIMENTAL, severity = WARNING)
public class JUnitAmbiguousTestClass extends BugChecker implements ClassTreeMatcher {

  private static final Matcher<ClassTree> matcher = allOf(
      isTestCaseDescendant,
      hasJUnit4TestCases);

  @Override
  public Description matchClass(ClassTree classTree, VisitorState state) {
    return matcher.matches(classTree, state) ? describeMatch(classTree) : NO_MATCH;
  }
}
