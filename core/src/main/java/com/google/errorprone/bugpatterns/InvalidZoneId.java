/*
 * Copyright 2018 The Error Prone Authors.
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

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.method.MethodMatchers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import java.time.DateTimeException;
import java.time.ZoneId;

/**
 * Validates ZoneId.
 *
 * @author bhagwani@google.com (Sumit Bhagwani)
 */
@BugPattern(
    name = "InvalidZoneId",
    summary = "Invalid zone identifier. ZoneId.of(String) will throw exception at runtime.",
    severity = ERROR,
    providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION)
public class InvalidZoneId extends BugChecker implements MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> METHOD_MATCHER =
      MethodMatchers.staticMethod()
          .onClass("java.time.ZoneId")
          .withSignature("of(java.lang.String)");

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, final VisitorState state) {
    if (!METHOD_MATCHER.matches(tree, state)) {
      return Description.NO_MATCH;
    }
    String value = ASTHelpers.constValue(getOnlyElement(tree.getArguments()), String.class);
    if (value == null) {
      // Value isn't a compile-time constant, so we can't know if it's unsafe.
      return Description.NO_MATCH;
    }
    if (isValidID(value)) {
      return Description.NO_MATCH;
    }

    return describeMatch(tree);
  }

  // https://docs.oracle.com/javase/8/docs/api/java/time/ZoneId.html#of-java.lang.String-
  private static boolean isValidID(String value) {
    try {
      ZoneId.of(value);
    } catch (DateTimeException e) { // ZoneRulesException is subclass of DateTimeException
      return false;
    }
    return true;
  }
}
