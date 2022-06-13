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

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.InjectMatchers.hasProvidesAnnotation;
import static com.google.errorprone.util.ASTHelpers.hasAnnotation;
import static com.google.errorprone.util.ASTHelpers.hasDirectAnnotationWithSimpleName;
import static com.google.errorprone.util.ASTHelpers.shouldKeep;

import com.google.errorprone.BugPattern;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.annotations.DoNotCall;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.bugpatterns.inlineme.InlinabilityResult.InlineValidationErrorReason;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.MethodTree;
import javax.lang.model.element.Modifier;

/** Checker that recommends using {@code @InlineMe} on single-statement deprecated APIs. */
@BugPattern(
    name = "InlineMeSuggester",
    summary =
        "This deprecated API looks inlineable. If you'd like the body of the API to be inlined to"
            + " its callers, please annotate it with @InlineMe.",
    severity = WARNING)
public final class Suggester extends BugChecker implements MethodTreeMatcher {
  private static final String INLINE_ME = "InlineMe";

  private final String inlineMe;

  public Suggester(ErrorProneFlags errorProneFlags) {
    inlineMe =
        errorProneFlags
            .get("InlineMe:annotation")
            .orElse("com.google.errorprone.annotations.InlineMe");
  }

  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    // only suggest @InlineMe on @Deprecated APIs
    if (!hasAnnotation(tree, Deprecated.class, state)) {
      return Description.NO_MATCH;
    }

    // if the API is already annotated with @InlineMe, then return no match
    if (hasDirectAnnotationWithSimpleName(tree, INLINE_ME)) {
      return Description.NO_MATCH;
    }

    // if the API is already annotated with @DoNotCall, then return no match
    if (hasAnnotation(tree, DoNotCall.class, state)) {
      return Description.NO_MATCH;
    }

    // don't suggest on APIs that get called reflectively
    if (shouldKeep(tree) || hasProvidesAnnotation().matches(tree, state)) {
      return Description.NO_MATCH;
    }

    // if the body is not inlineable, then return no match
    InlinabilityResult inlinabilityResult = InlinabilityResult.forMethod(tree, state);
    if (!inlinabilityResult.isValidForSuggester()) {
      return Description.NO_MATCH;
    }

    // We attempt to actually build the annotation as a SuggestedFix.
    SuggestedFix.Builder fixBuilder =
        SuggestedFix.builder()
            .addImport(inlineMe)
            .prefixWith(
                tree,
                InlineMeData.buildExpectedInlineMeAnnotation(state, inlinabilityResult.body())
                    .buildAnnotation());
    if (inlinabilityResult.error()
        == InlineValidationErrorReason.METHOD_CAN_BE_OVERIDDEN_BUT_CAN_BE_FIXED) {
      SuggestedFixes.addModifiers(tree, state, Modifier.FINAL).ifPresent(fixBuilder::merge);
    }
    return describeMatch(tree, fixBuilder.build());
  }
}
