/*
 * Copyright 2020 The Error Prone Authors.
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
package com.google.errorprone.bugpatterns.time;

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.fixes.SuggestedFixes.qualifyType;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.not;
import static com.google.errorprone.matchers.Matchers.packageStartsWith;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;
import static com.google.errorprone.util.ASTHelpers.constValue;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;

/**
 * This checker bans calls to {@code ZoneId.of("Z")} in favor of {@link java.time.ZoneOffset#UTC}.
 *
 * @author kak@google.com (Kurt Alfred Kluever)
 */
@BugPattern(
    name = "ZoneIdOfZ",
    summary = "Use ZoneOffset.UTC instead of ZoneId.of(\"Z\").",
    explanation =
        "Avoid the magic constant (ZoneId.of(\"Z\")) in favor of a more descriptive API: "
            + " ZoneOffset.UTC",
    severity = ERROR)
public final class ZoneIdOfZ extends BugChecker implements MethodInvocationTreeMatcher {
  private static final String ZONE_OFFSET = "java.time.ZoneOffset";

  private static final Matcher<ExpressionTree> ZONE_ID_OF =
      allOf(
          staticMethod().onClass("java.time.ZoneId").named("of"),
          not(anyOf(packageStartsWith("java."), packageStartsWith("tck.java."))));

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (ZONE_ID_OF.matches(tree, state)) {
      String zone = constValue(tree.getArguments().get(0), String.class);
      if (zone != null && zone.equals("Z")) {
        SuggestedFix.Builder fix = SuggestedFix.builder().addImport(ZONE_OFFSET);
        fix.replace(tree, String.format("%s.UTC", qualifyType(state, fix, ZONE_OFFSET)));
        return describeMatch(tree, fix.build());
      }
    }
    return Description.NO_MATCH;
  }
}
