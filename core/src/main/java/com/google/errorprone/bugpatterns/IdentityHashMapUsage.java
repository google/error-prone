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

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.method.MethodMatchers.constructor;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.AssignmentTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.NewClassTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Type;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    name = "IdentityHashMapUsage",
    summary = "IdentityHashMap usage shouldn't be intermingled with Map",
    severity = WARNING)
public class IdentityHashMapUsage extends BugChecker
    implements MethodInvocationTreeMatcher,
        AssignmentTreeMatcher,
        VariableTreeMatcher,
        NewClassTreeMatcher {

  private static final String IDENTITY_HASH_MAP = "java.util.IdentityHashMap";
  private static final Matcher<ExpressionTree> IHM_ONE_ARG_METHODS =
      instanceMethod().onExactClass(IDENTITY_HASH_MAP).namedAnyOf("equals", "putAll");
  private static final Matcher<ExpressionTree> IHM_CTOR_MAP_ARG =
      constructor().forClass(IDENTITY_HASH_MAP).withParameters("java.util.Map");

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (IHM_ONE_ARG_METHODS.matches(tree, state)
        && !ASTHelpers.isSameType(
            ASTHelpers.getType(tree.getArguments().get(0)),
            state.getTypeFromString(IDENTITY_HASH_MAP),
            state)) {
      return describeMatch(tree);
    }
    return Description.NO_MATCH;
  }

  @Override
  public Description matchAssignment(AssignmentTree tree, VisitorState state) {
    Type ihmType = state.getTypeFromString(IDENTITY_HASH_MAP);
    if (!ASTHelpers.isSameType(ASTHelpers.getType(tree.getExpression()), ihmType, state)) {
      return Description.NO_MATCH;
    }
    if (!ASTHelpers.isSameType(ASTHelpers.getType(tree.getVariable()), ihmType, state)) {
      return describeMatch(tree);
    }
    return Description.NO_MATCH;
  }

  @Override
  public Description matchVariable(VariableTree tree, VisitorState state) {
    if (tree.getInitializer() == null) {
      // method params don't have initializers.
      return Description.NO_MATCH;
    }
    Type ihmType = state.getTypeFromString(IDENTITY_HASH_MAP);
    if (ASTHelpers.isSameType(ASTHelpers.getType(tree.getType()), ihmType, state)) {
      return Description.NO_MATCH;
    }
    Type type = ASTHelpers.getType(tree.getInitializer());
    if (ASTHelpers.isSameType(type, ihmType, state)) {
      SuggestedFix.Builder fix = SuggestedFix.builder();
      fix.replace(tree.getType(), SuggestedFixes.qualifyType(state, fix, type));
      return describeMatch(tree, fix.build());
    }
    return Description.NO_MATCH;
  }

  @Override
  public Description matchNewClass(NewClassTree tree, VisitorState state) {
    if (IHM_CTOR_MAP_ARG.matches(tree, state)
        && !ASTHelpers.isSameType(
            ASTHelpers.getType(tree.getArguments().get(0)),
            state.getTypeFromString(IDENTITY_HASH_MAP),
            state)) {
      return describeMatch(tree);
    }
    return Description.NO_MATCH;
  }
}
