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
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.JUnitMatchers.JUNIT_AFTER_ANNOTATION;
import static com.google.errorprone.matchers.JUnitMatchers.JUNIT_AFTER_CLASS_ANNOTATION;
import static com.google.errorprone.matchers.JUnitMatchers.JUNIT_BEFORE_ANNOTATION;
import static com.google.errorprone.matchers.JUnitMatchers.JUNIT_BEFORE_CLASS_ANNOTATION;
import static com.google.errorprone.matchers.JUnitMatchers.hasJUnit4AfterAnnotations;
import static com.google.errorprone.matchers.JUnitMatchers.looksLikeJUnit3TearDown;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.not;

import com.google.errorprone.BugPattern;
import com.google.errorprone.matchers.Matcher;

import com.sun.source.tree.MethodTree;

import java.util.Arrays;
import java.util.List;

/**
 * Checks for the existence of a JUnit3 style tearDown() method in a JUnit4 test class.
 *
 * @author glorioso@google.com (Nick Glorioso)
 */
@BugPattern(name = "JUnit4TearDownNotRun",
    summary = "tearDown() method will not be run; Please add an @After annotation",
    explanation = "JUnit 3 provides the overridable method tearDown(), to be overridden by "
        + " subclasses when the test needs to perform some post-test de-initialization. "
        + " In JUnit 4, this is accomplished by annotating such a method with @After."
        + " The method that triggered this error matches the definition of tearDown() from"
        + " JUnit3, but was not annotated with @After and thus won't be run by the JUnit4"
        + " runner.\n\n"
        + " If you intend for this tearDown() method not to be run by the JUnit4 runner,"
        + " but perhaps be manually invoked after certain test methods, please rename the method "
        + " or mark it private.\n\n"
        + " If the method is part of an abstract test class hierarchy"
        + " where this class's tearDown() is invoked by a superclass method that is annotated with"
        + " @After, then please rename the abstract method or add @After to"
        + " the superclass's definition of tearDown()",
    category = JUNIT, maturity = MATURE, severity = ERROR)
public class JUnit4TearDownNotRun extends AbstractJUnit4InitMethodNotRun {
  @Override
  protected Matcher<MethodTree> methodMatcher() {
    return allOf(looksLikeJUnit3TearDown, not(hasJUnit4AfterAnnotations));
  }

  @Override
  protected String correctAnnotation() {
    return JUNIT_AFTER_ANNOTATION;
  }

  @Override
  protected List<AnnotationReplacements> annotationReplacements() {
    return Arrays.asList(
        new AnnotationReplacements(JUNIT_BEFORE_ANNOTATION, JUNIT_AFTER_ANNOTATION),
        new AnnotationReplacements(JUNIT_BEFORE_CLASS_ANNOTATION, JUNIT_AFTER_CLASS_ANNOTATION));
  }
}
