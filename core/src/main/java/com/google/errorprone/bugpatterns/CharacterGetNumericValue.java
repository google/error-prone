/*
 * Copyright 2021 The Error Prone Authors.
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
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.staticMethod;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.MethodInvocationTree;

/**
 * Checks for use of Character.getNumericValue and UCharacter.getNumericValue
 *
 * @author conklinh@google.com
 */
@BugPattern(
    summary =
        "getNumericValue has unexpected behaviour: it interprets A-Z as base-36 digits with values"
            + " 10-35, but also supports non-arabic numerals and miscellaneous numeric unicode"
            + " characters like ㊷; consider using Character.digit or"
            + " UCharacter.getUnicodeNumericValue instead",
    severity = WARNING)
public class CharacterGetNumericValue extends BugChecker implements MethodInvocationTreeMatcher {

  private static final Matcher<MethodInvocationTree> GET_NUMERIC_VALUE =
      anyOf(
          staticMethod().onClass("com.ibm.icu.lang.UCharacter").named("getNumericValue"),
          staticMethod().onClass("java.lang.Character").named("getNumericValue"));

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    return GET_NUMERIC_VALUE.matches(tree, state) ? describeMatch(tree) : Description.NO_MATCH;
  }
}
