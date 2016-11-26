/*
 * Copyright 2014 Google Inc. All Rights Reserved.
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

package com.google.errorprone.bugpatterns.threadsafety;

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.bugpatterns.threadsafety.GuardedByUtils.GuardedByValidationResult;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
  name = "GuardedByValidator",
  summary = "Invalid @GuardedBy expression",
  category = JDK,
  severity = ERROR
)
public class GuardedByValidator extends BugChecker
    implements VariableTreeMatcher, MethodTreeMatcher {

  private static final String MESSAGE_FORMAT = "Invalid @GuardedBy expression: %s";
  
  @Override
  public Description matchMethod(MethodTree tree, final VisitorState state) {
    return validate(this, tree, state);
  }

  @Override
  public Description matchVariable(VariableTree tree, VisitorState state) {
    // We only want to check field declarations. The VariableTree might be for a local or a
    // parameter, but they won't have @GuardedBy annotations.
    return validate(this, tree, state);
  }

  static Description validate(BugChecker checker, Tree tree, VisitorState state) {
    GuardedByValidationResult result = GuardedByUtils.isGuardedByValid(tree, state);
    if (result.isValid()) {
      return Description.NO_MATCH;
    }
    return BugChecker.buildDescriptionFromChecker(tree, checker)
        .setMessage(String.format(MESSAGE_FORMAT, result.message()))
        .build();
  }
}
