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

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.errorprone.BugPattern.ProvidesFix.NO_FIX;
import static com.google.errorprone.BugPattern.ProvidesFix.REQUIRES_HUMAN_ATTENTION;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.util.ASTHelpers.getSymbol;

import com.google.common.collect.Iterables;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.AnnotationMatcherUtils;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.matchers.method.MethodMatchers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreeScanner;

@BugPattern(
    name = "ProvidesFix",
    summary = "BugChecker has incorrect ProvidesFix tag, please update",
    severity = WARNING,
    providesFix = REQUIRES_HUMAN_ATTENTION)
public class ProvidesFixChecker extends BugChecker implements ClassTreeMatcher {

  private static final Matcher<ClassTree> IS_BUGCHECKER =
      Matchers.isSubtypeOf("com.google.errorprone.bugpatterns.BugChecker");
  private static final Matcher<MethodInvocationTree> DESCRIPTION_WITH_FIX =
      anyOf(
          MethodMatchers.instanceMethod()
              .onDescendantOf("com.google.errorprone.matchers.Description.Builder")
              .namedAnyOf("addFix", "addAllFixes"),
          MethodMatchers.instanceMethod()
              .onDescendantOf("com.google.errorprone.bugpatterns.BugChecker")
              .named("describeMatch")
              .withParameters("com.sun.source.tree.Tree", "com.google.errorprone.fixes.Fix"));
  private static final Matcher<ExpressionTree> DESCRIPTION_CONSTRUCTOR =
      Matchers.constructor()
          .forClass("com.google.errorprone.matchers.Description")
          .withParameters(
              "com.sun.source.tree.Tree",
              "java.lang.String",
              "com.google.errorprone.fixes.Fix",
              "com.google.errorprone.BugPattern.SeverityLevel");

  @Override
  public Description matchClass(ClassTree tree, VisitorState state) {
    if (!IS_BUGCHECKER.matches(tree, state)) {
      return NO_MATCH;
    }

    AnnotationTree bugPatternAnnotation =
        ASTHelpers.getAnnotationWithSimpleName(tree.getModifiers().getAnnotations(), "BugPattern");
    if (bugPatternAnnotation == null) {
      return NO_MATCH;
    }

    if (!providesFix(tree, state)) {
      // N.B. BugCheckers can provide a fix in ways we cannot detect.
      // Ignore checkers that don't appear to have a fix, but are labeled as having one.
      return NO_MATCH;
    }

    SuggestedFix.Builder fixBuilder =
        SuggestedFix.builder().addImport("com.google.errorprone.BugPattern.ProvidesFix");
    ExpressionTree providesFixArgument =
        AnnotationMatcherUtils.getArgument(bugPatternAnnotation, "providesFix");
    if (providesFixArgument == null) {
      // If providesFix argument not already present, add it to the end of @BugPattern args.
      fixBuilder.postfixWith(
          Iterables.getLast(bugPatternAnnotation.getArguments()),
          ", providesFix = ProvidesFix." + REQUIRES_HUMAN_ATTENTION.name());
    } else {
      if (!getSymbol(providesFixArgument).getSimpleName().contentEquals(NO_FIX.name())) {
        // If providesFix arg already states there is a fix, we're good.
        return NO_MATCH;
      }
      // If providesFix arg incorrectly states there is no fix, correct it.
      fixBuilder.replace(providesFixArgument, "ProvidesFix." + REQUIRES_HUMAN_ATTENTION.name());
    }
    return describeMatch(bugPatternAnnotation, fixBuilder.build());
  }

  private static boolean providesFix(Tree tree, VisitorState state) {
    return tree.accept(
        new TreeScanner<Boolean, Void>() {
          @Override
          public Boolean visitMethodInvocation(MethodInvocationTree callTree, Void unused) {
            return super.visitMethodInvocation(callTree, unused)
                || DESCRIPTION_WITH_FIX.matches(callTree, state);
          }

          @Override
          public Boolean visitNewClass(NewClassTree constructorTree, Void unused) {
            return super.visitNewClass(constructorTree, unused)
                || DESCRIPTION_CONSTRUCTOR.matches(constructorTree, state);
          }

          @Override
          public Boolean reduce(Boolean m1, Boolean m2) {
            return firstNonNull(m1, false) || firstNonNull(m2, false);
          }
        },
        null);
  }
}
