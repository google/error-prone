/*
 * Copyright 2015 The Error Prone Authors.
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
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.instanceEqualsInvocation;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.matchers.Matchers.staticEqualsInvocation;
import static com.google.errorprone.matchers.Matchers.staticMethod;
import static com.google.errorprone.matchers.Matchers.toType;
import static com.google.errorprone.util.ASTHelpers.getGeneratedBy;
import static com.google.errorprone.util.ASTHelpers.getReceiver;
import static com.google.errorprone.util.ASTHelpers.getReceiverType;
import static com.google.errorprone.util.ASTHelpers.getType;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MemberReferenceTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.TypeCompatibility.TypeCompatibilityReport;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.google.errorprone.util.Signatures;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Type;
import javax.inject.Inject;

/**
 * @author avenet@google.com (Arnaud J. Venet)
 */
@BugPattern(
    summary = "An equality test between objects with incompatible types always returns false",
    severity = WARNING)
public class EqualsIncompatibleType extends BugChecker
    implements MethodInvocationTreeMatcher, MemberReferenceTreeMatcher {
  private static final Matcher<ExpressionTree> STATIC_EQUALS_MATCHER = staticEqualsInvocation();

  private static final Matcher<ExpressionTree> INSTANCE_EQUALS_MATCHER = instanceEqualsInvocation();

  private static final Matcher<ExpressionTree> IS_EQUAL_MATCHER =
      staticMethod().onClass("java.util.function.Predicate").named("isEqual");

  private static final Matcher<Tree> ASSERT_FALSE_MATCHER =
      toType(
          MethodInvocationTree.class,
          anyOf(
              instanceMethod().anyClass().named("assertFalse"),
              staticMethod().anyClass().named("assertFalse")));

  private final TypeCompatibility typeCompatibility;

  @Inject
  EqualsIncompatibleType(TypeCompatibility typeCompatibility) {
    this.typeCompatibility = typeCompatibility;
  }

  @Override
  public Description matchMethodInvocation(
      MethodInvocationTree invocationTree, VisitorState state) {
    if (STATIC_EQUALS_MATCHER.matches(invocationTree, state)) {
      return match(
          invocationTree,
          getType(invocationTree.getArguments().get(0)),
          getType(invocationTree.getArguments().get(1)),
          state);
    }
    if (INSTANCE_EQUALS_MATCHER.matches(invocationTree, state)) {
      return match(
          invocationTree,
          getReceiverType(invocationTree),
          getType(invocationTree.getArguments().get(0)),
          state);
    }
    if (IS_EQUAL_MATCHER.matches(invocationTree, state)) {
      Type targetType = ASTHelpers.targetType(state).type();
      if (targetType.getTypeArguments().size() != 1) {
        return NO_MATCH;
      }
      return match(
          invocationTree,
          getType(invocationTree.getArguments().get(0)),
          state.getTypes().wildLowerBound(getOnlyElement(targetType.getTypeArguments())),
          state);
    }

    return NO_MATCH;
  }

  @Override
  public Description matchMemberReference(MemberReferenceTree tree, VisitorState state) {
    if (STATIC_EQUALS_MATCHER.matches(tree, state)) {
      Type type = state.getTypes().findDescriptorType(getType(tree));
      return match(tree, type.getParameterTypes().get(0), type.getParameterTypes().get(1), state);
    }
    if (INSTANCE_EQUALS_MATCHER.matches(tree, state)) {
      Type type = state.getTypes().findDescriptorType(getType(tree));
      return match(tree, getType(getReceiver(tree)), type.getParameterTypes().get(0), state);
    }
    if (IS_EQUAL_MATCHER.matches(tree, state)) {
      Type type = state.getTypes().findDescriptorType(getType(tree));
      if (type.getReturnType().getTypeArguments().size() != 1) {
        return NO_MATCH;
      }
      return match(
          tree,
          getOnlyElement(type.getReturnType().getTypeArguments()),
          type.getParameterTypes().get(0),
          state);
    }

    return NO_MATCH;
  }

  private Description match(
      ExpressionTree invocationTree, Type receiverType, Type argumentType, VisitorState state) {
    TypeCompatibilityReport compatibilityReport =
        typeCompatibility.compatibilityOfTypes(receiverType, argumentType, state);
    if (compatibilityReport.isCompatible()) {
      return NO_MATCH;
    }

    // Ignore callsites wrapped inside assertFalse:
    // assertFalse(objOfReceiverType.equals(objOfArgumentType))
    if (ASSERT_FALSE_MATCHER.matches(state.getPath().getParentPath().getLeaf(), state)) {
      return NO_MATCH;
    }

    if (getGeneratedBy(state).contains("com.google.auto.value.processor.AutoValueProcessor")) {
      return NO_MATCH;
    }

    // When we reach this point, we know that the two following facts hold:
    // (1) The types of the receiver and the argument to the eventual invocation of
    //     java.lang.Object.equals() are incompatible.
    // (2) No common superclass (other than java.lang.Object) or interface of the receiver and the
    //     argument defines an override of java.lang.Object.equals().
    // This equality test almost certainly evaluates to false, which is very unlikely to be the
    // programmer's intent. Hence, this is reported as an error. There is no sensible fix to suggest
    // in this situation.
    return buildDescription(invocationTree)
        .setMessage(
            getMessage(
                    invocationTree,
                    receiverType,
                    argumentType,
                    compatibilityReport.lhs(),
                    compatibilityReport.rhs(),
                    state)
                + compatibilityReport.extraReason())
        .build();
  }

  private static String getMessage(
      ExpressionTree invocationTree,
      Type receiverType,
      Type argumentType,
      Type conflictingReceiverType,
      Type conflictingArgumentType,
      VisitorState state) {
    TypeStringPair typeStringPair = new TypeStringPair(receiverType, argumentType);
    String baseMessage =
        "Calling "
            + ASTHelpers.getSymbol(invocationTree).getSimpleName()
            + " on incompatible types "
            + typeStringPair.getReceiverTypeString()
            + " and "
            + typeStringPair.getArgumentTypeString();

    if (state.getTypes().isSameType(receiverType, conflictingReceiverType)) {
      return baseMessage;
    }
    // If receiver/argument are incompatible due to a conflict in the generic type, message that out
    TypeStringPair conflictingTypes =
        new TypeStringPair(conflictingReceiverType, conflictingArgumentType);
    return baseMessage
        + ". They are incompatible because "
        + conflictingTypes.getReceiverTypeString()
        + " and "
        + conflictingTypes.getArgumentTypeString()
        + " are incompatible.";
  }

  private static class TypeStringPair {
    private String receiverTypeString;
    private String argumentTypeString;

    private TypeStringPair(Type receiverType, Type argumentType) {
      receiverTypeString = Signatures.prettyType(receiverType);
      argumentTypeString = Signatures.prettyType(argumentType);
      if (argumentTypeString.equals(receiverTypeString)) {
        receiverTypeString = receiverType.toString();
        argumentTypeString = argumentType.toString();
      }
    }

    private String getReceiverTypeString() {
      return receiverTypeString;
    }

    private String getArgumentTypeString() {
      return argumentTypeString;
    }
  }
}
