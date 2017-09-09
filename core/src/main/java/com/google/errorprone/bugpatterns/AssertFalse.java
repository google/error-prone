/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Matchers.assertionWithCondition;
import static com.google.errorprone.matchers.Matchers.booleanLiteral;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.AssertTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.AssertTree;

/** @author sebastian.h.monte@gmail.com (Sebastian Monte) */
@BugPattern(
  name = "AssertFalse",
  summary =
      "Assertions may be disabled at runtime and do not guarantee that execution will "
          + "halt here; consider throwing an exception instead",
  explanation =
      "Java assertions do not necessarily execute at runtime; they may be enabled and "
          + "disabled depending on which options are passed to the JVM invocation. An assert"
          + " false statement may be intended to ensure that the program never proceeds beyond "
          + "that statement. If the correct execution of the program depends on that being the "
          + "case, consider throwing an exception instead, so that execution is halted regardless "
          + "of runtime configuration.",
  category = JDK,
  severity = WARNING,
  providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION
)
public class AssertFalse extends BugChecker implements AssertTreeMatcher {

  private static final Matcher<AssertTree> ASSERT_FALSE_MATCHER =
      assertionWithCondition(booleanLiteral(false));

  @Override
  public Description matchAssert(AssertTree tree, VisitorState state) {
    if (!ASSERT_FALSE_MATCHER.matches(tree, state)) {
      return Description.NO_MATCH;
    }

    return describeMatch(tree, SuggestedFix.replace(tree, "throw new AssertionError()"));
  }
}
