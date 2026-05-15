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

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.common.collect.Sets.immutableEnumSet;
import static com.google.common.collect.Sets.intersection;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.fixes.SuggestedFixes.removeModifiers;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.annotationsAmong;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.hasDirectAnnotationWithSimpleName;
import static com.google.errorprone.util.ASTHelpers.streamSuperMethods;
import static java.lang.String.format;
import static java.util.stream.Collectors.joining;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Streams;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.InjectMatchers;
import com.google.errorprone.suppliers.Supplier;
import com.sun.source.tree.MethodTree;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.util.Name;
import java.util.Set;
import java.util.stream.Stream;
import javax.lang.model.element.Modifier;

/** Suggests restricting the visibility of methods which should only be called by a framework. */
@BugPattern(
    altNames = "RestrictInjectVisibility",
    summary =
        "Some methods (such as those annotated with @Inject or @Provides) are only intended to be"
            + " called by a framework, and so should have default visibility.",
    severity = WARNING)
public final class UnnecessarilyVisible extends BugChecker implements MethodTreeMatcher {
  private static final ImmutableSet<Modifier> VISIBILITY_MODIFIERS =
      immutableEnumSet(Modifier.PROTECTED, Modifier.PUBLIC);

  private static final Supplier<ImmutableSet<Name>> FRAMEWORK_ANNOTATIONS =
      VisitorState.memoize(
          s ->
              Streams.concat(
                      InjectMatchers.INJECT_ANNOTATIONS.stream(),
                      InjectMatchers.PROVIDES_ANNOTATIONS.stream(),
                      InjectMatchers.MULTIBINDINGS_ANNOTATIONS.stream(),
                      Stream.of(
                          "com.google.errorprone.refaster.annotation.AfterTemplate",
                          "com.google.errorprone.refaster.annotation.BeforeTemplate"))
                  .map(s::getName)
                  .collect(toImmutableSet()));

  private static final Supplier<ImmutableSet<Name>> INJECT_ANNOTATIONS =
      VisitorState.memoize(
          s ->
              InjectMatchers.INJECT_ANNOTATIONS.stream().map(s::getName).collect(toImmutableSet()));

  private static final String VISIBLE_FOR_TESTING_CAVEAT =
      " If this is only for testing purposes, consider annotating the element with"
          + " @VisibleForTesting.";

  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    MethodSymbol symbol = getSymbol(tree);
    var annotations = annotationsAmong(symbol, FRAMEWORK_ANNOTATIONS.get(state), state);
    if (annotations.isEmpty()) {
      return NO_MATCH;
    }
    if (streamSuperMethods(symbol, state.getTypes()).findAny().isPresent()) {
      return NO_MATCH;
    }
    if (hasDirectAnnotationWithSimpleName(tree, "VisibleForTesting")) {
      return NO_MATCH;
    }
    Set<Modifier> badModifiers = intersection(tree.getModifiers().getFlags(), VISIBILITY_MODIFIERS);
    if (badModifiers.isEmpty()) {
      return NO_MATCH;
    }
    return buildDescription(tree)
        .setMessage(
            format(
                "Methods annotated with %s are intended to be called by a framework, and so should"
                    + " have default visibility.",
                annotations.stream()
                    .map(n -> "@" + n.toString().replaceFirst("^.+\\.", ""))
                    .collect(joining(", "))))
        .addFix(
            removeModifiers(tree.getModifiers(), state, badModifiers)
                .orElse(SuggestedFix.emptyFix()))
        .setMessage(
            message()
                + (annotationsAmong(symbol, INJECT_ANNOTATIONS.get(state), state).isEmpty()
                    ? ""
                    : VISIBLE_FOR_TESTING_CAVEAT))
        .build();
  }
}
