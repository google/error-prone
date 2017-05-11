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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.annotations.concurrent.LockMethod;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.MethodTree;
import java.util.Set;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
  name = "LockMethodChecker",
  altNames = {"GuardedBy"},
  summary = "This method does not acquire the locks specified by its @LockMethod annotation",
  explanation =
      "Methods with the @LockMethod annotation are expected to acquire one or more"
          + " locks. The caller will hold the locks when the function finishes execution.",
  category = JDK,
  severity = ERROR
)
public class LockMethodChecker extends AbstractLockMethodChecker {

  @Override
  protected ImmutableList<String> getLockExpressions(MethodTree tree) {
    LockMethod lockMethod = ASTHelpers.getAnnotation(tree, LockMethod.class);
    return lockMethod == null
        ? ImmutableList.<String>of()
        : ImmutableList.copyOf(lockMethod.value());
  }

  @Override
  protected Set<GuardedByExpression> getActual(MethodTree tree, VisitorState state) {
    return ImmutableSet.copyOf(HeldLockAnalyzer.AcquiredLockFinder.find(tree.getBody(), state));
  }

  @Override
  protected Set<GuardedByExpression> getUnwanted(MethodTree tree, VisitorState state) {
    return ImmutableSet.copyOf(HeldLockAnalyzer.ReleasedLockFinder.find(tree.getBody(), state));
  }

  @Override
  protected String buildMessage(String unhandled) {
    return "The following locks are specifed in this method's @LockMethod annotation but are not"
        + " acquired: "
        + unhandled;
  }
}
