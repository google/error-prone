/*
 * Copyright 2019 The Error Prone Authors.
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
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.google.errorprone.util.ASTHelpers.getReceiver;
import static com.google.errorprone.util.ASTHelpers.getType;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Type;
import javax.lang.model.type.TypeKind;

/** Discourages inadvertently using reference equality on boxed primitives in AtomicReference. */
@BugPattern(
    name = "PrimitiveAtomicReference",
    summary =
        "Using compareAndSet with boxed primitives is dangerous, as reference rather than value"
            + " equality is used. Consider using AtomicInteger, AtomicLong, AtomicBoolean from JDK"
            + " or AtomicDouble from Guava instead.",
    severity = WARNING)
public final class PrimitiveAtomicReference extends BugChecker
    implements MethodInvocationTreeMatcher {
  private static final Matcher<ExpressionTree> COMPARE_AND_SET =
      instanceMethod()
          .onDescendantOf("java.util.concurrent.atomic.AtomicReference")
          .namedAnyOf("compareAndSet", "weakCompareAndSet");

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!COMPARE_AND_SET.matches(tree, state)) {
      return NO_MATCH;
    }
    ExpressionTree firstArgument = tree.getArguments().get(0);
    if (firstArgument instanceof LiteralTree && ((LiteralTree) firstArgument).getValue() == null) {
      return NO_MATCH;
    }
    Type receiverType = getType(getReceiver(tree));
    if (receiverType == null) {
      return NO_MATCH;
    }
    // There could be no type arguments if we're seeing a raw type.
    if (receiverType.getTypeArguments().isEmpty()) {
      return NO_MATCH;
    }
    Type typeArgument = receiverType.getTypeArguments().get(0);
    if (state.getTypes().unboxedType(typeArgument).getKind() == TypeKind.NONE) {
      return NO_MATCH;
    }
    return describeMatch(tree);
  }
}
