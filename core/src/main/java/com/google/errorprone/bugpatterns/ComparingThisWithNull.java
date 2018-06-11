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

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.BinaryTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.Tree.Kind;

/** @author seibelsabrina@google.com (Sabrina Seibel), kayco@google.com (Kayla Walker) */
/** Check for expressions containing {@code this != null} or {@code this == null} */
@BugPattern(
    name = "ComparingThisWithNull",
    summary = "this == null is always false, this != null is always true",
    explanation =
        "The boolean expression this != null always returns true"
            + " and similarly this == null always returns false.",
    severity = ERROR,
    providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION)
public class ComparingThisWithNull extends BugChecker implements BinaryTreeMatcher {

  private static final Matcher<BinaryTree> EQUAL_OR_NOT_EQUAL =
      Matchers.allOf(
          Matchers.anyOf(Matchers.kindIs(Kind.EQUAL_TO), Matchers.kindIs(Kind.NOT_EQUAL_TO)),
          Matchers.binaryTree(Matchers.kindIs(Kind.NULL_LITERAL), new ThisMatcher()));

  @Override
  public Description matchBinary(BinaryTree tree, VisitorState state) {
    if (EQUAL_OR_NOT_EQUAL.matches(tree, state)) {
      return describeMatch(tree);
    }
    return Description.NO_MATCH;
  }

  private static class ThisMatcher implements Matcher<ExpressionTree> {
    @Override
    public boolean matches(ExpressionTree thisExpression, VisitorState state) {
      if (thisExpression.getKind().equals(Kind.IDENTIFIER)) {
        IdentifierTree identifier = (IdentifierTree) thisExpression;
        if (identifier.getName().contentEquals("this")) {
          return true;
        }
      }
      return false;
    }
  }
}
