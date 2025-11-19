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
import static com.google.errorprone.matchers.Matchers.staticMethod;

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
    summary = "Possible unsafe operation related to the java.util.Locale class.",
    severity = WARNING)
public final class UnsafeLocaleUsage extends BugChecker
    implements MethodInvocationTreeMatcher, NewClassTreeMatcher {

  private static final Matcher<ExpressionTree> LOCALE_TO_STRING =
      instanceMethod().onExactClass("java.util.Locale").named("toString");
  private static final Matcher<ExpressionTree> LOCALE_OF =
      staticMethod().onClass("java.util.Locale").named("of");
  private static final Matcher<ExpressionTree> LOCALE_CONSTRUCTOR =
      constructor().forClass("java.util.Locale");

  /** Used for both Locale constructors and Locale.of static methods. */
  private static final String DESCRIPTION =
      " They do not check their arguments for"
          + " well-formedness. Prefer using Locale.forLanguageTag(String)"
          + " (which takes in an IETF BCP 47-formatted string) or a Locale.Builder"
          + " (which throws exceptions when the input is not well-formed).";

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (LOCALE_TO_STRING.matches(tree, state)) {
      return buildDescription(tree)
          .setMessage(
              "Avoid using Locale.toString() since it produces a value that"
                  + " misleadingly looks like a locale identifier. Prefer using"
                  + " Locale.toLanguageTag() since it produces an IETF BCP 47-formatted string"
                  + " that can be deserialized back into a Locale.")
          .addFix(SuggestedFixes.renameMethodInvocation(tree, "toLanguageTag", state))
          .build();
    }
    if (LOCALE_OF.matches(tree, state)) {
      Description.Builder descriptionBuilder =
          buildDescription(tree)
              .setMessage("Avoid using the Locale.of static methods." + DESCRIPTION);

      fixCallableWithArguments(
          descriptionBuilder, ImmutableList.copyOf(tree.getArguments()), tree, state);

      return descriptionBuilder.build();
    }
    return Description.NO_MATCH;
  }

  @Override
  public Description matchNewClass(NewClassTree tree, VisitorState state) {
    if (LOCALE_CONSTRUCTOR.matches(tree, state)) {
      Description.Builder descriptionBuilder =
          buildDescription(tree).setMessage("Avoid using the Locale constructors." + DESCRIPTION);

      fixCallableWithArguments(
          descriptionBuilder, ImmutableList.copyOf(tree.getArguments()), tree, state);

      return descriptionBuilder.build();
    }
    return Description.NO_MATCH;
  }

  /** Something that can be called with arguments, for example a method or constructor. */
  private static void fixCallableWithArguments(
      Description.Builder descriptionBuilder,
      ImmutableList<? extends ExpressionTree> arguments,
      ExpressionTree tree,
      VisitorState state) {

    // Only suggest a fix for constructor or Locale.of calls with one parameter since there's
    // too much variance in multi-parameter calls to be able to make a confident suggestion
    if (arguments.size() == 1) {
      // Locale.forLanguageTag() doesn't support underscores in language tags. We can replace this
      // ourselves when the constructor arg is a string literal. Otherwise, we can only append a
      // .replace() to it.
      ExpressionTree arg = arguments.get(0);
      String replacementArg =
          arg instanceof JCLiteral
              ? String.format( // Something like `new Locale("en_US")` or `Locale.of("en_US")`
                  "\"%s\"", ASTHelpers.constValue(arg, String.class).replace('_', '-'))
              : String.format("%s.replace('_', '-')", state.getSourceForNode(arguments.get(0)));
      descriptionBuilder.addFix(
          SuggestedFix.replace(tree, String.format("Locale.forLanguageTag(%s)", replacementArg)));
    }
  }
}
