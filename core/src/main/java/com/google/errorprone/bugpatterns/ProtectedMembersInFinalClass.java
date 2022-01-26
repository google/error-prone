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

package com.google.errorprone.bugpatterns;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.hasModifier;
import static java.util.stream.Collectors.joining;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PROTECTED;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol;
import javax.lang.model.element.Modifier;

/**
 * Flags protected members in final classes.
 *
 * @author bhagwani@google.com (Sumit Bhagwani)
 */
@BugPattern(
    summary = "Protected members in final classes can be package-private",
    severity = WARNING)
public class ProtectedMembersInFinalClass extends BugChecker implements ClassTreeMatcher {

  private static final Matcher<ClassTree> HAS_FINAL = hasModifier(FINAL);
  private static final Matcher<Tree> HAS_PROTECTED = hasModifier(PROTECTED);

  private static boolean methodHasNoParentMethod(MethodTree methodTree, VisitorState state) {
    return ASTHelpers.findSuperMethods(ASTHelpers.getSymbol(methodTree), state.getTypes())
        .isEmpty();
  }

  @Override
  public Description matchClass(final ClassTree tree, final VisitorState state) {
    if (!HAS_FINAL.matches(tree, state)) {
      return NO_MATCH;
    }

    ImmutableList<Tree> relevantMembers =
        tree.getMembers().stream()
            .filter(m -> (m instanceof MethodTree || m instanceof VariableTree))
            .filter(m -> HAS_PROTECTED.matches(m, state))
            .filter(
                m -> !(m instanceof MethodTree) || methodHasNoParentMethod((MethodTree) m, state))
            .filter(m -> !isSuppressed(m))
            .collect(toImmutableList());
    if (relevantMembers.isEmpty()) {
      return NO_MATCH;
    }
    SuggestedFix.Builder fix = SuggestedFix.builder();
    relevantMembers.forEach(
        m -> SuggestedFixes.removeModifiers(m, state, Modifier.PROTECTED).ifPresent(fix::merge));

    if (fix.isEmpty()) {
      return NO_MATCH;
    }
    String message =
        String.format(
            "Make members of final classes package-private: %s",
            relevantMembers.stream()
                .map(
                    m -> {
                      Symbol symbol = ASTHelpers.getSymbol(m);
                      return symbol.isConstructor()
                          ? symbol.owner.name.toString()
                          : symbol.name.toString();
                    })
                .collect(joining(", ")));
    return buildDescription(relevantMembers.get(0)).setMessage(message).addFix(fix.build()).build();
  }
}
