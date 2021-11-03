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
import static com.google.errorprone.matchers.Description.NO_MATCH;

import com.google.errorprone.BugPattern;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.MethodInvocationTree;

/** A BugChecker; see the associated BugPattern for details. */
@BugPattern(
    name = "BareDotMetacharacter",
    summary =
        "\".\" is rarely useful as a regex, as it matches any character. To match a literal '.'"
            + " character, instead write \"\\\\.\".",
    severity = WARNING,
    // So that suppressions added before this check was split into two apply to both halves.
    altNames = {"InvalidPatternSyntax"})
public class BareDotMetacharacter extends AbstractPatternSyntaxChecker
    implements MethodInvocationTreeMatcher {

  @Override
  protected final Description matchRegexLiteral(MethodInvocationTree tree, String regex) {
    if (regex.equals(".")) {
      return describeMatch(tree, SuggestedFix.replace(tree.getArguments().get(0), "\"\\\\.\""));
    } else {
      return NO_MATCH;
    }
  }
}
