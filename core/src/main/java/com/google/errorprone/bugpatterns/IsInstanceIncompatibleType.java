/*
 * Copyright 2017 The Error Prone Authors.
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

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.util.ASTHelpers.getReceiver;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.isCastable;
import static com.google.errorprone.util.Signatures.prettyType;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MemberReferenceTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Type;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * @author cushon@google.com (Liam Miller-Cushon)
 * @author eleanorh@google.com (Eleanor Harris)
 */
@BugPattern(summary = "This use of isInstance will always evaluate to false.", severity = ERROR)
public final class IsInstanceIncompatibleType extends BugChecker
    implements MethodInvocationTreeMatcher, MemberReferenceTreeMatcher {
  private static final Matcher<ExpressionTree> IS_INSTANCE =
      instanceMethod().onExactClass("java.lang.Class").named("isInstance");

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!IS_INSTANCE.matches(tree, state)) {
      return NO_MATCH;
    }

    Type receiverType = classTypeArgument(tree);
    if (receiverType == null) {
      return NO_MATCH;
    }
    Type argumentType = getType(tree.getArguments().get(0));

    return isCastable(argumentType, receiverType, state)
        ? NO_MATCH
        : buildMessage(argumentType, receiverType, tree, state);
  }

  @Override
  public Description matchMemberReference(MemberReferenceTree tree, VisitorState state) {
    if (!IS_INSTANCE.matches(tree, state)) {
      return NO_MATCH;
    }

    Type type = state.getTypes().findDescriptorType(getType(tree));
    Type receiverType = classTypeArgument(tree);
    if (receiverType == null) {
      return NO_MATCH;
    }
    Type argumentType = ASTHelpers.getUpperBound(type.getParameterTypes().get(0), state.getTypes());

    return isCastable(argumentType, receiverType, state)
        ? NO_MATCH
        : buildMessage(argumentType, receiverType, tree, state);
  }

  private static @Nullable Type classTypeArgument(ExpressionTree tree) {
    ExpressionTree receiver = getReceiver(tree);
    if (receiver == null) {
      return null;
    }
    List<Type> receiverTypeArguments = getType(receiver).getTypeArguments();
    return !receiverTypeArguments.isEmpty() ? getOnlyElement(receiverTypeArguments) : null;
  }

  private Description buildMessage(Type type1, Type type2, Tree tree, VisitorState state) {
    return buildDescription(tree)
        .setMessage(
            String.format(
                "This expression will always evaluate to false because %s cannot be cast to %s",
                prettyType(state.getTypes().erasure(type1)),
                prettyType(state.getTypes().erasure(type2))))
        .build();
  }
}
