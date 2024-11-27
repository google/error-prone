/*
 * Copyright 2024 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.fixes.SuggestedFix.delete;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.hasAnnotation;
import static com.google.errorprone.util.ASTHelpers.containsComments;
import static com.google.errorprone.util.ASTHelpers.getSymbol;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.JUnitMatchers;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.MethodTree;
import com.sun.tools.javac.code.Type;
import java.util.List;

/**
 * Deletes empty JUnit4 {@code @Before}, {@code @After}, {@code @BeforeClass}, and
 * {@code @AfterClass} methods.
 *
 * @author kak@google.com (Kurt Alfred Kluever)
 */
@BugPattern(
    summary =
        "Empty JUnit4 @Before, @After, @BeforeClass, and @AfterClass methods are unnecessary and"
            + " should be deleted.",
    severity = WARNING)
public final class JUnit4EmptyMethods extends BugChecker implements MethodTreeMatcher {

  private static final Matcher<MethodTree> JUNIT_METHODS =
      anyOf(
          // NOTE: we don't check @Test because there's a lot of existing violations, some of
          // which are valid (e.g. an empty startup test, or a test with a TODO)
          hasAnnotation(JUnitMatchers.JUNIT_BEFORE_CLASS_ANNOTATION),
          hasAnnotation(JUnitMatchers.JUNIT_AFTER_CLASS_ANNOTATION),
          hasAnnotation(JUnitMatchers.JUNIT_BEFORE_ANNOTATION),
          hasAnnotation(JUnitMatchers.JUNIT_AFTER_ANNOTATION));

  @Override
  public Description matchMethod(MethodTree method, VisitorState state) {
    // bail out if the method has any super type other than Object
    // this means we don't have to worry about Overrides or static shadows
    List<Type> types = state.getTypes().closure(getSymbol(method).owner.type);
    if (types.size() != 2) { // the class itself and java.lang.Object
      return Description.NO_MATCH;
    }
    // if it's an empty JUnit method, delete it
    if (method.getBody() != null
        && method.getBody().getStatements().isEmpty()
        && JUNIT_METHODS.matches(method, state)
        && !containsComments(method, state)) {
      return describeMatch(method, delete(method));
    }
    return Description.NO_MATCH;
  }
}
