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
import static com.google.errorprone.matchers.JUnitMatchers.hasJUnit4AfterAnnotations;
import static com.google.errorprone.matchers.JUnitMatchers.hasJUnit5AfterAll;
import static com.google.errorprone.matchers.JUnitMatchers.hasJUnit5AfterEach;
import static com.google.errorprone.matchers.JUnitMatchers.isJUnit5TestClass;
import static com.google.errorprone.matchers.JUnitMatchers.looksLikeJUnit3TearDown;
import static com.google.errorprone.matchers.JUnitMatchers.looksLikeJUnit4After;
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
 * Checks for the existence of a tearDown() method in a JUnit test class that will not be run;
 * suggests adding the appropriate lifecycle annotation.
 *
 * @author glorioso@google.com (Nick Glorioso)
 */
@BugPattern(
    summary =
        "tearDown() method will not be run; please add JUnit's @After or @AfterEach annotation",
    severity = ERROR,
    altNames = {"JUnit4TearDownNotRun"})
public class JUnitTearDownNotRun extends AbstractJUnit4InitMethodNotRun {
  @Override
  protected Matcher<MethodTree> methodMatcher() {
    return allOf(
        anyOf(looksLikeJUnit3TearDown, looksLikeJUnit4After),
        not(hasJUnit4AfterAnnotations),
        not(hasJUnit5AfterEach),
        not(hasJUnit5AfterAll));
  }

  @Override
  protected String correctAnnotation(VisitorState state) {
    return isJUnit5TestClass(state) ? JUNIT5_AFTER_EACH_ANNOTATION : JUNIT_AFTER_ANNOTATION;
  }

  @Override
  protected List<AnnotationReplacements> annotationReplacements(VisitorState state) {
    if (isJUnit5TestClass(state)) {
      return Arrays.asList(
          new AnnotationReplacements(JUNIT5_BEFORE_EACH_ANNOTATION, JUNIT5_AFTER_EACH_ANNOTATION),
          new AnnotationReplacements(JUNIT5_BEFORE_ALL_ANNOTATION, JUNIT5_AFTER_ALL_ANNOTATION));
    }
    return Arrays.asList(
        new AnnotationReplacements(JUNIT_BEFORE_ANNOTATION, JUNIT_AFTER_ANNOTATION),
        new AnnotationReplacements(JUNIT_BEFORE_CLASS_ANNOTATION, JUNIT_AFTER_CLASS_ANNOTATION));
  }
}
