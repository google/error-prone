/*
 * Copyright 2025 The Error Prone Authors.
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

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.constValue;
import static com.google.errorprone.util.ASTHelpers.getStartPosition;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;
import static java.util.Collections.nCopies;
import static java.util.stream.Collectors.joining;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.NewClassTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type.ArrayType;
import java.util.List;

/** A {@link BugChecker}; see the summary. */
@BugPattern(summary = "Avoid explicit array creation for varargs", severity = WARNING)
public final class ExplicitArrayForVarargs extends BugChecker
    implements MethodInvocationTreeMatcher, NewClassTreeMatcher {
  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    return handle(tree, getSymbol(tree), tree.getArguments(), state);
  }

  @Override
  public Description matchNewClass(NewClassTree tree, VisitorState state) {
    return handle(tree, getSymbol(tree), tree.getArguments(), state);
  }

  private Description handle(
      Tree tree, MethodSymbol symbol, List<? extends ExpressionTree> args, VisitorState state) {
    if (!symbol.isVarArgs()) {
      return NO_MATCH;
    }
    if (args.isEmpty()) {
      return NO_MATCH;
    }
    // The last argument isn't substituting for varargs if it isn't in place of the varargs
    // parameter.
    if (args.size() != symbol.getParameters().size()) {
      return NO_MATCH;
    }
    if (!(args.getLast() instanceof NewArrayTree newArrayTree)) {
      return NO_MATCH;
    }
    // Bail out if we're constructing a multidimensional array.
    if (((ArrayType) getType(newArrayTree)).getComponentType() instanceof ArrayType) {
      return NO_MATCH;
    }
    var initializers = newArrayTree.getInitializers();
    if (initializers == null) {
      var dimensions = newArrayTree.getDimensions();
      if (dimensions == null || dimensions.size() != 1) {
        return NO_MATCH;
      }
      if (!(constValue(getOnlyElement(dimensions)) instanceof Integer dimension)) {
        return NO_MATCH;
      }
      String replacement = nCopies(dimension, "null").stream().collect(joining(", "));
      return describeMatch(newArrayTree, SuggestedFix.replace(newArrayTree, replacement));
    }
    var fix =
        initializers.isEmpty()
            ? SuggestedFixes.removeElement(newArrayTree, args, state)
            : SuggestedFix.builder()
                .replace(
                    getStartPosition(newArrayTree), getStartPosition(initializers.getFirst()), "")
                .replace(
                    state.getEndPosition(
                        newArrayTree.getInitializers().get(initializers.size() - 1)),
                    state.getEndPosition(newArrayTree),
                    "")
                .build();
    return describeMatch(tree, fix);
  }
}
