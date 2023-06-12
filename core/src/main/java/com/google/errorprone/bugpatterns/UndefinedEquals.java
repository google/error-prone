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
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.isSameType;
import static com.google.errorprone.util.ASTHelpers.isSubtype;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.suppliers.Supplier;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

/**
 * Flags types which do not have well-defined equals behavior.
 *
 * @author eleanorh@google.com (Eleanor Harris)
 */
@BugPattern(
    summary = "This type is not guaranteed to implement a useful #equals method.",
    severity = WARNING)
public final class UndefinedEquals extends BugChecker implements MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> IS_EQUAL_TO =
      instanceMethod().onDescendantOf("com.google.common.truth.Subject").named("isEqualTo");

  private static final Matcher<MethodInvocationTree> ASSERT_THAT_EQUALS =
      allOf(
          instanceMethod()
              .onDescendantOf("com.google.common.truth.Subject")
              .namedAnyOf("isEqualTo", "isNotEqualTo"),
          receiverOfInvocation(
              anyOf(
                  staticMethod().onClass("com.google.common.truth.Truth").named("assertThat"),
                  instanceMethod()
                      .onDescendantOf("com.google.common.truth.StandardSubjectBuilder")
                      .named("that"))));

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    Tree receiver;
    Tree argument;
    List<? extends ExpressionTree> arguments = tree.getArguments();

    if (staticEqualsInvocation().matches(tree, state)
        || assertEqualsInvocation().matches(tree, state)
        || assertNotEqualsInvocation().matches(tree, state)) {
      receiver = arguments.get(arguments.size() - 2);
      argument = getLast(arguments);
    } else if (instanceEqualsInvocation().matches(tree, state)) {
      receiver = getReceiver(tree);
      argument = arguments.get(0);
    } else if (ASSERT_THAT_EQUALS.matches(tree, state)) {
      receiver = getOnlyElement(arguments);
      argument = getOnlyElement(((MethodInvocationTree) getReceiver(tree)).getArguments());
    } else {
      return Description.NO_MATCH;
    }

    return Arrays.stream(TypesWithUndefinedEquality.values())
        .filter(
            b -> b.matchesType(getType(receiver), state) || b.matchesType(getType(argument), state))
        .findFirst()
        .map(
            b ->
                buildDescription(tree)
                    .setMessage(
                        "Subtypes of "
                            + b.shortName()
                            + " are not guaranteed to implement a useful #equals method.")
                    .addFix(
                        generateFix(tree, state, receiver, argument)
                            .orElse(SuggestedFix.emptyFix()))
                    .build())
        .orElse(Description.NO_MATCH);
  }

  private static Optional<SuggestedFix> generateFix(
      MethodInvocationTree tree, VisitorState state, Tree receiver, Tree argument) {
    // Generate fix for certain Truth `isEqualTo` calls
    if (IS_EQUAL_TO.matches(tree, state)) {
      String methodText =
          state.getSourceForNode(tree.getMethodSelect()); // e.g. "assertThat(foo).isEqualTo"
      String assertThatWithArg = methodText.substring(0, methodText.lastIndexOf('.'));

      // If both the argument and receiver are subtypes of the given type, rewrites the isEqualTo
      // method invocation to use the replacement comparison method instead.
      BiFunction<Type, String, Optional<SuggestedFix>> generateTruthFix =
          (type, replacementMethod) -> {
            if (type != null
                && isSubtype(getType(argument), type, state)
                && isSubtype(getType(receiver), type, state)) {
              return Optional.of(
                  SuggestedFix.replace(
                      tree,
                      String.format(
                          "%s.%s(%s)",
                          assertThatWithArg, replacementMethod, state.getSourceForNode(receiver))));
            }
            return Optional.empty();
          };

      // If both are subtypes of Iterable, rewrite
      Type iterableType = state.getSymtab().iterableType;
      Type multimapType = COM_GOOGLE_COMMON_COLLECT_MULTIMAP.get(state);
      Optional<SuggestedFix> fix =
          firstPresent(
              generateTruthFix.apply(iterableType, "containsExactlyElementsIn"),
              generateTruthFix.apply(multimapType, "containsExactlyEntriesIn"));
      if (fix.isPresent()) {
        return fix;
      }
    }

    // Generate fix for CharSequence
    Type charSequenceType = JAVA_LANG_CHARSEQUENCE.get(state);
    BiFunction<Tree, Tree, Optional<SuggestedFix>> generateCharSequenceFix =
        (maybeCharSequence, maybeString) -> {
          if (charSequenceType != null
              && isSameType(getType(maybeCharSequence), charSequenceType, state)
              && isSameType(getType(maybeString), state.getSymtab().stringType, state)) {
            return Optional.of(SuggestedFix.postfixWith(maybeCharSequence, ".toString()"));
          }
          return Optional.empty();
        };
    return firstPresent(
        generateCharSequenceFix.apply(receiver, argument),
        generateCharSequenceFix.apply(argument, receiver));
  }

  private static <T> Optional<T> firstPresent(Optional<T>... optionals) {
    for (Optional<T> optional : optionals) {
      if (optional.isPresent()) {
        return optional;
      }
    }
    return Optional.empty();
  }

  private static final Supplier<Type> COM_GOOGLE_COMMON_COLLECT_MULTIMAP =
      VisitorState.memoize(state -> state.getTypeFromString("com.google.common.collect.Multimap"));

  private static final Supplier<Type> JAVA_LANG_CHARSEQUENCE =
      VisitorState.memoize(state -> state.getTypeFromString("java.lang.CharSequence"));
}
