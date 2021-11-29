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

package com.google.errorprone.bugpatterns.inlineme;

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.util.ASTHelpers.getSymbol;

import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.annotations.InlineMeValidationDisabled;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.google.errorprone.util.ErrorProneToken;
import com.google.errorprone.util.ErrorProneTokens;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.MethodTree;
import com.sun.tools.javac.util.Context;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

/** Checker that ensures the {@code @InlineMe} annotation is used correctly. */
@BugPattern(
    name = "InlineMeValidator",
    summary = "Ensures that the @InlineMe annotation is used correctly.",
    suppressionAnnotations = InlineMeValidationDisabled.class,
    documentSuppression = false,
    severity = ERROR)
public final class Validator extends BugChecker implements MethodTreeMatcher {

  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    return InlineMeData.createFromSymbol(getSymbol(tree))
        .map(data -> match(data, tree, state))
        .orElse(Description.NO_MATCH);
  }

  private Description match(InlineMeData existingAnnotation, MethodTree tree, VisitorState state) {
    InlinabilityResult result = InlinabilityResult.forMethod(tree, state);
    if (!result.isValidForValidator()) {
      return buildDescription(tree)
          .setMessage(result.errorMessage())
          // This method is un-inlineable, so let's remove the annotation (since we can't fix it)
          .addFix(SuggestedFix.delete(getInlineMeAnnotationTree(tree)))
          .build();
    }

    InlineMeData inferredFromMethodBody =
        InlineMeData.buildExpectedInlineMeAnnotation(state, result.body());
    Set<MismatchedInlineMeComponents> mismatches =
        compatibleWithAnnotation(inferredFromMethodBody, existingAnnotation, state.context);
    if (mismatches.isEmpty()) {
      return Description.NO_MATCH;
    }

    // There's some mismatch, render an error.
    return buildDescription(tree)
        .setMessage(renderInlineMeMismatch(inferredFromMethodBody, existingAnnotation, mismatches))
        .addFix(
            SuggestedFix.replace(
                getInlineMeAnnotationTree(tree), inferredFromMethodBody.buildAnnotation()))
        .build();
  }

  private static AnnotationTree getInlineMeAnnotationTree(MethodTree tree) {
    return ASTHelpers.getAnnotationWithSimpleName(tree.getModifiers().getAnnotations(), "InlineMe");
  }

  private static String renderInlineMeMismatch(
      InlineMeData inferredFromMethodBody,
      InlineMeData existingAnnotation,
      Set<MismatchedInlineMeComponents> mismatches) {
    StringBuilder message =
        new StringBuilder(
            "There is a mismatch between the implementation of the method and the replacement"
                + " suggested in the annotation.");

    if (mismatches.contains(MismatchedInlineMeComponents.REPLACEMENT_STRING)) {
      message.append(
          String.format(
              "\nReplacement text: \n  InferredFromBody: %s\n  FromAnnotation: %s",
              inferredFromMethodBody.replacement(), existingAnnotation.replacement()));
    }
    if (mismatches.contains(MismatchedInlineMeComponents.IMPORTS)) {
      message.append(
          String.format(
              "\nImports: \n  InferredFromBody: %s\n  FromAnnotation: %s",
              inferredFromMethodBody.imports(), existingAnnotation.imports()));
    }
    if (mismatches.contains(MismatchedInlineMeComponents.STATIC_IMPORTS)) {
      message.append(
          String.format(
              "\nStatic imports: \n  InferredFromBody: %s\n  FromAnnotation: %s",
              inferredFromMethodBody.staticImports(), existingAnnotation.staticImports()));
    }
    return message.toString();
  }

  private enum MismatchedInlineMeComponents {
    REPLACEMENT_STRING,
    IMPORTS,
    STATIC_IMPORTS
  }

  private static Set<MismatchedInlineMeComponents> compatibleWithAnnotation(
      InlineMeData inferredFromMethodBody, InlineMeData anno, Context context) {
    EnumSet<MismatchedInlineMeComponents> mismatches =
        EnumSet.noneOf(MismatchedInlineMeComponents.class);

    // Developers can customize the @InlineMe implementation a bit, so we have some leniency in
    // determining if an annotation properly represents the implementation of a method.
    if (!parseAndCheckForTokenEquivalence(
        anno.replacement(), inferredFromMethodBody.replacement(), context)) {
      mismatches.add(MismatchedInlineMeComponents.REPLACEMENT_STRING);
    }
    if (!inferredFromMethodBody.imports().equals(anno.imports())) {
      mismatches.add(MismatchedInlineMeComponents.IMPORTS);
    }
    if (!inferredFromMethodBody.staticImports().equals(anno.staticImports())) {
      mismatches.add(MismatchedInlineMeComponents.STATIC_IMPORTS);
    }
    return mismatches;
  }

  private static final CharMatcher SEMICOLON = CharMatcher.is(';');

  /** Determines if the first and second token strings are equivalent. */
  private static boolean parseAndCheckForTokenEquivalence(
      String first, String second, Context context) {
    ImmutableList<ErrorProneToken> tokens1 =
        ErrorProneTokens.getTokens(SEMICOLON.trimTrailingFrom(first), context);
    ImmutableList<ErrorProneToken> tokens2 =
        ErrorProneTokens.getTokens(SEMICOLON.trimTrailingFrom(second), context);

    if (tokens1.size() != tokens2.size()) {
      return false;
    }

    for (int i = 0; i < tokens1.size(); i++) {
      ErrorProneToken token1 = tokens1.get(i);
      ErrorProneToken token2 = tokens2.get(i);

      if (!token1.kind().equals(token2.kind())) {
        return false;
      }

      // note we specifically avoid checking ErrorProneToken::comments
      if (mismatch(token1, token2, ErrorProneToken::hasName, ErrorProneToken::name)
          || mismatch(token1, token2, ErrorProneToken::hasStringVal, ErrorProneToken::stringVal)
          || mismatch(token1, token2, ErrorProneToken::hasRadix, ErrorProneToken::radix)) {
        return false;
      }
    }

    return true;
  }

  private static <T> boolean mismatch(
      ErrorProneToken first,
      ErrorProneToken second,
      Predicate<ErrorProneToken> guard,
      Function<ErrorProneToken, T> extractor) {
    return guard.test(first) && !extractor.apply(first).equals(extractor.apply(second));
  }
}
