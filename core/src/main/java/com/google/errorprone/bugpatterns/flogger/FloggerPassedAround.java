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

package com.google.errorprone.bugpatterns.flogger;

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.predicates.TypePredicates.anyOf;
import static com.google.errorprone.predicates.TypePredicates.isDescendantOf;
import static com.google.errorprone.util.ASTHelpers.getType;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.predicates.TypePredicate;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Type;

/** Flags flogger instances being passed around. */
@BugPattern(
    name = "FloggerPassedAround",
    summary =
        "There is no advantage to passing around a logger rather than declaring one in the class"
            + " that needs it.",
    severity = WARNING)
public final class FloggerPassedAround extends BugChecker implements MethodTreeMatcher {
  private static final TypePredicate LOGGER_TYPE =
      anyOf(
          isDescendantOf("com.google.common.flogger.FluentLogger"),
          isDescendantOf("com.google.common.flogger.FluentLogger"),
          isDescendantOf("com.google.common.flogger.android.AndroidFluentLogger"));

  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    for (Tree parameter : tree.getParameters()) {
      Type type = getType(parameter);
      if (type != null && LOGGER_TYPE.apply(type, state) && !isSuppressed(parameter)) {
        state.reportMatch(describeMatch(parameter));
      }
    }
    return NO_MATCH;
  }
}
