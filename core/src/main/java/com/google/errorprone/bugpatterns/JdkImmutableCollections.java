/*
 * Copyright 2024 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;
import static com.google.errorprone.util.ASTHelpers.getReceiverType;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import java.util.Optional;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    summary =
        "Prefer Google's immutable collections to the convenience methods in collection interfaces",
    severity = ERROR)
public final class JdkImmutableCollections extends BugChecker
    implements MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> MATCHER =
      staticMethod()
          .onClassAny("java.util.List", "java.util.Set", "java.util.Map")
          .namedAnyOf("of", "copyOf", "ofEntries");

  private static final Matcher<ExpressionTree> OF_VARARGS =
      staticMethod().anyClass().withSignature("<E>of(E...)");

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!MATCHER.matches(tree, state)) {
      return NO_MATCH;
    }
    String typeName = getReceiverType(tree).asElement().getSimpleName().toString();
    Description.Builder description = buildDescription(tree);
    buildFix(tree, typeName, state).ifPresent(description::addFix);
    return description
        .setMessage(
            String.format(
                "Prefer Immutable%s to the convenience methods in %s", typeName, typeName))
        .build();
  }

  private static Optional<Fix> buildFix(
      MethodInvocationTree tree, String typeName, VisitorState state) {
    SuggestedFix.Builder fix = SuggestedFix.builder();
    if (tree.getArguments().size() == 1 && OF_VARARGS.matches(tree, state)) {
      // ImmutableList.of(x) is always a single-element list containing `x`;
      // List.of(x) is a single-element list containing `x` *unless x is an Object[]*,
      // in which case the varargs overload of List.of is selected and it returns a
      // list containing the elements of `x`. We rewrite to ImmutableList.copyOf
      // in that case to be semantics preserving.
      // (see corresponding Guava changes in cl/14626886, cl//24840447)
      fix.merge(SuggestedFixes.renameMethodInvocation(tree, "copyOf", state));
    }
    fix.replace(
        ASTHelpers.getReceiver(tree),
        SuggestedFixes.qualifyType(state, fix, "com.google.common.collect.Immutable" + typeName));
    return Optional.of(fix.build());
  }
}
