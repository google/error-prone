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

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.bugpatterns.ImplementAssertionWithChaining.makeCheckDescription;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.matchers.Matchers.staticMethod;
import static com.google.errorprone.util.ASTHelpers.getDeclaredSymbol;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.isSubtype;
import static com.sun.source.tree.Tree.Kind.CLASS;
import static com.sun.source.tree.Tree.Kind.METHOD_INVOCATION;
import static java.lang.String.format;
import static java.util.stream.Stream.concat;
import static javax.lang.model.element.Modifier.STATIC;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.annotations.FormatMethod;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import javax.annotation.Nullable;

/**
 * Identifies calls to {@code assertThat} and similar methods inside the implementation of a {@code
 * Subject} assertion method. These calls should instead use {@code check(...)}.
 *
 * <pre>{@code
 * // Before:
 * public void hasFoo() {
 *   assertThat(actual().foo()).isEqualTo(expected);
 * }
 *
 * // After:
 * public void hasFoo() {
 *   check("foo()").that(actual().foo()).isEqualTo(expected);
 * }
 * }</pre>
 */
@BugPattern(
    name = "ChainedAssertionLosesContext",
    summary =
        "Inside a Subject, use check(...) instead of assert*() to preserve user-supplied messages"
            + " and other settings.",
    severity = WARNING)
