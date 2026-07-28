/*
 * Copyright 2014 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.JUnitMatchers.JUNIT_AFTER_ANNOTATION;
import static com.google.errorprone.matchers.JUnitMatchers.JUNIT_AFTER_CLASS_ANNOTATION;
import static com.google.errorprone.matchers.JUnitMatchers.JUNIT_BEFORE_ANNOTATION;
import static com.google.errorprone.matchers.JUnitMatchers.JUNIT_BEFORE_CLASS_ANNOTATION;
import static com.google.errorprone.matchers.JUnitMatchers.JUNIT5_AFTER_EACH_ANNOTATION;
import static com.google.errorprone.matchers.JUnitMatchers.JUNIT5_AFTER_ALL_ANNOTATION;
import static com.google.errorprone.matchers.JUnitMatchers.JUNIT5_BEFORE_EACH_ANNOTATION;
import static com.google.errorprone.matchers.JUnitMatchers.JUNIT5_BEFORE_ALL_ANNOTATION;
import static com.google.errorprone.matchers.JUnitMatchers.hasJUnit4BeforeAnnotations;
import static com.google.errorprone.matchers.JUnitMatchers.hasJUnit5BeforeAll;
import static com.google.errorprone.matchers.JUnitMatchers.hasJUnit5BeforeEach;
import static com.google.errorprone.matchers.JUnitMatchers.hasJUnit5TestCases;
import static com.google.errorprone.matchers.JUnitMatchers.isJUnit5TestClass;
import static com.google.errorprone.matchers.JUnitMatchers.looksLikeJUnit3SetUp;
import static com.google.errorprone.matchers.JUnitMatchers.looksLikeJUnit4Before;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.not;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.MethodTree;
import java.util.Arrays;
import java.util.List;

/**
 * Checks for the existence of a setUp() method in a JUnit test class that will not be run; suggests
 * adding the appropriate lifecycle annotation.
 *
 * @author glorioso@google.com (Nick Glorioso)
 */
@BugPattern(
    summary =
        "setUp() method will not be run; please add JUnit's @Before or @BeforeEach annotation",
    severity = ERROR,
    altNames = {"JUnit4SetUpNotRun"})
public class JUnitSetUpNotRun extends AbstractJUnit4InitMethodNotRun {
  @Override
  protected Matcher<MethodTree> methodMatcher() {
    return allOf(
        anyOf(looksLikeJUnit3SetUp, looksLikeJUnit4Before),
        not(hasJUnit4BeforeAnnotations),
        not(hasJUnit5BeforeEach),
        not(hasJUnit5BeforeAll));
  }

  @Override
  protected String correctAnnotation(VisitorState state) {
    return isJUnit5TestClass(state) ? JUNIT5_BEFORE_EACH_ANNOTATION : JUNIT_BEFORE_ANNOTATION;
  }

  @Override
  protected List<AnnotationReplacements> annotationReplacements(VisitorState state) {
    if (isJUnit5TestClass(state)) {
      return Arrays.asList(
          new AnnotationReplacements(JUNIT5_AFTER_EACH_ANNOTATION, JUNIT5_BEFORE_EACH_ANNOTATION),
          new AnnotationReplacements(JUNIT5_AFTER_ALL_ANNOTATION, JUNIT5_BEFORE_ALL_ANNOTATION));
    }
    return Arrays.asList(
        new AnnotationReplacements(JUNIT_AFTER_ANNOTATION, JUNIT_BEFORE_ANNOTATION),
        new AnnotationReplacements(JUNIT_AFTER_CLASS_ANNOTATION, JUNIT_BEFORE_CLASS_ANNOTATION));
  }
}
