/*
 * Copyright 2023 The Error Prone Authors.
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

import static com.google.errorprone.util.ASTHelpers.stripParentheses;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.SynchronizedTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.SynchronizedTree;
import com.sun.source.tree.Tree;

/**
 * Bug checker to detect the usage of lock on the class other than the enclosing class of the code
 * block.
 */
@BugPattern(
    summary =
        "Lock on the class other than the enclosing class of the code block can unintentionally"
            + " prevent the locked class being used properly.",
    severity = SeverityLevel.WARNING)
public class LockOnNonEnclosingClassLiteral extends BugChecker implements SynchronizedTreeMatcher {

  @Override
  public Description matchSynchronized(SynchronizedTree tree, VisitorState state) {
    ExpressionTree lock = stripParentheses(tree.getExpression());
    Matcher<SynchronizedTree> lockOnEnclosingClassMatcher =
        Matchers.enclosingClass(
            (t, s) ->
                ASTHelpers.getSymbol(t)
                    .equals(ASTHelpers.getSymbol(((MemberSelectTree) lock).getExpression())));

    if (isClassLiteral(lock) && !lockOnEnclosingClassMatcher.matches(tree, state)) {
      return describeMatch(tree);
    }

    return Description.NO_MATCH;
  }

  private static boolean isClassLiteral(Tree tree) {
    if (tree.getKind() != Tree.Kind.MEMBER_SELECT) {
      return false;
    }
    return ((MemberSelectTree) tree).getIdentifier().contentEquals("class");
  }
}
