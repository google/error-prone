/*
 * Copyright 2022 The Error Prone Authors.
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

import static com.google.common.collect.Iterables.getLast;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.fixes.SuggestedFixes.qualifyType;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getReceiver;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static java.util.stream.Collectors.joining;

import com.google.common.collect.Streams;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.method.MethodMatchers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import java.util.stream.Stream;

/** A {@link BugChecker}; see the summary. */
@BugPattern(
    summary = "Migrate off a deprecated overload of org.robolectric.shadow.api.Shadow#directlyOn",
    severity = WARNING)
public class RobolectricShadowDirectlyOn extends BugChecker implements MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> MATCHER =
      MethodMatchers.staticMethod()
          .onClass("org.robolectric.shadow.api.Shadow")
          .withSignature("<T>directlyOn(T,java.lang.Class<T>)");

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!MATCHER.matches(tree, state)) {
      return NO_MATCH;
    }
    TreePath path = state.getPath().getParentPath();
    if (!(path.getLeaf() instanceof MemberSelectTree)) {
      return NO_MATCH;
    }
    path = path.getParentPath();
    Tree parentTree = path.getLeaf();
    if (!(parentTree instanceof MethodInvocationTree)) {
      return NO_MATCH;
    }
    MethodInvocationTree parent = (MethodInvocationTree) parentTree;
    if (!tree.equals(getReceiver(parent))) {
      return NO_MATCH;
    }
    SuggestedFix.Builder fix = SuggestedFix.builder();
    MethodSymbol symbol = getSymbol(parent);
    String argReplacement =
        Streams.concat(
                Stream.of(state.getConstantExpression(symbol.getSimpleName().toString())),
                Streams.zip(
                    symbol.getParameters().stream(),
                    parent.getArguments().stream(),
                    (p, a) ->
                        String.format(
                            "ClassParameter.from(%s.class, %s)",
                            qualifyType(state, fix, state.getTypes().erasure(p.asType())),
                            state.getSourceForNode(a))))
            .collect(joining(", ", ", ", ""));
    fix.replace(state.getEndPosition(tree), state.getEndPosition(parent), "")
        .postfixWith(getLast(tree.getArguments()), argReplacement)
        .addImport("org.robolectric.util.ReflectionHelpers.ClassParameter");
    return describeMatch(tree, fix.build());
  }
}