public final class ChainedAssertionLosesContext extends BugChecker
    implements MethodInvocationTreeMatcher {
  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!inInstanceMethodOfSubjectImplementation(state)) {
      return NO_MATCH;
    }

    if (STANDARD_ASSERT_THAT.matches(tree, state)) {
      String checkDescription = makeCheckDescription(getOnlyElement(tree.getArguments()), state);
      if (checkDescription == null) {
        // TODO(cpovirk): Generate a suggested fix without a check description.
        return NO_MATCH;
      }
      return replace(tree.getMethodSelect(), "check(%s).that", checkDescription);
    } else if (ANY_ASSERT_THAT.matches(tree, state)) {
      FactoryMethodName factory = tryFindFactory(tree, state);
      if (factory == null) {
        // TODO(cpovirk): Generate a warning that instructs the user to find or expose a factory.
        return NO_MATCH;
      }
      if (tree.getArguments().size() != 1) {
        // TODO(cpovirk): Generate a suggested fix without a check description.
        return NO_MATCH;
      }
      String checkDescription = makeCheckDescription(getOnlyElement(tree.getArguments()), state);
      if (checkDescription == null) {
        // TODO(cpovirk): Generate a suggested fix without a check description.
        return NO_MATCH;
      }
      return describeMatch(
          tree,
          SuggestedFix.builder()
              .addStaticImport(factory.clazz() + "." + factory.method())
              .replace(
                  tree.getMethodSelect(),
                  format("check(%s).about(%s()).that", checkDescription, factory.method()))
              .build());
    } else if (ASSERT_ABOUT.matches(tree, state)) {
      String checkDescription = findThatCallAndMakeCheckDescription(state);
      if (checkDescription == null) {
        // TODO(cpovirk): Generate a suggested fix without a check description.
        return NO_MATCH;
      }
      return replace(tree.getMethodSelect(), "check(%s).about", checkDescription);
    } else if (ASSERT_WITH_MESSAGE.matches(tree, state)) {
      String checkDescription = findThatCallAndMakeCheckDescription(state);
      if (checkDescription == null) {
        // TODO(cpovirk): Generate a suggested fix without a check description.
        return NO_MATCH;
      }
      return replace(tree.getMethodSelect(), "check(%s).withMessage", checkDescription);
    } else if (ASSERT.matches(tree, state)) {
      String checkDescription = findThatCallAndMakeCheckDescription(state);
      if (checkDescription == null) {
        // TODO(cpovirk): Generate a suggested fix without a check description.
        return NO_MATCH;
      }
      return replace(tree, "check(%s)", checkDescription);
    } else {
      /*
       * TODO(cpovirk): If it's an assertThat method other than Truth.assertThat, then find the
       * appropriate Subject.Factory, and generate check().about(...).that(...).
       */
      return NO_MATCH;
    }
  }

  /**
   * Starting from a {@code VisitorState} pointing at part of a fluent assertion statement (like
   * {@code check()} or {@code assertWithMessage()}, walks up the tree and returns the subsequent
   * call to {@code that(...)}.
   *
   * <p>Often, the call is made directly on the result of the given tree -- like when the input is
   * {@code check()}, which is part of the expression {@code check().that(...)}. But sometimes there
   * is an intervening call to {@code withMessage}, {@code about}, or both.
   */
  @Nullable
  static MethodInvocationTree findThatCall(VisitorState state) {
    TreePath path = state.getPath();
    /*
     * Each iteration walks 1 method call up the tree, but it's actually 2 steps in the tree because
     * there's a MethodSelectTree between each pair of MethodInvocationTrees.
     */
    while (true) {
      path = path.getParentPath().getParentPath();
      Tree leaf = path.getLeaf();
      if (leaf.getKind() != METHOD_INVOCATION) {
        return null;
      }
      MethodInvocationTree maybeThatCall = (MethodInvocationTree) leaf;
      if (WITH_MESSAGE_OR_ABOUT.matches(maybeThatCall, state)) {
        continue;
      } else if (SUBJECT_BUILDER_THAT.matches(maybeThatCall, state)) {
        return maybeThatCall;
      } else {
        return null;
      }
    }
  }

  @FormatMethod
  private Description replace(Tree tree, String format, Object... args) {
    return describeMatch(tree, SuggestedFix.replace(tree, String.format(format, args)));
  }

  @AutoValue
  abstract static class FactoryMethodName {
    static FactoryMethodName create(String clazz, String method) {
      return new AutoValue_ChainedAssertionLosesContext_FactoryMethodName(clazz, method);
    }

    @Nullable
    static FactoryMethodName tryCreate(MethodSymbol symbol) {
      return symbol.params.isEmpty()
          ? create(symbol.owner.getQualifiedName().toString(), symbol.getSimpleName().toString())
          : null;
    }

    abstract String clazz();

    abstract String method();
  }

  @Nullable
  private static FactoryMethodName tryFindFactory(
      MethodInvocationTree assertThatCall, VisitorState state) {
    MethodSymbol assertThatSymbol = getSymbol(assertThatCall);
    if (assertThatSymbol == null) {
      return null;
    }
    /*
     * First, a special case for ProtoTruth.protos(). Usually the main case below finds it OK, but
     * sometimes it misses it, I believe because it can't decide between that and
     * IterableOfProtosSubject.iterableOfMessages.
     */
    if (assertThatSymbol.owner.getQualifiedName().contentEquals(PROTO_TRUTH_CLASS)) {
      return FactoryMethodName.create(PROTO_TRUTH_CLASS, "protos");
    }
    ImmutableSet<MethodSymbol> factories =
        concat(
                // The class that assertThat is declared in:
                assertThatSymbol.owner.getEnclosedElements().stream(),
                // The Subject class (possibly the same; if so, toImmutableSet() will deduplicate):
                assertThatSymbol.getReturnType().asElement().getEnclosedElements().stream())
            .filter(s -> s instanceof MethodSymbol)
            .map(s -> (MethodSymbol) s)
            .filter(
                s ->
                    returns(s, SUBJECT_FACTORY_CLASS, state)
                        || returns(s, CUSTOM_SUBJECT_BUILDER_FACTORY_CLASS, state))
            .collect(toImmutableSet());
    return factories.size() == 1 ? FactoryMethodName.tryCreate(getOnlyElement(factories)) : null;
    // TODO(cpovirk): If multiple factories exist, try filtering to visible ones only.
  }

  private static boolean returns(MethodSymbol symbol, String returnType, VisitorState state) {
    return isSubtype(symbol.getReturnType(), state.getTypeFromString(returnType), state);
  }

  private static boolean inInstanceMethodOfSubjectImplementation(VisitorState state) {
    /*
     * All the checks here are mostly a no-op because, in static methods or methods outside Subject,
     * makeCheckDescription will fail to find a call to actual(), so the check won't fire. But they
     * set up for a future in which we issue a warning even if we can't produce a check description
     * for the suggested fix automatically.
     */
    TreePath pathToEnclosingMethod = state.findPathToEnclosing(MethodTree.class);
    if (pathToEnclosingMethod == null) {
      return false;
    }
    MethodTree enclosingMethod = (MethodTree) pathToEnclosingMethod.getLeaf();
    if (enclosingMethod.getModifiers().getFlags().contains(STATIC)) {
      return false;
    }
    Tree enclosingClass = pathToEnclosingMethod.getParentPath().getLeaf();
    if (enclosingClass == null || enclosingClass.getKind() != CLASS) {
      return false;
    }
    /*
     * TODO(cpovirk): Ideally we'd also recognize types nested inside Subject implementations, like
     * IterableSubject.UsingCorrespondence.
     */
    return isSubtype(
        getDeclaredSymbol(enclosingClass).type, state.getTypeFromString(SUBJECT_CLASS), state);
  }

  @Nullable
  private static String findThatCallAndMakeCheckDescription(VisitorState state) {
    MethodInvocationTree thatCall = findThatCall(state);
    if (thatCall == null) {
      return null;
    }
    return makeCheckDescription(getOnlyElement(thatCall.getArguments()), state);
  }

  private static final String TRUTH_CLASS = "com.google.common.truth.Truth";
  private static final String PROTO_TRUTH_CLASS =
      "com.google.common.truth.extensions.proto.ProtoTruth";
  private static final String SUBJECT_CLASS = "com.google.common.truth.Subject";
  private static final String SUBJECT_FACTORY_CLASS = "com.google.common.truth.Subject.Factory";
  private static final String CUSTOM_SUBJECT_BUILDER_FACTORY_CLASS =
      "com.google.common.truth.CustomSubjectBuilder.Factory";

  private static final Matcher<ExpressionTree> STANDARD_ASSERT_THAT =
      staticMethod().onClass(TRUTH_CLASS).named("assertThat");
  private static final Matcher<ExpressionTree> ANY_ASSERT_THAT =
      staticMethod().anyClass().named("assertThat");
  private static final Matcher<ExpressionTree> ASSERT_ABOUT =
      staticMethod().onClass(TRUTH_CLASS).named("assertAbout");
  private static final Matcher<ExpressionTree> ASSERT_WITH_MESSAGE =
      staticMethod().onClass(TRUTH_CLASS).named("assertWithMessage");
  private static final Matcher<ExpressionTree> ASSERT =
      staticMethod().onClass(TRUTH_CLASS).named("assert_");

  private static final Matcher<ExpressionTree> SUBJECT_BUILDER_THAT =
      anyOf(
          instanceMethod()
              .onDescendantOf("com.google.common.truth.CustomSubjectBuilder")
              .named("that"),
          instanceMethod()
              .onDescendantOf("com.google.common.truth.SimpleSubjectBuilder")
              .named("that"),
          instanceMethod()
              .onDescendantOf("com.google.common.truth.StandardSubjectBuilder")
              .named("that"));

  private static final Matcher<ExpressionTree> WITH_MESSAGE_OR_ABOUT =
      instanceMethod()
          .onDescendantOf("com.google.common.truth.StandardSubjectBuilder")
          .namedAnyOf("withMessage", "about");
}
