/*
 * Copyright 2022 The Error Prone Authors.
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
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.targetType;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.util.TreePath;
import javax.lang.model.type.TypeKind;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    summary =
        "Conversion from long to double may lose precision; use an explicit cast to double if this"
            + " was intentional",
    severity = WARNING)
public final class LongDoubleConversion extends BugChecker implements MethodInvocationTreeMatcher {

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    for (ExpressionTree argument : tree.getArguments()) {
      checkArgument(argument, state);
    }
    return NO_MATCH;
  }

  private void checkArgument(ExpressionTree argument, VisitorState state) {
    if (!getType(argument).getKind().equals(TypeKind.LONG)) {
      return;
    }
    Object constant = constValue(argument);
    if (constant instanceof Long && constant.equals((long) ((Long) constant).doubleValue())) {
      return;
    }
    ASTHelpers.TargetType targetType =
        targetType(state.withPath(new TreePath(state.getPath(), argument)));
    if (targetType != null && targetType.type().getKind().equals(TypeKind.DOUBLE)) {
      String replacement = SuggestedFixes.castTree(argument, "double", state);
      state.reportMatch(describeMatch(argument, SuggestedFix.replace(argument, replacement)));
    }
  }
}
