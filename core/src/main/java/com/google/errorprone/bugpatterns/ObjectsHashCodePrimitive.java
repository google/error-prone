/*
 * Copyright 2018 The Error Prone Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.argument;
import static com.google.errorprone.matchers.Matchers.isPrimitiveType;
import static com.google.errorprone.matchers.Matchers.staticMethod;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.MethodInvocationTree;
import java.util.Objects;

/**
 * Check for calls to Objects' {@link Objects#hashCode} with a primitive parameter.
 *
 * @author seibelsabrina@google.com (Sabrina Seibel)
 */
@BugPattern(
    name = "ObjectsHashCodePrimitive",
    summary = "Objects.hashCode(Object o) should not be passed a primitive value",
    severity = WARNING,
    providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION)
public final class ObjectsHashCodePrimitive extends BugChecker
    implements MethodInvocationTreeMatcher {
  private static final Matcher<MethodInvocationTree> OBJECTS_HASHCODE_CALLS =
      allOf(
          staticMethod().onClass("java.util.Objects").named("hashCode"),
          argument(0, isPrimitiveType()));

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    return OBJECTS_HASHCODE_CALLS.matches(tree, state)
        ? describeMatch(tree, adjustHashCodeCall(tree, state))
        : Description.NO_MATCH;
  }

  private static Fix adjustHashCodeCall(MethodInvocationTree tree, VisitorState state) {
    String argumentClass =
        state
            .getTypes()
            .boxedTypeOrType(ASTHelpers.getType(tree.getArguments().get(0)))
            .tsym
            .getSimpleName()
            .toString();
    return SuggestedFix.builder()
        .prefixWith(tree, argumentClass + ".hashCode(")
        .replace(tree, state.getSourceForNode(tree.getArguments().get(0)))
        .postfixWith(tree, ")")
        .build();
  }
}
