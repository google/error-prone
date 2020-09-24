/*
 * Copyright 2016 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.isSubtype;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.tools.javac.code.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Suggests to remove the unchecked throws clause.
 *
 * @author yulissa@google.com (Yulissa Arroyo-Paredes)
 */
@BugPattern(
    name = "ThrowsUncheckedException",
    summary = "Unchecked exceptions do not need to be declared in the method signature.",
    severity = SUGGESTION)
public class ThrowsUncheckedException extends BugChecker implements MethodTreeMatcher {
  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    if (tree.getThrows().isEmpty()) {
      return NO_MATCH;
    }
    List<ExpressionTree> uncheckedExceptions = new ArrayList<>();
    for (ExpressionTree exception : tree.getThrows()) {
      Type exceptionType = getType(exception);
      if (isSubtype(exceptionType, state.getSymtab().runtimeExceptionType, state)
          || isSubtype(exceptionType, state.getSymtab().errorType, state)) {
        uncheckedExceptions.add(exception);
      }
    }
    if (uncheckedExceptions.isEmpty()) {
      return NO_MATCH;
    }
    return describeMatch(
        uncheckedExceptions.get(0),
        SuggestedFixes.deleteExceptions(tree, state, uncheckedExceptions));
  }
}
