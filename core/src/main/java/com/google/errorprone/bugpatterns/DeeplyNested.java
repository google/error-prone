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

import static com.google.common.base.CaseFormat.UPPER_CAMEL;
import static com.google.common.base.CaseFormat.UPPER_UNDERSCORE;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.fixes.SuggestedFixes.prettyType;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getResultType;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.isSameType;
import static com.google.errorprone.util.ASTHelpers.isStatic;

import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import com.google.errorprone.BugPattern;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.CompilationUnitTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.google.errorprone.util.ErrorProneToken;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.parser.Tokens;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(
    summary = "Very deeply nested code may lead to StackOverflowErrors during compilation",
    severity = WARNING)
public class DeeplyNested extends BugChecker implements CompilationUnitTreeMatcher {

  private final int maxDepth;

  @Inject
  DeeplyNested(ErrorProneFlags flags) {
    maxDepth = flags.getInteger("DeeplyNested:MaxDepth").orElse(1000);
  }

  @Override
  public Description matchCompilationUnit(CompilationUnitTree tree, VisitorState state) {
    TreePath result =
        new SuppressibleTreePathScanner<TreePath, Integer>(state) {

          @Override
          public TreePath scan(Tree tree, Integer depth) {
            if (depth > maxDepth) {
              return getCurrentPath();
            }
            return super.scan(tree, depth + 1);
          }

          @Override
          public TreePath reduce(TreePath r1, TreePath r2) {
            return r1 != null ? r1 : r2;
          }
        }.scan(state.getPath(), 0);
    if (result != null) {
      return describeMatch(result.getLeaf(), buildFix(result, state));
    }
    return NO_MATCH;
  }

  private static Fix buildFix(TreePath path, VisitorState state) {
    PeekingIterator<Tree> it = Iterators.peekingIterator(path.iterator());

    // Find enclosing chained calls that return a Builder type
    while (it.hasNext() && !builderResult(it.peek())) {
      it.next();
    }
    if (!it.hasNext()) {
      return SuggestedFix.emptyFix();
    }
    ExpressionTree receiver = null;
    while (it.hasNext() && builderResult(it.peek())) {
      receiver = (ExpressionTree) it.peek();
      it.next();
    }

    // Look for an enclosing terminal .build() call
    it.next(); // skip past the enclosing MemberSelectTree
    if (!terminalBuilder(it.peek())) {
      return SuggestedFix.emptyFix();
    }

    // Look for the enclosing tree, e.g. a field that is being initialized, or a return
    it.next();
    Tree enclosing = it.next();

    // Descend back into the tree to final all chained calls on the same builder
    Type builderType = getResultType(receiver);
    List<ExpressionTree> chain = new ArrayList<>();
    while (receiver != null && isSameType(getResultType(receiver), builderType, state)) {
      chain.add(receiver);
      if (receiver instanceof NewClassTree) {
        break;
      }
      receiver = ASTHelpers.getReceiver(receiver);
    }
    Collections.reverse(chain);

    // Emit a fix
    SuggestedFix.Builder fix = SuggestedFix.builder();
    StringBuilder replacement = new StringBuilder();
    // Create a variable to hold the builder
    replacement.append(
        String.format(
            "%s builder = %s;",
            prettyType(state, fix, builderType), state.getSourceForNode(chain.get(0))));
    // Update all subsequence chained calls to use the variable as their receiver
    for (int i = 1; i < chain.size(); i++) {
      int start = state.getEndPosition(chain.get(i - 1));
      int end = state.getEndPosition(chain.get(i));
      List<ErrorProneToken> tokens = state.getOffsetTokens(start, end);
      int dot =
          tokens.stream().filter(t -> t.kind() == Tokens.TokenKind.DOT).findFirst().get().pos();
      replacement.append(
          String.format(
              "%sbuilder%s;",
              state.getSourceCode().subSequence(start, dot),
              state.getSourceCode().subSequence(dot, end)));
    }

    if (enclosing instanceof ReturnTree) {
      // update `return <builder>.build();` to use a variable in the same scope
      replacement.append("return builder.build();");
      fix.replace(enclosing, replacement.toString());
      return fix.build();
    } else if (enclosing instanceof VariableTree variableTree && isStatic(getSymbol(enclosing))) {
      // update `static FOO = <builder>` to declare a helper method named create<builder>
      String factory =
          String.format(
              "create%s",
              UPPER_UNDERSCORE.converterTo(UPPER_CAMEL).convert(variableTree.getName().toString()));
      fix.replace(variableTree.getInitializer(), String.format("%s()", factory));
      fix.postfixWith(
          variableTree,
          String.format(
              "private static %s %s() { %s return builder.build(); }",
              prettyType(state, fix, getType(variableTree)), factory, replacement));
      return fix.build();
    } else {
      // TODO: support other patterns
      return SuggestedFix.emptyFix();
    }
  }

  private static boolean builderResult(Tree leaf) {
    if (!(leaf instanceof ExpressionTree expressionTree)) {
      return false;
    }
    Type resultType = getResultType(expressionTree);
    if (resultType == null) {
      return false;
    }
    return resultType.asElement().getSimpleName().contentEquals("Builder");
  }

  private static boolean terminalBuilder(Tree leaf) {
    if (!(leaf instanceof MethodInvocationTree methodInvocationTree)) {
      return false;
    }
    ExpressionTree select = methodInvocationTree.getMethodSelect();
    return select instanceof MemberSelectTree memberSelectTree
        && memberSelectTree.getIdentifier().toString().startsWith("build");
  }
}
