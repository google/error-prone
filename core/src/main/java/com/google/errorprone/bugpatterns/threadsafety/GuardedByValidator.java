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
import static com.google.errorprone.BugPattern.MaturityLevel.EXPERIMENTAL;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.matchers.Description;

import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;

/**
 * @author cushon@google.com (Liam Miller-Cushon)
 */
@BugPattern(name = "GuardedByValidator",
    summary = "Invalid @GuardedBy expression",
    explanation = "@GuardedBy(lock) documents that a field or method should be accessed only with "
      + "a specific lock held. The lock argument identifies the lock that should be held when "
      + "accessing the annotated field or method. The possible values for lock are: "
      + "* @GuardedBy(\"this\"), meaning the intrinsic lock on the containing object (the "
      + "object of which the method or field is a member);"
      + "* @GuardedBy(\"fieldName\"), meaning the lock associated with the object referenced by "
      + "the named field, either an intrinsic lock (for fields that do not refer to a Lock) or an "
      + "explicit Lock (for fields that refer to a Lock);"
      + "* @GuardedBy(\"ClassName.fieldName\"), like @GuardedBy(\"fieldName\"), but referencing "
      + "a lock object held in a static field of another class;"
      + "* @GuardedBy(\"methodName()\"), meaning the lock object that is returned by calling the "
      + "named method;"
      + "* @GuardedBy(\"ClassName.class\"), meaning the class literal object for the named class.",
      category = JDK, severity = ERROR, maturity = EXPERIMENTAL)
public class GuardedByValidator extends BugChecker implements VariableTreeMatcher,
    MethodTreeMatcher {

  @Override
  public Description matchMethod(MethodTree tree, final VisitorState state) {
    return GuardedByUtils.isGuardedByValid(tree, state)
        ? Description.NO_MATCH
        : describeMatch(tree);
  }

  @Override
  public Description matchVariable(VariableTree tree, VisitorState state) {
    // We only want to check field declarations. The VariableTree might be for a local or a
    // parameter, but they won't have @GuardedBy annotations.
    return GuardedByUtils.isGuardedByValid(tree, state)
        ? Description.NO_MATCH
        : describeMatch(tree);
  }
}
