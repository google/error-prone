/*
 * Copyright 2017 The Error Prone Authors.
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

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;

/**
 * Warns against overriding toString() in a Throwable class and suggests getMessage()
 *
 * @author mariasam@google.com (Maria Sam)
 */
@BugPattern(
    name = "OverrideThrowableToString",
    summary =
        "To return a custom message with a Throwable class, one should "
            + "override getMessage() instead of toString().",
    severity = WARNING,
    providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION)
public final class OverrideThrowableToString extends BugChecker implements ClassTreeMatcher {

  @Override
  public Description matchClass(ClassTree classTree, VisitorState state) {
    if (!ASTHelpers.isSubtype(
        ASTHelpers.getType(classTree), state.getSymtab().throwableType, state)) {
      return Description.NO_MATCH;
    }
    ImmutableList<MethodTree> methods =
        classTree.getMembers().stream()
            .filter(m -> m instanceof MethodTree)
            .map(m -> (MethodTree) m)
            .collect(toImmutableList());
    if (methods.stream().anyMatch(m -> m.getName().contentEquals("getMessage"))) {
      return Description.NO_MATCH;
    }
    return methods.stream()
        .filter(m -> Matchers.toStringMethodDeclaration().matches(m, state))
        .findFirst()
        .map(m -> describeMatch(classTree, SuggestedFixes.renameMethod(m, "getMessage", state)))
        .orElse(Description.NO_MATCH);
  }
}
