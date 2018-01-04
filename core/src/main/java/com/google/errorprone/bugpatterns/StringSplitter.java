/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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
import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.google.errorprone.util.Regexes.convertRegexToLiteral;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.tree.JCTree;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
  name = "StringSplitter",
  summary = "Prefer Splitter to String.split",
  severity = WARNING,
  providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION
)
public class StringSplitter extends BugChecker implements MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> MATCHER =
      instanceMethod().onExactClass("java.lang.String").withSignature("split(java.lang.String)");

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!MATCHER.matches(tree, state)) {
      return NO_MATCH;
    }
    String value = ASTHelpers.constValue(getOnlyElement(tree.getArguments()), String.class);
    boolean maybeRegex = false;
    if (value != null) {
      Optional<String> regexAsLiteral = convertRegexToLiteral(value);
      if (regexAsLiteral.isPresent()) {
        value = regexAsLiteral.get();
      } else {
        maybeRegex = true;
      }
    }
    Tree parent = state.getPath().getParentPath().getLeaf();
    if (parent instanceof EnhancedForLoopTree
        && ((EnhancedForLoopTree) parent).getExpression().equals(tree)) {
      // fix for `for (... : s.split(...)) {}` -> `for (... : Splitter.on(...).split(s)) {}`
      return describeMatch(
          tree,
          replaceWithSplitter(SuggestedFix.builder(), tree, state, "split", maybeRegex).build());
    }
    // If the result of split is assigned to a variable, try to fix all uses of the variable in the
    // enclosing method. If we don't know how to fix any of them, bail out.
    if (!(parent instanceof VariableTree)) {
      return NO_MATCH;
    }
    VariableTree varTree = (VariableTree) parent;
    if (!varTree.getInitializer().equals(tree)) {
      return NO_MATCH;
    }
    VarSymbol sym = ASTHelpers.getSymbol(varTree);
    TreePath enclosing = findEnclosing(state);
    if (enclosing == null) {
      return NO_MATCH;
    }
    // find all uses of the variable in the enclosing method
    List<TreePath> uses = new ArrayList<>();
    new TreePathScanner<Void, Void>() {
      @Override
      public Void visitIdentifier(IdentifierTree tree, Void unused) {
        if (Objects.equals(sym, ASTHelpers.getSymbol(tree))) {
          uses.add(getCurrentPath());
        }
        return super.visitIdentifier(tree, null);
      }
    }.scan(enclosing, null);
    SuggestedFix.Builder fix = SuggestedFix.builder();
    // a mutable boolean to track whether we want split or splitToList
    boolean[] needsList = {false};
    // try to fix all uses of the variable
    for (TreePath path : uses) {
      class UseFixer extends SimpleTreeVisitor<Boolean, Void> {
        @Override
        public Boolean visitEnhancedForLoop(EnhancedForLoopTree tree, Void unused) {
          // The syntax for looping over an array or iterable variable is the same, so there's no
          // fix here.
          return sym.equals(ASTHelpers.getSymbol(tree.getExpression()));
        }

        @Override
        public Boolean visitArrayAccess(ArrayAccessTree tree, Void unused) {
          // replace `pieces[N]` with `pieces.get(N)`
          ExpressionTree expression = tree.getExpression();
          ExpressionTree index = tree.getIndex();
          if (!sym.equals(ASTHelpers.getSymbol(expression))) {
            return false;
          }
          fix.replace(
                  /* startPos= */ state.getEndPosition(expression),
                  /* endPos= */ ((JCTree) index).getStartPosition(),
                  ".get(")
              .replace(state.getEndPosition(index), state.getEndPosition(tree), ")");
          // we want a list for indexing
          needsList[0] = true;
          return true;
        }

        @Override
        public Boolean visitMemberSelect(MemberSelectTree tree, Void aVoid) {
          // replace `pieces.length` with `pieces.size`
          if (sym.equals(ASTHelpers.getSymbol(tree.getExpression()))
              && tree.getIdentifier().contentEquals("length")) {
            fix.replace(
                state.getEndPosition(tree.getExpression()), state.getEndPosition(tree), ".size()");
            needsList[0] = true;
            return true;
          }
          return false;
        }
      };
      if (!firstNonNull(path.getParentPath().getLeaf().accept(new UseFixer(), null), false)) {
        return NO_MATCH;
      }
    }
    if (needsList[0]) {
      fix.replace((varTree).getType(), "List<String>").addImport("java.util.List");
      replaceWithSplitter(fix, tree, state, "splitToList", maybeRegex);
    } else {
      fix.replace((varTree).getType(), "Iterable<String>");
      replaceWithSplitter(fix, tree, state, "split", maybeRegex);
    }
    return describeMatch(tree, fix.build());
  }

  private SuggestedFix.Builder replaceWithSplitter(
      SuggestedFix.Builder fix,
      MethodInvocationTree tree,
      VisitorState state,
      String splitMethod,
      boolean maybeRegex) {
    ExpressionTree receiver = ASTHelpers.getReceiver(tree);
    return fix.addImport("com.google.common.base.Splitter")
        .prefixWith(
            receiver,
            "Splitter."
                + (maybeRegex ? "onPattern" : "on")
                + "("
                + state.getSourceForNode(getOnlyElement(tree.getArguments()))
                + ")."
                + splitMethod
                + "(")
        .replace(state.getEndPosition(receiver), state.getEndPosition(tree), ")");
  }

  private TreePath findEnclosing(VisitorState state) {
    for (TreePath path = state.getPath(); path != null; path = path.getParentPath()) {
      switch (path.getLeaf().getKind()) {
        case METHOD:
        case LAMBDA_EXPRESSION:
          return path;
        case CLASS:
          return null;
        default: // fall out
      }
    }
    return null;
  }
}
