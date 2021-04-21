/*
 * Copyright 2021 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;
import static com.google.errorprone.util.ASTHelpers.getReceiver;
import static com.google.errorprone.util.SideEffectAnalysis.hasSideEffect;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.predicates.type.DescendantOf;
import com.google.errorprone.suppliers.Suppliers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;

/** Highlights cases where a proto's build method has its return value ignored. */
@BugPattern(
    name = "ProtoBuilderReturnValueIgnored",
    summary =
        "Unnecessary call to proto's #build() method.  If you don't consume the return value of "
            + "#build(), the result is discarded and the only effect is to verify that all "
            + "required fields are set, which can be expressed more directly with "
            + "#isInitialized().",
    severity = ERROR)
public final class ProtoBuilderReturnValueIgnored extends AbstractReturnValueIgnored {

  private static final Matcher<ExpressionTree> BUILDER =
      instanceMethod()
          .onDescendantOf("com.google.protobuf.MessageLite.Builder")
          .namedAnyOf("build", "buildPartial");

  public static final Matcher<ExpressionTree> MATCHER =
      allOf(
          BUILDER,
          // Don't match expressions beginning with a newBuilder call; these should be covered by
          // ModifiedButNotUsed.
          ProtoBuilderReturnValueIgnored::doesNotTerminateInNewBuilder);

  private static final Matcher<ExpressionTree> ROOT_INVOCATIONS_TO_IGNORE =
      anyOf(
          staticMethod()
              .onClass(
                  new DescendantOf(Suppliers.typeFromString("com.google.protobuf.MessageLite")))
              .namedAnyOf("newBuilder"),
          instanceMethod()
              .onDescendantOf("com.google.protobuf.MessageLite")
              .namedAnyOf("toBuilder"));

  @Override
  public Matcher<? super ExpressionTree> specializedMatcher() {
    return MATCHER;
  }

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    Description description = super.matchMethodInvocation(tree, state);
    if (description.equals(Description.NO_MATCH)) {
      return Description.NO_MATCH;
    }
    return buildDescription(tree).addAllFixes(generateFixes(tree, state)).build();
  }

  private static ImmutableList<SuggestedFix> generateFixes(
      MethodInvocationTree tree, VisitorState state) {
    SuggestedFix.Builder simpleFixBuilder =
        SuggestedFix.builder()
            .setShortDescription(
                "Remove the call to #build. Note that this will not validate the presence "
                    + "of required fields.");
    ExpressionTree receiver = getReceiver(tree);
    if (receiver instanceof IdentifierTree || receiver instanceof MemberSelectTree) {
      simpleFixBuilder.replace(state.getPath().getParentPath().getLeaf(), "");
      if (hasSideEffect(receiver)) {
        simpleFixBuilder.setShortDescription(
            "Remove the call to #build. Note that this will not validate the presence "
                + "of required fields, and removes any side effects of the receiver expression.");
      }
    } else {
      simpleFixBuilder.replace(state.getEndPosition(receiver), state.getEndPosition(tree), "");
    }
    SuggestedFix simpleFix = simpleFixBuilder.build();
    SuggestedFix withRequiredFieldCheck =
        SuggestedFix.builder()
            .setShortDescription(
                "Replace the call to #build with an explicit check that required "
                    + "fields are present.")
            .addStaticImport("com.google.common.base.Preconditions.checkState")
            .prefixWith(tree, "checkState(")
            .replace(
                state.getEndPosition(receiver), state.getEndPosition(tree), ".isInitialized())")
            .build();
    return ImmutableList.of(simpleFix, withRequiredFieldCheck);
  }

  private static boolean doesNotTerminateInNewBuilder(ExpressionTree tree, VisitorState state) {
    for (ExpressionTree receiver = getReceiver(tree);
        receiver instanceof MethodInvocationTree;
        receiver = getReceiver(receiver)) {
      if (ROOT_INVOCATIONS_TO_IGNORE.matches(receiver, state)) {
        return false;
      }
    }
    return true;
  }
}
