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
import static com.google.errorprone.BugPattern.StandardTags.FRAGILE_CODE;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.argument;
import static com.google.errorprone.matchers.Matchers.enclosingClass;
import static com.google.errorprone.matchers.Matchers.enclosingNode;
import static com.google.errorprone.matchers.Matchers.instanceEqualsInvocation;
import static com.google.errorprone.matchers.Matchers.isSubtypeOf;
import static com.google.errorprone.matchers.Matchers.kindIs;
import static com.google.errorprone.matchers.Matchers.toType;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;
import static com.google.errorprone.util.ASTHelpers.getReceiver;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    name = "EqualsNull",
    summary =
        "The contract of Object.equals() states that for any non-null reference value x,"
            + " x.equals(null) should return false. If x is null, a NullPointerException is thrown."
            + " Consider replacing equals() with the == operator.",
    tags = FRAGILE_CODE,
    severity = WARNING)
public class EqualsNull extends BugChecker implements MethodInvocationTreeMatcher {

  private static final Matcher<MethodInvocationTree> EQUALS_NULL =
      allOf(instanceEqualsInvocation(), argument(0, kindIs(Kind.NULL_LITERAL)));

  private static final Matcher<Tree> INSIDE_ASSERT_CLASS =
      enclosingClass(anyOf(isSubtypeOf("org.junit.Assert"), isSubtypeOf("junit.framework.Assert")));

  private static final Matcher<Tree> ENCLOSED_BY_ASSERT =
      enclosingNode(
          toType(
              MethodInvocationTree.class,
              staticMethod()
                  .onClassAny(
                      "com.google.common.truth.Truth",
                      "com.google.common.truth.Truth8",
                      "junit.framework.Assert",
                      "org.junit.Assert")));

  @Override
  public Description matchMethodInvocation(
      MethodInvocationTree invocationTree, VisitorState state) {
    if (!EQUALS_NULL.matches(invocationTree, state)) {
      return NO_MATCH;
    }
    if (ASTHelpers.isJUnitTestCode(state)
        || ASTHelpers.isTestNgTestCode(state)
        || INSIDE_ASSERT_CLASS.matches(invocationTree, state)
        || ENCLOSED_BY_ASSERT.matches(invocationTree, state)) {
      // Allow x.equals(null) in test code for testing the equals contract.
      return NO_MATCH;
    }
    Tree parentTree = state.getPath().getParentPath().getLeaf();
    boolean negated = parentTree.getKind() == Kind.LOGICAL_COMPLEMENT;
    String operator = negated ? "!=" : "==";
    Tree treeToFix = negated ? parentTree : invocationTree;
    String fixedCode =
        String.format("%s %s null", state.getSourceForNode(getReceiver(invocationTree)), operator);
    return describeMatch(treeToFix, SuggestedFix.replace(treeToFix, fixedCode));
  }
}
