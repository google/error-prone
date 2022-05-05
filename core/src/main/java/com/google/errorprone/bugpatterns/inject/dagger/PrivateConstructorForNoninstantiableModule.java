/*
 * Copyright 2016 The Error Prone Authors.
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
package com.google.errorprone.bugpatterns.inject.dagger;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.errorprone.BugPattern.SeverityLevel.SUGGESTION;
import static com.google.errorprone.bugpatterns.inject.dagger.DaggerAnnotations.isBindingDeclarationMethod;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.isStatic;
import static com.google.errorprone.util.ASTHelpers.createPrivateConstructor;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.isGeneratedConstructor;
import static com.sun.source.tree.Tree.Kind.CLASS;
import static com.sun.source.tree.Tree.Kind.METHOD;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import java.util.function.Predicate;

/**
 * @author gak@google.com (Gregory Kick)
 */
@BugPattern(
    summary = "Add a private constructor to modules that will not be instantiated by Dagger.",
    severity = SUGGESTION)
public class PrivateConstructorForNoninstantiableModule extends BugChecker
    implements ClassTreeMatcher {
  @Override
  public Description matchClass(ClassTree classTree, VisitorState state) {
    if (!DaggerAnnotations.isAnyModule().matches(classTree, state)) {
      return NO_MATCH;
    }

    // if a module is declared as an interface, skip it
    if (!classTree.getKind().equals(CLASS)) {
      return NO_MATCH;
    }

    ImmutableList<Tree> nonSyntheticMembers =
        classTree.getMembers().stream()
            .filter(
                tree ->
                    !(tree.getKind().equals(METHOD) && isGeneratedConstructor((MethodTree) tree)))
            .collect(toImmutableList());

    // ignore empty modules
    if (nonSyntheticMembers.isEmpty()) {
      return NO_MATCH;
    }

    if (nonSyntheticMembers.stream().anyMatch(tree -> getSymbol(tree).isConstructor())) {
      return NO_MATCH;
    }

    boolean hasBindingDeclarationMethods =
        nonSyntheticMembers.stream()
            .anyMatch(matcherAsPredicate(isBindingDeclarationMethod(), state));

    if (hasBindingDeclarationMethods) {
      return describeMatch(classTree, addPrivateConstructor(classTree, state));
    }

    boolean allStaticMembers =
        nonSyntheticMembers.stream().allMatch(matcherAsPredicate(isStatic(), state));

    if (allStaticMembers) {
      return describeMatch(classTree, addPrivateConstructor(classTree, state));
    }

    return NO_MATCH;
  }

  private static Fix addPrivateConstructor(ClassTree classTree, VisitorState state) {
    return SuggestedFixes.addMembers(classTree, state, createPrivateConstructor(classTree));
  }

  private static <T extends Tree> Predicate<T> matcherAsPredicate(
      Matcher<? super T> matcher, VisitorState state) {
    return t -> matcher.matches(t, state);
  }
}
