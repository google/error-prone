/*
 * Copyright 2018 The Error Prone Authors.
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

import static com.google.common.collect.Iterables.getLast;
import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.assertEqualsInvocation;
import static com.google.errorprone.matchers.Matchers.assertNotEqualsInvocation;
import static com.google.errorprone.matchers.Matchers.instanceEqualsInvocation;
import static com.google.errorprone.matchers.Matchers.receiverOfInvocation;
import static com.google.errorprone.matchers.Matchers.staticEqualsInvocation;
import static com.google.errorprone.matchers.Matchers.staticMethod;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.google.errorprone.util.ASTHelpers.getReceiver;
import static com.google.errorprone.util.ASTHelpers.getReceiverType;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.isSameType;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Type;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Collection, Iterable, Multimap, and Queue do not have well-defined equals behavior.
 *
 * @author eleanorh@google.com (Eleanor Harris)
 */
@BugPattern(
    name = "UndefinedEquals",
    summary = "Collection, Iterable, Multimap, and Queue do not have well-defined equals behavior",
    severity = WARNING)
public final class UndefinedEquals extends BugChecker implements MethodInvocationTreeMatcher {

  private static final ImmutableList<String> BAD_CLASS_LIST =
      ImmutableList.of(
          "com.google.common.collect.Multimap",
          "java.lang.Iterable",
          "java.util.Collection",
          "java.util.Queue");

  private static final Matcher<MethodInvocationTree> ASSERT_THAT_EQUALS =
      allOf(
          instanceMethod()
              .onDescendantOf("com.google.common.truth.Subject")
              .withNameMatching(Pattern.compile("is(Not)?EqualTo")),
          receiverOfInvocation(
              anyOf(
                  staticMethod().onClass("com.google.common.truth.Truth").named("assertThat"),
                  instanceMethod()
                      .onDescendantOf("com.google.common.truth.StandardSubjectBuilder")
                      .named("that"))));

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    Type receiverType;
    Type argumentType;
    List<? extends ExpressionTree> arguments = tree.getArguments();

    if (staticEqualsInvocation().matches(tree, state)
        || assertEqualsInvocation().matches(tree, state)
        || assertNotEqualsInvocation().matches(tree, state)) {
      receiverType = getType(arguments.get(arguments.size() - 2));
      argumentType = getType(getLast(arguments));
    } else if (instanceEqualsInvocation().matches(tree, state)) {
      receiverType = getReceiverType(tree);
      argumentType = getType(arguments.get(0));
    } else if (ASSERT_THAT_EQUALS.matches(tree, state)) {
      receiverType = getType(getOnlyElement(arguments));
      argumentType =
          getType(getOnlyElement(((MethodInvocationTree) getReceiver(tree)).getArguments()));
    } else {
      return Description.NO_MATCH;
    }

    return BAD_CLASS_LIST
        .stream()
        .filter(
            t ->
                isSameType(receiverType, state.getTypeFromString(t), state)
                    || isSameType(argumentType, state.getTypeFromString(t), state))
        .findFirst()
        .map(
            b ->
                buildDescription(tree)
                    .setMessage(b + " does not have well-defined equals behavior")
                    .build())
        .orElse(Description.NO_MATCH);
  }
}
