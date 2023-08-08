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

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.constValue;
import static com.google.errorprone.util.ASTHelpers.targetType;
import static com.sun.source.tree.Tree.Kind.UNARY_MINUS;
import static com.sun.tools.javac.code.TypeTag.DOUBLE;
import static com.sun.tools.javac.code.TypeTag.FLOAT;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.UnaryTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.UnaryTree;

/** A BugPattern; see the summary. */
@BugPattern(
    severity = WARNING,
    summary = "-0 is the same as 0. For the floating-point negative zero, use -0.0.")
public class AttemptedNegativeZero extends BugChecker implements UnaryTreeMatcher {
  @Override
  public Description matchUnary(UnaryTree tree, VisitorState state) {
    if (tree.getKind() != UNARY_MINUS) {
      return NO_MATCH;
    }
    Object negatedConstValue = constValue(tree.getExpression());
    if (negatedConstValue == null) {
      return NO_MATCH;
    }
    if (!negatedConstValue.equals(0) && !negatedConstValue.equals(0L)) {
      return NO_MATCH;
    }
    String replacement;
    switch (targetType(state).type().getTag()) {
      case DOUBLE:
        replacement = "-0.0";
        break;
      case FLOAT:
        replacement = "-0.0f";
        break;
      default:
        return NO_MATCH;
    }
    return describeMatch(tree, SuggestedFix.builder().replace(tree, replacement).build());
  }
}
