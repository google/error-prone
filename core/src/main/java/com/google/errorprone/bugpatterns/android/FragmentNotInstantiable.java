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

package com.google.errorprone.bugpatterns.android;

import static com.google.errorprone.BugPattern.Category.ANDROID;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.isSubtypeOf;
import static com.google.errorprone.matchers.Matchers.kindIs;
import static com.google.errorprone.matchers.Matchers.nestingKind;
import static com.sun.source.tree.Tree.Kind.CLASS;
import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.NestingKind.MEMBER;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import java.util.List;
import java.util.stream.Collectors;

/** @author avenet@google.com (Arnaud J. Venet) */
@BugPattern(
    name = "FragmentNotInstantiable",
    altNames = {"ValidFragment"},
    summary =
        "Subclasses of Fragment must be instantiable via Class#newInstance():"
            + " the class must be public, static and have a public nullary constructor",
    category = ANDROID,
    severity = WARNING,
    tags = StandardTags.LIKELY_ERROR)
public class FragmentNotInstantiable extends BugChecker implements ClassTreeMatcher {
  private static final String MESSAGE_BASE = "Fragment is not instantiable: ";

  private static final String FRAGMENT_CLASS = "android.app.Fragment";

  // Static library support version of the framework's Fragment. Used to write apps that run on
  // platforms prior to Android 3.0.
  private static final String FRAGMENT_CLASS_V4 = "android.support.v4.app.Fragment";

  private final ImmutableSet<String> fragmentClasses;
  private final Matcher<ClassTree> fragmentMatcher;

  public FragmentNotInstantiable() {
    this(ImmutableSet.of());
  }

  protected FragmentNotInstantiable(Iterable<String> additionalFragmentClasses) {
    fragmentClasses =
        ImmutableSet.<String>builder()
            .add(FRAGMENT_CLASS)
            .add(FRAGMENT_CLASS_V4)
            .addAll(additionalFragmentClasses)
            .build();
    fragmentMatcher =
        allOf(
            kindIs(CLASS),
            anyOf(
                fragmentClasses.stream()
                    .map(fragmentClass -> isSubtypeOf(fragmentClass))
                    .collect(Collectors.toList())));
  }

  private Description buildErrorMessage(Tree tree, String explanation) {
    Description.Builder description = buildDescription(tree);
    description.setMessage(MESSAGE_BASE + explanation + ".");
    return description.build();
  }

  @Override
  public Description matchClass(ClassTree classTree, VisitorState state) {
    if (!fragmentMatcher.matches(classTree, state)) {
      return Description.NO_MATCH;
    }

    String className = ASTHelpers.getSymbol(classTree).toString();
    if (fragmentClasses.contains(className)) {
      // Do not check the base class declarations (or their stubs).
      return Description.NO_MATCH;
    }

    // The check doesn't apply to abstract classes.
    if (classTree.getModifiers().getFlags().contains(ABSTRACT)) {
      return Description.NO_MATCH;
    }
    // A fragment must be public.
    if (!classTree.getModifiers().getFlags().contains(PUBLIC)) {
      return buildErrorMessage(classTree, "a fragment must be public");
    }

    // A fragment that is an inner class must be static.
    if (nestingKind(MEMBER).matches(classTree, state)
        && !ASTHelpers.getSymbol(classTree).isStatic()) {
      return buildErrorMessage(classTree, "a fragment inner class must be static");
    }

    List<MethodTree> constructors = ASTHelpers.getConstructors(classTree);
    for (MethodTree constructor : constructors) {
      if (constructor.getParameters().isEmpty()) {
        // The nullary constructor exists. We must make sure that it is public.
        if (!constructor.getModifiers().getFlags().contains(PUBLIC)) {
          return buildErrorMessage(constructor, "the nullary constructor must be public");
        }
        return Description.NO_MATCH;
      }
    }

    // If we reach this point, we know for sure that the class has at least one constructor
    // but no nullary constructor.
    return buildErrorMessage(classTree, "the nullary constructor is missing");
  }
}
