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

package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.hashCodeMethodDeclaration;
import static com.google.errorprone.matchers.Matchers.instanceHashCodeInvocation;
import static com.google.errorprone.matchers.Matchers.not;
import static com.google.errorprone.matchers.Matchers.singleStatementReturnMatcher;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;

/**
 * Classes that override {@link Object#hashCode} should consider overriding {@link Object#toString}.
 *
 * @author bhagwani@google.com (Sumit Bhagwani)
 */
@BugPattern(
    name = "HashCodeToString",
    summary = "Classes that override hashCode should also consider overriding toString.",
    severity = SUGGESTION,
    tags = StandardTags.FRAGILE_CODE)
public class HashCodeToString extends BugChecker implements ClassTreeMatcher {

  private static final Matcher<MethodTree> NON_TRIVIAL_HASHCODE =
      allOf(
          hashCodeMethodDeclaration(),
          not(singleStatementReturnMatcher(instanceHashCodeInvocation())));

  @Override
  public Description matchClass(ClassTree classTree, VisitorState state) {
    if (ASTHelpers.hasAnnotation(classTree, "com.google.auto.value.AutoValue", state)) {
      return NO_MATCH;
    }
    MethodTree methodTree =
        EqualsHashCode.checkMethodPresence(
            classTree, state, NON_TRIVIAL_HASHCODE, /* expectedNoArgMethod= */ "toString");
    if (methodTree == null || isSuppressed(methodTree)) {
      return NO_MATCH;
    }
    return describeMatch(methodTree);
  }
}
