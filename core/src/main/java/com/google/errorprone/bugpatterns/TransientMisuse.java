/*
 * Copyright 2019 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.LinkType.NONE;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.VariableTree;
import javax.lang.model.element.Modifier;

/**
 * Warns against use of both {@code static} and {@code transient} modifiers on field declarations.
 */
@BugPattern(
    summary = "Static fields are implicitly transient, so the explicit modifier is unnecessary",
    linkType = NONE,
    severity = WARNING)
public class TransientMisuse extends BugChecker implements VariableTreeMatcher {
  @Override
  public Description matchVariable(VariableTree tree, VisitorState state) {
    if (tree.getModifiers()
        .getFlags()
        .containsAll(ImmutableList.of(Modifier.STATIC, Modifier.TRANSIENT))) {
      return describeMatch(
          tree,
          SuggestedFixes.removeModifiers(tree, state, Modifier.TRANSIENT)
              .orElse(SuggestedFix.emptyFix()));
    }
    return Description.NO_MATCH;
  }
}
