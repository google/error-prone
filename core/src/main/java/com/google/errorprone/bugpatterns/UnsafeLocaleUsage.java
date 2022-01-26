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

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Matchers.constructor;
import static com.google.errorprone.matchers.Matchers.instanceMethod;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.NewClassTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.tools.javac.tree.JCTree.JCLiteral;

/** Flags unsafe usages of the {@link java.util.Locale} constructor and class methods. */
@BugPattern(
    summary = "Possible unsafe operation related to the java.util.Locale library.",
    severity = WARNING)
public final class UnsafeLocaleUsage extends BugChecker
    implements MethodInvocationTreeMatcher, NewClassTreeMatcher {

  private static final Matcher<ExpressionTree> LOCALE_TO_STRING =
      instanceMethod().onExactClass("java.util.Locale").named("toString");
  private static final Matcher<ExpressionTree> LOCALE_CONSTRUCTOR =
      constructor().forClass("java.util.Locale");

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (LOCALE_TO_STRING.matches(tree, state)) {
      return buildDescription(tree)
          .setMessage(
              "Avoid using Locale.toString() since it produces a value that"
                  + " misleadingly looks like a locale identifier. Prefer using"
                  + " Locale.toLanguageTag() since it produces an IETF BCP 47-formatted string that"
                  + " can be deserialized back into a Locale.")
          .addFix(SuggestedFixes.renameMethodInvocation(tree, "toLanguageTag", state))
          .build();
    }
    return Description.NO_MATCH;
  }

  @Override
  public Description matchNewClass(NewClassTree tree, VisitorState state) {
    if (LOCALE_CONSTRUCTOR.matches(tree, state)) {
      Description.Builder descriptionBuilder =
          buildDescription(tree)
              .setMessage(
                  "Avoid using Locale constructors, and prefer using"
                      + " Locale.forLanguageTag(String) which takes in an IETF BCP 47-formatted"
                      + " string or a Locale Builder.");

      // Only suggest a fix for constructor calls with one or two parameters since there's
      // too much variance in multi-parameter calls to be able to make a confident suggestion
      ImmutableList<ExpressionTree> constructorArguments =
          ImmutableList.copyOf(tree.getArguments());
      if (constructorArguments.size() == 1) {
        // Locale.forLanguageTag() doesn't support underscores in language tags. We can replace this
        // ourselves when the constructor arg is a string literal. Otherwise, we can only append a
        // .replace() to it.
        ExpressionTree arg = constructorArguments.get(0);
        String replacementArg =
            arg instanceof JCLiteral
                ? String.format(
                    "\"%s\"", ASTHelpers.constValue(arg, String.class).replace("_", "-"))
                : String.format(
                    "%s.replace(\"_\", \"-\")",
                    state.getSourceForNode(constructorArguments.get(0)));

        descriptionBuilder.addFix(
            SuggestedFix.replace(tree, String.format("Locale.forLanguageTag(%s)", replacementArg)));
      } else if (constructorArguments.size() == 2) {
        descriptionBuilder.addFix(
            SuggestedFix.replace(
                tree,
                String.format(
                    "new Locale.Builder().setLanguage(%s).setRegion(%s).build()",
                    state.getSourceForNode(constructorArguments.get(0)),
                    state.getSourceForNode(constructorArguments.get(1)))));
      }
      return descriptionBuilder.build();
    }
    return Description.NO_MATCH;
  }
}
